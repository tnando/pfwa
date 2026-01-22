# Epic 1: Authentication User Stories

## Epic Overview
**Epic ID:** EPIC-001
**Epic Name:** Authentication
**Story Points:** 16
**Priority:** Must Have (MVP Critical)
**Status:** Ready for Development

### Epic Description
Implement secure user authentication system with registration, login, password reset, and session management using JWT tokens.

### Technical Stack
- Backend: Spring Security with JWT
- Database: PostgreSQL (users table with encrypted passwords)
- Email: SMTP integration for verification and password reset
- Security: BCrypt password hashing, rate limiting, HTTPS

---

## User Stories

### AUTH-001: User Registration
**Priority:** Must Have
**Story Points:** 5
**Status:** Ready

#### User Story
As a **new user**,
I want to **register for an account with my email and password**,
So that **I can access the personal finance application and track my finances**.

#### Acceptance Criteria

**Given** I am on the registration page
**When** I enter a valid email address, strong password, and confirm password
**Then** my account is created and I receive a verification email

**Given** I am on the registration page
**When** I enter an email that is already registered
**Then** I see an error message "Email already exists"

**Given** I am on the registration page
**When** I enter a password that doesn't meet security requirements
**Then** I see specific validation errors for:
- Minimum 8 characters
- At least one uppercase letter
- At least one lowercase letter
- At least one number
- At least one special character

**Given** I am on the registration page
**When** I enter mismatched passwords in password and confirm password fields
**Then** I see an error message "Passwords do not match"

**Given** I am on the registration page
**When** I submit the form with invalid email format
**Then** I see an error message "Invalid email format"

**Given** I have attempted to register 5 times in 1 minute
**When** I try to register again
**Then** I see an error message "Too many registration attempts. Please try again later."

#### Technical Requirements
- Password hashed using BCrypt (strength 12)
- Email validation using regex pattern
- Rate limiting: 5 attempts per minute per IP
- Database constraint: unique email (case-insensitive)
- Trim whitespace from email input
- Store user with status: PENDING_VERIFICATION

#### API Endpoint
```
POST /api/v1/auth/register
Request Body:
{
  "email": "user@example.com",
  "password": "SecurePass123!",
  "confirmPassword": "SecurePass123!"
}

Response: 201 Created
{
  "message": "Registration successful. Please check your email to verify your account.",
  "userId": "uuid"
}
```

---

### AUTH-002: Email Verification
**Priority:** Should Have
**Story Points:** 3
**Status:** Ready

#### User Story
As a **newly registered user**,
I want to **verify my email address via a link sent to my inbox**,
So that **the system confirms I own the email and can recover my account if needed**.

#### Acceptance Criteria

**Given** I have just registered for an account
**When** I check my email inbox
**Then** I receive an email with a verification link within 2 minutes

**Given** I received a verification email
**When** I click the verification link
**Then** my account status changes to ACTIVE and I am redirected to login page with success message

**Given** I received a verification email
**When** I click the verification link after 24 hours
**Then** I see an error message "Verification link has expired. Please request a new one."

**Given** I have not verified my email
**When** I try to log in
**Then** I see an error message "Please verify your email before logging in"

**Given** my verification link has expired
**When** I request a new verification email
**Then** a new verification link is sent to my email (max 3 resends per hour)

#### Technical Requirements
- Verification token: UUID, stored in database with expiration (24 hours)
- Email template with professional styling
- Link format: `https://app.pfwa.com/verify?token={token}`
- Rate limiting on resend: 3 emails per hour per user
- Delete expired tokens via scheduled job (daily)

#### API Endpoints
```
GET /api/v1/auth/verify?token={token}
Response: 200 OK
{
  "message": "Email verified successfully. You can now log in."
}

POST /api/v1/auth/resend-verification
Request Body:
{
  "email": "user@example.com"
}
Response: 200 OK
{
  "message": "Verification email sent."
}
```

