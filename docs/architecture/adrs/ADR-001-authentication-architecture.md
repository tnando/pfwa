# ADR-001: Authentication Architecture for PFWA

## Status

**Accepted**

Date: 2026-01-21

## Context

The Personal Finance Web Application (PFWA) requires a secure authentication system to protect sensitive financial data. Users need to register, log in, manage sessions, and recover passwords while the system must protect against common attack vectors.

### Requirements

1. **Security**: Protect against session hijacking, CSRF, XSS, and credential theft
2. **Scalability**: Support horizontal scaling of backend services
3. **User Experience**: Seamless session management without frequent re-authentication
4. **Compliance**: Follow security best practices (OWASP guidelines)
5. **Multi-device Support**: Allow users to be logged in on multiple devices

### Options Considered

#### Option 1: Session-based Authentication (Server-side Sessions)

Store session data in server memory or a shared session store (Redis).

**Pros:**
- Simple to implement
- Easy to invalidate sessions
- Session data stays on server

**Cons:**
- Requires sticky sessions or shared session store for horizontal scaling
- Server-side state management overhead
- Not ideal for microservices architecture

#### Option 2: JWT with LocalStorage

Store JWT access tokens in browser localStorage.

**Pros:**
- Stateless backend, easy horizontal scaling
- Simple frontend implementation
- Works well with APIs

**Cons:**
- Vulnerable to XSS attacks (JavaScript can access tokens)
- Cannot easily revoke tokens
- No protection if token is stolen

#### Option 3: JWT with HttpOnly Cookies + Refresh Token Rotation

Store JWT access tokens in HttpOnly cookies with short expiration. Use refresh tokens with rotation for session renewal.

**Pros:**
- Protected from XSS (JavaScript cannot access HttpOnly cookies)
- CSRF protection via SameSite cookies
- Stateless access token validation
- Revocable via refresh token invalidation
- Good balance of security and scalability

**Cons:**
- More complex implementation
- Requires cookie handling on frontend
- Cross-origin requests require CORS configuration

## Decision

We will implement **JWT-based authentication with HttpOnly cookies and refresh token rotation** (Option 3).

### Architecture Overview

```
+-------------+     +------------------+     +------------+
|   Browser   |     |   Spring Boot    |     | PostgreSQL |
|  (React)    |     |   Backend        |     |            |
+------+------+     +--------+---------+     +-----+------+
       |                     |                      |
       | 1. POST /login      |                      |
       | {email, password}   |                      |
       +-------------------->|                      |
       |                     | 2. Validate          |
       |                     |    credentials       |
       |                     +--------------------->|
       |                     |<--------------------+
       |                     |                      |
       |                     | 3. Generate tokens   |
       |                     |    Store refresh     |
       |                     +--------------------->|
       |                     |                      |
       | 4. Set-Cookie:      |                      |
       |    accessToken      |                      |
       |    refreshToken     |                      |
       |<--------------------+                      |
       |                     |                      |
       | 5. GET /api/...     |                      |
       | Cookie: accessToken |                      |
       +-------------------->|                      |
       |                     | 6. Validate JWT      |
       |                     |    (stateless)       |
       |                     |                      |
       | 7. Response         |                      |
       |<--------------------+                      |
```

### Token Architecture

#### Access Token (JWT)

- **Storage**: HttpOnly, Secure, SameSite=Strict cookie
- **Expiration**: 15 minutes
- **Content**: User ID, email, roles, token version, issued at, expiration
- **Validation**: Stateless signature verification
- **Algorithm**: HS256 (HMAC-SHA256) with rotating secrets in production

```json
{
  "sub": "550e8400-e29b-41d4-a716-446655440000",
  "email": "user@example.com",
  "roles": ["USER"],
  "tokenVersion": 1,
  "iat": 1705833000,
  "exp": 1705833900
}
```

#### Refresh Token

- **Storage**: HttpOnly, Secure, SameSite=Strict cookie + database record
- **Expiration**: 7 days (30 days with "Remember Me")
- **Content**: Opaque token ID (UUID) referencing database record
- **Validation**: Database lookup required
- **Rotation**: New token issued on each refresh, old token invalidated

### Refresh Token Rotation

To detect and prevent token theft, we implement refresh token rotation with family tracking:

```
Initial Login:
  - Generate access token + refresh token R1
  - Store R1 with family_id=F1 in database

First Refresh:
  - Client sends R1
  - Server validates R1, marks as used
  - Server generates new access token + refresh token R2 (same family F1)
  - Store R2, return to client

Token Reuse Attack Detection:
  - Attacker tries to use stolen R1
  - Server detects R1 was already used
  - Server invalidates ALL tokens in family F1
  - User is forced to re-authenticate
  - Security alert email sent to user
```

