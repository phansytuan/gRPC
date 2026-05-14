package com.example.userservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * User Service — gRPC server.
 *
 * What starts up:
 *   • Spring application context
 *   • JPA / H2 database
 *   • gRPC server on port 9090  (configured in application.yml)
 *   • HTTP management server on port 8081 (H2 console, actuator)
 */
@SpringBootApplication
public class UserServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(UserServiceApplication.class, args);
    }
}
