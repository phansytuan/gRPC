package com.example.userservice.grpc;

import com.example.grpc.*;
import com.example.userservice.entity.User;
import com.example.userservice.repository.UserRepository;
import com.google.protobuf.Empty;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.server.service.GrpcService;

/**
 * gRPC server implementation for UserService.
 *
 * @GrpcService is a Spring stereotype that:
 *   1. Registers this class as a Spring bean.
 *   2. Automatically binds it to the gRPC server started by the starter.
 *
 * No additional @Configuration or manual server wiring is needed.
 */
@GrpcService
@RequiredArgsConstructor
@Slf4j
public class UserGrpcService extends UserServiceGrpc.UserServiceImplBase {

    private final UserRepository userRepository;

    // ── Unary RPC: GetUser ────────────────────────────────────────

    /**
     * Fetch a single user by ID.
     *
     * Pattern:
     *   1. Business logic / DB call
     *   2. Map domain object → proto response
     *   3. responseObserver.onNext(response)
     *   4. responseObserver.onCompleted()
     *
     * On error: responseObserver.onError(Status.XXX.asRuntimeException())
     */
    @Override
    public void getUser(GetUserRequest request,
                        StreamObserver<UserResponse> responseObserver) {
        log.info("GetUser called for id={}", request.getUserId());

        userRepository.findById(request.getUserId())
            .ifPresentOrElse(
                user -> {
                    responseObserver.onNext(toProto(user));
                    responseObserver.onCompleted();
                },
                () -> responseObserver.onError(
                    Status.NOT_FOUND
                        .withDescription("User not found: " + request.getUserId())
                        .asRuntimeException()
                )
            );
    }

    // ── Unary RPC: CreateUser ─────────────────────────────────────

    /**
     * Persist a new user and return the saved record (with generated ID).
     */
    @Override
    public void createUser(CreateUserRequest request,
                           StreamObserver<UserResponse> responseObserver) {
        log.info("CreateUser called for email={}", request.getEmail());

        try {
            User saved = userRepository.save(
                User.builder()
                    .name(request.getName())
                    .email(request.getEmail())
                    .age(request.getAge())
                    .build()
            );

            responseObserver.onNext(toProto(saved));
            responseObserver.onCompleted();

        } catch (Exception e) {
            log.error("CreateUser failed", e);
            responseObserver.onError(
                Status.INTERNAL
                    .withDescription("Failed to create user: " + e.getMessage())
                    .withCause(e)
                    .asRuntimeException()
            );
        }
    }

    // ── Server-streaming RPC: ListUsers ───────────────────────────

    /**
     * Stream all users one by one.
     *
     * Server-streaming pattern:
     *   - Call onNext() for each item in the result set.
     *   - Call onCompleted() once all items are sent.
     *   - The client receives them as a lazy iterator / reactive stream.
     *
     * In production you'd paginate the DB query to avoid loading
     * the entire table into memory at once.
     */
    @Override
    public void listUsers(Empty request,
                          StreamObserver<UserResponse> responseObserver) {
        log.info("ListUsers called — streaming all users");

        userRepository.findAll().forEach(user ->
            responseObserver.onNext(toProto(user))
        );

        responseObserver.onCompleted();
    }

    // ── Helpers ───────────────────────────────────────────────────

    /** Map a JPA User entity to the generated proto UserResponse. */
    private UserResponse toProto(User user) {
        return UserResponse.newBuilder()
            .setUserId(user.getId())
            .setName(user.getName())
            .setEmail(user.getEmail())
            .setAge(user.getAge())
            .build();
    }
}
