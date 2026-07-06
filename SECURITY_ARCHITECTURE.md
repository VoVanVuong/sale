# 🔐 Security Architecture - UserTool

## 📌 Yêu cầu chính

### 1. **Authentication (Xác thực)**
- Login với username/email + password
- Generate JWT token (Access Token + Refresh Token)
- Password hashing với BCrypt
- Token validation trên mỗi request

### 2. **Authorization (Phân quyền)**
- Role-based access control (RBAC)
- Roles: `USER`, `ADMIN`
- Endpoint protection dựa trên role
- Check permission tại @PreAuthorize

### 3. **Token Management**
- **Access Token**: Ngắn hạn (30 phút)
- **Refresh Token**: Dài hạn (30 ngày)
- Token refresh endpoint
- Token logout/revocation

### 4. **Redis Integration**
- Cache token validation (versionToken check)
- Store blacklist tokens (logout)
- Session management
- Performance optimization

---

## 🏗️ Architecture Overview

```
┌─────────────────────────────────────────────────────────────┐
│                         Client                               │
└──────────────────┬──────────────────────────────────────────┘
                   │ Request + JWT Token
                   ▼
┌─────────────────────────────────────────────────────────────┐
│           Spring Security Filter Chain                       │
│  ┌──────────────────────────────────────────────────────┐   │
│  │ 1. JwtAuthenticationFilter (Extract Token)          │   │
│  │ 2. Validate Token (Signature, Expiry)               │   │
│  │ 3. Redis Check (Token Blacklist, versionToken)      │   │
│  │ 4. Build Authentication (UserDetails, Authorities)  │   │
│  │ 5. @PreAuthorize (Role Check)                       │   │
│  └──────────────────────────────────────────────────────┘   │
└──────────────────┬──────────────────────────────────────────┘
                   │ Valid Token + Permission ✅
                   ▼
          ┌────────────────────┐
          │   Controller       │
          │   Service Layer    │
          │   Business Logic   │
          └────────────────────┘
```

---

## 🔑 Key Components

### 1. **JWT Token Structure**

#### Access Token (Payload)
```json
{
  "sub": "user123",
  "username": "john_doe",
  "email": "john@example.com",
  "role": "USER",
  "versionToken": "v1_abc123",
  "iat": 1720243200,
  "exp": 1720244100,
  "jti": "uuid-token-id"
}
```

#### Refresh Token (Payload)
```json
{
  "sub": "user123",
  "versionToken": "v1_abc123",
  "iat": 1720243200,
  "exp": 1727625600,
  "jti": "uuid-refresh-token-id"
}
```

### 2. **Redis Storage**

#### Key Structures
```
1. Token Blacklist (Logout)
   Key: "token:blacklist:{jti}"
   Value: expiry_time
   TTL: Auto-delete after token expiry

2. Version Token Cache (User Session Validation)
   Key: "user:versionToken:{userId}"
   Value: "v1_abc123"
   TTL: 24 hours (sliding expiry - update mỗi khi access)
   
   ℹ️ Note: 
   - Không cần isDeleted flag - nếu user bị delete, DB query sẽ fail
   - TTL 24 hours = inactive user tokens tự động invalid
   - Mỗi lần validate token, Redis TTL được refresh lại

3. Session Management
   Key: "user:session:{userId}"
   Value: {jti, createdAt, lastUsedAt}
   TTL: 7 days

4. Token Revocation
   Key: "user:revoked:{userId}"
   Value: [jti1, jti2, jti3]
   TTL: 7 days
```

---

## 🔄 Authentication Flow

