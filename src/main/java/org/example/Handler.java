package org.example;


import java.io.BufferedOutputStream;
@FunctionalInterface
public interface Handler {

    void handle(Request request, BufferedOutputStream bufferedOutputStream);

}