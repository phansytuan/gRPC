package com.example.userservice.grpc;

import io.grpc.*;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.server.interceptor.GrpcGlobalServerInterceptor;

/**
 * Global server interceptor — runs before every RPC handler.
 *
 * @GrpcGlobalServerInterceptor registers this as a Spring bean AND
 * automatically attaches it to all gRPC services in this application.
 *
 * Common use-cases:
 *   - JWT / token validation
 *   - Request logging / tracing
 *   - Rate limiting
 *   - Tenant resolution in multi-tenant systems
 */
@GrpcGlobalServerInterceptor
@Slf4j
public class AuthServerInterceptor implements ServerInterceptor {

    private static final Metadata.Key<String> AUTH_KEY =
        Metadata.Key.of("authorization", Metadata.ASCII_STRING_MARSHALLER);

    @Override
    public <Req, Resp> ServerCall.Listener<Req> interceptCall(
            ServerCall<Req, Resp> call,
            Metadata headers,
            ServerCallHandler<Req, Resp> next) {

        String method = call.getMethodDescriptor().getFullMethodName();
        String token  = headers.get(AUTH_KEY);

        log.debug("Intercepting call to {}", method);

        if (!isValidToken(token)) {
            log.warn("Rejected unauthenticated call to {}", method);
            call.close(
                Status.UNAUTHENTICATED.withDescription("Missing or invalid Bearer token"),
                new Metadata()
            );
            // Return a no-op listener; the call is already closed.
            return new ServerCall.Listener<>() {};
        }

        log.debug("Authenticated call to {}", method);
        return next.startCall(call, headers);
    }

    /**
     * Minimal token check — replace with real JWT validation in production.
     * Accepted tokens: any string that starts with "Bearer ".
     */
    private boolean isValidToken(String token) {
        return token != null && token.startsWith("Bearer ");
    }
}
