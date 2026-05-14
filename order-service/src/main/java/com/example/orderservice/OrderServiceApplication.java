package com.example.orderservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Order Service — gRPC client + REST API.
 *
 * What starts up:
 *   • Spring MVC web server on port 8080  (configured in application.yml)
 *   • Managed gRPC channel to user-service:9090 (lazy — created on first use)
 *
 * REST endpoints (all delegate to user-service via gRPC):
 *   GET  http://localhost:8080/users
 *   GET  http://localhost:8080/users/{id}
 *   POST http://localhost:8080/users
 */
@SpringBootApplication
public class OrderServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(OrderServiceApplication.class, args);
    }
}
