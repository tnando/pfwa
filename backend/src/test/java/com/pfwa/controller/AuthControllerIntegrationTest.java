package com.pfwa.controller;

import com.pfwa.IntegrationTestBase;
import com.pfwa.dto.auth.ForgotPasswordRequest;
import com.pfwa.dto.auth.LoginRequest;
import com.pfwa.dto.auth.RegisterRequest;
import com.pfwa.dto.auth.ResendVerificationRequest;
import com.pfwa.dto.auth.ResetPasswordRequest;
import com.pfwa.dto.auth.VerifyEmailRequest;
import com.pfwa.entity.RefreshToken;
import com.pfwa.entity.User;
import com.pfwa.entity.VerificationToken;
import com.pfwa.service.TokenService;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integration tests for AuthController.
 * Tests authentication endpoints with real database and Spring Security.
 */
@DisplayName("AuthController Integration Tests")
class AuthControllerIntegrationTest extends IntegrationTestBase {

    // ==================== Registration Tests ====================

    @Nested
    @DisplayName("POST /auth/register - Registration Flow")
    class RegistrationTests {

        @Test
        @DisplayName("Should return 201 CREATED on successful registration")
        void register_success_returns201() throws Exception {
            // Given
            RegisterRequest request = new RegisterRequest(
                    "newuser@example.com",
                    TEST_PASSWORD,
                    TEST_PASSWORD
            );

            // When/Then
            mockMvc.perform(post("/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.userId").isNotEmpty())
                    .andExpect(jsonPath("$.message").value(
                            "Registration successful. Please check your email to verify your account."
                    ));

            // Verify user was created in database
            Optional<User> savedUser = userRepository.findByEmail("newuser@example.com");
            assertThat(savedUser).isPresent();
            assertThat(savedUser.get().isEmailVerified()).isFalse();
        }

        @Test
        @DisplayName("Should return 409 CONFLICT when email already exists")
        void register_duplicateEmail_returns409() throws Exception {
            // Given - create existing user
            createVerifiedUser(TEST_EMAIL, TEST_PASSWORD);

            RegisterRequest request = new RegisterRequest(
                    TEST_EMAIL,
                    TEST_PASSWORD,
                    TEST_PASSWORD
            );

            // When/Then
            mockMvc.perform(post("/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.error").value("CONFLICT"))
                    .andExpect(jsonPath("$.message").value("Email already exists"));
        }

        @Test
        @DisplayName("Should return 400 BAD REQUEST for invalid email format")
        void register_invalidEmail_returns400() throws Exception {
            // Given
            RegisterRequest request = new RegisterRequest(
                    "invalid-email",
                    TEST_PASSWORD,
                    TEST_PASSWORD
            );

            // When/Then
            mockMvc.perform(post("/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"));
        }

        @Test
        @DisplayName("Should return 400 BAD REQUEST for weak password - too short")
        void register_weakPassword_tooShort_returns400() throws Exception {
            // Given
            RegisterRequest request = new RegisterRequest(
                    "user@example.com",
                    "Short1!",  // Less than 8 characters
                    "Short1!"
            );

            // When/Then
            mockMvc.perform(post("/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"));
        }

        @Test
        @DisplayName("Should return 400 BAD REQUEST for weak password - no uppercase")
        void register_weakPassword_noUppercase_returns400() throws Exception {
            // Given
            RegisterRequest request = new RegisterRequest(
                    "user@example.com",
                    "password123!",  // No uppercase letter
                    "password123!"
            );

            // When/Then
            mockMvc.perform(post("/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"));
        }

        @Test
        @DisplayName("Should return 400 BAD REQUEST for weak password - no special character")
        void register_weakPassword_noSpecial_returns400() throws Exception {
            // Given
            RegisterRequest request = new RegisterRequest(
                    "user@example.com",
                    "Password123",  // No special character
                    "Password123"
            );

            // When/Then
            mockMvc.perform(post("/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"));
        }

        @Test
        @DisplayName("Should return 400 BAD REQUEST when passwords do not match")
        void register_passwordMismatch_returns400() throws Exception {
            // Given
            RegisterRequest request = new RegisterRequest(
                    "user@example.com",
                    TEST_PASSWORD,
                    "DifferentPass123!"
            );

            // When/Then
            mockMvc.perform(post("/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"));
        }