---

### AUTH-003: User Login
**Priority:** Must Have
**Story Points:** 3
**Status:** Ready

#### User Story
As a **registered user**,
I want to **log in with my email and password**,
So that **I can securely access my financial data**.

#### Acceptance Criteria

**Given** I am on the login page with a verified account
**When** I enter my correct email and password
**Then** I am logged in and redirected to the dashboard with access and refresh tokens stored

**Given** I am on the login page
**When** I enter an incorrect password
**Then** I see an error message "Invalid email or password" (no specific indication)

**Given** I am on the login page
**When** I enter an email that doesn't exist
**Then** I see an error message "Invalid email or password" (same as incorrect password)

**Given** I have entered wrong credentials 5 times in 15 minutes
**When** I try to log in again
**Then** my account is locked for 30 minutes and I see "Account temporarily locked due to too many failed attempts"

**Given** I have a verified account
**When** I successfully log in
**Then** I receive:
- Access token (JWT, expires in 15 minutes)
- Refresh token (expires in 7 days)
- User profile data (id, email, name)

**Given** I am on the login page
**When** I check the "Remember me" option
**Then** my refresh token expires in 30 days instead of 7 days

#### Technical Requirements
- JWT access token payload: userId, email, roles, exp (15 min)
- JWT refresh token: stored in database with user association
- Secure httpOnly cookies for tokens
- Failed login tracking: IP + email combination
- Account lockout: 5 failed attempts in 15 minutes = 30-minute lock
- Password comparison using BCrypt

#### API Endpoint
```
POST /api/v1/auth/login
Request Body:
{
  "email": "user@example.com",
  "password": "SecurePass123!",
  "rememberMe": false
}

Response: 200 OK
Headers:
  Set-Cookie: accessToken={jwt}; HttpOnly; Secure; SameSite=Strict
  Set-Cookie: refreshToken={jwt}; HttpOnly; Secure; SameSite=Strict

Body:
{
  "user": {
    "id": "uuid",
    "email": "user@example.com",
    "firstName": "John",
    "lastName": "Doe"
  },
  "expiresIn": 900
}
```

---

### AUTH-004: Token Refresh
**Priority:** Must Have
**Story Points:** 2
**Status:** Ready

#### User Story
As a **logged-in user**,
I want **my session to be automatically renewed before my access token expires**,
So that **I don't have to log in repeatedly while actively using the app**.

#### Acceptance Criteria

**Given** I am logged in and my access token is about to expire (within 2 minutes)
**When** I make an API request
**Then** the frontend automatically requests a new access token using my refresh token

**Given** I have a valid refresh token
**When** I request a new access token
**Then** I receive a new access token with a fresh 15-minute expiration

**Given** my refresh token has expired
**When** I try to refresh my access token
**Then** I am logged out and redirected to login page

**Given** I am logged in on multiple devices
**When** I refresh my token on one device
**Then** the refresh token is rotated and old token is invalidated (single-use)

**Given** my refresh token has been used already
**When** I try to use it again
**Then** I receive an error and all sessions for my account are invalidated (security breach detected)

#### Technical Requirements
- Refresh token rotation: each refresh generates new access + refresh tokens
- Store refresh token family ID to detect reuse attacks
- Frontend: auto-refresh when access token has < 2 minutes remaining
- Blacklist revoked refresh tokens until their expiration
- Log all token refresh events for security audit

#### API Endpoint
```
POST /api/v1/auth/refresh
Headers:
  Cookie: refreshToken={jwt}

Response: 200 OK
Headers:
  Set-Cookie: accessToken={newJwt}; HttpOnly; Secure
  Set-Cookie: refreshToken={newRefreshJwt}; HttpOnly; Secure

Body:
{
  "expiresIn": 900
}
```

---

### AUTH-005: User Logout
**Priority:** Must Have
**Story Points:** 1
**Status:** Ready

