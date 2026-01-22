package com.pfwa.repository;

import com.pfwa.entity.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for RefreshToken entity operations.
 * Handles JWT refresh token management with rotation support.
 */
@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, UUID> {

    /**
     * Finds a refresh token by its hash.
     *
     * @param tokenHash the SHA-256 hash of the token
     * @return Optional containing the token if found
     */
    Optional<RefreshToken> findByTokenHash(String tokenHash);

    /**
     * Finds an active (not revoked, not expired) token by its hash.
     *
     * @param tokenHash the SHA-256 hash of the token
     * @param now the current timestamp for expiration check
     * @return Optional containing the token if found and active
     */
    @Query("SELECT t FROM RefreshToken t WHERE t.tokenHash = :tokenHash " +
           "AND t.revokedAt IS NULL AND t.expiresAt > :now")
    Optional<RefreshToken> findActiveByTokenHash(@Param("tokenHash") String tokenHash,
                                                  @Param("now") Instant now);

    /**
     * Finds all active sessions for a user.
     * Used for session management display.
     *
     * @param userId the user ID
     * @return list of active refresh tokens ordered by creation date
     */
    @Query("SELECT t FROM RefreshToken t WHERE t.user.id = :userId " +
           "AND t.revokedAt IS NULL AND t.expiresAt > :now " +
           "ORDER BY t.createdAt DESC")
    List<RefreshToken> findActiveSessionsByUserId(@Param("userId") UUID userId, @Param("now") Instant now);

    /**
     * Counts active sessions for a user.
     * Used to enforce maximum session limit.
     *
     * @param userId the user ID
     * @param now the current timestamp
     * @return count of active sessions
     */
    @Query("SELECT COUNT(t) FROM RefreshToken t WHERE t.user.id = :userId " +
           "AND t.revokedAt IS NULL AND t.expiresAt > :now")
    long countActiveSessionsByUserId(@Param("userId") UUID userId, @Param("now") Instant now);

    /**
     * Revokes all tokens in a token family.
     * Used when token reuse is detected (potential theft).
     *
     * @param familyId the token family ID
     * @param revokedAt the revocation timestamp
     * @return number of tokens revoked
     */
    @Modifying
    @Query("UPDATE RefreshToken t SET t.revokedAt = :revokedAt " +
           "WHERE t.familyId = :familyId AND t.revokedAt IS NULL")
    int revokeByFamilyId(@Param("familyId") UUID familyId, @Param("revokedAt") Instant revokedAt);

    /**
     * Revokes all active tokens for a user.
     * Used for logout-all-sessions functionality.
     *
     * @param userId the user ID
     * @param revokedAt the revocation timestamp
     * @return number of tokens revoked
     */
    @Modifying
    @Query("UPDATE RefreshToken t SET t.revokedAt = :revokedAt " +
           "WHERE t.user.id = :userId AND t.revokedAt IS NULL")
    int revokeAllByUserId(@Param("userId") UUID userId, @Param("revokedAt") Instant revokedAt);

    /**
     * Revokes a specific token by its ID.
     *
     * @param tokenId the token ID
     * @param revokedAt the revocation timestamp
     * @return number of tokens revoked (0 or 1)
     */
    @Modifying
    @Query("UPDATE RefreshToken t SET t.revokedAt = :revokedAt WHERE t.id = :tokenId")
    int revokeById(@Param("tokenId") UUID tokenId, @Param("revokedAt") Instant revokedAt);

    /**
     * Deletes all expired tokens.
     * Should be run periodically to clean up the table.
     *
     * @param now the current timestamp
     * @return number of tokens deleted
     */
    @Modifying
    @Query("DELETE FROM RefreshToken t WHERE t.expiresAt < :now")
    int deleteExpiredTokens(@Param("now") Instant now);

    /**
     * Deletes all revoked tokens older than the specified timestamp.
     * Used for additional cleanup of revoked tokens.
     *
     * @param before tokens revoked before this timestamp will be deleted
     * @return number of tokens deleted
     */
    @Modifying
    @Query("DELETE FROM RefreshToken t WHERE t.revokedAt IS NOT NULL AND t.revokedAt < :before")
    int deleteRevokedTokensBefore(@Param("before") Instant before);

    /**
     * Finds tokens by family ID.
     * Used for debugging and audit purposes.
     *
     * @param familyId the token family ID
     * @return list of tokens in the family
     */
    List<RefreshToken> findByFamilyIdOrderByCreatedAtDesc(UUID familyId);

    /**
     * Checks if any token in the family has been revoked.
     * Used to detect if a token reuse attack has occurred.
     *
     * @param familyId the token family ID
     * @return true if any token in the family is revoked
     */
    @Query("SELECT CASE WHEN COUNT(t) > 0 THEN true ELSE false END FROM RefreshToken t " +
           "WHERE t.familyId = :familyId AND t.revokedAt IS NOT NULL")
    boolean isFamilyCompromised(@Param("familyId") UUID familyId);

    /**
     * Gets the oldest active session for a user.
     * Used when enforcing maximum session limits.
     *
     * @param userId the user ID
     * @param now the current timestamp
     * @return Optional containing the oldest active token
     */
    @Query("SELECT t FROM RefreshToken t WHERE t.user.id = :userId " +
           "AND t.revokedAt IS NULL AND t.expiresAt > :now " +
           "ORDER BY t.createdAt ASC LIMIT 1")
    Optional<RefreshToken> findOldestActiveSessionByUserId(@Param("userId") UUID userId,
                                                            @Param("now") Instant now);

    /**
     * Checks if a token exists and is active.
     *
     * @param tokenHash the SHA-256 hash of the token
     * @param now the current timestamp
     * @return true if the token exists and is active
     */
    @Query("SELECT CASE WHEN COUNT(t) > 0 THEN true ELSE false END FROM RefreshToken t " +
           "WHERE t.tokenHash = :tokenHash AND t.revokedAt IS NULL AND t.expiresAt > :now")
    boolean isTokenActive(@Param("tokenHash") String tokenHash, @Param("now") Instant now);
}
