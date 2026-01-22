package com.pfwa.repository;

import com.pfwa.entity.TokenType;
import com.pfwa.entity.VerificationToken;
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
 * Repository for VerificationToken entity operations.
 * Handles email verification and password reset tokens.
 */
@Repository
public interface VerificationTokenRepository extends JpaRepository<VerificationToken, UUID> {

    /**
     * Finds a token by its value.
     *
     * @param token the token string
     * @return Optional containing the token if found
     */
    Optional<VerificationToken> findByToken(String token);

    /**
     * Finds a valid (unused, not expired) token by its value.
     *
     * @param token the token string
     * @param now the current timestamp for expiration check
     * @return Optional containing the token if found and valid
     */
    @Query("SELECT t FROM VerificationToken t WHERE t.token = :token " +
           "AND t.usedAt IS NULL AND t.expiresAt > :now")
    Optional<VerificationToken> findValidToken(@Param("token") String token, @Param("now") Instant now);

    /**
     * Finds all tokens for a user with a specific type.
     *
     * @param userId the user ID
     * @param tokenType the type of token
     * @return list of matching tokens
     */
    List<VerificationToken> findByUserIdAndTokenType(UUID userId, TokenType tokenType);

    /**
     * Finds the most recent unused token for a user with a specific type.
     *
     * @param userId the user ID
     * @param tokenType the type of token
     * @return Optional containing the most recent valid token
     */
    @Query("SELECT t FROM VerificationToken t WHERE t.user.id = :userId " +
           "AND t.tokenType = :tokenType AND t.usedAt IS NULL " +
           "ORDER BY t.createdAt DESC LIMIT 1")
    Optional<VerificationToken> findLatestUnusedByUserIdAndTokenType(@Param("userId") UUID userId,
                                                                      @Param("tokenType") TokenType tokenType);

    /**
     * Counts unused tokens created within the time window for a user and type.
     * Used for rate limiting token creation.
     *
     * @param userId the user ID
     * @param tokenType the type of token
     * @param since the start of the time window
     * @return count of tokens created in the window
     */
    @Query("SELECT COUNT(t) FROM VerificationToken t WHERE t.user.id = :userId " +
           "AND t.tokenType = :tokenType AND t.createdAt >= :since")
    long countTokensCreatedSince(@Param("userId") UUID userId,
                                  @Param("tokenType") TokenType tokenType,
                                  @Param("since") Instant since);

    /**
     * Invalidates all existing tokens for a user of a specific type.
     * Used when generating a new token to invalidate previous ones.
     *
     * @param userId the user ID
     * @param tokenType the type of token
     * @param usedAt the timestamp to mark as used
     * @return number of tokens invalidated
     */
    @Modifying
    @Query("UPDATE VerificationToken t SET t.usedAt = :usedAt " +
           "WHERE t.user.id = :userId AND t.tokenType = :tokenType AND t.usedAt IS NULL")
    int invalidateTokensByUserIdAndType(@Param("userId") UUID userId,
                                         @Param("tokenType") TokenType tokenType,
                                         @Param("usedAt") Instant usedAt);

    /**
     * Deletes all expired tokens.
     * Should be run periodically (e.g., daily) to clean up the table.
     *
     * @param now the current timestamp
     * @return number of tokens deleted
     */
    @Modifying
    @Query("DELETE FROM VerificationToken t WHERE t.expiresAt < :now")
    int deleteExpiredTokens(@Param("now") Instant now);

    /**
     * Deletes all used tokens older than the specified timestamp.
     * Used for additional cleanup of used tokens.
     *
     * @param before tokens used before this timestamp will be deleted
     * @return number of tokens deleted
     */
    @Modifying
    @Query("DELETE FROM VerificationToken t WHERE t.usedAt IS NOT NULL AND t.usedAt < :before")
    int deleteUsedTokensBefore(@Param("before") Instant before);

    /**
     * Checks if a token exists and is valid.
     *
     * @param token the token string
     * @param now the current timestamp
     * @return true if the token exists and is valid
     */
    @Query("SELECT CASE WHEN COUNT(t) > 0 THEN true ELSE false END FROM VerificationToken t " +
           "WHERE t.token = :token AND t.usedAt IS NULL AND t.expiresAt > :now")
    boolean isTokenValid(@Param("token") String token, @Param("now") Instant now);

    /**
     * Marks a token as used.
     *
     * @param tokenId the token ID
     * @param usedAt the timestamp when used
     * @return number of rows affected
     */
    @Modifying
    @Query("UPDATE VerificationToken t SET t.usedAt = :usedAt WHERE t.id = :tokenId")
    int markAsUsed(@Param("tokenId") UUID tokenId, @Param("usedAt") Instant usedAt);
}
