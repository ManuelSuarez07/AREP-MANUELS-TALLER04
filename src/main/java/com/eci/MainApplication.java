package com.eci;

import com.eci.Server.WebServerT4;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URISyntaxException;

@SpringBootApplication
public class MainApplication {

    public static void main(String[] args) {
        // Iniciar la aplicación Spring Boot
        SpringApplication.run(MainApplication.class, args);
    }

    /**
     * Bean para configurar el puerto del servidor HTTP (WebServerT4).
     */
    @Bean
    public Integer serverPort() {
        return 35000; // Puerto único para el servidor HTTP y Spring Boot
    }

    /**
     * Componente para iniciar el servidor HTTP (WebServerT4) cuando Spring Boot esté listo.
     */
    @Component
    public static class WebServerInitializer {

        private final Integer serverPort;

        public WebServerInitializer(Integer serverPort) {
            this.serverPort = serverPort;
        }

        @EventListener(ContextRefreshedEvent.class)
        public void startWebServer() {
            try {
                // Iniciar el servidor HTTP en el puerto configurado
                WebServerT4.startServer(serverPort);
                System.out.println("Servidor HTTP iniciado en el puerto: " + serverPort);
            } catch (IOException | URISyntaxException e) {
                System.err.println("Error al iniciar el servidor HTTP: " + e.getMessage());
            }
        }
    }
}