package org.example;

import java.io.BufferedOutputStream;

public class Main {
    public static void main(String[] args) {
        Logger logger = new Logger();
        final var server = new Server();
//         код инициализации сервера (из вашего предыдущего ДЗ)
//
        // добавление хендлеров (обработчиков)
        server.addHandler("GET", "/messages", new Handler() {
            public void handle(Request request, BufferedOutputStream responseStream) {
                // TODO: handlers code
                //Здесь я не понял и в звдвче не сказано, нужно ли написать сам хэндлер,
                // поскольку в условиях нет-решил оставить
            }
        });
        server.addHandler("POST", "/messages", new Handler() {
            public void handle(Request request, BufferedOutputStream responseStream) {
                // TODO: handlers code
            }
        });
        server.listen(779);
    }
}