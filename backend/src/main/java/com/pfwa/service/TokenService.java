package com.pfwa.service;

import com.pfwa.config.AppProperties;
import com.pfwa.entity.RefreshToken;
import com.pfwa.entity.TokenType;
import com.pfwa.entity.User;
import com.pfwa.entity.VerificationToken;
import com.pfwa.exception.InvalidTokenException;
import com.pfwa.exception.TokenAlreadyUsedException;
import com.pfwa.exception.TokenExpiredException;
import com.pfwa.exception.TokenReuseException;
import com.pfwa.repository.RefreshTokenRepository;
import com.pfwa.repository.VerificationTokenRepository;
import com.pfwa.security.JwtTokenProvider;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Service for managing tokens (access, refresh, verification, password reset).
 */
@Service
public class TokenService {

    private static final Logger logger = LoggerFactory.getLogger(TokenService.class);

    private static final SecureRandom secureRandom = new SecureRandom();

    private final JwtTokenProvider jwtTokenProvider;
    private final RefreshTokenRepository refreshTokenRepository;
    private final VerificationTokenRepository verificationTokenRepository;
    private final AppProperties appProperties;

    public TokenService(JwtTokenProvider jwtTokenProvider,
                        RefreshTokenRepository refreshTokenRepository,
                        VerificationTokenRepository verificationTokenRepository,
                        AppProperties appProperties) {
        this.jwtTokenProvider = jwtTokenProvider;
        this.refreshTokenRepository = refreshTokenRepository;
        this.verificationTokenRepository = verificationTokenRepository;
        this.appProperties = appProperties;
    }

    /**
     * Creates an access token for the given user and session.
     */
    public String createAccessToken(User user, UUID sessionId) {
        return jwtTokenProvider.generateAccessToken(user, sessionId);
    }

    /**
     * Creates a new refresh token for the user.
     *
     * @param user the user
     * @param rememberMe whether to use extended expiration
     * @param request the HTTP request for device info
     * @return the refresh token details containing the token and session ID
     */
    @Transactional
    public RefreshTokenDetails createRefreshToken(User user, boolean rememberMe, HttpServletRequest request) {
        // Generate a new family ID for this login session
        UUID familyId = UUID.randomUUID();
        return createRefreshTokenInFamily(user, familyId, rememberMe, request);
    }

    /**
     * Creates a new refresh token within an existing family (for rotation).
     */
    @Transactional
    public RefreshTokenDetails createRefreshTokenInFamily(User user, UUID familyId,
                                                           boolean rememberMe, HttpServletRequest request) {
        // Check session limit
        enforceSessionLimit(user);

        // Generate token
        String token = generateSecureToken();
        String tokenHash = hashToken(token);

        // Calculate expiration
        int expirationDays = rememberMe ?
                appProperties.getJwt().getRefreshTokenRememberMeDays() :
                appProperties.getJwt().getRefreshTokenExpirationDays();
        Instant expiresAt = Instant.now().plus(expirationDays, ChronoUnit.DAYS);

        // Extract device info
        Map<String, String> deviceInfo = extractDeviceInfo(request);

        // Create and save refresh token
        RefreshToken refreshToken = new RefreshToken(user, tokenHash, familyId, expiresAt, deviceInfo);
        refreshTokenRepository.save(refreshToken);

        logger.debug("Created refresh token for user {} with family {}", user.getId(), familyId);

        return new RefreshTokenDetails(token, refreshToken.getId(), familyId, expiresAt);
    }

    /**
     * Validates and rotates a refresh token.
     *
     * @param token the refresh token
     * @param rememberMe whether to use extended expiration for the new token
     * @param request the HTTP request
     * @return the new refresh token details
     */
    @Transactional
    public RefreshTokenRotationResult rotateRefreshToken(String token, boolean rememberMe,
                                                          HttpServletRequest request) {
        String tokenHash = hashToken(token);

        // Find the token
        Optional<RefreshToken> existingTokenOpt = refreshTokenRepository.findByTokenHash(tokenHash);

        if (existingTokenOpt.isEmpty()) {
            logger.debug("Refresh token not found");
            throw new InvalidTokenException("Invalid refresh token");
        }

        RefreshToken existingToken = existingTokenOpt.get();

        // Check if token is revoked (potential reuse attack)
        if (existingToken.isRevoked()) {
            // Token reuse detected! Revoke entire family
            logger.warn("Refresh token reuse detected for user {} - revoking all tokens in family {}",
                    existingToken.getUser().getId(), existingToken.getFamilyId());

            refreshTokenRepository.revokeByFamilyId(existingToken.getFamilyId(), Instant.now());

            throw new TokenReuseException(
                    "Security alert: Token reuse detected. All sessions have been invalidated."
            );
        }

        // Check if expired
        if (existingToken.isExpired()) {
            logger.debug("Refresh token expired");
            throw new TokenExpiredException("Refresh token has expired. Please log in again.");
        }

        // Revoke the current token
        existingToken.revoke();
        refreshTokenRepository.save(existingToken);

        // Create a new token in the same family
        User user = existingToken.getUser();
        RefreshTokenDetails newToken = createRefreshTokenInFamily(
                user, existingToken.getFamilyId(), rememberMe, request
        );

        // Create new access token
        String accessToken = createAccessToken(user, newToken.sessionId());

        return new RefreshTokenRotationResult(user, accessToken, newToken);
    }

