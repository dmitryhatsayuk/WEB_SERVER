package org.example;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class Main {
    public static void main(String[] args) {
        Logger logger = new Logger();
        final var server = new Server();
//         код инициализации сервера (из вашего предыдущего ДЗ)
//
        // добавление хендлеров (обработчиков)
        server.addHandler("GET", "/messages", new Handler() {
            public void handle(Request request, BufferedOutputStream responseStream) throws IOException {
                final var filePath = Path.of(".", "public", request.path);
                final var mimeType = Files.probeContentType(filePath);
                final var length = Files.size(filePath);
                responseStream.write(("HTTP/1.1 200 OK\r\n" + "Content-Type: " + mimeType + "\r\n" + "Content-Length: " + length + "\r\n" + "Connection: keep-alive\r\n" + "\r\n").getBytes());
                Files.copy(filePath, responseStream);
                responseStream.flush();
                logger.log(request.path + " OK");
            }
        });
        server.addHandler("POST", "/messages", new Handler() {
            public void handle(Request request, BufferedOutputStream responseStream) throws IOException {
                final var filePath = Path.of(".", "public", request.path);
                final var mimeType = Files.probeContentType(filePath);
                final var length = Files.size(filePath);
                //насколько я понял без Query String метод POST должен просто вернуть страницу
                responseStream.write(("HTTP/1.1 200 OK\r\n" + "Content-Type: " + mimeType + "\r\n" + "Content-Length: " + length + "\r\n" + "Connection: keep-alive\r\n" + "\r\n" + "<h1> Hello </h1>").getBytes());
                Files.copy(filePath, responseStream);
                responseStream.flush();
                logger.log(request.path + " OK");
            }
        });
        server.listen(519);
    }
}