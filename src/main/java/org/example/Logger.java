package org.example;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class Logger {

    public void log(String msg) {

        System.out.println(">>>>" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd hh-mm-ss")) + " " + msg);
    }
}
