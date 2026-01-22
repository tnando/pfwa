package com.pfwa.service;

import com.pfwa.config.AppProperties;
import com.pfwa.dto.auth.LoginRequest;
import com.pfwa.dto.auth.RegisterRequest;
import com.pfwa.dto.auth.RegisterResponse;
import com.pfwa.entity.User;
import com.pfwa.exception.AccountLockedException;
import com.pfwa.exception.EmailAlreadyExistsException;
import com.pfwa.exception.EmailNotVerifiedException;
import com.pfwa.exception.InvalidCredentialsException;
import com.pfwa.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for AuthService.
 */
@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private TokenService tokenService;

    @Mock
    private EmailService emailService;

    @Mock
    private RateLimitService rateLimitService;

    @Mock
    private AppProperties appProperties;

    @InjectMocks
    private AuthService authService;

    private AppProperties.RateLimit rateLimit;
    private AppProperties.Security security;

    @BeforeEach
    void setUp() {
        // Set up rate limit config
        rateLimit = new AppProperties.RateLimit();
        AppProperties.RateLimitConfig registrationConfig = rateLimit.getRegistration();
        registrationConfig.setMaxAttempts(5);
        registrationConfig.setWindowMinutes(1);

        AppProperties.RateLimitConfig loginConfig = rateLimit.getLogin();
        loginConfig.setMaxAttempts(5);
        loginConfig.setWindowMinutes(15);

        // Set up security config
        security = new AppProperties.Security();
        security.setMaxFailedLoginAttempts(5);
        security.setAccountLockoutMinutes(30);

        when(appProperties.getRateLimit()).thenReturn(rateLimit);
        when(appProperties.getSecurity()).thenReturn(security);
    }

    @Nested
    @DisplayName("Registration Tests")
    class RegistrationTests {

        @Test
        @DisplayName("Should successfully register a new user")
        void register_success() {
            // Given
            RegisterRequest request = new RegisterRequest(
                    "user@example.com",
                    "SecurePass123!",
                    "SecurePass123!"
            );
            String clientIp = "192.168.1.1";

            User savedUser = new User("user@example.com", "hashedPassword");
            savedUser.setId(UUID.randomUUID());

            when(userRepository.existsByEmail(anyString())).thenReturn(false);
            when(passwordEncoder.encode(anyString())).thenReturn("hashedPassword");
            when(userRepository.save(any(User.class))).thenReturn(savedUser);
            when(tokenService.createVerificationToken(any(User.class))).thenReturn("verification-token");
            doNothing().when(emailService).sendVerificationEmail(any(User.class), anyString());

            // When
            RegisterResponse response = authService.register(request, clientIp);

            // Then
            assertThat(response).isNotNull();
            assertThat(response.userId()).isEqualTo(savedUser.getId());
            assertThat(response.message()).contains("Registration successful");

            verify(userRepository).save(any(User.class));
            verify(tokenService).createVerificationToken(any(User.class));
            verify(emailService).sendVerificationEmail(any(User.class), eq("verification-token"));
        }

        @Test
        @DisplayName("Should throw exception when email already exists")
        void register_emailAlreadyExists() {
            // Given
            RegisterRequest request = new RegisterRequest(
                    "existing@example.com",
                    "SecurePass123!",
                    "SecurePass123!"
            );
            String clientIp = "192.168.1.1";

            when(userRepository.existsByEmail(anyString())).thenReturn(true);

            // When/Then
            assertThatThrownBy(() -> authService.register(request, clientIp))
                    .isInstanceOf(EmailAlreadyExistsException.class)
                    .hasMessageContaining("Email already exists");

            verify(userRepository, never()).save(any(User.class));
        }

        @Test
        @DisplayName("Should normalize email to lowercase")
        void register_normalizeEmail() {
            // Given
            RegisterRequest request = new RegisterRequest(
                    "  USER@EXAMPLE.COM  ",
                    "SecurePass123!",
                    "SecurePass123!"
            );
            String clientIp = "192.168.1.1";

            User savedUser = new User("user@example.com", "hashedPassword");
            savedUser.setId(UUID.randomUUID());

            when(userRepository.existsByEmail("user@example.com")).thenReturn(false);
            when(passwordEncoder.encode(anyString())).thenReturn("hashedPassword");
            when(userRepository.save(any(User.class))).thenReturn(savedUser);
            when(tokenService.createVerificationToken(any(User.class))).thenReturn("verification-token");

            // When
            authService.register(request, clientIp);

            // Then
            ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
            verify(userRepository).save(userCaptor.capture());
            assertThat(userCaptor.getValue().getEmail()).isEqualTo("user@example.com");
        }
    }

    @Nested
    @DisplayName("Login Tests")
    class LoginTests {

        @Test
        @DisplayName("Should throw InvalidCredentialsException when user not found")
        void login_userNotFound() {
            // Given
            LoginRequest request = new LoginRequest("unknown@example.com", "password", false);
            HttpServletRequest httpRequest = mock(HttpServletRequest.class);
            when(httpRequest.getRemoteAddr()).thenReturn("192.168.1.1");

            when(userRepository.findByEmail(anyString())).thenReturn(Optional.empty());

            // When/Then
            assertThatThrownBy(() -> authService.login(request, httpRequest))
                    .isInstanceOf(InvalidCredentialsException.class)
                    .hasMessage("Invalid email or password");
        }

        @Test
        @DisplayName("Should throw InvalidCredentialsException when password is wrong")
        void login_wrongPassword() {
            // Given
            LoginRequest request = new LoginRequest("user@example.com", "wrongPassword", false);
            HttpServletRequest httpRequest = mock(HttpServletRequest.class);
            when(httpRequest.getRemoteAddr()).thenReturn("192.168.1.1");

            User user = new User("user@example.com", "hashedPassword");
            user.setId(UUID.randomUUID());
            user.setEmailVerified(true);

            when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(user));
            when(passwordEncoder.matches(anyString(), anyString())).thenReturn(false);
            when(userRepository.save(any(User.class))).thenReturn(user);

            // When/Then
            assertThatThrownBy(() -> authService.login(request, httpRequest))
                    .isInstanceOf(InvalidCredentialsException.class)
                    .hasMessage("Invalid email or password");
        }

        @Test
        @DisplayName("Should throw EmailNotVerifiedException when email not verified")
        void login_emailNotVerified() {
            // Given
            LoginRequest request = new LoginRequest("user@example.com", "SecurePass123!", false);
            HttpServletRequest httpRequest = mock(HttpServletRequest.class);
            when(httpRequest.getRemoteAddr()).thenReturn("192.168.1.1");

            User user = new User("user@example.com", "hashedPassword");
            user.setId(UUID.randomUUID());
            user.setEmailVerified(false); // Not verified

            when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(user));
            when(passwordEncoder.matches(anyString(), anyString())).thenReturn(true);

            // When/Then
            assertThatThrownBy(() -> authService.login(request, httpRequest))
                    .isInstanceOf(EmailNotVerifiedException.class)
                    .hasMessageContaining("verify your email");
        }

        @Test
        @DisplayName("Should throw AccountLockedException when account is locked")
        void login_accountLocked() {
            // Given
            LoginRequest request = new LoginRequest("user@example.com", "SecurePass123!", false);
            HttpServletRequest httpRequest = mock(HttpServletRequest.class);
            when(httpRequest.getRemoteAddr()).thenReturn("192.168.1.1");

            User user = new User("user@example.com", "hashedPassword");
            user.setId(UUID.randomUUID());
            user.setEmailVerified(true);
            user.lockAccount(Instant.now().plus(30, ChronoUnit.MINUTES));

            when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(user));

            // When/Then
            assertThatThrownBy(() -> authService.login(request, httpRequest))
                    .isInstanceOf(AccountLockedException.class)
                    .hasMessageContaining("Account temporarily locked");
        }

        @Test
        @DisplayName("Should unlock account if lock has expired")
        void login_unlockExpiredAccount() {
            // Given
            LoginRequest request = new LoginRequest("user@example.com", "SecurePass123!", false);
            HttpServletRequest httpRequest = mock(HttpServletRequest.class);
            when(httpRequest.getRemoteAddr()).thenReturn("192.168.1.1");
            when(httpRequest.getHeader("User-Agent")).thenReturn("Mozilla/5.0");

            User user = new User("user@example.com", "hashedPassword");
            user.setId(UUID.randomUUID());
            user.setEmailVerified(true);
            // Lock expired 1 minute ago
            user.lockAccount(Instant.now().minus(1, ChronoUnit.MINUTES));

            TokenService.RefreshTokenDetails refreshTokenDetails = new TokenService.RefreshTokenDetails(
                    "refresh-token",
                    UUID.randomUUID(),
                    UUID.randomUUID(),
                    Instant.now().plus(7, ChronoUnit.DAYS)
            );

            when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(user));
            when(passwordEncoder.matches(anyString(), anyString())).thenReturn(true);
            when(userRepository.save(any(User.class))).thenReturn(user);
            when(tokenService.createRefreshToken(any(User.class), eq(false), any(HttpServletRequest.class)))
                    .thenReturn(refreshTokenDetails);
            when(tokenService.createAccessToken(any(User.class), any(UUID.class)))
                    .thenReturn("access-token");
            when(tokenService.getAccessTokenExpirationSeconds()).thenReturn(900);

            // When
            AuthService.LoginResult result = authService.login(request, httpRequest);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.response().user().email()).isEqualTo("user@example.com");

            // Verify account was unlocked
            ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
            verify(userRepository).save(userCaptor.capture());
            User savedUser = userCaptor.getValue();
            assertThat(savedUser.isAccountLocked()).isFalse();
        }

        @Test
        @DisplayName("Should increment failed login attempts on wrong password")
        void login_incrementFailedAttempts() {
            // Given
            LoginRequest request = new LoginRequest("user@example.com", "wrongPassword", false);
            HttpServletRequest httpRequest = mock(HttpServletRequest.class);
            when(httpRequest.getRemoteAddr()).thenReturn("192.168.1.1");

            User user = new User("user@example.com", "hashedPassword");
            user.setId(UUID.randomUUID());
            user.setEmailVerified(true);
            user.setFailedLoginAttempts(0);

            when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(user));
            when(passwordEncoder.matches(anyString(), anyString())).thenReturn(false);
            when(userRepository.save(any(User.class))).thenReturn(user);

            // When/Then
            assertThatThrownBy(() -> authService.login(request, httpRequest))
                    .isInstanceOf(InvalidCredentialsException.class);

            ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
            verify(userRepository).save(userCaptor.capture());
            assertThat(userCaptor.getValue().getFailedLoginAttempts()).isEqualTo(1);
        }

        @Test
        @DisplayName("Should lock account after max failed attempts")
        void login_lockAccountAfterMaxAttempts() {
            // Given
            LoginRequest request = new LoginRequest("user@example.com", "wrongPassword", false);
            HttpServletRequest httpRequest = mock(HttpServletRequest.class);
            when(httpRequest.getRemoteAddr()).thenReturn("192.168.1.1");

            User user = new User("user@example.com", "hashedPassword");
            user.setId(UUID.randomUUID());
            user.setEmailVerified(true);
            user.setFailedLoginAttempts(4); // One more attempt will lock

            when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(user));
            when(passwordEncoder.matches(anyString(), anyString())).thenReturn(false);
            when(userRepository.save(any(User.class))).thenReturn(user);

            // When/Then
            assertThatThrownBy(() -> authService.login(request, httpRequest))
                    .isInstanceOf(InvalidCredentialsException.class);

            ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
            verify(userRepository).save(userCaptor.capture());
            User savedUser = userCaptor.getValue();
            assertThat(savedUser.isAccountLocked()).isTrue();
            assertThat(savedUser.getAccountLockedUntil()).isNotNull();
        }
    }

    @Nested
    @DisplayName("Password Reset Tests")
    class PasswordResetTests {

        @Test
        @DisplayName("Should silently succeed when email does not exist")
        void requestPasswordReset_emailNotFound() {
            // Given
            String email = "unknown@example.com";
            String clientIp = "192.168.1.1";

            when(userRepository.findByEmail(anyString())).thenReturn(Optional.empty());

            // When
            authService.requestPasswordReset(email, clientIp);

            // Then - no exception thrown, no email sent
            verify(emailService, never()).sendPasswordResetEmail(any(User.class), anyString());
        }

        @Test
        @DisplayName("Should send password reset email when user exists")
        void requestPasswordReset_success() {
            // Given
            String email = "user@example.com";
            String clientIp = "192.168.1.1";

            User user = new User(email, "hashedPassword");
            user.setId(UUID.randomUUID());

            AppProperties.RateLimitConfig resetConfig = rateLimit.getPasswordReset();
            resetConfig.setMaxAttempts(3);
            resetConfig.setWindowMinutes(60);

            when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(user));
            when(tokenService.countPasswordResetRequests(any(UUID.class), eq(60))).thenReturn(0L);
            when(tokenService.createPasswordResetToken(any(User.class))).thenReturn("reset-token");

            // When
            authService.requestPasswordReset(email, clientIp);

            // Then
            verify(tokenService).createPasswordResetToken(user);
            verify(emailService).sendPasswordResetEmail(user, "reset-token");
        }

        @Test
        @DisplayName("Should reset password and invalidate all sessions")
        void resetPassword_success() {
            // Given
            String token = "valid-reset-token";
            String newPassword = "NewSecurePass123!";
            String clientIp = "192.168.1.1";

            User user = new User("user@example.com", "oldHashedPassword");
            user.setId(UUID.randomUUID());

            when(tokenService.validatePasswordResetToken(token)).thenReturn(user);
            when(passwordEncoder.encode(newPassword)).thenReturn("newHashedPassword");
            when(userRepository.updatePassword(any(UUID.class), anyString())).thenReturn(1);
            when(userRepository.incrementTokenVersion(any(UUID.class))).thenReturn(1);

            // When
            authService.resetPassword(token, newPassword, clientIp);

            // Then
            verify(userRepository).updatePassword(user.getId(), "newHashedPassword");
            verify(userRepository).incrementTokenVersion(user.getId());
            verify(tokenService).revokeAllUserTokens(user.getId());
            verify(emailService).sendPasswordChangeConfirmation(user, clientIp);
        }
    }

    @Nested
    @DisplayName("Email Verification Tests")
    class EmailVerificationTests {

        @Test
        @DisplayName("Should verify email successfully")
        void verifyEmail_success() {
            // Given
            String token = "valid-verification-token";
            User user = new User("user@example.com", "hashedPassword");
            user.setId(UUID.randomUUID());

            when(tokenService.validateVerificationToken(token)).thenReturn(user);
            when(userRepository.verifyEmail(any(UUID.class))).thenReturn(1);

            // When
            authService.verifyEmail(token);

            // Then
            verify(tokenService).validateVerificationToken(token);
            verify(userRepository).verifyEmail(user.getId());
        }

        @Test
        @DisplayName("Should resend verification email")
        void resendVerificationEmail_success() {
            // Given
            String email = "user@example.com";
            User user = new User(email, "hashedPassword");
            user.setId(UUID.randomUUID());
            user.setEmailVerified(false);

            AppProperties.RateLimitConfig emailConfig = rateLimit.getEmailResend();
            emailConfig.setMaxAttempts(3);
            emailConfig.setWindowMinutes(60);

            when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(user));
            when(tokenService.countVerificationEmailsSent(any(UUID.class), eq(60))).thenReturn(0L);
            when(tokenService.createVerificationToken(any(User.class))).thenReturn("new-verification-token");

            // When
            authService.resendVerificationEmail(email);

            // Then
            verify(tokenService).createVerificationToken(user);
            verify(emailService).sendVerificationEmail(user, "new-verification-token");
        }

        @Test
        @DisplayName("Should throw exception when resending to already verified email")
        void resendVerificationEmail_alreadyVerified() {
            // Given
            String email = "user@example.com";
            User user = new User(email, "hashedPassword");
            user.setId(UUID.randomUUID());
            user.setEmailVerified(true); // Already verified

            when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(user));

            // When/Then
            assertThatThrownBy(() -> authService.resendVerificationEmail(email))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("already been verified");
        }
    }
}