#### User Story
As a **logged-in user**,
I want to **log out of my account**,
So that **my session is terminated and my account is secure on shared devices**.

#### Acceptance Criteria

**Given** I am logged in
**When** I click the logout button
**Then** my tokens are invalidated, cleared from browser, and I am redirected to login page

**Given** I am logged in on multiple devices
**When** I log out from one device
**Then** only that device's session is terminated (other devices remain logged in)

**Given** I log out
**When** I try to make an authenticated API request with the old token
**Then** I receive a 401 Unauthorized error

**Given** I log out
**When** I navigate to a protected route
**Then** I am redirected to the login page

#### Technical Requirements
- Add refresh token to blacklist (Redis or database)
- Clear httpOnly cookies
- Frontend: clear all auth state from memory and local storage
- Blacklist persists until token's natural expiration
- Log logout events for audit trail

#### API Endpoint
```
POST /api/v1/auth/logout
Headers:
  Cookie: refreshToken={jwt}

Response: 200 OK
Headers:
  Set-Cookie: accessToken=; Max-Age=0
  Set-Cookie: refreshToken=; Max-Age=0

Body:
{
  "message": "Logged out successfully"
}
```

---

### AUTH-006: Logout All Sessions
**Priority:** Should Have
**Story Points:** 1
**Status:** Ready

#### User Story
As a **logged-in user**,
I want to **log out of all devices at once**,
So that **I can secure my account if I suspect unauthorized access**.

#### Acceptance Criteria

**Given** I am logged in on multiple devices
**When** I click "Logout all devices" in my account settings
**Then** all active sessions are terminated across all devices

**Given** I initiated logout all sessions
**When** I am on another device where I was logged in
**Then** I am automatically logged out and must log in again

**Given** I log out all sessions
**When** I try to use any old refresh tokens
**Then** all are invalidated and I receive 401 Unauthorized

#### Technical Requirements
- Increment user's token version number in database
- All tokens include version number in payload
- Token validation checks version matches current user version
- Send email notification when all sessions are logged out

#### API Endpoint
```
POST /api/v1/auth/logout-all
Headers:
  Authorization: Bearer {accessToken}

Response: 200 OK
{
  "message": "All sessions have been terminated. You will need to log in again on all devices."
}
```

---

### AUTH-007: Password Reset Request
**Priority:** Must Have
**Story Points:** 2
**Status:** Ready

#### User Story
As a **user who forgot their password**,
I want to **request a password reset link via email**,
So that **I can regain access to my account securely**.

#### Acceptance Criteria

**Given** I am on the forgot password page
**When** I enter my registered email address
**Then** I receive an email with a password reset link within 2 minutes

**Given** I am on the forgot password page
**When** I enter an email that is not registered
**Then** I see a generic success message (to prevent email enumeration): "If the email exists, you will receive a password reset link."

**Given** I requested a password reset
**When** I request another reset within 15 minutes
**Then** the previous token is invalidated and only the new link works

**Given** I request password resets
**When** I have requested 3 times in 1 hour
**Then** I am rate-limited and see "Too many password reset requests. Please try again later."

**Given** I receive a password reset email
**When** I check the email
**Then** the link expires in 1 hour and this is clearly stated in the email

#### Technical Requirements
- Reset token: cryptographically secure random token (32 bytes)
- Store hashed token in database with user association and expiration
- Rate limiting: 3 requests per hour per email
- Email template with clear instructions and expiration notice
- Link format: `https://app.pfwa.com/reset-password?token={token}`
- Do not reveal if email exists (security best practice)

#### API Endpoint
```
POST /api/v1/auth/forgot-password
Request Body:
{
  "email": "user@example.com"
}

Response: 200 OK (always, even if email doesn't exist)
{
  "message": "If the email exists in our system, you will receive a password reset link."
}
```

---

### AUTH-008: Password Reset Completion
**Priority:** Must Have
**Story Points:** 2
**Status:** Ready