    /**
     * Revokes a refresh token.
     */
    @Transactional
    public void revokeRefreshToken(String token) {
        String tokenHash = hashToken(token);
        refreshTokenRepository.findByTokenHash(tokenHash)
                .ifPresent(refreshToken -> {
                    refreshToken.revoke();
                    refreshTokenRepository.save(refreshToken);
                    logger.debug("Revoked refresh token {}", refreshToken.getId());
                });
    }

    /**
     * Revokes all refresh tokens for a user.
     */
    @Transactional
    public void revokeAllUserTokens(UUID userId) {
        int revoked = refreshTokenRepository.revokeAllByUserId(userId, Instant.now());
        logger.debug("Revoked {} refresh tokens for user {}", revoked, userId);
    }

    /**
     * Revokes a specific session by ID.
     */
    @Transactional
    public void revokeSession(UUID sessionId) {
        refreshTokenRepository.revokeById(sessionId, Instant.now());
        logger.debug("Revoked session {}", sessionId);
    }

    /**
     * Creates an email verification token.
     */
    @Transactional
    public String createVerificationToken(User user) {
        // Invalidate existing verification tokens
        verificationTokenRepository.invalidateTokensByUserIdAndType(
                user.getId(), TokenType.EMAIL_VERIFICATION, Instant.now()
        );

        // Generate token
        String token = UUID.randomUUID().toString();
        Instant expiresAt = Instant.now().plus(
                appProperties.getEmail().getVerificationExpirationHours(), ChronoUnit.HOURS
        );

        VerificationToken verificationToken = new VerificationToken(
                user, token, TokenType.EMAIL_VERIFICATION, expiresAt
        );
        verificationTokenRepository.save(verificationToken);

        logger.debug("Created email verification token for user {}", user.getId());
        return token;
    }

    /**
     * Validates an email verification token.
     */
    @Transactional
    public User validateVerificationToken(String token) {
        VerificationToken verificationToken = verificationTokenRepository
                .findValidToken(token, Instant.now())
                .orElseThrow(() -> {
                    // Check if token exists but is used/expired
                    Optional<VerificationToken> existingToken = verificationTokenRepository.findByToken(token);
                    if (existingToken.isPresent()) {
                        if (existingToken.get().isUsed()) {
                            throw new TokenAlreadyUsedException("Email has already been verified.");
                        }
                        if (existingToken.get().isExpired()) {
                            throw new TokenExpiredException("Verification link has expired. Please request a new one.");
                        }
                    }
                    return new InvalidTokenException("Invalid verification token");
                });

        // Mark as used
        verificationToken.markAsUsed();
        verificationTokenRepository.save(verificationToken);

        return verificationToken.getUser();
    }

    /**
     * Creates a password reset token.
     */
    @Transactional
    public String createPasswordResetToken(User user) {
        // Invalidate existing password reset tokens
        verificationTokenRepository.invalidateTokensByUserIdAndType(
                user.getId(), TokenType.PASSWORD_RESET, Instant.now()
        );

        // Generate a secure random token (32 bytes = 64 hex characters)
        String token = generateSecureToken();
        Instant expiresAt = Instant.now().plus(
                appProperties.getEmail().getPasswordResetExpirationHours(), ChronoUnit.HOURS
        );

        VerificationToken resetToken = new VerificationToken(
                user, token, TokenType.PASSWORD_RESET, expiresAt
        );
        verificationTokenRepository.save(resetToken);

        logger.debug("Created password reset token for user {}", user.getId());
        return token;
    }

    /**
     * Validates a password reset token.
     */
    @Transactional
    public User validatePasswordResetToken(String token) {
        VerificationToken resetToken = verificationTokenRepository
                .findValidToken(token, Instant.now())
                .orElseThrow(() -> {
                    Optional<VerificationToken> existingToken = verificationTokenRepository.findByToken(token);
                    if (existingToken.isPresent()) {
                        if (existingToken.get().isUsed()) {
                            throw new TokenAlreadyUsedException("This password reset link has already been used.");
                        }
                        if (existingToken.get().isExpired()) {
                            throw new TokenExpiredException("Password reset link has expired. Please request a new one.");
                        }
                    }
                    return new InvalidTokenException("Invalid password reset token");
                });

        // Verify it's a password reset token
        if (resetToken.getTokenType() != TokenType.PASSWORD_RESET) {
            throw new InvalidTokenException("Invalid password reset token");
        }

        // Mark as used
        resetToken.markAsUsed();
        verificationTokenRepository.save(resetToken);

        return resetToken.getUser();
    }

