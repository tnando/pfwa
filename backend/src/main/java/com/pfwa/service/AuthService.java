package com.pfwa.service;

import com.pfwa.config.AppProperties;
import com.pfwa.dto.auth.LoginRequest;
import com.pfwa.dto.auth.LoginResponse;
import com.pfwa.dto.auth.RegisterRequest;
import com.pfwa.dto.auth.RegisterResponse;
import com.pfwa.dto.auth.TokenRefreshResponse;
import com.pfwa.dto.auth.UserProfile;
import com.pfwa.entity.User;
import com.pfwa.exception.AccountLockedException;
import com.pfwa.exception.EmailAlreadyExistsException;
import com.pfwa.exception.EmailNotVerifiedException;
import com.pfwa.exception.InvalidCredentialsException;
import com.pfwa.exception.RateLimitExceededException;
import com.pfwa.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.UUID;

/**
 * Core authentication service handling registration, login, logout, and password operations.
 */
@Service
public class AuthService {

    private static final Logger logger = LoggerFactory.getLogger(AuthService.class);

    private static final String RATE_LIMIT_REGISTRATION = "registration";
    private static final String RATE_LIMIT_LOGIN = "login";
    private static final String RATE_LIMIT_PASSWORD_RESET = "password_reset";
    private static final String RATE_LIMIT_EMAIL_RESEND = "email_resend";

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final TokenService tokenService;
    private final EmailService emailService;
    private final RateLimitService rateLimitService;
    private final AppProperties appProperties;

