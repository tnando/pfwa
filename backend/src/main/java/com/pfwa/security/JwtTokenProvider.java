package com.pfwa.security;

import com.pfwa.config.AppProperties;
import com.pfwa.entity.User;
import com.pfwa.exception.InvalidTokenException;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;

/**
 * JWT token provider for generating and validating access tokens.
 */
@Component
public class JwtTokenProvider {

    private static final Logger logger = LoggerFactory.getLogger(JwtTokenProvider.class);

    private static final String CLAIM_EMAIL = "email";
    private static final String CLAIM_TOKEN_VERSION = "tokenVersion";
    private static final String CLAIM_SESSION_ID = "sessionId";

    private final SecretKey secretKey;
    private final AppProperties appProperties;

    public JwtTokenProvider(AppProperties appProperties) {
        this.appProperties = appProperties;
        this.secretKey = Keys.hmacShaKeyFor(
                appProperties.getJwt().getSecret().getBytes(StandardCharsets.UTF_8)
        );
    }

    /**
     * Generates an access token for the given user.
     *
     * @param user the user to generate token for
     * @param sessionId the session ID to include in the token
     * @return the JWT access token
     */
    public String generateAccessToken(User user, UUID sessionId) {
        Instant now = Instant.now();
        Instant expiration = now.plusSeconds(
                appProperties.getJwt().getAccessTokenExpirationMinutes() * 60L
        );

        return Jwts.builder()
                .subject(user.getId().toString())
                .claim(CLAIM_EMAIL, user.getEmail())
                .claim(CLAIM_TOKEN_VERSION, user.getTokenVersion())
                .claim(CLAIM_SESSION_ID, sessionId.toString())
                .issuer(appProperties.getJwt().getIssuer())
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiration))
                .signWith(secretKey)
                .compact();
    }

    /**
     * Validates an access token and returns its claims.
     *
     * @param token the JWT token to validate
     * @return the token claims
     * @throws InvalidTokenException if the token is invalid
     */
    public Claims validateAccessToken(String token) {
        try {
            return Jwts.parser()
                    .verifyWith(secretKey)
                    .requireIssuer(appProperties.getJwt().getIssuer())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (ExpiredJwtException e) {
            logger.debug("JWT token expired");
            throw new InvalidTokenException("Access token has expired");
        } catch (JwtException e) {
            logger.debug("Invalid JWT token: {}", e.getMessage());
            throw new InvalidTokenException("Invalid access token");
        }
    }

    /**
     * Extracts the user ID from a token.
     *
     * @param token the JWT token
     * @return the user ID
     */
    public UUID getUserIdFromToken(String token) {
        Claims claims = validateAccessToken(token);
        return UUID.fromString(claims.getSubject());
    }

    /**
     * Extracts the email from a token.
     *
     * @param token the JWT token
     * @return the email
     */
    public String getEmailFromToken(String token) {
        Claims claims = validateAccessToken(token);
        return claims.get(CLAIM_EMAIL, String.class);
    }

    /**
     * Extracts the token version from a token.
     *
     * @param token the JWT token
     * @return the token version
     */
    public int getTokenVersionFromToken(String token) {
        Claims claims = validateAccessToken(token);
        return claims.get(CLAIM_TOKEN_VERSION, Integer.class);
    }

    /**
     * Extracts the session ID from a token.
     *
     * @param token the JWT token
     * @return the session ID
     */
    public UUID getSessionIdFromToken(String token) {
        Claims claims = validateAccessToken(token);
        String sessionId = claims.get(CLAIM_SESSION_ID, String.class);
        return sessionId != null ? UUID.fromString(sessionId) : null;
    }

    /**
     * Gets the access token expiration time in seconds.
     *
     * @return expiration time in seconds
     */
    public int getAccessTokenExpirationSeconds() {
        return appProperties.getJwt().getAccessTokenExpirationMinutes() * 60;
    }

    /**
     * Validates that the token version matches the user's current version.
     *
     * @param token the JWT token
     * @param currentVersion the user's current token version
     * @return true if versions match
     */
    public boolean validateTokenVersion(String token, int currentVersion) {
        try {
            int tokenVersion = getTokenVersionFromToken(token);
            return tokenVersion == currentVersion;
        } catch (InvalidTokenException e) {
            return false;
        }
    }

    /**
     * Checks if a token is expired without throwing an exception.
     *
     * @param token the JWT token
     * @return true if expired
     */
    public boolean isTokenExpired(String token) {
        try {
            validateAccessToken(token);
            return false;
        } catch (InvalidTokenException e) {
            return true;
        }
    }
}