#### User Story
As a **user who requested a password reset**,
I want to **set a new password using the reset link**,
So that **I can access my account with a new secure password**.

#### Acceptance Criteria

**Given** I clicked a valid password reset link
**When** I arrive at the reset password page
**Then** I see a form to enter and confirm my new password

**Given** I am on the reset password page
**When** I enter a valid new password and confirmation
**Then** my password is updated, all existing sessions are logged out, and I am redirected to login

**Given** I am on the reset password page
**When** I enter a password that doesn't meet security requirements
**Then** I see the same validation errors as registration

**Given** I received a reset link
**When** I try to use it after 1 hour
**Then** I see "Password reset link has expired. Please request a new one."

**Given** I successfully reset my password
**When** I try to use the same reset link again
**Then** I see "This password reset link has already been used."

**Given** I reset my password
**When** the password is updated
**Then** I receive a confirmation email that my password was changed

#### Technical Requirements
- Validate token exists, not expired, and not already used
- Hash new password with BCrypt (strength 12)
- Invalidate all refresh tokens (force re-login on all devices)
- Mark reset token as used
- Send confirmation email after successful reset
- Security notification email includes timestamp and IP address

#### API Endpoint
```
POST /api/v1/auth/reset-password
Request Body:
{
  "token": "reset-token-string",
  "newPassword": "NewSecurePass123!",
  "confirmPassword": "NewSecurePass123!"
}

Response: 200 OK
{
  "message": "Password has been reset successfully. Please log in with your new password."
}
```

---

### AUTH-009: Session Management
**Priority:** Should Have
**Story Points:** 2
**Status:** Ready

#### User Story
As a **logged-in user**,
I want to **view and manage all my active sessions**,
So that **I can monitor where I'm logged in and revoke access from unknown devices**.

#### Acceptance Criteria

**Given** I am logged in
**When** I navigate to account settings > sessions
**Then** I see a list of all active sessions with:
- Device type (browser, OS)
- Location (city, country based on IP)
- Last active timestamp
- Current session indicator

**Given** I am viewing my active sessions
**When** I see a session I don't recognize
**Then** I can click "Revoke" to terminate that specific session

**Given** I revoke a session
**When** that session tries to make an API request
**Then** it receives 401 Unauthorized and is forced to log in again

**Given** I have multiple sessions
**When** a session is inactive for 7 days
**Then** it is automatically revoked

#### Technical Requirements
- Store session metadata: refresh token ID, user agent, IP address, created at, last used at
- Parse user agent for device/browser information
- Use IP geolocation API for approximate location (optional)
- Scheduled job: cleanup sessions inactive for 7+ days
- Limit: maximum 5 active sessions per user

#### API Endpoints
```
GET /api/v1/auth/sessions
Response: 200 OK
{
  "sessions": [
    {
      "id": "session-uuid",
      "deviceType": "Chrome 120 on macOS",
      "location": "San Francisco, US",
      "lastActive": "2026-01-21T10:30:00Z",
      "isCurrent": true
    }
  ]
}

DELETE /api/v1/auth/sessions/{sessionId}
Response: 200 OK
{
  "message": "Session revoked successfully"
}
```

---

## Story Summary

| Story ID | Title | Priority | Story Points | Dependencies |
|----------|-------|----------|--------------|--------------|
| AUTH-001 | User Registration | Must Have | 5 | None |
| AUTH-002 | Email Verification | Should Have | 3 | AUTH-001 |
| AUTH-003 | User Login | Must Have | 3 | AUTH-001 |
| AUTH-004 | Token Refresh | Must Have | 2 | AUTH-003 |
| AUTH-005 | User Logout | Must Have | 1 | AUTH-003 |
| AUTH-006 | Logout All Sessions | Should Have | 1 | AUTH-005 |
| AUTH-007 | Password Reset Request | Must Have | 2 | AUTH-001 |
| AUTH-008 | Password Reset Completion | Must Have | 2 | AUTH-007 |
| AUTH-009 | Session Management | Should Have | 2 | AUTH-004 |

