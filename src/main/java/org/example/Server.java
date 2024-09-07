package org.example;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Класс-сервер для обработки http запросов и ответа на них
 */
public class Server {
    public static final String GET = "GET";
    public static final String POST = "POST";
    static Logger logger = new Logger();
    final List<String> allowedMethods = List.of(GET, POST);
    private final ExecutorService threadPool = Executors.newFixedThreadPool(64);
    private final ConcurrentHashMap<String, Handler> handlerMap = new ConcurrentHashMap<>();

    /**
     * <p>Метод вычитывающий конкретный заголовок запроса</p>
     * Метод принимает List с заголовками и искомый заголовок и возвращает его значение
     *
     * @param headers List с заголовками запроса
     * @param header  Искомый заголовок
     * @return Optional <String>
     */
    private static Optional<String> extractHeader(List<String> headers, String header) {
        return headers.stream().filter(o -> o.startsWith(header)).map(o -> o.substring(o.indexOf(" "))).map(String::trim).findFirst();
    }

    /**
     * <p>Метод направляющий Bad Request на не корректные запросы</p>
     *
     * @param out Исходящий поток для отправки Response
     */
    private static void badRequest(BufferedOutputStream out) throws IOException {
        out.write(("HTTP/1.1 400 Bad Request\r\n" + "Content-Length: 0\r\n" + "Connection: keep-alive\r\n" + "\r\n").getBytes());
        out.flush();
        logger.log("Bad request sent");
    }

    /**
     * <p>Метод поиска вхождения массива байт в другой массив</p>
     * Метод взят из Google Guava с модификациями. Производится поиск искомого массива
     * на определённом участке байт исходного массива
     *
     * @param array  исходный массив байт
     * @param target искомый массив байт
     * @param start  указание начала места поиска в исходном массиве
     * @param max    указание конца места поиска в исходном массиве
     * @return int
     */
    private static int indexOf(byte[] array, byte[] target, int start, int max) {
        outer:
        for (int i = start; i < max - target.length + 1; i++) {
            for (int j = 0; j < target.length; j++) {
                if (array[i + j] != target[j]) {
                    continue outer;
                }
            }
            return i;
        }
        return -1;
    }

    /**
     * <p>Метод для запуска экземпляра Server</p>
     * Метод создает серверный сокет,при подключении к которому создается клиентский сокет,
     * работа с входящими/исходящими данными на котором происходит в отдельном потоке.
     *
     * @param port порт, который слушает сервер
     */
    public void listen(int port) {
        int th = 0;
        try (final var serverSocket = new ServerSocket(port)) {
            while (true) {
                final var socket = serverSocket.accept();
                logger.log("New connection established");
                Runnable logic = () -> {
                    try {
                        processing(socket);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                };
                threadPool.submit(logic);
                logger.log(" Thread " + th + " added ");
                th++;
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * <p>Метод обработки запросов на сервер</p>
     * Логика обработки запросов поступающих на сервер и ответа на них.
     * Сначала парсится requestLine из которого создается объект Request,
     * который также наполняется остальными данными из запроса включая заголовки и тело при их наличии
     * и идет поиск подходящего обработчика-Handler по полученным параметрам.
     * Также поддерживается изначальная логика с поиском по известным папкам сервера для простых запросов
     *
     * @param socket клиентский порт для приема и отправки данных
     */
    private void processing(Socket socket) throws IOException {

        try (final var in = new BufferedInputStream(socket.getInputStream()); final var out = new BufferedOutputStream(socket.getOutputStream())) {
            while (true) {
                // лимит на request line + заголовки
                final var limit = 4096;

                in.mark(limit);
                final var buffer = new byte[limit];
                final var read = in.read(buffer);

                // ищем request line
                final var requestLineDelimiter = new byte[]{'\r', '\n'};
                final var requestLineEnd = indexOf(buffer, requestLineDelimiter, 0, read);
                if (requestLineEnd == -1) {
                    badRequest(out);
                    continue;
                }

                // читаем request line
                final var requestLine = new String(Arrays.copyOf(buffer, requestLineEnd)).split(" ");
                if (requestLine.length != 3) {
                    badRequest(out);
                    continue;
                }

                final var method = requestLine[0];
                if (!allowedMethods.contains(method)) {
                    badRequest(out);
                    continue;
                }
                logger.log("METHOD: " + method);
                //вычитываем path и queryString, уродливо, но надежно
                final var path = requestLine[1].split("\\?")[0];
                final String queryString = "?" + (requestLine[1].split("\\?")[1]);
                logger.log("Q STRING: " + queryString);
                if (!path.startsWith("/")) {
                    badRequest(out);
                    continue;
                }
                logger.log("PATH :" + path);

                // ищем заголовки
                final var headersDelimiter = new byte[]{'\r', '\n', '\r', '\n'};
                final var headersStart = requestLineEnd + requestLineDelimiter.length;
                final var headersEnd = indexOf(buffer, headersDelimiter, headersStart, read);
                if (headersEnd == -1) {
                    badRequest(out);
                    continue;
                }

                // отматываем на начало буфера
                in.reset();
                // пропускаем requestLine
                in.skip(headersStart);

                final var headersBytes = in.readNBytes(headersEnd - headersStart);
                final var headers = Arrays.asList(new String(headersBytes).split("\r\n"));
                logger.log("HEADERS: " + headers);
                String body = null;
                if (!method.equals(GET)) {
                    in.skip(headersDelimiter.length);
                    // вычитываем Content-Length, чтобы прочитать body
                    final var contentLength = extractHeader(headers, "Content-Length");
                    if (contentLength.isPresent()) {
                        final var length = Integer.parseInt(contentLength.get());
                        final var bodyBytes = in.readNBytes(length);

                        body = new String(bodyBytes);
                    }
                }
                logger.log("BODY: " + body);

                Request request = new Request(method, path, headers.toString(), body, queryString);
                //Здесь я проверял через Postman, что параметры из queryString и Body читаются верно
                //System.out.println("VALUE OF TEST PARAM=" + request.getQueryParam("TEST"));
                //System.out.println(">>>>>>>>>"+ request.getPostParam("TEST"));
                //для начала ищем в хэндлерах подходящий обработчик, если нашли-хэндлим и идем на новый круг,
                // если нет-то просто 200-ОК
                if (handlerMap.containsKey(request.meth + request.path)) {
                    logger.log("Handler found!");
                    handlerMap.get(request.meth + request.path).handle(request, out);
                    continue;
                }
                out.write(("HTTP/1.1 200 OK\r\n" + "Content-Length: 0\r\n" + "Connection: keep-alive\r\n" + "\r\n").getBytes());
                out.flush();
            }

            //конец цикла
        }

    }

    /**
     * <p>Метод добавляющий хэндлер</p>
     * Я решил что любая пара метод+путь будут иметь уникальный метод,
     * поэтому почему бы не использовать сразу пару в качестве ключа
     *
     * @param met     метод http запроса
     * @param path    путь из http запроса
     * @param handler уникальный хэндлер для пары met+path
     */
    public void addHandler(String met, String path, Handler handler) {

        handlerMap.put(met + path, handler);
        logger.log("Handler successfully added");
    }

}

