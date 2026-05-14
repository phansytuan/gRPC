package com.example.orderservice.service;

import com.example.grpc.*;
import com.example.orderservice.exception.UserNotFoundException;
import com.google.protobuf.Empty;
import io.grpc.Metadata;
import io.grpc.StatusRuntimeException;
import io.grpc.stub.MetadataUtils;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Client wrapper that calls user-service over gRPC.
 *
 * @GrpcClient("user-service") tells the starter to:
 *   1. Look up the channel config under grpc.client.user-service in application.yml.
 *   2. Create a managed channel with the configured address and TLS settings.
 *   3. Inject a pre-built blocking stub — no manual Channel/ManagedChannelBuilder needed.
 *
 * Stub types (all auto-available once the channel is configured):
 *   • BlockingStub  — synchronous, simplest to use
 *   • FutureStub    — returns ListenableFuture, integrates with Guava/Spring async
 *   • AsyncStub     — full async with StreamObserver callbacks
 */
@Service
@Slf4j
public class UserClientService {

    /**
     * Blocking stub — each call blocks the calling thread until the server responds.
     * Good for typical Spring MVC request-handling threads.
     * For reactive/WebFlux services, prefer the FutureStub instead.
     */
    @GrpcClient("user-service")
    private UserServiceGrpc.UserServiceBlockingStub userStub;

    // ── Outbound header with Bearer token ─────────────────────────

    /**
     * Attach a Bearer token to a stub instance.
     *
     * withInterceptors() returns a NEW stub — the original is unmodified.
     * In production, retrieve the real token from a SecurityContext or
     * secrets manager; never hard-code it.
     */
    private UserServiceGrpc.UserServiceBlockingStub authenticatedStub() {
        Metadata headers = new Metadata();
        headers.put(
            Metadata.Key.of("authorization", Metadata.ASCII_STRING_MARSHALLER),
            "Bearer demo-token-change-in-prod"
        );
        return userStub.withInterceptors(MetadataUtils.newAttachHeadersInterceptor(headers));
    }

    // ── Unary: GetUser ─────────────────────────────────────────────

    /**
     * Fetch a single user by ID.
     * Maps gRPC status codes to Spring/HTTP exceptions for clean REST responses.
     */
    public UserResponse getUser(String userId) {
        log.info("Calling GetUser id={}", userId);
        GetUserRequest request = GetUserRequest.newBuilder()
            .setUserId(userId)
            .build();
        try {
            return authenticatedStub().getUser(request);
        } catch (StatusRuntimeException e) {
            return handleError(e, userId);
        }
    }

    // ── Unary: CreateUser ──────────────────────────────────────────

    public UserResponse createUser(String name, String email, int age) {
        log.info("Calling CreateUser email={}", email);
        try {
            return authenticatedStub().createUser(
                CreateUserRequest.newBuilder()
                    .setName(name)
                    .setEmail(email)
                    .setAge(age)
                    .build()
            );
        } catch (StatusRuntimeException e) {
            return handleError(e, email);
        }
    }

    // ── Server-streaming: ListUsers ────────────────────────────────

    /**
     * Consume the server-side stream and collect results into a List.
     *
     * The blocking stub returns an Iterator<UserResponse>; the connection
     * stays open until the server calls onCompleted() or onError().
     * For large result sets, consider processing items lazily rather than
     * collecting them all into memory.
     */
    public List<UserResponse> listAllUsers() {
        log.info("Calling ListUsers (server-streaming)");
        Iterator<UserResponse> stream =
            authenticatedStub().listUsers(Empty.getDefaultInstance());

        List<UserResponse> results = new ArrayList<>();
        stream.forEachRemaining(results::add);

        log.info("Received {} users from stream", results.size());
        return results;
    }

    // ── Error mapping ──────────────────────────────────────────────

    /**
     * Translate gRPC status codes into typed exceptions.
     * Spring's @ResponseStatus or @ControllerAdvice can then map these
     * to the appropriate HTTP status codes.
     */
    private <T> T handleError(StatusRuntimeException e, String context) {
        log.error("gRPC call failed — status={} desc={}",
                  e.getStatus().getCode(), e.getStatus().getDescription());

        switch (e.getStatus().getCode()) {
            case NOT_FOUND       -> throw new UserNotFoundException(context);
            case UNAUTHENTICATED -> throw new SecurityException("gRPC auth failed: " + e.getStatus().getDescription());
            case UNAVAILABLE     -> throw new RuntimeException("user-service is unavailable — retry later");
            default              -> throw new RuntimeException("gRPC error: " + e.getMessage());
        }
    }
}