**Total Story Points:** 21 (MVP Must-Have: 15, Should-Have: 6)

---

## MoSCoW Prioritization

### Must Have (MVP Critical)
- AUTH-001: User Registration
- AUTH-003: User Login
- AUTH-004: Token Refresh
- AUTH-005: User Logout
- AUTH-007: Password Reset Request
- AUTH-008: Password Reset Completion

### Should Have (Important for Security)
- AUTH-002: Email Verification
- AUTH-006: Logout All Sessions
- AUTH-009: Session Management

### Could Have (Post-MVP)
- Social login (Google, GitHub)
- Two-factor authentication (2FA)
- Passwordless login (magic links)
- Login activity notifications

### Won't Have (Not in Scope)
- Biometric authentication
- Single Sign-On (SSO) integration
- OAuth provider capabilities

---

## Security Considerations

### Password Security
- Minimum requirements: 8 chars, uppercase, lowercase, number, special char
- BCrypt hashing with strength 12
- Password history: prevent reuse of last 3 passwords (future enhancement)

### Rate Limiting
| Endpoint | Limit | Window |
|----------|-------|--------|
| Registration | 5 attempts | 1 minute |
| Login | 5 failed attempts | 15 minutes (30 min lockout) |
| Password Reset Request | 3 requests | 1 hour |
| Email Verification Resend | 3 emails | 1 hour |

### Token Security
- Access token: 15-minute expiration
- Refresh token: 7-day expiration (30 days with "remember me")
- Refresh token rotation on every use
- Token reuse detection triggers full account logout
- HttpOnly, Secure, SameSite=Strict cookies

### Account Security
- Email verification required before login (should have)
- Account lockout after 5 failed login attempts
- Password reset tokens expire in 1 hour
- All sessions invalidated on password change
- Security notification emails for critical actions

### Data Protection
- Never expose whether email exists (password reset, login errors)
- Mask email in responses (u***@example.com)
- HTTPS enforced for all endpoints
- No sensitive data in JWT payload
- Audit logging for all authentication events

---

## Database Schema

### users table
```sql
CREATE TABLE users (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  email VARCHAR(255) UNIQUE NOT NULL,
  password_hash VARCHAR(255) NOT NULL,
  first_name VARCHAR(100),
  last_name VARCHAR(100),
  status VARCHAR(50) NOT NULL DEFAULT 'PENDING_VERIFICATION',
  token_version INT NOT NULL DEFAULT 1,
  failed_login_attempts INT DEFAULT 0,
  locked_until TIMESTAMP,
  created_at TIMESTAMP NOT NULL DEFAULT NOW(),
  updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_users_email ON users(email);
```

### verification_tokens table
```sql
CREATE TABLE verification_tokens (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  token VARCHAR(255) UNIQUE NOT NULL,
  type VARCHAR(50) NOT NULL, -- EMAIL_VERIFICATION, PASSWORD_RESET
  expires_at TIMESTAMP NOT NULL,
  used_at TIMESTAMP,
  created_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_verification_tokens_token ON verification_tokens(token);
CREATE INDEX idx_verification_tokens_expires ON verification_tokens(expires_at);
```

### refresh_tokens table
```sql
CREATE TABLE refresh_tokens (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  token_hash VARCHAR(255) UNIQUE NOT NULL,
  family_id UUID NOT NULL,
  user_agent TEXT,
  ip_address VARCHAR(45),
  expires_at TIMESTAMP NOT NULL,
  last_used_at TIMESTAMP NOT NULL DEFAULT NOW(),
  revoked_at TIMESTAMP,
  created_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_refresh_tokens_user ON refresh_tokens(user_id);
CREATE INDEX idx_refresh_tokens_family ON refresh_tokens(family_id);
CREATE INDEX idx_refresh_tokens_expires ON refresh_tokens(expires_at);
```

