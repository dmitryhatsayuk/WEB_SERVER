package org.example;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Server {
    private final List<String> validPaths = List.of("/index.html", "/spring.svg", "/spring.png", "/resources.html", "/styles.css", "/app.js", "/links.html", "/forms.html", "/classic.html", "/events.html", "/events.js");
    private final ExecutorService threadPool = Executors.newFixedThreadPool(64);
    private final ConcurrentHashMap<String, Handler> handlerMap = new ConcurrentHashMap<>();
    Logger logger = new Logger();

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

        } catch (
                IOException e) {
            e.printStackTrace();
        }
    }

    private void processing(Socket socket) throws IOException {

        try (
                final var in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                final var out = new BufferedOutputStream(socket.getOutputStream())
        ) {
            while (true) {
                // проверили на адекватнось ревестлайн
                String requestLine = in.readLine();
                final var parts = requestLine.split(" ");
                logger.log("Line readed");
                if (parts.length != 3) {
                    logger.log("Wrong requestline!!!");
                    //я решил тут не закрывать сокет, а просто ждать верного запроса
                    continue;
                }

//Создаем Request
                String path = parts[1];
                String meth = parts[0];
                String headers = allLineReader(in);
                String body = allLineReader(in);
                Request request = new Request(meth, path, headers, body);
                logger.log("Request received!");
//оставляем старую реализацию с папками
                if (!validPaths.contains(path)) {
                    out.write((
                            """
                                    HTTP/1.1 404 Not Found\r
                                    Content-Length: 0\r
                                    Connection: close\r
                                    \r
                                    """
                    ).getBytes());
                    out.flush();
                    logger.log("404");
                    continue;
                }

                final var filePath = Path.of(".", "public", path);
                final var mimeType = Files.probeContentType(filePath);

                // special case for classic
                if (path.equals("/classic.html")) {
                    final var template = Files.readString(filePath);
                    final var content = template.replace(
                            "{time}",
                            LocalDateTime.now().toString()
                    ).getBytes();
                    out.write((
                            "HTTP/1.1 200 OK\r\n" +
                                    "Content-Type: " + mimeType + "\r\n" +
                                    "Content-Length: " + content.length + "\r\n" +
                                    "Connection: keep-alive\r\n" +
                                    "\r\n"
                    ).getBytes());
                    out.write(content);
                    out.flush();
                    logger.log("classic received");
                    continue;
                }

                if (validPaths.contains(path)) {
                    logger.log("Path valid");
                    final var length = Files.size(filePath);
                    out.write((
                            "HTTP/1.1 200 OK\r\n" +
                                    "Content-Type: " + mimeType + "\r\n" +
                                    "Content-Length: " + length + "\r\n" +
                                    "Connection: keep-alive\r\n" +
                                    "\r\n"
                    ).getBytes());
                    Files.copy(filePath, out);
                    out.flush();
                    logger.log(path + " OK");
                }
//добавляем новую реализацию с хэндлером
                else if (handlerMap.containsKey(request.meth + request.path)) {
                    logger.log("Handler found!");
                    handlerMap.get(request.meth + request.body).handle(request, out);
                }
            }
        }
        //конец цикла
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

    }

    /**
     * <p>Метод читает сразу несколько строк из буфера и передает одной строкой с переносами</p>
     * Метод будет читать строку из НЕ пустого буфера и до тех пор пока не наткнется на пустую строку-разделитель
     *
     * @param in не прочтенный, не пустой буфер
     */

    public String allLineReader(BufferedReader in) throws IOException {
        StringBuilder builder = new StringBuilder();
        String text;
        String newline = System.lineSeparator();
        String result = null;
        if (in.ready()) {
            while (!(text = in.readLine()).isEmpty()) {
                builder.append(text);
                builder.append(newline);
                result = builder.toString();
            }
        }
        return result;
    }
}