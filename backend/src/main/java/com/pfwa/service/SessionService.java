package com.pfwa.service;

import com.pfwa.dto.auth.SessionDto;
import com.pfwa.entity.RefreshToken;
import com.pfwa.entity.User;
import com.pfwa.exception.SessionNotFoundException;
import com.pfwa.repository.RefreshTokenRepository;
import com.pfwa.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Service for managing user sessions.
 */
@Service
public class SessionService {

    private static final Logger logger = LoggerFactory.getLogger(SessionService.class);

    private final RefreshTokenRepository refreshTokenRepository;
    private final UserRepository userRepository;
    private final EmailService emailService;

    public SessionService(RefreshTokenRepository refreshTokenRepository,
                          UserRepository userRepository,
                          EmailService emailService) {
        this.refreshTokenRepository = refreshTokenRepository;
        this.userRepository = userRepository;
        this.emailService = emailService;
    }

    /**
     * Gets all active sessions for a user.
     *
     * @param userId the user ID
     * @param currentSessionId the current session ID to mark as current
     * @return list of active sessions
     */
    @Transactional(readOnly = true)
    public List<SessionDto> getActiveSessions(UUID userId, UUID currentSessionId) {
        List<RefreshToken> sessions = refreshTokenRepository.findActiveSessionsByUserId(userId, Instant.now());

        return sessions.stream()
                .map(session -> SessionDto.fromEntity(session, currentSessionId))
                .collect(Collectors.toList());
    }

    /**
     * Revokes a specific session.
     *
     * @param userId the user ID (for authorization check)
     * @param sessionId the session ID to revoke
     * @param currentSessionId the current session ID (cannot revoke current session)
     */
    @Transactional
    public void revokeSession(UUID userId, UUID sessionId, UUID currentSessionId) {
        // Check if trying to revoke current session
        if (sessionId.equals(currentSessionId)) {
            throw new IllegalArgumentException("Cannot revoke current session. Use logout instead.");
        }

        // Find the session
        RefreshToken session = refreshTokenRepository.findById(sessionId)
                .orElseThrow(() -> new SessionNotFoundException("Session not found"));

        // Verify ownership
        if (!session.getUser().getId().equals(userId)) {
            logger.warn("User {} attempted to revoke session {} belonging to another user",
                    userId, sessionId);
            throw new AccessDeniedException("You do not have permission to revoke this session");
        }

        // Check if already revoked
        if (session.isRevoked()) {
            throw new SessionNotFoundException("Session not found");
        }

        // Revoke the session
        session.revoke();
        refreshTokenRepository.save(session);

        logger.info("Session {} revoked by user {}", sessionId, userId);
    }

    /**
     * Revokes all sessions for a user (logout all devices).
     *
     * @param userId the user ID
     */
    @Transactional
    public void revokeAllSessions(UUID userId) {
        // Increment token version to invalidate all JWTs
        userRepository.incrementTokenVersion(userId);

        // Revoke all refresh tokens
        int revoked = refreshTokenRepository.revokeAllByUserId(userId, Instant.now());

        logger.info("Revoked {} sessions for user {} (logout all)", revoked, userId);

        // Send security alert email
        userRepository.findById(userId).ifPresent(emailService::sendAllSessionsLogoutAlert);
    }

    /**
     * Counts active sessions for a user.
     *
     * @param userId the user ID
     * @return number of active sessions
     */
    @Transactional(readOnly = true)
    public long countActiveSessions(UUID userId) {
        return refreshTokenRepository.countActiveSessionsByUserId(userId, Instant.now());
    }

    /**
     * Checks if a session is valid (exists, not revoked, not expired).
     *
     * @param sessionId the session ID
     * @return true if session is valid
     */
    @Transactional(readOnly = true)
    public boolean isSessionValid(UUID sessionId) {
        return refreshTokenRepository.findById(sessionId)
                .map(RefreshToken::isActive)
                .orElse(false);
    }

    /**
     * Gets session details by ID.
     *
     * @param sessionId the session ID
     * @param currentSessionId the current session ID
     * @return the session DTO or null if not found
     */
    @Transactional(readOnly = true)
    public SessionDto getSessionById(UUID sessionId, UUID currentSessionId) {
        return refreshTokenRepository.findById(sessionId)
                .filter(RefreshToken::isActive)
                .map(session -> SessionDto.fromEntity(session, currentSessionId))
                .orElse(null);
    }
}