---

## Testing Requirements

### Unit Tests
- Password validation logic
- JWT token generation and validation
- BCrypt password hashing
- Token expiration logic
- Rate limiting counters

### Integration Tests
- Full registration flow
- Login with correct/incorrect credentials
- Token refresh flow
- Password reset flow
- Account lockout mechanism
- Email sending (mocked)

### E2E Tests (Cypress)
- User can register, verify email, and login
- User can reset forgotten password
- User cannot login with wrong password
- Session expires and refreshes correctly
- User can logout and all tokens are cleared

### Security Tests
- SQL injection attempts fail
- XSS attempts are sanitized
- CSRF protection with SameSite cookies
- Rate limiting blocks excessive requests
- Token reuse is detected and blocked

---

## Definition of Done

Each story is considered complete when:

1. **Code Complete**
   - Backend API implemented with Spring Security
   - Frontend forms and auth context implemented
   - All validation rules enforced

2. **Tests Pass**
   - Unit tests: >80% coverage
   - Integration tests: all happy and sad paths
   - E2E tests: critical user flows

3. **Security Review**
   - OWASP Top 10 considerations addressed
   - Rate limiting implemented
   - Tokens properly secured

4. **Documentation**
   - API endpoints documented in OpenAPI/Swagger
   - README updated with auth setup instructions
   - Security considerations documented

5. **Code Review**
   - PR approved by at least one team member
   - No critical or high-severity issues from static analysis

6. **Acceptance Criteria Met**
   - All Given/When/Then scenarios pass
   - Product Owner accepts the story

---

## Sprint Planning Recommendation

### Sprint 1 (Must Have Core - 10 points)
- AUTH-001: User Registration (5)
- AUTH-003: User Login (3)
- AUTH-004: Token Refresh (2)

### Sprint 2 (Must Have Completion - 5 points + Should Have - 3 points)
- AUTH-005: User Logout (1)
- AUTH-007: Password Reset Request (2)
- AUTH-008: Password Reset Completion (2)
- AUTH-002: Email Verification (3)

### Sprint 3 (Should Have - 3 points)
- AUTH-006: Logout All Sessions (1)
- AUTH-009: Session Management (2)

---

## Notes for Development Team

1. **Environment Variables Required:**
   - `JWT_SECRET` - Strong random string for signing tokens
   - `JWT_ACCESS_EXPIRATION` - Access token expiration (900 seconds)
   - `JWT_REFRESH_EXPIRATION` - Refresh token expiration (604800 seconds)
   - `SMTP_HOST`, `SMTP_PORT`, `SMTP_USER`, `SMTP_PASSWORD` - Email configuration
   - `FRONTEND_URL` - For email links
   - `BCRYPT_STRENGTH` - Password hashing rounds (12)

2. **Third-party Libraries:**
   - Spring Security
   - jjwt (JWT for Java)
   - Spring Boot Starter Mail
   - Redis (optional, for token blacklist)

3. **Frontend Libraries:**
   - React Hook Form (form validation)
   - Axios (HTTP client with interceptors for token refresh)
   - React Router (protected routes)
   - JWT Decode (parse token expiration)

4. **Infrastructure:**
   - SMTP server or service (SendGrid, Mailgun, AWS SES)
   - Redis (recommended for rate limiting and token blacklist)
   - HTTPS certificate (Let's Encrypt for production)

---

## References

- [OWASP Authentication Cheat Sheet](https://cheatsheetseries.owasp.org/cheatsheets/Authentication_Cheat_Sheet.html)
- [JWT Best Practices](https://datatracker.ietf.org/doc/html/rfc8725)
- [Spring Security Documentation](https://spring.io/projects/spring-security)
- PFWA Architecture Decision Records (ADRs)
