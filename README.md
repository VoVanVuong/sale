# UserTool

A small Spring Boot REST API for user management, built as a practice project.

## Tech stack

| Component   | Choice                                   |
|-------------|------------------------------------------|
| Language    | Java 17                                  |
| Framework   | Spring Boot 4.1.0 (Web MVC + Data JPA)   |
| Database    | PostgreSQL                               |
| Cache       | Redis (per-user auth state)              |
| Validation  | Jakarta Bean Validation                  |
| Security    | BCrypt password hashing (spring-security-crypto) |
| Build       | Maven (wrapper included)                 |

## Project structure

```
com.example.usertool
├── UserToolApplication         Application entry point
├── config/AppConfig            PasswordEncoder (BCrypt) bean
├── controller/UserController   REST endpoints under /users
├── service/UserService         Business logic + soft delete
├── cache/UserCache             Per-user auth-state cache abstraction
│   └── RedisUserCache          Redis Hash impl (user:auth:{id})
├── repository/UserRepository   Spring Data JPA repository
├── entity/User                 JPA entity mapped to "users"
├── enums/Role                  USER, ADMIN
├── common/ApiResponse<T>       Standard { code, message, result } envelope
└── exception/
    ├── AppException            Runtime exception carrying an ErrorCode
    ├── ErrorCode               Business error catalog (code + message + HTTP status)
    └── GlobalExceptionHandler  Centralized @RestControllerAdvice
└── dto/
    ├── request/UserCreationRequest   Validated create payload
    ├── request/UserUpdateRequest     Validated (optional) update payload
    └── response/UserResponse         User view without the password
```

## Getting started

### Prerequisites
- JDK 17 or higher (ensure `JAVA_HOME` is set)
- A running PostgreSQL instance
- A running Redis instance (e.g. `docker run -p 6379:6379 redis`)

### Database setup
Create the database used by the app:

```sql
CREATE DATABASE user_tool;
```

Connection settings live in `src/main/resources/application.properties`:

```properties
spring.datasource.url=jdbc:postgresql://localhost:5432/user_tool
spring.datasource.username=postgres
spring.datasource.password=123456
```

> ⚠️ Credentials are currently hard-coded for local development. Move them to
> environment variables before deploying anywhere real.

Hibernate is configured with `ddl-auto=update`, so the `users` table is created
automatically on first run.

### Redis auth state
On successful registration, the app seeds a per-user Redis Hash that will back
future JWT validation:

```
Key:   user:auth:{id}      (Hash)
Field: tokenVersion = 0    (bump to invalidate all of a user's tokens at once)
Field: enabled      = true (account on/off)
```

Seeding is best-effort: if Redis is unavailable the registration still succeeds
(the failure is logged), and Redis timeouts are kept short so a missing Redis
never stalls the request.

### Run

```bash
# Windows
mvnw.cmd spring-boot:run

# macOS / Linux
./mvnw spring-boot:run
```

The API starts on `http://localhost:8080`.

## API

All responses use the `ApiResponse` envelope:

```json
{
  "code": 1000,
  "message": "Success",
  "result": { }
}
```

### Endpoints

| Method | Path          | Description                                    |
|--------|---------------|------------------------------------------------|
| POST   | `/users`      | Create a user (hashes password, rejects duplicate username) |
| GET    | `/users`      | List all active users                          |
| GET    | `/users/{id}` | Get a single active user                       |
| PUT    | `/users/{id}` | Update a user's email and/or password          |
| DELETE | `/users/{id}` | Soft-delete a user (sets `deleted = true`)     |

### Example: create a user

```bash
curl -X POST http://localhost:8080/users \
  -H "Content-Type: application/json" \
  -d '{"username":"alice","email":"alice@example.com","password":"secret123"}'
```

```json
{
  "code": 1000,
  "message": "User created",
  "result": {
    "id": 1,
    "username": "alice",
    "email": "alice@example.com",
    "role": "USER"
  }
}
```

### Error codes

| Code | HTTP | Meaning                     |
|------|------|-----------------------------|
| 1000 | 200  | Success                     |
| 1001 | 400  | Invalid message key         |
| 1002 | 400  | Invalid request data (validation) |
| 1003 | 404  | User not found              |
| 1004 | 409  | User already exists         |
| 1005 | 401  | Unauthenticated             |
| 1006 | 403  | Forbidden                   |
| 9999 | 500  | Uncategorized exception     |

## Testing

Tests run against an in-memory H2 database (configured in
`src/test/resources/application.properties`), so no PostgreSQL is required:

```bash
./mvnw test
```

Coverage:
- `UserServiceTest` — service logic (JUnit 5 + Mockito)
- `UserControllerTest` — web layer (`@WebMvcTest` + `MockMvc`)
- `UserToolApplicationTests` — application context loads

## Roadmap

- [ ] Authentication & authorization (JWT / Spring Security)
- [ ] Move DB credentials to environment variables
- [ ] Environment-specific profiles + database migrations (Flyway/Liquibase)
- [x] Unit and integration tests for the service and controller layers
