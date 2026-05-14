package com.example.orderservice.controller;

import com.example.grpc.UserResponse;
import com.example.orderservice.service.UserClientService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * REST façade that translates HTTP requests into gRPC calls to user-service.
 *
 * This pattern is common when:
 *   • External clients (browsers, mobile) speak REST/JSON.
 *   • Internal services communicate over gRPC.
 *   • This service acts as a BFF (Backend for Frontend) or API gateway layer.
 *
 * Endpoints:
 *   GET  /users          → listUsers  (server-streaming gRPC → REST array)
 *   GET  /users/{id}     → getUser    (unary gRPC)
 *   POST /users          → createUser (unary gRPC)
 */
@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserController {

    private final UserClientService userClientService;

    /** List all users — internally triggers a server-streaming gRPC call. */
    @GetMapping
    public ResponseEntity<List<UserResponse>> listUsers() {
        return ResponseEntity.ok(userClientService.listAllUsers());
    }

    /** Fetch a single user by their UUID. */
    @GetMapping("/{id}")
    public ResponseEntity<UserResponse> getUser(@PathVariable String id) {
        return ResponseEntity.ok(userClientService.getUser(id));
    }

    /** Create a new user via a unary gRPC call. */
    @PostMapping
    public ResponseEntity<UserResponse> createUser(@RequestBody CreateUserRequest body) {
        UserResponse created = userClientService.createUser(
            body.name(), body.email(), body.age()
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    /** Simple health check. */
    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        return ResponseEntity.ok(Map.of("status", "UP", "service", "order-service"));
    }

    // ── Inner record for request body deserialization ─────────────
    record CreateUserRequest(String name, String email, int age) {}
}