    public AuthService(UserRepository userRepository,
                       PasswordEncoder passwordEncoder,
                       TokenService tokenService,
                       EmailService emailService,
                       RateLimitService rateLimitService,
                       AppProperties appProperties) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.tokenService = tokenService;
        this.emailService = emailService;
        this.rateLimitService = rateLimitService;
        this.appProperties = appProperties;
    }

    /**
     * Registers a new user.
     *
     * @param request the registration request
     * @param clientIp the client IP address for rate limiting
     * @return registration response with user ID
     */
    @Transactional
    public RegisterResponse register(RegisterRequest request, String clientIp) {
        // Check rate limit
        checkRegistrationRateLimit(clientIp);

        String normalizedEmail = request.normalizedEmail();

        // Check if email already exists
        if (userRepository.existsByEmail(normalizedEmail)) {
            logger.debug("Registration attempt with existing email");
            throw new EmailAlreadyExistsException("Email already exists");
        }

        // Hash password
        String passwordHash = passwordEncoder.encode(request.password());

        // Create user
        User user = new User(normalizedEmail, passwordHash);
        user = userRepository.save(user);

        logger.info("User registered: {}", maskEmail(normalizedEmail));

        // Send verification email
        String verificationToken = tokenService.createVerificationToken(user);
        emailService.sendVerificationEmail(user, verificationToken);

        return RegisterResponse.success(user.getId());
    }

    /**
     * Authenticates a user and generates tokens.
     *
     * @param request the login request
     * @param httpRequest the HTTP request for device info and rate limiting
     * @return login result containing user, tokens, and expiration
     */
    @Transactional
    public LoginResult login(LoginRequest request, HttpServletRequest httpRequest) {
        String normalizedEmail = request.normalizedEmail();
        String clientIp = getClientIpAddress(httpRequest);
        String rateLimitKey = normalizedEmail + ":" + clientIp;

        // Find user
        Optional<User> userOptional = userRepository.findByEmail(normalizedEmail);

        if (userOptional.isEmpty()) {
            // User not found - record failed attempt but don't reveal this
            recordFailedLoginAttempt(rateLimitKey);
            throw new InvalidCredentialsException();
        }

        User user = userOptional.get();

        // Check if account is locked
        if (user.isAccountLocked()) {
            if (user.isLockExpired()) {
                user.resetFailedLoginAttempts();
                userRepository.save(user);
            } else {
                logger.info("Login attempt on locked account: {}", maskEmail(normalizedEmail));
                throw new AccountLockedException(
                        "Account temporarily locked due to too many failed attempts. Please try again in " +
                                appProperties.getSecurity().getAccountLockoutMinutes() + " minutes.",
                        user.getAccountLockedUntil()
                );
            }
        }

        // Verify password
        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            handleFailedLogin(user, rateLimitKey);
            throw new InvalidCredentialsException();
        }

        // Check if email is verified
        if (!user.isEmailVerified()) {
            logger.debug("Login attempt with unverified email: {}", maskEmail(normalizedEmail));
            throw new EmailNotVerifiedException("Please verify your email before logging in");
        }

        // Reset failed login attempts on successful login
        if (user.getFailedLoginAttempts() > 0) {
            user.resetFailedLoginAttempts();
            userRepository.save(user);
        }
        rateLimitService.resetRateLimit(RATE_LIMIT_LOGIN, rateLimitKey);

        // Generate tokens
        TokenService.RefreshTokenDetails refreshToken = tokenService.createRefreshToken(
                user, request.isRememberMe(), httpRequest
        );
        String accessToken = tokenService.createAccessToken(user, refreshToken.sessionId());

        logger.info("User logged in: {}", maskEmail(normalizedEmail));

        LoginResponse response = new LoginResponse(
                UserProfile.fromEntity(user),
                tokenService.getAccessTokenExpirationSeconds()
        );

        return new LoginResult(response, accessToken, refreshToken);
    }

    /**
     * Logs out a user by revoking the refresh token.
     *
     * @param refreshToken the refresh token to revoke
     */
    @Transactional
    public void logout(String refreshToken) {
        if (refreshToken != null) {
            tokenService.revokeRefreshToken(refreshToken);
            logger.debug("User logged out");
        }
    }

    /**
     * Refreshes the access token using a valid refresh token.
     *
     * @param refreshToken the refresh token
     * @param httpRequest the HTTP request for device info
     * @return refresh result containing new tokens
     */
    @Transactional
    public RefreshResult refreshToken(String refreshToken, HttpServletRequest httpRequest) {
        TokenService.RefreshTokenRotationResult result = tokenService.rotateRefreshToken(
                refreshToken, false, httpRequest
        );

        TokenRefreshResponse response = new TokenRefreshResponse(
                tokenService.getAccessTokenExpirationSeconds()
        );

        return new RefreshResult(response, result.accessToken(), result.refreshToken());
    }

    /**
     * Requests a password reset email.
     *
     * @param email the email address
     * @param clientIp the client IP for rate limiting
     */
    @Transactional
    public void requestPasswordReset(String email, String clientIp) {
        String normalizedEmail = email.trim().toLowerCase();

        // Always return success to prevent email enumeration
        Optional<User> userOptional = userRepository.findByEmail(normalizedEmail);

        if (userOptional.isEmpty()) {
            logger.debug("Password reset requested for non-existent email");
            return; // Silent return
        }

        User user = userOptional.get();

        // Check rate limit
        AppProperties.RateLimitConfig config = appProperties.getRateLimit().getPasswordReset();
        long recentRequests = tokenService.countPasswordResetRequests(
                user.getId(), config.getWindowMinutes()
        );

        if (recentRequests >= config.getMaxAttempts()) {
            logger.info("Password reset rate limit exceeded for {}", maskEmail(normalizedEmail));
            throw new RateLimitExceededException(
                    "Too many password reset requests. Please try again later.",
                    config.getWindowMinutes() * 60L
            );
        }

        // Generate token and send email
        String resetToken = tokenService.createPasswordResetToken(user);
        emailService.sendPasswordResetEmail(user, resetToken);

        logger.info("Password reset email sent to {}", maskEmail(normalizedEmail));
    }

    /**
     * Resets a user's password using a valid reset token.
     *
     * @param token the reset token
     * @param newPassword the new password
     * @param clientIp the client IP for the notification email
     */
    @Transactional
    public void resetPassword(String token, String newPassword, String clientIp) {
        // Validate token and get user
        User user = tokenService.validatePasswordResetToken(token);

        // Update password
        String passwordHash = passwordEncoder.encode(newPassword);
        userRepository.updatePassword(user.getId(), passwordHash);

        // Increment token version to invalidate all existing tokens
        userRepository.incrementTokenVersion(user.getId());

        // Revoke all refresh tokens
        tokenService.revokeAllUserTokens(user.getId());

        logger.info("Password reset completed for {}", maskEmail(user.getEmail()));

        // Send confirmation email
        emailService.sendPasswordChangeConfirmation(user, clientIp);
    }

    /**
     * Verifies a user's email using a verification token.
     *
     * @param token the verification token
     */
    @Transactional
    public void verifyEmail(String token) {
        User user = tokenService.validateVerificationToken(token);

        // Mark email as verified
        userRepository.verifyEmail(user.getId());

        logger.info("Email verified for {}", maskEmail(user.getEmail()));
    }

    /**
     * Resends the verification email.
     *
     * @param email the email address
     */
    @Transactional
    public void resendVerificationEmail(String email) {
        String normalizedEmail = email.trim().toLowerCase();

        Optional<User> userOptional = userRepository.findByEmail(normalizedEmail);

        if (userOptional.isEmpty()) {
            // Return success to prevent email enumeration
            logger.debug("Verification resend requested for non-existent email");
            return;
        }

        User user = userOptional.get();

        // Check if already verified
        if (user.isEmailVerified()) {
            throw new IllegalStateException("Email has already been verified.");
        }

        // Check rate limit
        AppProperties.RateLimitConfig config = appProperties.getRateLimit().getEmailResend();
        long recentEmails = tokenService.countVerificationEmailsSent(
                user.getId(), config.getWindowMinutes()
        );

        if (recentEmails >= config.getMaxAttempts()) {
            logger.info("Verification email rate limit exceeded for {}", maskEmail(normalizedEmail));
            throw new RateLimitExceededException(
                    "Too many verification emails requested. Please try again later.",
                    config.getWindowMinutes() * 60L
            );
        }

        // Generate new token and send email
        String verificationToken = tokenService.createVerificationToken(user);
        emailService.sendVerificationEmail(user, verificationToken);

        logger.info("Verification email resent to {}", maskEmail(normalizedEmail));
    }

    /**
     * Checks registration rate limit.
     */
    private void checkRegistrationRateLimit(String clientIp) {
        AppProperties.RateLimitConfig config = appProperties.getRateLimit().getRegistration();
        rateLimitService.checkRateLimit(
                RATE_LIMIT_REGISTRATION,
                clientIp,
                config.getMaxAttempts(),
                config.getWindowMinutes()
        );
    }

    /**
     * Records a failed login attempt for rate limiting.
     */
    private void recordFailedLoginAttempt(String rateLimitKey) {
        rateLimitService.recordAttempt(RATE_LIMIT_LOGIN, rateLimitKey);
    }

    /**
     * Handles a failed login attempt (increments counter, possibly locks account).
     */
    private void handleFailedLogin(User user, String rateLimitKey) {
        recordFailedLoginAttempt(rateLimitKey);

        user.incrementFailedLoginAttempts();

        int maxAttempts = appProperties.getSecurity().getMaxFailedLoginAttempts();

        if (user.getFailedLoginAttempts() >= maxAttempts) {
            Instant lockUntil = Instant.now().plus(
                    appProperties.getSecurity().getAccountLockoutMinutes(),
                    ChronoUnit.MINUTES
            );
            user.lockAccount(lockUntil);
            logger.warn("Account locked due to failed login attempts: {}", maskEmail(user.getEmail()));
        }

        userRepository.save(user);
    }

    /**
     * Gets the client IP address from the request.
     */
    private String getClientIpAddress(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    /**
     * Masks an email address for logging.
     */
    private String maskEmail(String email) {
        if (email == null || !email.contains("@")) {
            return "***";
        }
        int atIndex = email.indexOf("@");
        if (atIndex <= 1) {
            return "***" + email.substring(atIndex);
        }
        return email.charAt(0) + "***" + email.substring(atIndex);
    }

    /**
     * Result of a login operation.
     */
    public record LoginResult(
            LoginResponse response,
            String accessToken,
            TokenService.RefreshTokenDetails refreshToken
    ) {}

    /**
     * Result of a token refresh operation.
     */
    public record RefreshResult(
            TokenRefreshResponse response,
            String accessToken,
            TokenService.RefreshTokenDetails refreshToken
    ) {}
}
