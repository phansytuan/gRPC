# gRPC Spring Boot Demo

A minimal, runnable example of **two Spring Boot microservices communicating via gRPC**.

```
┌─────────────────────────────────────────────────────────────────┐
│                        grpc-demo (parent)                       │
│                                                                 │
│  ┌──────────────┐   proto stubs   ┌───────────────────────────┐ │
│  │   grpc-api   │ ──────────────► │      user-service         │ │
│  │  (shared     │                 │  @GrpcService             │ │
│  │   .proto)    │ ──────────────► │  gRPC server :9090        │ │
│  └──────────────┘   proto stubs   │  H2 DB + JPA              │ │
│                                   │  HTTP mgmt :8081          │ │
│                                   └─────────────┬─────────────┘ │
│                                                 │ gRPC :9090    │
│                                   ┌─────────────▼─────────────┐ │
│                                   │      order-service        │ │
│                                   │  @GrpcClient              │ │
│                                   │  REST API :8080           │ │
│                                   └───────────────────────────┘ │
└─────────────────────────────────────────────────────────────────┘
```

## Project Layout

```
grpc-demo/
├── pom.xml                          ← parent POM, manages versions
│
├── grpc-api/                        ← shared module: .proto → Java stubs
│   ├── pom.xml
│   └── src/main/proto/user.proto    ← THE CONTRACT (single source of truth)
│
├── user-service/                    ← gRPC server
│   ├── pom.xml
│   └── src/main/java/com/example/userservice/
│       ├── UserServiceApplication.java
│       ├── DataSeeder.java                    ← seeds H2 on startup
│       ├── entity/User.java
│       ├── repository/UserRepository.java
│       └── grpc/
│           ├── UserGrpcService.java           ← @GrpcService handler
│           └── AuthServerInterceptor.java     ← @GrpcGlobalServerInterceptor
│
└── order-service/                   ← gRPC client + REST API
    ├── pom.xml
    └── src/main/java/com/example/orderservice/
        ├── OrderServiceApplication.java
        ├── controller/UserController.java     ← REST → gRPC façade
        ├── service/UserClientService.java     ← @GrpcClient wrapper
        └── exception/UserNotFoundException.java
```

## Prerequisites

| Tool | Version |
|------|---------|
| Java | 17+     |
| Maven | 3.8+   |

## Running

### 1. Build everything (generates proto stubs)

```bash
cd grpc-demo
mvn clean install -DskipTests
```

The `protobuf-maven-plugin` in `grpc-api` runs during `compile` and generates
Java classes under `target/generated-sources/`.

### 2. Start user-service (gRPC server)

```bash
cd user-service
mvn spring-boot:run
```

Logs you should see:
```
gRPC Server started, listening on address: *, port: 9090
Seeded 3 demo users
```

### 3. Start order-service (gRPC client + REST)

In a new terminal:
```bash
cd order-service
mvn spring-boot:run
```

### 4. Try it out

```bash
# List all users (server-streaming gRPC behind the scenes)
curl http://localhost:8080/users

# Get a specific user (replace UUID with one from the list above)
curl http://localhost:8080/users/<uuid>

# Create a new user
curl -X POST http://localhost:8080/users \
  -H "Content-Type: application/json" \
  -d '{"name":"Dave","email":"dave@example.com","age":28}'

# Health check
curl http://localhost:8080/users/health
```

## Key Concepts

### Proto as the contract

`grpc-api/src/main/proto/user.proto` is the **single source of truth**.
Both services depend on the `grpc-api` JAR — they can never drift out of sync.

### @GrpcService (server)

```java
@GrpcService   // ← registers + binds to the gRPC server automatically
public class UserGrpcService extends UserServiceGrpc.UserServiceImplBase {
    // override generated stub methods
}
```

### @GrpcClient (client)

```java
@GrpcClient("user-service")   // ← name matches grpc.client.user-service in yml
private UserServiceGrpc.UserServiceBlockingStub userStub;
```

The starter creates the channel, manages its lifecycle, and injects the stub.
No `ManagedChannelBuilder` boilerplate required.

### Error handling

```java
} catch (StatusRuntimeException e) {
    switch (e.getStatus().getCode()) {
        case NOT_FOUND   -> throw new UserNotFoundException(id);
        case UNAVAILABLE -> throw new RuntimeException("Service down");
        // ...
    }
}
```

### RPC patterns

| Pattern | Proto syntax | Used in demo |
|---------|-------------|-------------|
| Unary | `rpc Get(Req) returns (Resp)` | GetUser, CreateUser |
| Server streaming | `returns (stream Resp)` | ListUsers |
| Client streaming | `rpc Upload(stream Req) returns (Resp)` | — |
| Bidirectional | `rpc Chat(stream Req) returns (stream Resp)` | — |

## Production Checklist

- [ ] Enable TLS: `negotiation-type: tls` + configure certificates
- [ ] Replace `demo-token-change-in-prod` with real JWT validation
- [ ] Replace H2 with PostgreSQL / MySQL
- [ ] Add distributed tracing (Micrometer + Zipkin/Jaeger)
- [ ] Move proto to a Git submodule shared across repos
- [ ] Add `@GrpcGlobalClientInterceptor` for retry / circuit-breaker