### Login Flow
```
┌─────────────────────────────────────────────────────────┐
│ 1. Client: POST /api/auth/login                         │
│    Body: { username, password }                         │
└──────────────┬──────────────────────────────────────────┘
               │
┌──────────────▼──────────────────────────────────────────┐
│ 2. AuthService.authenticate()                           │
│    - Find user by username/email                        │
│    - Validate password (BCrypt)                         │
│    - Check if user 30 min, role, versionToken)          │
│    - Refresh Token (30 days, versionToken)──────────────┘
               │
┌──────────────▼──────────────────────────────────────────┐
│ 3. Generate Tokens                                       │
│    - Access Token (15 min, role, versionToken)          │
│    - Refresh Token (7 days, versionToken)               │
│    - JTI (unique token ID)                              │
└──────────────┬──────────────────────────────────────────┘
               │
┌──────────────▼──────────────────────────────────────────┐
│ 4. Save to Redis                                        │
│    - user:versionToken:{userId} = v1_abc123            │
│    - user:session:{userId} = {jti, timestamps}         │
│    - TTL: based on token expiry                         │
└──────────────┬──────────────────────────────────────────┘
               │
┌──────────────▼──────────────────────────────────────────┐
│ 5. Response to Client                                   │
│    { accessToken, refreshToken, expiresIn, tokenType } │
└─────────────────────────────────────────────────────────┘
```

### Token Validation Flow (Mỗi Request)
```
┌─────────────────────────────────────────────────────────┐
│ 1. JwtAuthenticationFilter intercepts request           │
│    Extract token từ header: Authorization: Bearer {token}
└──────────────┬──────────────────────────────────────────┘
               │
┌──────────────▼──────────────────────────────────────────┐
│ 2. Validate Token Signature & Expiry                    │
│    - Check JWT signature (secret key)                   │
│    - Check expiry time                                  │
└──────────────┬──────────────────────────────────────────┘
               │
        YES ✅ │
               │ NO ❌ → Throw JwtException
               ▼
┌─────────────────────────────────────────────────────────┐
│ 3. Redis Validation (versionToken check)                │
│    Key: user:versionToken:{userId}                     │
│    Compare: Redis_version == Token_version             │
└──────────────┬──────────────────────────────────────────┘
               │
        YES ✅ │
               │ NO ❌ → Token revoked/invalid
               ▼
┌─────────────────────────────────────────────────────────┐
│ 4. Check Token Blacklist (Logout)                       │
│    Key: token:blacklist:{jti}                          │
│    If exists → Token revoked                           │
└──────────────┬──────────────────────────────────────────┘
               │
        NOT IN │ 
        BLACKLIST │ YES ✅
               ▼
┌─────────────────────────────────────────────────────────┐
│ 5. Build Authentication Object                          │
│    - Username, Email, Role, Authorities                │
│    - Set in SecurityContext                            │
└──────────────┬──────────────────────────────────────────┘
               │
┌──────────────▼──────────────────────────────────────────┐
│ 6. Allow Request to proceed to Controller               │
│    Authorization check via @PreAuthorize                │
└─────────────────────────────────────────────────────────┘
```

---

## 📊 Database Schema Changes

### Users Table (Add fields)
```sql
ALTER TABLE users ADD COLUMN version_token VARCHAR(500);
-- versionToken: Used to invalidate all tokens when password changed, etc.
-- When user changes password: new versionToken = UUID
-- All old tokens with old versionToken become invalid
```

---

## 🔌 API Endpoints

### Authentication Endpoints

#### 1. Login
```
POST /api/auth/login
Content-Type: application/json

Request:
{
  "username": "john_doe",
  "password": "password123"
}

Response (200 OK):
{
  "code": 1000,
  "message": "Login successful",
  "result": {
    "accessToken": "eyJhbGciOiJIUzI1NiIs...",
    "refreshToken": "eyJhbGciOiJIUzI1NiIs...",
    "expiresIn": 1800,
    "tokenType": "Bearer",
    "user": {
      "id": 1,
      "username": "john_doe",
      "email": "john@example.com",
      "role": "USER"
    }
  },
  "timestamp": "2026-07-05T23:45:00"
}
```