        @Test
        @DisplayName("Should normalize email to lowercase and trim whitespace")
        void register_normalizeEmail() throws Exception {
            // Given
            RegisterRequest request = new RegisterRequest(
                    "  User@EXAMPLE.COM  ",
                    TEST_PASSWORD,
                    TEST_PASSWORD
            );

            // When
            mockMvc.perform(post("/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated());

            // Then - verify email was normalized
            Optional<User> savedUser = userRepository.findByEmail("user@example.com");
            assertThat(savedUser).isPresent();
        }
    }

    // ==================== Login Tests ====================

    @Nested
    @DisplayName("POST /auth/login - Login Flow")
    class LoginTests {

        @Test
        @DisplayName("Should return 200 OK with tokens in cookies on successful login")
        void login_success_returns200WithCookies() throws Exception {
            // Given
            User user = createVerifiedUserWithName(TEST_EMAIL, TEST_PASSWORD, "John", "Doe");

            LoginRequest request = new LoginRequest(TEST_EMAIL, TEST_PASSWORD, false);

            // When/Then
            MvcResult result = mockMvc.perform(post("/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.user.id").value(user.getId().toString()))
                    .andExpect(jsonPath("$.user.email").value(TEST_EMAIL))
                    .andExpect(jsonPath("$.user.firstName").value("John"))
                    .andExpect(jsonPath("$.user.lastName").value("Doe"))
                    .andExpect(jsonPath("$.expiresIn").value(900))
                    .andExpect(cookie().exists("accessToken"))
                    .andExpect(cookie().exists("refreshToken"))
                    .andReturn();

            // Verify cookie attributes
            Cookie accessTokenCookie = result.getResponse().getCookie("accessToken");
            assertThat(accessTokenCookie).isNotNull();
            assertThat(accessTokenCookie.isHttpOnly()).isTrue();
        }

        @Test
        @DisplayName("Should return 401 UNAUTHORIZED for invalid credentials")
        void login_invalidCredentials_returns401() throws Exception {
            // Given
            createVerifiedUser(TEST_EMAIL, TEST_PASSWORD);

            LoginRequest request = new LoginRequest(TEST_EMAIL, "WrongPassword123!", false);

            // When/Then
            mockMvc.perform(post("/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.error").value("UNAUTHORIZED"))
                    .andExpect(jsonPath("$.message").value("Invalid email or password"));
        }

        @Test
        @DisplayName("Should return 401 UNAUTHORIZED for non-existent email")
        void login_nonExistentEmail_returns401() throws Exception {
            // Given
            LoginRequest request = new LoginRequest("nonexistent@example.com", TEST_PASSWORD, false);

            // When/Then
            mockMvc.perform(post("/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.error").value("UNAUTHORIZED"))
                    .andExpect(jsonPath("$.message").value("Invalid email or password"));
        }

        @Test
        @DisplayName("Should return 403 FORBIDDEN for unverified email")
        void login_unverifiedEmail_returns403() throws Exception {
            // Given
            createUnverifiedUser(TEST_EMAIL, TEST_PASSWORD);

            LoginRequest request = new LoginRequest(TEST_EMAIL, TEST_PASSWORD, false);

            // When/Then
            mockMvc.perform(post("/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.error").value("FORBIDDEN"))
                    .andExpect(jsonPath("$.message").containsString("verify your email"));
        }

        @Test
        @DisplayName("Should return 403 FORBIDDEN for locked account")
        void login_lockedAccount_returns403() throws Exception {
            // Given
            User user = createVerifiedUser(TEST_EMAIL, TEST_PASSWORD);
            user.lockAccount(Instant.now().plus(30, ChronoUnit.MINUTES));
            userRepository.save(user);

            LoginRequest request = new LoginRequest(TEST_EMAIL, TEST_PASSWORD, false);

            // When/Then
            mockMvc.perform(post("/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.error").value("FORBIDDEN"))
                    .andExpect(jsonPath("$.message").containsString("Account temporarily locked"));
        }

        @Test
        @DisplayName("Should lock account after 5 failed login attempts")
        void login_lockAccountAfterMaxAttempts() throws Exception {
            // Given
            User user = createVerifiedUser(TEST_EMAIL, TEST_PASSWORD);
            user.setFailedLoginAttempts(4);  // One more attempt will lock
            userRepository.save(user);

            LoginRequest request = new LoginRequest(TEST_EMAIL, "WrongPassword123!", false);

            // When - attempt failed login
            mockMvc.perform(post("/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isUnauthorized());

            // Then - verify account is locked
            User updatedUser = userRepository.findByEmail(TEST_EMAIL).orElseThrow();
            assertThat(updatedUser.isAccountLocked()).isTrue();
            assertThat(updatedUser.getAccountLockedUntil()).isNotNull();
        }

        @Test
        @DisplayName("Should reset failed attempts on successful login")
        void login_resetFailedAttemptsOnSuccess() throws Exception {
            // Given
            User user = createVerifiedUser(TEST_EMAIL, TEST_PASSWORD);
            user.setFailedLoginAttempts(3);
            userRepository.save(user);

            LoginRequest request = new LoginRequest(TEST_EMAIL, TEST_PASSWORD, false);

            // When
            mockMvc.perform(post("/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk());

            // Then
            User updatedUser = userRepository.findByEmail(TEST_EMAIL).orElseThrow();
            assertThat(updatedUser.getFailedLoginAttempts()).isZero();
        }
    }

    // ==================== Token Refresh Tests ====================

    @Nested
    @DisplayName("POST /auth/refresh - Token Refresh Flow")
    class TokenRefreshTests {

        @Test
        @DisplayName("Should return 200 OK with new tokens for valid refresh token")
        void refresh_validToken_returns200() throws Exception {
            // Given
            User user = createVerifiedUser(TEST_EMAIL, TEST_PASSWORD);
            TokenService.RefreshTokenDetails refreshToken = createRefreshTokenForUser(user);

            // When/Then
            mockMvc.perform(post("/auth/refresh")
                            .cookie(new Cookie("refreshToken", refreshToken.token())))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.expiresIn").value(900))
                    .andExpect(cookie().exists("accessToken"))
                    .andExpect(cookie().exists("refreshToken"));
        }

        @Test
        @DisplayName("Should return 401 UNAUTHORIZED for expired refresh token")
        void refresh_expiredToken_returns401() throws Exception {
            // Given
            User user = createVerifiedUser(TEST_EMAIL, TEST_PASSWORD);
            TokenService.RefreshTokenDetails refreshToken = createRefreshTokenForUser(user);

            // Manually expire the token
            RefreshToken dbToken = refreshTokenRepository.findById(refreshToken.sessionId()).orElseThrow();
            dbToken.setExpiresAt(Instant.now().minus(1, ChronoUnit.HOURS));
            refreshTokenRepository.save(dbToken);

            // When/Then
            mockMvc.perform(post("/auth/refresh")
                            .cookie(new Cookie("refreshToken", refreshToken.token())))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.error").value("UNAUTHORIZED"))
                    .andExpect(jsonPath("$.message").containsString("expired"));
        }

        @Test
        @DisplayName("Should return 401 UNAUTHORIZED for revoked refresh token")
        void refresh_revokedToken_returns401() throws Exception {
            // Given
            User user = createVerifiedUser(TEST_EMAIL, TEST_PASSWORD);
            TokenService.RefreshTokenDetails refreshToken = createRefreshTokenForUser(user);

            // Revoke the token
            tokenService.revokeRefreshToken(refreshToken.token());

            // When/Then
            mockMvc.perform(post("/auth/refresh")
                            .cookie(new Cookie("refreshToken", refreshToken.token())))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.error").value("UNAUTHORIZED"))
                    .andExpect(jsonPath("$.message").containsString("Token reuse"));
        }

        @Test
        @DisplayName("Should detect token reuse and revoke entire family")
        void refresh_tokenReuse_revokesFamily() throws Exception {
            // Given
            User user = createVerifiedUser(TEST_EMAIL, TEST_PASSWORD);
            TokenService.RefreshTokenDetails refreshToken = createRefreshTokenForUser(user);

            // Use the token once
            mockMvc.perform(post("/auth/refresh")
                            .cookie(new Cookie("refreshToken", refreshToken.token())))
                    .andExpect(status().isOk());

            // When - try to use the same token again (reuse attack simulation)
            mockMvc.perform(post("/auth/refresh")
                            .cookie(new Cookie("refreshToken", refreshToken.token())))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.message").containsString("Token reuse"));

            // Then - verify all tokens in family are revoked
            long activeTokens = refreshTokenRepository.countActiveSessionsByUserId(user.getId(), Instant.now());
            assertThat(activeTokens).isZero();
        }

        @Test
        @DisplayName("Should return 401 UNAUTHORIZED when no refresh token cookie present")
        void refresh_noToken_returns401() throws Exception {
            // When/Then
            mockMvc.perform(post("/auth/refresh"))
                    .andExpect(status().isUnauthorized());
        }
    }

    // ==================== Password Reset Tests ====================

    @Nested
    @DisplayName("POST /auth/password/forgot - Password Reset Request")
    class PasswordResetRequestTests {

        @Test
        @DisplayName("Should return 200 OK regardless of email existence - prevents enumeration")
        void forgotPassword_anyEmail_returns200() throws Exception {
            // Given
            ForgotPasswordRequest request = new ForgotPasswordRequest("nonexistent@example.com");

            // When/Then - should always return 200 to prevent email enumeration
            mockMvc.perform(post("/auth/password/forgot")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value(
                            "If the email exists in our system, you will receive a password reset link."
                    ));
        }

        @Test
        @DisplayName("Should return 200 OK for existing email")
        void forgotPassword_existingEmail_returns200() throws Exception {
            // Given
            createVerifiedUser(TEST_EMAIL, TEST_PASSWORD);
            ForgotPasswordRequest request = new ForgotPasswordRequest(TEST_EMAIL);

            // When/Then
            mockMvc.perform(post("/auth/password/forgot")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").containsString("If the email exists"));
        }

        @Test
        @DisplayName("Should return 400 BAD REQUEST for invalid email format")
        void forgotPassword_invalidEmail_returns400() throws Exception {
            // Given
            ForgotPasswordRequest request = new ForgotPasswordRequest("invalid-email");

            // When/Then
            mockMvc.perform(post("/auth/password/forgot")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"));
        }
    }

    @Nested
    @DisplayName("POST /auth/password/reset - Password Reset Completion")
    class PasswordResetCompletionTests {

        @Test
        @DisplayName("Should return 200 OK and reset password with valid token")
        void resetPassword_validToken_returns200() throws Exception {
            // Given
            User user = createVerifiedUser(TEST_EMAIL, TEST_PASSWORD);
            String resetToken = createPasswordResetTokenForUser(user);

            String newPassword = "NewSecurePass123!";
            ResetPasswordRequest request = new ResetPasswordRequest(resetToken, newPassword, newPassword);

            // When/Then
            mockMvc.perform(post("/auth/password/reset")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value(
                            "Password has been reset successfully. Please log in with your new password."
                    ));

            // Verify user can login with new password
            LoginRequest loginRequest = new LoginRequest(TEST_EMAIL, newPassword, false);
            mockMvc.perform(post("/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(loginRequest)))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("Should return 400 BAD REQUEST for used token")
        void resetPassword_usedToken_returns400() throws Exception {
            // Given
            User user = createVerifiedUser(TEST_EMAIL, TEST_PASSWORD);
            String resetToken = createPasswordResetTokenForUser(user);

            // Use the token
            String newPassword = "NewSecurePass123!";
            ResetPasswordRequest request = new ResetPasswordRequest(resetToken, newPassword, newPassword);
            mockMvc.perform(post("/auth/password/reset")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk());

            // When - try to use the same token again
            mockMvc.perform(post("/auth/password/reset")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").containsString("already been used"));
        }

        @Test
        @DisplayName("Should return 400 BAD REQUEST for weak new password")
        void resetPassword_weakPassword_returns400() throws Exception {
            // Given
            User user = createVerifiedUser(TEST_EMAIL, TEST_PASSWORD);
            String resetToken = createPasswordResetTokenForUser(user);

            ResetPasswordRequest request = new ResetPasswordRequest(resetToken, "weak", "weak");

            // When/Then
            mockMvc.perform(post("/auth/password/reset")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"));
        }

        @Test
        @DisplayName("Should invalidate all sessions after password reset")
        void resetPassword_invalidatesAllSessions() throws Exception {
            // Given
            User user = createVerifiedUser(TEST_EMAIL, TEST_PASSWORD);
            TokenService.RefreshTokenDetails refreshToken = createRefreshTokenForUser(user);
            String resetToken = createPasswordResetTokenForUser(user);

            String newPassword = "NewSecurePass123!";
            ResetPasswordRequest request = new ResetPasswordRequest(resetToken, newPassword, newPassword);

            // When
            mockMvc.perform(post("/auth/password/reset")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk());

            // Then - verify refresh token is invalidated
            mockMvc.perform(post("/auth/refresh")
                            .cookie(new Cookie("refreshToken", refreshToken.token())))
                    .andExpect(status().isUnauthorized());
        }
    }

    // ==================== Email Verification Tests ====================

    @Nested
    @DisplayName("POST /auth/verify-email - Email Verification")
    class EmailVerificationTests {

        @Test
        @DisplayName("Should return 200 OK and verify email with valid token")
        void verifyEmail_validToken_returns200() throws Exception {
            // Given
            User user = createUnverifiedUser(TEST_EMAIL, TEST_PASSWORD);
            String verificationToken = createVerificationTokenForUser(user);

            VerifyEmailRequest request = new VerifyEmailRequest(verificationToken);

            // When/Then
            mockMvc.perform(post("/auth/verify-email")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value(
                            "Email verified successfully. You can now log in."
                    ));

            // Verify user can now login
            User updatedUser = userRepository.findByEmail(TEST_EMAIL).orElseThrow();
            assertThat(updatedUser.isEmailVerified()).isTrue();
        }

        @Test
        @DisplayName("Should return 400 BAD REQUEST for already used token")
        void verifyEmail_usedToken_returns400() throws Exception {
            // Given
            User user = createUnverifiedUser(TEST_EMAIL, TEST_PASSWORD);
            String verificationToken = createVerificationTokenForUser(user);

            // Use the token
            VerifyEmailRequest request = new VerifyEmailRequest(verificationToken);
            mockMvc.perform(post("/auth/verify-email")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk());

            // When - try to use again
            mockMvc.perform(post("/auth/verify-email")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").containsString("already been verified"));
        }

        @Test
        @DisplayName("Should return 404 NOT FOUND for invalid token")
        void verifyEmail_invalidToken_returns404() throws Exception {
            // Given
            VerifyEmailRequest request = new VerifyEmailRequest(UUID.randomUUID().toString());

            // When/Then
            mockMvc.perform(post("/auth/verify-email")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.error").value("NOT_FOUND"));
        }
    }

    @Nested
    @DisplayName("POST /auth/verify-email/resend - Resend Verification Email")
    class ResendVerificationTests {

        @Test
        @DisplayName("Should return 200 OK for unverified user")
        void resendVerification_unverifiedUser_returns200() throws Exception {
            // Given
            createUnverifiedUser(TEST_EMAIL, TEST_PASSWORD);
            ResendVerificationRequest request = new ResendVerificationRequest(TEST_EMAIL);

            // When/Then
            mockMvc.perform(post("/auth/verify-email/resend")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value("Verification email sent."));
        }

        @Test
        @DisplayName("Should return 400 BAD REQUEST for already verified email")
        void resendVerification_alreadyVerified_returns400() throws Exception {
            // Given
            createVerifiedUser(TEST_EMAIL, TEST_PASSWORD);
            ResendVerificationRequest request = new ResendVerificationRequest(TEST_EMAIL);

            // When/Then
            mockMvc.perform(post("/auth/verify-email/resend")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").containsString("already been verified"));
        }

        @Test
        @DisplayName("Should return 200 OK for non-existent email - prevents enumeration")
        void resendVerification_nonExistentEmail_returns200() throws Exception {
            // Given
            ResendVerificationRequest request = new ResendVerificationRequest("nonexistent@example.com");

            // When/Then - should return 200 to prevent email enumeration
            mockMvc.perform(post("/auth/verify-email/resend")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk());
        }
    }

    // ==================== Session Management Tests ====================

    @Nested
    @DisplayName("GET /auth/sessions - List Sessions")
    class ListSessionsTests {

        @Test
        @DisplayName("Should return 200 OK with list of active sessions")
        void getSessions_authenticated_returns200() throws Exception {
            // Given
            User user = createVerifiedUser(TEST_EMAIL, TEST_PASSWORD);
            TokenService.RefreshTokenDetails refreshToken = createRefreshTokenForUser(user);
            String accessToken = tokenService.createAccessToken(user, refreshToken.sessionId());

            // When/Then
            mockMvc.perform(get("/auth/sessions")
                            .cookie(new Cookie("accessToken", accessToken)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.sessions").isArray())
                    .andExpect(jsonPath("$.sessions.length()").value(1));
        }

        @Test
        @DisplayName("Should return 401 UNAUTHORIZED without authentication")
        void getSessions_unauthenticated_returns401() throws Exception {
            // When/Then
            mockMvc.perform(get("/auth/sessions"))
                    .andExpect(status().isUnauthorized());
        }
    }

    @Nested
    @DisplayName("DELETE /auth/sessions - Revoke All Sessions")
    class RevokeAllSessionsTests {

        @Test
        @DisplayName("Should return 200 OK and revoke all sessions")
        void revokeAllSessions_authenticated_returns200() throws Exception {
            // Given
            User user = createVerifiedUser(TEST_EMAIL, TEST_PASSWORD);
            TokenService.RefreshTokenDetails refreshToken = createRefreshTokenForUser(user);
            String accessToken = tokenService.createAccessToken(user, refreshToken.sessionId());

            // Create additional sessions
            createRefreshTokenForUser(user);
            createRefreshTokenForUser(user);

            // When/Then
            mockMvc.perform(delete("/auth/sessions")
                            .cookie(new Cookie("accessToken", accessToken)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").containsString("All sessions have been terminated"));

            // Verify all sessions are revoked
            long activeTokens = refreshTokenRepository.countActiveSessionsByUserId(user.getId(), Instant.now());
            assertThat(activeTokens).isZero();
        }
    }

    @Nested
    @DisplayName("DELETE /auth/sessions/{sessionId} - Revoke Specific Session")
    class RevokeSpecificSessionTests {

        @Test
        @DisplayName("Should return 200 OK and revoke specific session")
        void revokeSession_validSession_returns200() throws Exception {
            // Given
            User user = createVerifiedUser(TEST_EMAIL, TEST_PASSWORD);
            TokenService.RefreshTokenDetails currentSession = createRefreshTokenForUser(user);
            TokenService.RefreshTokenDetails otherSession = createRefreshTokenForUser(user);
            String accessToken = tokenService.createAccessToken(user, currentSession.sessionId());

            // When/Then
            mockMvc.perform(delete("/auth/sessions/{sessionId}", otherSession.sessionId())
                            .cookie(new Cookie("accessToken", accessToken)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value("Session revoked successfully"));

            // Verify the other session is revoked
            RefreshToken revokedSession = refreshTokenRepository.findById(otherSession.sessionId()).orElseThrow();
            assertThat(revokedSession.isRevoked()).isTrue();
        }

        @Test
        @DisplayName("Should return 400 BAD REQUEST when trying to revoke current session")
        void revokeSession_currentSession_returns400() throws Exception {
            // Given
            User user = createVerifiedUser(TEST_EMAIL, TEST_PASSWORD);
            TokenService.RefreshTokenDetails currentSession = createRefreshTokenForUser(user);
            String accessToken = tokenService.createAccessToken(user, currentSession.sessionId());

            // When/Then - cannot revoke current session via this endpoint
            mockMvc.perform(delete("/auth/sessions/{sessionId}", currentSession.sessionId())
                            .cookie(new Cookie("accessToken", accessToken)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error").value("BAD_REQUEST"))
                    .andExpect(jsonPath("$.message").value("Cannot revoke current session. Use logout instead."));
        }
    }

    // ==================== Logout Tests ====================

    @Nested
    @DisplayName("POST /auth/logout - Logout")
    class LogoutTests {

        @Test
        @DisplayName("Should return 200 OK and clear cookies")
        void logout_success_returns200() throws Exception {
            // Given
            User user = createVerifiedUser(TEST_EMAIL, TEST_PASSWORD);
            TokenService.RefreshTokenDetails refreshToken = createRefreshTokenForUser(user);

            // When/Then
            mockMvc.perform(post("/auth/logout")
                            .cookie(new Cookie("refreshToken", refreshToken.token())))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value("Logged out successfully"));
        }

        @Test
        @DisplayName("Should return 200 OK even without cookies")
        void logout_noCookies_returns200() throws Exception {
            // When/Then - logout should succeed even without cookies
            mockMvc.perform(post("/auth/logout"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value("Logged out successfully"));
        }
    }
}
