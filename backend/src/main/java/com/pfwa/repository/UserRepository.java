package com.pfwa.repository;

import com.pfwa.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for User entity operations.
 * Provides methods for user lookup and authentication-related queries.
 */
@Repository
public interface UserRepository extends JpaRepository<User, UUID> {

    /**
     * Finds a user by their email address (case-insensitive).
     *
     * @param email the email address to search for
     * @return Optional containing the user if found
     */
    @Query("SELECT u FROM User u WHERE LOWER(u.email) = LOWER(:email)")
    Optional<User> findByEmail(@Param("email") String email);

    /**
     * Checks if a user exists with the given email address (case-insensitive).
     *
     * @param email the email address to check
     * @return true if a user exists with this email
     */
    @Query("SELECT CASE WHEN COUNT(u) > 0 THEN true ELSE false END FROM User u WHERE LOWER(u.email) = LOWER(:email)")
    boolean existsByEmail(@Param("email") String email);

    /**
     * Finds a user by ID and checks if they are verified.
     *
     * @param id the user ID
     * @return Optional containing the user if found and verified
     */
    @Query("SELECT u FROM User u WHERE u.id = :id AND u.emailVerified = true")
    Optional<User> findByIdAndEmailVerified(@Param("id") UUID id);

    /**
     * Increments the token version for a user (used for logout-all-sessions).
     *
     * @param userId the user ID
     * @return number of rows affected
     */
    @Modifying
    @Query("UPDATE User u SET u.tokenVersion = u.tokenVersion + 1, u.updatedAt = CURRENT_TIMESTAMP WHERE u.id = :userId")
    int incrementTokenVersion(@Param("userId") UUID userId);

    /**
     * Unlocks accounts whose lock period has expired.
     *
     * @param now the current timestamp
     * @return number of accounts unlocked
     */
    @Modifying
    @Query("UPDATE User u SET u.accountLocked = false, u.accountLockedUntil = null, " +
           "u.failedLoginAttempts = 0, u.updatedAt = CURRENT_TIMESTAMP " +
           "WHERE u.accountLocked = true AND u.accountLockedUntil < :now")
    int unlockExpiredAccounts(@Param("now") Instant now);

    /**
     * Updates user's failed login attempts and optionally locks the account.
     *
     * @param userId the user ID
     * @param failedAttempts the new failed attempts count
     * @param accountLocked whether to lock the account
     * @param lockedUntil when the lock expires (null if not locked)
     * @return number of rows affected
     */
    @Modifying
    @Query("UPDATE User u SET u.failedLoginAttempts = :failedAttempts, " +
           "u.accountLocked = :accountLocked, u.accountLockedUntil = :lockedUntil, " +
           "u.updatedAt = CURRENT_TIMESTAMP WHERE u.id = :userId")
    int updateLoginAttempts(@Param("userId") UUID userId,
                            @Param("failedAttempts") int failedAttempts,
                            @Param("accountLocked") boolean accountLocked,
                            @Param("lockedUntil") Instant lockedUntil);

    /**
     * Resets failed login attempts on successful login.
     *
     * @param userId the user ID
     * @return number of rows affected
     */
    @Modifying
    @Query("UPDATE User u SET u.failedLoginAttempts = 0, u.accountLocked = false, " +
           "u.accountLockedUntil = null, u.updatedAt = CURRENT_TIMESTAMP WHERE u.id = :userId")
    int resetFailedLoginAttempts(@Param("userId") UUID userId);

    /**
     * Marks a user's email as verified.
     *
     * @param userId the user ID
     * @return number of rows affected
     */
    @Modifying
    @Query("UPDATE User u SET u.emailVerified = true, u.updatedAt = CURRENT_TIMESTAMP WHERE u.id = :userId")
    int verifyEmail(@Param("userId") UUID userId);

    /**
     * Updates user's password hash.
     *
     * @param userId the user ID
     * @param passwordHash the new BCrypt password hash
     * @return number of rows affected
     */
    @Modifying
    @Query("UPDATE User u SET u.passwordHash = :passwordHash, u.updatedAt = CURRENT_TIMESTAMP WHERE u.id = :userId")
    int updatePassword(@Param("userId") UUID userId, @Param("passwordHash") String passwordHash);
}
