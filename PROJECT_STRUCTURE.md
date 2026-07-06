# 📁 Project Structure - UserTool

## 🌳 Complete Project Tree

```
sale/
├── pom.xml                          # Maven configuration
├── HELP.md
├── docker-compose.yml               # Docker container setup
├── Dockerfile                       # App container image
├── SECURITY_ARCHITECTURE.md         # Security design doc
└── src/
    ├── main/
    │   ├── java/
    │   │   └── com/example/usertool/
    │   │       ├── UserToolApplication.java          # Main entry point
    │   │       │
    │   │       ├── config/                           # 🔧 Configuration
    │   │       │   ├── SecurityConfig.java           # Spring Security config
    │   │       │   ├── RedisConfig.java              # Redis config
    │   │       │   └── JacksonConfig.java            # JSON serialization config
    │   │       │
    │   │       ├── security/                         # 🔐 JWT & Security
    │   │       │   ├── JwtUtil.java                  # JWT token generation & validation
    │   │       │   ├── JwtAuthenticationFilter.java  # JWT authentication filter
    │   │       │   ├── JwtException.java             # JWT exception
    │   │       │   └── UserPrincipal.java            # Custom user principal
    │   │       │
    │   │       ├── entity/                           # 📊 Database entities
    │   │       │   ├── Users.java                    # User entity
    │   │       │   └── AuditBaseEntity.java          # Base entity for audit fields
    │   │       │
    │   │       ├── dto/                              # 📨 Data Transfer Objects
    │   │       │   ├── UserRequest.java              # User creation/update request
    │   │       │   ├── UserResponse.java             # User response (no password)
    │   │       │   ├── ValidationErrorResponse.java  # Validation error detail
    │   │       │   ├── LoginRequest.java             # Login request
    │   │       │   ├── LoginResponse.java            # Login response with tokens
    │   │       │   ├── RefreshTokenRequest.java      # Token refresh request
    │   │       │   └── ChangePasswordRequest.java    # Password change request
    │   │       │
    │   │       ├── enums/                            # 📋 Enumerations
    │   │       │   └── RoleEnum.java                 # USER, ADMIN roles
    │   │       │
    │   │       ├── repository/                       # 💾 Database access
    │   │       │   └── UserRepository.java           # User CRUD + queries
    │   │       │
    │   │       ├── service/                          # 🔧 Business logic
    │   │       │   ├── UserService.java              # User CRUD operations
    │   │       │   ├── AuthService.java              # Authentication & JWT
    │   │       │   └── RedisTokenService.java        # Redis token management
    │   │       │
    │   │       ├── controller/                       # 🌐 API endpoints
    │   │       │   ├── UserController.java           # User endpoints (CRUD)
    │   │       │   └── AuthController.java           # Authentication endpoints
    │   │       │
    │   │       ├── exception/                        # ⚠️ Exception handling
    │   │       │   ├── AppException.java             # Custom app exception
    │   │       │   ├── ErrorCode.java                # Error codes enum
    │   │       │   ├── JwtException.java             # JWT-specific exception
    │   │       │   └── GlobalExceptionHandler.java   # Global exception handler
    │   │       │
    │   │       ├── common/                           # 📦 Common utilities
    │   │       │   ├── ApiResponse.java              # Standard API response
    │   │       │   ├── ResponseBuilder.java          # Build API responses
    │   │       │   └── DateUtils.java                # Date utilities
    │   │       │
    │   │       └── util/                             # 🛠️ Utilities
    │   │           ├── JwtUtils.java                 # JWT helper methods
    │   │           ├── EncryptionUtil.java           # Password encryption
    │   │           └── ValidationUtil.java           # Input validation
    │   │
    │   └── resources/
    │       ├── application.properties                # Default config
    │       ├── application-security.properties       # Security config
    │       ├── application-redis.properties          # Redis config
    │       ├── application-dev.properties            # Development profile
    │       ├── application-prod.properties           # Production profile
    │       ├── static/                               # Static files
    │       └── templates/                            # HTML templates
    │
    └── test/
        └── java/
            └── com/example/usertool/
                ├── UserToolApplicationTests.java
                ├── security/
                │   ├── JwtUtilTest.java
                │   └── JwtAuthenticationFilterTest.java
                ├── service/
                │   ├── UserServiceTest.java
                │   ├── AuthServiceTest.java
                │   └── RedisTokenServiceTest.java
                └── controller/
                    ├── UserControllerTest.java
                    └── AuthControllerTest.java

target/                                              # Build output
├── classes/
├── generated-sources/
├── generated-test-sources/
├── maven-archiver/
├── maven-status/
└── test-classes/
```

---

## 📦 Key Directories Explained

### 1. **config/** - Configuration
```java
SecurityConfig.java
├── Spring Security configuration
├── Filter chain setup
├── CORS settings
└── Authentication entry points

RedisConfig.java
├── Redis connection pool
├── Template configuration
└── Serialization strategy

JacksonConfig.java
├── JSON serialization settings
└── Custom serializers
```

### 2. **security/** - JWT & Authentication
```java
JwtUtil.java
├── generateAccessToken()
├── generateRefreshToken()
├── validateToken()
├── extractClaims()
└── getExpirationTime()

JwtAuthenticationFilter.java
├── doFilterInternal()
├── Extract token from header
├── Build Authentication
└── Handle exceptions
```

### 3. **dto/** - Request/Response Models
```
Request Objects:
├── UserRequest        → Create/Update user
├── LoginRequest       → Username + password
├── RefreshTokenRequest → Refresh token
└── ChangePasswordRequest → Old + new password

Response Objects:
├── UserResponse       → User data (no password)
├── LoginResponse      → Tokens + user info
└── ValidationErrorResponse → Field + message
```

