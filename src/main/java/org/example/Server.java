package org.example;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Server {
    public static void main(String[] args) {
        new Server();
    }

    final List<String> validPaths = List.of("/index.html", "/spring.svg", "/spring.png", "/resources.html", "/styles.css", "/app.js", "/links.html", "/forms.html", "/classic.html", "/events.html", "/events.js");


    private Server() {
        final int COUNT = 64;
        final int PORT = 9999;
        ExecutorService executorService = Executors.newFixedThreadPool(COUNT);
        try (var serverSocket = new ServerSocket(PORT)) {
            while (true) {
                var socket = serverSocket.accept();
                var thread = new Thread(() -> {
                    try {
                        BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                        BufferedOutputStream out = new BufferedOutputStream(socket.getOutputStream());
                        logicResponse(in, out);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                });
                executorService.submit(thread);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void logicResponse(BufferedReader in, BufferedOutputStream out) throws IOException {
        final var requestLine = in.readLine();
        final var parts = requestLine.split(" ");
        final var path = parts[1];
        final var filePath = Path.of(".", "public", path);
        final var mimeType = Files.probeContentType(filePath);

        checkBadRequest(out, parts);

        if (checkNotFound(out, path)) {

            // special case for classic
            refreshWeb(out, path, filePath, mimeType);

            sendResponse(out, filePath, mimeType);
        }
    }

    private static void sendResponse(BufferedOutputStream out, Path filePath, String mimeType) throws IOException {
        final var length = Files.size(filePath);
        out.write((
                "HTTP/1.1 200 OK\r\n" +
                        "Content-Type: " + mimeType + "\r\n" +
                        "Content-Length: " + length + "\r\n" +
                        "Connection: close\r\n" +
                        "\r\n"
        ).getBytes());
        Files.copy(filePath, out);
        out.flush();
    }

    private static void refreshWeb(BufferedOutputStream out, String path, Path filePath, String mimeType) throws IOException {
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
                            "Connection: close\r\n" +
                            "\r\n"
            ).getBytes());
            out.write(content);
            out.flush();
//                            continue;
        }
    }

    private boolean checkNotFound(BufferedOutputStream out, String path) throws IOException {
        if (!validPaths.contains(path)) {
            out.write((
                    "HTTP/1.1 404 Not Found\r\n" +
                            "Content-Length: 0\r\n" +
                            "Connection: close\r\n" +
                            "\r\n"
            ).getBytes());
            out.flush();
//                            continue;
            return false;
        }
        return true;
    }

    private static void checkBadRequest(BufferedOutputStream out, String[] parts) throws IOException {
        if (parts.length != 3) {
            out.write((
                    "HTTP/1.1 400 Bad Request\r\n" +
                            "Content-Length: 0\r\n" +
                            "Connection: close\r\n" +
                            "\r\n"
            ).getBytes());
            out.flush();
            // just close socket
//                            continue;
        }
    }


}
