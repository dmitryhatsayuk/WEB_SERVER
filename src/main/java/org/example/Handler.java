package org.example;

import java.io.BufferedOutputStream;

public interface Handler {
    default void handle(Request request, BufferedOutputStream outputStream) {
    }
}
