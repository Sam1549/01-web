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
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Server {
    private final static Map<String, Map<String, Handler>> handlers = new ConcurrentHashMap<>();

    public static void main(String[] args) {
        for (String validPath : validPaths) {
            addHandler("GET", validPath, (request, responseStream) -> {
                try {
                    sendResponse(request, responseStream);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });
        }

        addHandler("POST", "/resources.html", (request, responseStream) -> {
            try {
                sendResponse(request, responseStream);
            } catch (IOException e) {
                e.printStackTrace();
            }
        });

        addHandler("GET", "/index2.html", (request, responseStream) -> {
            try {
                sendResponse(request, responseStream);
            } catch (IOException e) {
                e.printStackTrace();
            }
        });

        addHandler("GET", "/vinni.png", (request, responseStream) -> {
            try {
                sendResponse(request, responseStream);
            } catch (IOException e) {
                e.printStackTrace();
            }
        });

        new Server();
    }

    final static List<String> validPaths = List.of("/index.html", "/spring.svg", "/spring.png", "/resources.html", "/styles.css", "/app.js", "/links.html", "/forms.html", "/classic.html", "/events.html", "/events.js");


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
        Request request = getRequest(in, out);
        Handler handler = handlers.get(request.getMethod()).get(request.getPath());
        handler.handle(request, out);
    }

    private static Request getRequest(BufferedReader in, BufferedOutputStream out) throws IOException {
        final var requestLine = in.readLine();
        final var parts = requestLine.split(" ");
        checkBadRequest(out, parts);
        Request request = new Request(parts[0], parts[1], in);
        checkNotFound(out, request);
        return request;
    }

    private static void sendResponse(Request request, BufferedOutputStream out) throws IOException {
        final Path filePath = Path.of(".", "public", request.getPath());
        final String mimeType = Files.probeContentType(filePath);

        if (request.getPath().equals("/classic.html")) {
            final String template = Files.readString(filePath);
            final byte[] content = template.replace(
                    "{time}", LocalDateTime.now().toString()).getBytes();

            out.write((
                    "HTTP/1.1 200 OK\r\n" +
                            "Content-Type: " + mimeType + "\r\n" +
                            "Content-Length: " + content.length + "\r\n" +
                            "Connection: close\r\n" +
                            "\r\n"
            ).getBytes());
            out.write(content);
        }

        final Long length = Files.size(filePath);
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


    private static void checkNotFound(BufferedOutputStream out, Request request) throws IOException {
        if (!handlers.get(request.getMethod()).containsKey(request.getPath())) {
            out.write((
                    "HTTP/1.1 404 Not Found\r\n" +
                            "Content-Length: 0\r\n" +
                            "Connection: close\r\n" +
                            "\r\n"
            ).getBytes());
            out.flush();

        }

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
        }
    }

    public static void addHandler(String method, String path, Handler handler) {
        if (handlers.containsKey(method)) {
            handlers.get(method).put(path, handler);
        } else {
            handlers.put(method, new ConcurrentHashMap<>(Map.of(path, handler)));
        }
    }


}