    /**
     * Counts verification emails sent to a user in the time window.
     */
    public long countVerificationEmailsSent(UUID userId, int windowMinutes) {
        Instant since = Instant.now().minus(windowMinutes, ChronoUnit.MINUTES);
        return verificationTokenRepository.countTokensCreatedSince(
                userId, TokenType.EMAIL_VERIFICATION, since
        );
    }

    /**
     * Counts password reset requests for a user in the time window.
     */
    public long countPasswordResetRequests(UUID userId, int windowMinutes) {
        Instant since = Instant.now().minus(windowMinutes, ChronoUnit.MINUTES);
        return verificationTokenRepository.countTokensCreatedSince(
                userId, TokenType.PASSWORD_RESET, since
        );
    }

    /**
     * Gets the access token expiration time in seconds.
     */
    public int getAccessTokenExpirationSeconds() {
        return jwtTokenProvider.getAccessTokenExpirationSeconds();
    }

    /**
     * Generates a cryptographically secure random token.
     */
    private String generateSecureToken() {
        byte[] tokenBytes = new byte[32];
        secureRandom.nextBytes(tokenBytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(tokenBytes);
    }

    /**
     * Hashes a token using SHA-256.
     */
    private String hashToken(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(token.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm not available", e);
        }
    }

    /**
     * Extracts device information from the request.
     */
    private Map<String, String> extractDeviceInfo(HttpServletRequest request) {
        Map<String, String> deviceInfo = new HashMap<>();

        if (request != null) {
            String userAgent = request.getHeader("User-Agent");
            if (userAgent != null) {
                deviceInfo.put("userAgent", userAgent);
                deviceInfo.put("browser", parseBrowser(userAgent));
                deviceInfo.put("os", parseOperatingSystem(userAgent));
            }

            String ipAddress = getClientIpAddress(request);
            if (ipAddress != null) {
                deviceInfo.put("ip", ipAddress);
            }
        }

        return deviceInfo;
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
     * Parses browser name from User-Agent.
     */
    private String parseBrowser(String userAgent) {
        if (userAgent.contains("Chrome") && !userAgent.contains("Edg")) {
            return "Chrome";
        } else if (userAgent.contains("Firefox")) {
            return "Firefox";
        } else if (userAgent.contains("Safari") && !userAgent.contains("Chrome")) {
            return "Safari";
        } else if (userAgent.contains("Edg")) {
            return "Edge";
        } else if (userAgent.contains("MSIE") || userAgent.contains("Trident")) {
            return "Internet Explorer";
        }
        return "Unknown";
    }

    /**
     * Parses operating system from User-Agent.
     */
    private String parseOperatingSystem(String userAgent) {
        if (userAgent.contains("Windows")) {
            return "Windows";
        } else if (userAgent.contains("Mac OS X")) {
            return "macOS";
        } else if (userAgent.contains("Linux")) {
            return "Linux";
        } else if (userAgent.contains("iPhone") || userAgent.contains("iPad")) {
            return "iOS";
        } else if (userAgent.contains("Android")) {
            return "Android";
        }
        return "Unknown";
    }

    /**
     * Enforces the maximum sessions per user limit.
     */
    private void enforceSessionLimit(User user) {
        int maxSessions = appProperties.getSecurity().getMaxSessionsPerUser();
        long activeSessionCount = refreshTokenRepository.countActiveSessionsByUserId(user.getId(), Instant.now());

        if (activeSessionCount >= maxSessions) {
            // Revoke the oldest session
            refreshTokenRepository.findOldestActiveSessionByUserId(user.getId(), Instant.now())
                    .ifPresent(oldestSession -> {
                        oldestSession.revoke();
                        refreshTokenRepository.save(oldestSession);
                        logger.debug("Revoked oldest session {} to enforce session limit for user {}",
                                oldestSession.getId(), user.getId());
                    });
        }
    }

    /**
     * Record containing refresh token details.
     */
    public record RefreshTokenDetails(
            String token,
            UUID sessionId,
            UUID familyId,
            Instant expiresAt
    ) {}

    /**
     * Record containing the result of token rotation.
     */
    public record RefreshTokenRotationResult(
            User user,
            String accessToken,
            RefreshTokenDetails refreshToken
    ) {}
}