### Cookie Configuration

```java
// Access Token Cookie
ResponseCookie.from("accessToken", jwtToken)
    .httpOnly(true)
    .secure(true)
    .sameSite("Strict")
    .path("/")
    .maxAge(Duration.ofMinutes(15))
    .build();

// Refresh Token Cookie
ResponseCookie.from("refreshToken", refreshTokenId)
    .httpOnly(true)
    .secure(true)
    .sameSite("Strict")
    .path("/api/v1/auth")  // Restricted path
    .maxAge(Duration.ofDays(7))
    .build();
```

### Security Headers

```
Strict-Transport-Security: max-age=31536000; includeSubDomains
X-Content-Type-Options: nosniff
X-Frame-Options: DENY
X-XSS-Protection: 1; mode=block
Content-Security-Policy: default-src 'self'
Referrer-Policy: strict-origin-when-cross-origin
```

### Password Security

- **Hashing Algorithm**: BCrypt with strength factor 12
- **Validation Requirements**:
  - Minimum 8 characters
  - At least one uppercase letter (A-Z)
  - At least one lowercase letter (a-z)
  - At least one digit (0-9)
  - At least one special character (!@#$%^&*()_+-=[]{}|;:,.<>?)
  - Maximum 128 characters

```java
@Bean
public PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder(12);
}
```

### Rate Limiting Strategy

| Endpoint | Limit | Window | Lockout |
|----------|-------|--------|---------|
| POST /register | 5 attempts | 1 minute | No account, just delay |
| POST /login | 5 failures | 15 minutes | 30 minute account lock |
| POST /password/forgot | 3 requests | 1 hour | Delay only |
| POST /verify-email/resend | 3 emails | 1 hour | Delay only |
| POST /refresh | 30 requests | 1 minute | Delay only |

Implementation using Bucket4j or Redis-based rate limiting:

```java
@RateLimit(
    requests = 5,
    duration = 1,
    timeUnit = TimeUnit.MINUTES,
    key = "#request.remoteAddr"
)
public ResponseEntity<?> register(...) { }
```

### Account Lockout Mechanism

```java
// On failed login
user.incrementFailedAttempts();
if (user.getFailedAttempts() >= MAX_FAILED_ATTEMPTS) {
    user.setLockedUntil(Instant.now().plus(30, ChronoUnit.MINUTES));
    // Send security alert email
}

// On successful login
user.setFailedAttempts(0);
user.setLockedUntil(null);
```

### Token Version for Global Logout

Each user has a `token_version` field that is included in the JWT. When a user logs out of all sessions or changes their password:

1. Increment `token_version` in database
2. All existing JWTs become invalid (version mismatch)
3. User must re-authenticate on all devices

```java
// JWT validation
Claims claims = Jwts.parser().setSigningKey(key).parseClaimsJws(token).getBody();
User user = userRepository.findById(claims.getSubject());
if (claims.get("tokenVersion") != user.getTokenVersion()) {
    throw new InvalidTokenException("Token has been revoked");
}
```

### Email Verification Flow

1. User registers with email and password
2. Account created with status `PENDING_VERIFICATION`
3. Verification token (UUID) generated and stored with 24-hour expiration
4. Email sent with verification link: `https://app.pfwa.com/verify?token={token}`
5. User clicks link, frontend calls `POST /verify-email` with token
6. Backend validates token, updates user status to `ACTIVE`
7. User can now log in

### Password Reset Flow

1. User requests reset via `POST /password/forgot`
2. If email exists, generate secure random token (32 bytes, hex-encoded)
3. Store hashed token with 1-hour expiration
4. Send email with reset link: `https://app.pfwa.com/reset-password?token={token}`
5. User enters new password, frontend calls `POST /password/reset`
6. Backend validates token, updates password, invalidates all sessions
7. Send confirmation email with timestamp and IP address

### Database Schema

```sql
-- Core user table
CREATE TABLE users (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    email VARCHAR(255) UNIQUE NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    first_name VARCHAR(100),
    last_name VARCHAR(100),
    status VARCHAR(50) NOT NULL DEFAULT 'PENDING_VERIFICATION',
    token_version INTEGER NOT NULL DEFAULT 1,
    failed_login_attempts INTEGER DEFAULT 0,
    locked_until TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

-- Refresh tokens with rotation tracking
CREATE TABLE refresh_tokens (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    token_hash VARCHAR(255) UNIQUE NOT NULL,
    family_id UUID NOT NULL,
    user_agent TEXT,
    ip_address VARCHAR(45),
    expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
    last_used_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    revoked_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

-- Verification and reset tokens
CREATE TABLE verification_tokens (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    token_hash VARCHAR(255) UNIQUE NOT NULL,
    type VARCHAR(50) NOT NULL CHECK (type IN ('EMAIL_VERIFICATION', 'PASSWORD_RESET')),
    expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
    used_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);
```

### Frontend Integration

The React frontend should:

1. **Axios Interceptor**: Automatically refresh tokens when access token expires

```typescript
axios.interceptors.response.use(
    (response) => response,
    async (error) => {
        if (error.response?.status === 401 && !error.config._retry) {
            error.config._retry = true;
            await axios.post('/api/v1/auth/refresh');
            return axios(error.config);
        }
        return Promise.reject(error);
    }
);
```

2. **Proactive Refresh**: Check token expiration before it expires

```typescript
const checkTokenExpiration = () => {
    const expiresAt = getExpirationFromCookie();
    const timeUntilExpiry = expiresAt - Date.now();

    if (timeUntilExpiry < 2 * 60 * 1000) { // 2 minutes
        refreshToken();
    }
};
```

3. **Auth Context**: Manage authentication state

```typescript
interface AuthContextType {
    user: User | null;
    isAuthenticated: boolean;
    isLoading: boolean;
    login: (email: string, password: string) => Promise<void>;
    logout: () => Promise<void>;
    register: (data: RegisterData) => Promise<void>;
}
```

## Consequences

### Positive

1. **Strong Security**: HttpOnly cookies protect against XSS, SameSite protects against CSRF
2. **Horizontal Scalability**: Stateless access token validation allows any server to handle requests
3. **Revocability**: Refresh token rotation enables session invalidation
4. **Attack Detection**: Token reuse detection catches and mitigates token theft
5. **User Experience**: Automatic token refresh provides seamless experience
6. **Audit Trail**: All auth events logged for security monitoring

### Negative

1. **Complexity**: More complex than simple session-based auth
2. **Database Dependency**: Refresh token validation requires database lookup
3. **Cookie Limitations**: Must handle CORS properly for cross-origin requests
4. **Clock Synchronization**: JWT validation depends on accurate server clocks

### Risks and Mitigations

| Risk | Mitigation |
|------|------------|
| JWT secret compromise | Use secret rotation, monitor for anomalies |
| Database token table growth | Scheduled cleanup of expired tokens |
| Refresh token theft | Token rotation, family-based invalidation |
| Brute force attacks | Rate limiting, account lockout |
| Email enumeration | Generic error messages, rate limiting |

## Implementation Notes

### Dependencies

```xml
<!-- pom.xml -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-security</artifactId>
</dependency>
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
```

### Environment Variables

```properties
# JWT Configuration
JWT_SECRET=your-256-bit-secret-key-here-minimum-32-characters
JWT_ACCESS_EXPIRATION_MINUTES=15
JWT_REFRESH_EXPIRATION_DAYS=7
JWT_REFRESH_REMEMBER_ME_DAYS=30

# Security
BCRYPT_STRENGTH=12
MAX_FAILED_LOGIN_ATTEMPTS=5
ACCOUNT_LOCKOUT_MINUTES=30

# Email
SMTP_HOST=smtp.example.com
SMTP_PORT=587
SMTP_USER=noreply@pfwa.com
SMTP_PASSWORD=your-smtp-password
FRONTEND_URL=https://app.pfwa.com
```

### Testing Strategy

1. **Unit Tests**: Token generation, validation, password encoding
2. **Integration Tests**: Full auth flows, rate limiting, lockout
3. **Security Tests**: SQL injection, XSS, CSRF, token manipulation
4. **E2E Tests**: Complete user journeys through Cypress

## References

- [OWASP Authentication Cheat Sheet](https://cheatsheetseries.owasp.org/cheatsheets/Authentication_Cheat_Sheet.html)
- [OWASP Session Management Cheat Sheet](https://cheatsheetseries.owasp.org/cheatsheets/Session_Management_Cheat_Sheet.html)
- [JWT Best Practices (RFC 8725)](https://datatracker.ietf.org/doc/html/rfc8725)
- [Spring Security Reference](https://docs.spring.io/spring-security/reference/)
- [Refresh Token Rotation](https://auth0.com/docs/secure/tokens/refresh-tokens/refresh-token-rotation)

## Changelog

| Date | Author | Description |
|------|--------|-------------|
| 2026-01-21 | Architecture Team | Initial version |
