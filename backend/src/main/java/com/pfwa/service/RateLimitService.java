package com.pfwa.service;

import com.pfwa.exception.RateLimitExceededException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory rate limiting service.
 * Uses a simple sliding window approach for rate limiting.
 *
 * Note: For production with multiple instances, consider using Redis-based rate limiting.
 */
@Service
public class RateLimitService {

    private static final Logger logger = LoggerFactory.getLogger(RateLimitService.class);

    // Map of operation:key -> list of timestamps
    private final Map<String, RateLimitBucket> buckets = new ConcurrentHashMap<>();

    /**
     * Checks if the operation is allowed for the given key.
     *
     * @param operation the operation type (e.g., "registration", "login")
     * @param key the key to rate limit by (e.g., IP address, email)
     * @param maxAttempts maximum number of attempts allowed
     * @param windowMinutes time window in minutes
     * @throws RateLimitExceededException if rate limit is exceeded
     */
    public void checkRateLimit(String operation, String key, int maxAttempts, int windowMinutes) {
        String bucketKey = operation + ":" + key;
        Instant now = Instant.now();
        Instant windowStart = now.minusSeconds(windowMinutes * 60L);

        RateLimitBucket bucket = buckets.compute(bucketKey, (k, existing) -> {
            if (existing == null) {
                return new RateLimitBucket();
            }
            existing.cleanOldEntries(windowStart);
            return existing;
        });

        synchronized (bucket) {
            if (bucket.getCount() >= maxAttempts) {
                long retryAfterSeconds = windowMinutes * 60L -
                        (now.getEpochSecond() - bucket.getOldestTimestamp().getEpochSecond());
                retryAfterSeconds = Math.max(retryAfterSeconds, 1);

                logger.info("Rate limit exceeded for {} on operation {}", key, operation);
                throw new RateLimitExceededException(
                        "Too many attempts. Please try again later.",
                        retryAfterSeconds
                );
            }
            bucket.addAttempt(now);
        }
    }

    /**
     * Records an attempt without checking the limit.
     * Useful for tracking failed attempts separately.
     *
     * @param operation the operation type
     * @param key the key to track
     */
    public void recordAttempt(String operation, String key) {
        String bucketKey = operation + ":" + key;
        Instant now = Instant.now();

        buckets.compute(bucketKey, (k, existing) -> {
            if (existing == null) {
                RateLimitBucket bucket = new RateLimitBucket();
                bucket.addAttempt(now);
                return bucket;
            }
            existing.addAttempt(now);
            return existing;
        });
    }

    /**
     * Resets the rate limit for a specific operation and key.
     *
     * @param operation the operation type
     * @param key the key to reset
     */
    public void resetRateLimit(String operation, String key) {
        String bucketKey = operation + ":" + key;
        buckets.remove(bucketKey);
    }

    /**
     * Gets the remaining attempts for an operation and key.
     *
     * @param operation the operation type
     * @param key the key
     * @param maxAttempts maximum allowed attempts
     * @param windowMinutes time window
     * @return remaining attempts
     */
    public int getRemainingAttempts(String operation, String key, int maxAttempts, int windowMinutes) {
        String bucketKey = operation + ":" + key;
        Instant windowStart = Instant.now().minusSeconds(windowMinutes * 60L);

        RateLimitBucket bucket = buckets.get(bucketKey);
        if (bucket == null) {
            return maxAttempts;
        }

        synchronized (bucket) {
            bucket.cleanOldEntries(windowStart);
            return Math.max(0, maxAttempts - bucket.getCount());
        }
    }

    /**
     * Cleans up expired rate limit entries.
     * Should be called periodically (e.g., every hour).
     */
    public void cleanup() {
        Instant cutoff = Instant.now().minusSeconds(3600); // 1 hour
        buckets.entrySet().removeIf(entry -> {
            RateLimitBucket bucket = entry.getValue();
            synchronized (bucket) {
                bucket.cleanOldEntries(cutoff);
                return bucket.isEmpty();
            }
        });
    }

    /**
     * Internal bucket for tracking rate limit attempts.
     */
    private static class RateLimitBucket {
        private final java.util.Deque<Instant> attempts = new java.util.ArrayDeque<>();

        void addAttempt(Instant timestamp) {
            attempts.addLast(timestamp);
        }

        void cleanOldEntries(Instant windowStart) {
            while (!attempts.isEmpty() && attempts.peekFirst().isBefore(windowStart)) {
                attempts.pollFirst();
            }
        }

        int getCount() {
            return attempts.size();
        }

        boolean isEmpty() {
            return attempts.isEmpty();
        }

        Instant getOldestTimestamp() {
            return attempts.peekFirst();
        }
    }
}
