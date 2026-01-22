package com.pfwa.service;

import com.pfwa.repository.RefreshTokenRepository;
import com.pfwa.repository.UserRepository;
import com.pfwa.repository.VerificationTokenRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

/**
 * Service for periodic cleanup of expired tokens and unlocking accounts.
 */
@Service
public class CleanupService {

    private static final Logger logger = LoggerFactory.getLogger(CleanupService.class);

    private final RefreshTokenRepository refreshTokenRepository;
    private final VerificationTokenRepository verificationTokenRepository;
    private final UserRepository userRepository;
    private final RateLimitService rateLimitService;

    public CleanupService(RefreshTokenRepository refreshTokenRepository,
                          VerificationTokenRepository verificationTokenRepository,
                          UserRepository userRepository,
                          RateLimitService rateLimitService) {
        this.refreshTokenRepository = refreshTokenRepository;
        this.verificationTokenRepository = verificationTokenRepository;
        this.userRepository = userRepository;
        this.rateLimitService = rateLimitService;
    }

    /**
     * Cleans up expired tokens daily at 2 AM.
     */
    @Scheduled(cron = "0 0 2 * * *")
    @Transactional
    public void cleanupExpiredTokens() {
        logger.info("Starting scheduled token cleanup");
        Instant now = Instant.now();

        // Delete expired refresh tokens
        int expiredRefreshTokens = refreshTokenRepository.deleteExpiredTokens(now);
        logger.info("Deleted {} expired refresh tokens", expiredRefreshTokens);

        // Delete revoked refresh tokens older than 7 days
        Instant sevenDaysAgo = now.minus(7, ChronoUnit.DAYS);
        int revokedRefreshTokens = refreshTokenRepository.deleteRevokedTokensBefore(sevenDaysAgo);
        logger.info("Deleted {} old revoked refresh tokens", revokedRefreshTokens);

        // Delete expired verification tokens
        int expiredVerificationTokens = verificationTokenRepository.deleteExpiredTokens(now);
        logger.info("Deleted {} expired verification tokens", expiredVerificationTokens);

        // Delete used verification tokens older than 30 days
        Instant thirtyDaysAgo = now.minus(30, ChronoUnit.DAYS);
        int usedVerificationTokens = verificationTokenRepository.deleteUsedTokensBefore(thirtyDaysAgo);
        logger.info("Deleted {} old used verification tokens", usedVerificationTokens);

        logger.info("Token cleanup completed");
    }

    /**
     * Unlocks expired account locks every 5 minutes.
     */
    @Scheduled(fixedRate = 300000) // 5 minutes
    @Transactional
    public void unlockExpiredAccounts() {
        Instant now = Instant.now();
        int unlockedAccounts = userRepository.unlockExpiredAccounts(now);

        if (unlockedAccounts > 0) {
            logger.info("Unlocked {} expired account locks", unlockedAccounts);
        }
    }

    /**
     * Cleans up rate limit entries every hour.
     */
    @Scheduled(fixedRate = 3600000) // 1 hour
    public void cleanupRateLimits() {
        rateLimitService.cleanup();
        logger.debug("Rate limit cleanup completed");
    }
}
