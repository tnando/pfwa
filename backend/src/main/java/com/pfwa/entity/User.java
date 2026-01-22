package com.pfwa.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * User entity representing the users table.
 * Contains authentication and profile information.
 */
@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "email", nullable = false, unique = true, length = 255)
    private String email;

    @Column(name = "password_hash", nullable = false, length = 255)
    private String passwordHash;

    @Column(name = "first_name", length = 100)
    private String firstName;

    @Column(name = "last_name", length = 100)
    private String lastName;

    @Column(name = "email_verified", nullable = false)
    private boolean emailVerified = false;

    @Column(name = "account_locked", nullable = false)
    private boolean accountLocked = false;

    @Column(name = "account_locked_until")
    private Instant accountLockedUntil;

    @Column(name = "failed_login_attempts", nullable = false)
    private int failedLoginAttempts = 0;

    @Column(name = "token_version", nullable = false)
    private int tokenVersion = 0;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    // Default constructor required by JPA
    public User() {
    }

    // Constructor for creating new users
    public User(String email, String passwordHash) {
        this.email = email;
        this.passwordHash = passwordHash;
    }

    // Getters and Setters

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public boolean isEmailVerified() {
        return emailVerified;
    }

    public void setEmailVerified(boolean emailVerified) {
        this.emailVerified = emailVerified;
    }

    public boolean isAccountLocked() {
        return accountLocked;
    }

    public void setAccountLocked(boolean accountLocked) {
        this.accountLocked = accountLocked;
    }

    public Instant getAccountLockedUntil() {
        return accountLockedUntil;
    }

    public void setAccountLockedUntil(Instant accountLockedUntil) {
        this.accountLockedUntil = accountLockedUntil;
    }

    public int getFailedLoginAttempts() {
        return failedLoginAttempts;
    }

    public void setFailedLoginAttempts(int failedLoginAttempts) {
        this.failedLoginAttempts = failedLoginAttempts;
    }

    public int getTokenVersion() {
        return tokenVersion;
    }

    public void setTokenVersion(int tokenVersion) {
        this.tokenVersion = tokenVersion;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }

    // Business logic methods

    /**
     * Increments failed login attempts counter.
     */
    public void incrementFailedLoginAttempts() {
        this.failedLoginAttempts++;
    }

    /**
     * Resets failed login attempts to zero on successful login.
     */
    public void resetFailedLoginAttempts() {
        this.failedLoginAttempts = 0;
        this.accountLocked = false;
        this.accountLockedUntil = null;
    }

    /**
     * Locks the account for the specified duration.
     *
     * @param lockUntil the instant when the lock expires
     */
    public void lockAccount(Instant lockUntil) {
        this.accountLocked = true;
        this.accountLockedUntil = lockUntil;
    }

    /**
     * Checks if the account lock has expired.
     *
     * @return true if the account was locked but the lock has expired
     */
    public boolean isLockExpired() {
        if (!accountLocked || accountLockedUntil == null) {
            return false;
        }
        return Instant.now().isAfter(accountLockedUntil);
    }

    /**
     * Unlocks the account if the lock has expired.
     *
     * @return true if the account was unlocked
     */
    public boolean unlockIfExpired() {
        if (isLockExpired()) {
            resetFailedLoginAttempts();
            return true;
        }
        return false;
    }

    /**
     * Increments token version to invalidate all existing tokens.
     * Used for logout-all-sessions functionality.
     */
    public void incrementTokenVersion() {
        this.tokenVersion++;
    }

    /**
     * Gets the user's full name.
     *
     * @return full name or null if neither first nor last name is set
     */
    public String getFullName() {
        if (firstName == null && lastName == null) {
            return null;
        }
        if (firstName == null) {
            return lastName;
        }
        if (lastName == null) {
            return firstName;
        }
        return firstName + " " + lastName;
    }

    // equals and hashCode based on id (business key)

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        User user = (User) o;
        return id != null && Objects.equals(id, user.id);
    }

    @Override
    public int hashCode() {
        // Use a constant hash code for unsaved entities
        // This ensures proper behavior in collections before and after persist
        return getClass().hashCode();
    }

    @Override
    public String toString() {
        return "User{" +
                "id=" + id +
                ", email='" + email + '\'' +
                ", firstName='" + firstName + '\'' +
                ", lastName='" + lastName + '\'' +
                ", emailVerified=" + emailVerified +
                ", accountLocked=" + accountLocked +
                ", createdAt=" + createdAt +
                '}';
    }
}
