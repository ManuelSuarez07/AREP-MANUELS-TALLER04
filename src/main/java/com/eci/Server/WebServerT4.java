package com.eci.Server;

import com.eci.annotation.GetMapping;
import com.eci.annotation.PostMapping;
import com.eci.annotation.RestController;

import java.io.*;
import java.lang.reflect.Method;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class WebServerT4 {

    private static final Map<String, Service> services = new HashMap<>();
    private static final Map<String, MethodHandler> routes = new HashMap<>();
    private static volatile boolean isRunning = true;
    private static ExecutorService threadPool;

    public static void startServer(int port) throws IOException, URISyntaxException {
        // Registra el servicio de conversión
        services.put("/convertir", (req, resp) -> CurrencyConverter.handleCurrencyConversion(req, resp));

        // Registra controladores y rutas dinámicas
        try {
            registerControllers();
        } catch (Exception e) {
            System.err.println("Error registering controllers: " + e.getMessage());
        }

        // Inicia el pool de hilos
        threadPool = Executors.newCachedThreadPool();

        // Agrega un hook para apagar el servidor de manera segura
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("Shutting down server gracefully...");
            isRunning = false;
            threadPool.shutdown();
            System.out.println("Server has been shut down.");
        }));

        // Inicia el servidor en el puerto especificado
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.println("Server started on port " + port + "...");

            while (isRunning) {
                try {
                    Socket clientSocket = serverSocket.accept();
                    threadPool.submit(() -> handleClient(clientSocket));
                } catch (IOException e) {
                    if (isRunning) {
                        System.err.println("Error accepting client connection: " + e.getMessage());
                    }
                }
            }
        }
    }

    private static void handleClient(Socket clientSocket) {
        try (
                BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true)
        ) {
            String inputLine = in.readLine();
            if (inputLine == null) {
                return;
            }

            String[] requestParts = inputLine.split(" ");
            String method = requestParts[0];
            String path = requestParts[1];
            URI resourceURI = new URI(path);

            String response = handleRequest(method, resourceURI, clientSocket.getOutputStream());

            out.println(response);
        } catch (Exception e) {
            System.err.println("Error handling client: " + e.getMessage());
        } finally {
            try {
                clientSocket.close();
            } catch (IOException e) {
                System.err.println("Error closing client socket: " + e.getMessage());
            }
        }
    }

    private static String handleRequest(String httpMethod, URI resourceURI, OutputStream out) throws IOException {
        String path = resourceURI.getPath();

        // Si la ruta es "/", cargamos el index.html como la página principal
        if (path.equals("/") || path.isEmpty()) {
            return obtainFile("/index.html", out); // Carga el archivo index.html
        }

        // Si es un archivo estático (CSS, JS, imágenes, etc.), lo servimos
        if (path.endsWith(".css") || path.endsWith(".js") || path.matches(".*\\.(jpg|jpeg|png|gif|bmp)")) {
            return obtainFile(path, out); // Sirve archivos estáticos
        }

        // Si la ruta es un servicio
        if (services.containsKey(path)) {
            Request req = new Request(resourceURI.getQuery());
            Response resp = new Response();

            Service service = services.get(path);
            if (service != null) {
                String responseBody = service.getValue(req, resp);
                return "HTTP/1.1 200 OK\r\nContent-Type: application/json\r\n\r\n" + responseBody;
            } else {
                return "HTTP/1.1 404 Not Found\r\nContent-Type: text/plain\r\n\r\nService not found";
            }
        }

        // Si la ruta es dinámica (controladores)
        MethodHandler handler = routes.get(httpMethod + " " + path);
        if (handler != null) {
            try {
                return "HTTP/1.1 200 OK\r\nContent-Type: application/json\r\n\r\n" +
                        handler.invoke(new Request(resourceURI.getQuery()));
            } catch (Exception e) {
                return "HTTP/1.1 500 Internal Server Error\r\nContent-Type: text/plain\r\n\r\n" + e.getMessage();
            }
        }

        // Si no corresponde a ningún archivo estático, servicio o ruta, devolvemos 404
        return "HTTP/1.1 404 Not Found\r\nContent-Type: text/plain\r\n\r\nFile not found";
    }

    private static void registerControllers() throws Exception {
        List<Class<?>> controllers = List.of(
                Class.forName("com.eci.controller.CurrencyController"),
                Class.forName("com.eci.controller.GreetingController")
        );

        for (Class<?> controllerClass : controllers) {
            if (controllerClass.isAnnotationPresent(RestController.class)) {
                Object controllerInstance = controllerClass.getDeclaredConstructor().newInstance();
                for (Method method : controllerClass.getDeclaredMethods()) {
                    if (method.isAnnotationPresent(GetMapping.class)) {
                        GetMapping mapping = method.getAnnotation(GetMapping.class);
                        routes.put("GET " + mapping.value(), new MethodHandler(controllerInstance, method));
                        System.out.println("Registered GET route: " + mapping.value());
                    } else if (method.isAnnotationPresent(PostMapping.class)) {
                        PostMapping mapping = method.getAnnotation(PostMapping.class);
                        routes.put("POST " + mapping.value(), new MethodHandler(controllerInstance, method));
                        System.out.println("Registered POST route: " + mapping.value());
                    }
                }
            }
        }
    }

     public static String obtainFile(String path, OutputStream out) throws IOException {
        String file = path.equals("/") ? "index.html" : path.substring(1);
        String extension = file.contains(".") ? file.substring(file.lastIndexOf('.') + 1) : "";
        String resourcePath = "/static/" + file;
        String responseHeader = "HTTP/1.1 200 OK\r\nContent-Type: " + obtainContentType(extension) + "\r\n\r\n";
        String notFoundResponse = "HTTP/1.1 404 Not Found\r\nContent-Type: text/plain\r\n\r\nFile not found";

        try (InputStream inputStream = WebServerT4.class.getResourceAsStream(resourcePath)) {
            if (inputStream != null) {
                if (extension.matches("jpg|jpeg|png")) {
                    out.write(responseHeader.getBytes());
                    byte[] buffer = new byte[1024];
                    int bytesRead;
                    while ((bytesRead = inputStream.read(buffer)) != -1) {
                        out.write(buffer, 0, bytesRead);
                    }
                    return "";
                } else {
                    String content = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
                    return responseHeader + content;
                }
            } else {
                return notFoundResponse;
            }
        } catch (IOException e) {
            return "HTTP/1.1 500 Internal Server Error\r\nContent-Type: text/plain\r\n\r\n" + e.getMessage();
        }
    }

    public static String obtainContentType(String extension) {
        return switch (extension) {
            case "html", "css" ->
                "text/" + extension;
            case "js" ->
                "text/javascript";
            case "jpg", "jpeg" ->
                "image/jpeg";
            case "png" ->
                "image/png";
            default ->
                "text/plain";
        };
    }

    // Clase interna para manejar métodos dinámicos
    private static class MethodHandler {
        private final Object instance;
        private final Method method;

        public MethodHandler(Object instance, Method method) {
            this.instance = instance;
            this.method = method;
        }

        public String invoke(Request request) throws Exception {
            return (String) method.invoke(instance, request);
        }
    }
}