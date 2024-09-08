package org.example;

import java.io.BufferedOutputStream;
import java.io.IOException;

/**
 * Интерфейс для унификации реакции сервера на заранее определенные запросы
 */
public interface Handler {
    void handle(Request request, BufferedOutputStream out) throws IOException;
}