#### 2. Refresh Token
```
POST /api/auth/refresh
Content-Type: application/json

Request:
{
  "refreshToken": "eyJhbGciOiJIUzI1NiIs..."
}

Response (200 OK):
{
  "code": 1000,
  "message": "Token refreshed",
  "result": {
    "accessToken": "eyJhbGciOiJIUzI1NiIs...",
    "expiresIn": 1800,
    "tokenType": "Bearer"
  },
  "timestamp": "2026-07-05T23:45:00"
}
```

#### 3. Logout (Revoke Token)
```
POST /api/auth/logout
Headers: Authorization: Bearer {accessToken}

Response (200 OK):
{
  "code": 1000,
  "message": "Logout successful",
  "result": null,
  "timestamp": "2026-07-05T23:45:00"
}

Redis: Add token to blacklist
Key: token:blacklist:{jti}
Value: expiry_time
TTL: token remaining time
```

#### 4. Change Password (Invalidate all tokens)
```
PUT /api/users/{id}/change-password
Headers: Authorization: Bearer {accessToken}

Request:
{
  "oldPassword": "password123",
  "newPassword": "newPassword456"
}

Response (200 OK):
{
  "code": 1000,
  "message": "Password changed successfully",
  "result": null,
  "timestamp": "2026-07-05T23:45:00"
}

Action:
- Update password in DB
- Generate new versionToken
- Update in Redis: user:versionToken:{userId} = new_version
- All old tokens automatically invalid
```

---

## 🔐 Protected User Endpoints

### With @PreAuthorize

```
1. GET /api/users - VIEW_ALL (ADMIN only)
   @PreAuthorize("hasRole('ADMIN')")

2. GET /api/users/{id} - VIEW_OWN (USER, ADMIN)
   @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
   - USER: chỉ xem được info của chính mình
   - ADMIN: xem được tất cả users

3. PUT /api/users/{id} - EDIT_OWN (USER, ADMIN)
   @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
   - USER: edit info của chính mình
   - ADMIN: edit bất kỳ user

4. DELETE /api/users/{id} - DELETE (ADMIN only)
   @PreAuthorize("hasRole('ADMIN')")

5. POST /api/users - CREATE (ADMIN only)
   @PreAuthorize("hasRole('ADMIN')")
```

---

## 🛠️ Implementation Stack

### Dependencies
```xml
<!-- Spring Security -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-security</artifactId>
</dependency>

<!-- JWT -->
<dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt-api</artifactId>
    <version>0.12.3</version>
</dependency>
<dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt-impl</artifactId>
    <version>0.12.3</version>
    <scope>runtime</scope>
</dependency>
<dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt-jackson</artifactId>
    <version>0.12.3</version>
    <scope>runtime</scope>
</dependency>

<!-- Redis -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-redis</artifactId>
</dependency>
<dependency>
    <groupId>redis.clients</groupId>
    <artifactId>jedis</artifactId>
</dependency>

<!-- BCrypt -->
<!-- Included in spring-security-crypto -->
```

### Classes to Create
```
1. JwtUtil
   - generateAccessToken()
   - generateRefreshToken()
   - validateToken()
   - getClaimsFromToken()
   - extractUsername()
   - extractVersionToken()

2. JwtAuthenticationFilter
   - doFilterInternal()
   - Extract token from header
   - Validate & authenticate

3. RedisTokenService
   - saveVersionToken()
   - getVersionToken()
   - addTokenToBlacklist()
   - isTokenBlacklisted()
   - invalidateUserTokens()

4. AuthService
   - authenticate()
   - refreshToken()
   - logout()
   - changePassword()

5. AuthController
   - login()
   - refresh()
   - logout()

6. UserSecurityController (Protected endpoints)
   - All user endpoints with @PreAuthorize
```

---

## 🔒 Security Best Practices

### 1. **Password Security**
- ✅ Hash dengan BCrypt (strength: 12)
- ✅ Never return password in response
- ✅ Validate password strength (min 6 chars, mix case, numbers)