### 4. **service/** - Business Logic
```
UserService
├── createUser()       → Validate + save + hash password
├── getUserById()      → Find by ID
├── updateUser()       → Update user data
├── deleteUser()       → Soft delete (set deleted=true)
└── changePassword()   → Update password + new versionToken

AuthService
├── authenticate()     → Login user + generate tokens
├── refreshToken()     → Issue new access token
├── logout()           → Add token to blacklist
└── validateToken()    → Check token validity

RedisTokenService
├── saveVersionToken() → Store user version token
├── getVersionToken()  → Retrieve from Redis
├── isTokenBlacklisted() → Check logout status
└── invalidateUserTokens() → Clear all user tokens
```

### 5. **controller/** - API Endpoints
```
AuthController
├── POST /api/auth/login           → Authenticate
├── POST /api/auth/refresh         → Get new access token
├── POST /api/auth/logout          → Revoke token
└── POST /api/auth/verify          → Verify token

UserController (Protected)
├── POST /api/users                → Create (ADMIN)
├── GET /api/users                 → List all (ADMIN)
├── GET /api/users/{id}            → Get by ID
├── GET /api/users/username/{name} → Get by username
├── PUT /api/users/{id}            → Update
├── DELETE /api/users/{id}         → Delete
└── PUT /api/users/{id}/change-password → Change password
```

### 6. **exception/** - Error Handling
```
ErrorCode.java (Enum)
├── USER_NOT_FOUND(1003)
├── USER_EXISTED(1004)
├── VALIDATION_ERROR(1002)
├── UNAUTHENTICATED(1005)
├── UNAUTHORIZED(1006)
├── NOT_FOUND(1007)
└── UNCATEGORIZED_EXCEPTION(9999)

GlobalExceptionHandler.java
├── handleAppException()
├── handleValidationException()
├── handleJwtException()
├── handleNoHandlerFoundException()
└── handleUncategorizedException()
```

---

## 🔑 Critical Files Summary

| File | Purpose | Key Methods |
|------|---------|-------------|
| **JwtUtil.java** | JWT token operations | generateAccessToken, validateToken |
| **SecurityConfig.java** | Security setup | securityFilterChain, passwordEncoder |
| **RedisTokenService.java** | Token caching | saveVersionToken, isTokenBlacklisted |
| **AuthService.java** | Login logic | authenticate, refreshToken |
| **UserService.java** | User CRUD | createUser, changePassword |
| **AuthController.java** | Auth endpoints | login, refresh, logout |
| **UserController.java** | User endpoints | All CRUD with @PreAuthorize |

---

## 🚀 Build & Package Structure

### After `mvn clean package`:
```
target/
├── UserTool-0.0.1-SNAPSHOT.jar    # Executable JAR
├── UserTool-0.0.1-SNAPSHOT-sources.jar
├── classes/                        # Compiled .class files
│   ├── com/example/usertool/**    # All Java classes
│   └── application*.properties
├── generated-sources/              # Annotation processors
└── test-classes/                   # Test compiled files
```

---

## 📝 Application Properties Files

### application.properties
```properties
spring.application.name=UserTool
spring.profiles.active=dev
spring.config.import=classpath:application-security.properties,classpath:application-redis.properties
```

### application-security.properties
```properties
# JWT Configuration
jwt.secret.key=${JWT_SECRET:your-secret-key-min-256-chars}
jwt.expiration.access=1800000
jwt.expiration.refresh=2592000000

# Security
spring.security.user.name=admin
spring.security.user.password=${ADMIN_PASSWORD:admin123}
```

### application-redis.properties
```properties
# Redis Configuration
spring.redis.host=${REDIS_HOST:localhost}
spring.redis.port=${REDIS_PORT:6379}
spring.redis.password=${REDIS_PASSWORD:}
spring.redis.timeout=60000
spring.redis.jedis.pool.max-active=8
spring.redis.jedis.pool.max-idle=8
spring.redis.jedis.pool.min-idle=2
```

---

## ✅ Directory Creation Steps

```bash
# Security
mkdir -p src/main/java/com/example/usertool/security
mkdir -p src/main/java/com/example/usertool/config

# DTOs
mkdir -p src/main/java/com/example/usertool/dto

# Tests
mkdir -p src/test/java/com/example/usertool/security
mkdir -p src/test/java/com/example/usertool/service
mkdir -p src/test/java/com/example/usertool/controller

# Resources
mkdir -p src/main/resources
```

---

## 🎯 Implementation Sequence

### Phase 1: Foundation (Week 1)
- [x] Create entity + repository + basic service
- [x] Create DTOs
- [x] Exception handling

### Phase 2: Authentication (Week 2)
- [ ] Add JWT dependencies
- [ ] Create JwtUtil
- [ ] Create AuthService
- [ ] Create AuthController
- [ ] Test login + token generation

### Phase 3: Authorization (Week 3)
- [ ] Create SecurityConfig
- [ ] Create JwtAuthenticationFilter
- [ ] Add @PreAuthorize to controllers
- [ ] Test role-based access

### Phase 4: Redis Integration (Week 4)
- [ ] Add Redis dependency
- [ ] Create RedisTokenService
- [ ] Implement token blacklist
- [ ] Test logout & token revocation

### Phase 5: Testing & Deployment (Week 5)
- [ ] Unit tests for all services
- [ ] Integration tests
- [ ] End-to-end API testing
- [ ] Docker build & deploy

---

**Status**: 📁 Structure ready for implementation
**Last Updated**: 2026-07-05
