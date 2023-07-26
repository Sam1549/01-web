package org.example;

import org.apache.http.client.utils.URLEncodedUtils;

import java.io.*;
import java.net.ServerSocket;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Server {
    public static final String GET = "GET";
    public static final String POST = "POST";
    final static List<String> allowedMethods = List.of(GET, POST);
    private final static Map<String, Map<String, Handler>> handlers = new ConcurrentHashMap<>();

    public static void main(String[] args) {
        addHandler("GET", "/vinni.png", (request, bufferedOutputStream) -> {
            final Path filePath = Path.of(".", "public", request.getPath());
            final String mimeType;
            try {
                mimeType = Files.probeContentType(filePath);
                final long length = Files.size(filePath);
                bufferedOutputStream.write((
                        "HTTP/1.1 200 OK\r\n" +
                                "Content-Type: " + mimeType + "\r\n" +
                                "Content-Length: " + length + "\r\n" +
                                "Connection: close\r\n" +
                                "\r\n"
                ).getBytes());
                Files.copy(filePath, bufferedOutputStream);
                bufferedOutputStream.flush();
            } catch (IOException e) {
                e.printStackTrace();
            }
        });

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
                        BufferedInputStream in = new BufferedInputStream(socket.getInputStream());
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

    private void logicResponse(BufferedInputStream in, BufferedOutputStream out) throws IOException {
        Request request = getRequest(in, out);
        Handler handler = handlers.get(request.getMethod()).get(request.getPath());
        handler.handle(request, out);
        System.out.println(request);
        System.out.println(request.getQueryParams());
    }

    private static Request getRequest(BufferedInputStream in, BufferedOutputStream out) throws IOException {

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
//                continue;
        }

        // читаем request line
        final var requestLine = new String(Arrays.copyOf(buffer, requestLineEnd)).split(" ");
        if (requestLine.length != 3) {
            badRequest(out);
//                continue;
        }

        final var method = requestLine[0];
        if (!allowedMethods.contains(method)) {
            badRequest(out);
//                continue;
        }
        System.out.println(method);

        var path = requestLine[1].contains("?") ? requestLine[1].substring(0,requestLine[1].indexOf("?")) : requestLine[1];
        if (!path.startsWith("/")) {
            badRequest(out);
//                continue;
        }

        System.out.println(path);

        // ищем заголовки
        final var headersDelimiter = new byte[]{'\r', '\n', '\r', '\n'};
        final var headersStart = requestLineEnd + requestLineDelimiter.length;
        final var headersEnd = indexOf(buffer, headersDelimiter, headersStart, read);
        if (headersEnd == -1) {
            badRequest(out);
//                continue;
        }

        // отматываем на начало буфера
        in.reset();
        // пропускаем requestLine
        in.skip(headersStart);

        final byte[] headersBytes = in.readNBytes(headersEnd - headersStart);
        final var headers = Arrays.asList(new String(headersBytes).split("\r\n"));
        System.out.println(headers);

        // для GET тела нет
        String body = "";
        if (!method.equals(GET)) {
            in.skip(headersDelimiter.length);
            // вычитываем Content-Length, чтобы прочитать body
            final var contentLength = extractHeader(headers);
            if (contentLength.isPresent()) {
                final var length = Integer.parseInt(contentLength.get());
                final var bodyBytes = in.readNBytes(length);

                body = new String(bodyBytes);
                System.out.println(body);
            }
        }
        Request request = new Request(method, path, headers, body);
        URI url = null;
        try {
            url = new URI(requestLine[1]);
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
        assert url != null;
        request.setQueryParams(URLEncodedUtils.parse(url, StandardCharsets.UTF_8));
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

        final long length = Files.size(filePath);
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


    public static void addHandler(String method, String path, Handler handler) {
        if (handlers.containsKey(method)) {
            handlers.get(method).put(path, handler);
        } else {
            handlers.put(method, new ConcurrentHashMap<>(Map.of(path, handler)));
        }
    }

    private static Optional<String> extractHeader(List<String> headers) {
        return headers.stream()
                .filter(o -> o.startsWith("Content-Length"))
                .map(o -> o.substring(o.indexOf(" ")))
                .map(String::trim)
                .findFirst();
    }

    private static void badRequest(BufferedOutputStream out) throws IOException {
        out.write((
                "HTTP/1.1 400 Bad Request\r\n" +
                        "Content-Length: 0\r\n" +
                        "Connection: close\r\n" +
                        "\r\n"
        ).getBytes());
        out.flush();
    }

    // from Google guava with modifications
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


}