### 2. **JWT Security**
- ✅ Use HS256 (HMAC-SHA256) signature
- ✅ Short expiry for Access Token (15 min)
- ✅ Long expiry for Refresh Token (7 days)
- ✅ Include JTI (unique token ID) để revoke
- ✅ Include versionToken để invalidate all tokens

### 3. **Redis Security**
- ✅ Não store sensitive data in plain text
- ✅ Use TTL để auto-expire entries
- ✅ Limit Redis connections with auth

### 4. **HTTPS & Headers**
- ✅ In production: HTTPS only
- ✅ Add CORS headers
- ✅ Add security headers (HSTS, X-Frame-Options)

### 5. **Token Validation**
- ✅ Validate token signature
- ✅ Check expiry
- ✅ Check Redis versionToken
- ✅ Check blacklist

---

## 📋 Implementation Checklist

- [ ] Add BCrypt dependency
- [ ] Add JWT (JJWT) dependency
- [ ] Add Redis dependency
- [ ] Create JwtUtil class
- [ ] Create RedisTokenService class
- [ ] Create JwtAuthenticationFilter class
- [ ] Create AuthService class
- [ ] Create AuthController class
- [ ] Update UserController với @PreAuthorize
- [ ] Configure SecurityConfiguration
- [ ] Add application-security.properties
- [ ] Add versionToken field to Users entity
- [ ] Add password hashing in UserService.createUser()
- [ ] Add password hashing in UserService.updateUser()
- [ ] Add changePassword endpoint in UserController
- [ ] Test Authentication flows
- [ ] Test Authorization (role checks)
- [ ] Test Token expiry & refresh
- [ ] Test Logout & blacklist
- [ ] Test Password change invalidates tokens

---

## 🚀 Deployment Considerations

### Production Config
```properties
# JWT
jwt.secret.key=your-super-secret-key-min-256-chars
jwt.expiration.access=1800000  # 30 minutes
jwt.expiration.refresh=2592000000  # 30 days

# Redis
spring.redis.host=your-redis-host
spring.redis.port=6379
spring.redis.password=your-redis-password
spring.redis.timeout=60000

# HTTPS
server.ssl.enabled=true
server.ssl.key-store=classpath:keystore.p12
server.ssl.key-store-password=password
server.ssl.key-store-type=PKCS12
```

---

## 📝 Notes

1. **versionToken**: Sinh ra khi:
   - User tạo mới (login lần đầu)
   - User đổi password
   - Admin reset password
   - User logout all (logout từ tất cả device)

2. **Token Blacklist**: 
   - Khi logout: add token JTI vào blacklist
   - Khi change password: invalidate all tokens bằng cách update versionToken
   - Redis TTL tự động delete khi token expired

3. **Redis Connection Pool**:
   - Min connections: 2
   - Max connections: 8
   - Test on borrow: true

4. **CORS Configuration**:
   - Allow origins: frontend URL
   - Allow methods: GET, POST, PUT, DELETE
   - Allow headers: Authorization, Content-Type
   - Allow credentials: true

---

## 🎯 Acceptance Criteria

✅ User có thể login với username/email + password
✅ Server generate Access Token (30 min) + Refresh Token (30 days)
✅ Token được validate trên mỗi request
✅ Redis store versionToken (TTL 24h, sliding expiry) để check token validity
✅ Token được thêm vào blacklist khi logout
✅ Endpoints được protect với @PreAuthorize(role)
✅ USER chỉ access được info của chính mình
✅ ADMIN access được tất cả endpoints
✅ Password change invalidate all tokens
✅ Refresh token endpoint hoạt động
✅ 404 Not Found trả về consistent response
✅ Validation errors trả về tất cả field errors
✅ Logging được thêm vào GlobalExceptionHandler

---

**Status**: 📋 Ready for implementation review
**Last Updated**: 2026-07-05
