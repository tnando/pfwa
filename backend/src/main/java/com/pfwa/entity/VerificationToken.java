package com.pfwa.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcType;
import org.hibernate.dialect.PostgreSQLEnumJdbcType;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * VerificationToken entity representing the verification_tokens table.
 * Used for email verification and password reset tokens.
 */
@Entity
@Table(name = "verification_tokens")
public class VerificationToken {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "token", nullable = false, unique = true, length = 255)
    private String token;

    @Enumerated(EnumType.STRING)
    @Column(name = "token_type", nullable = false, columnDefinition = "token_type")
    @JdbcType(PostgreSQLEnumJdbcType.class)
    private TokenType tokenType;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "used_at")
    private Instant usedAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    // Default constructor required by JPA
    public VerificationToken() {
    }

    // Constructor for creating new tokens
    public VerificationToken(User user, String token, TokenType tokenType, Instant expiresAt) {
        this.user = user;
        this.token = token;
        this.tokenType = tokenType;
        this.expiresAt = expiresAt;
    }

    // Getters and Setters

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public TokenType getTokenType() {
        return tokenType;
    }

    public void setTokenType(TokenType tokenType) {
        this.tokenType = tokenType;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(Instant expiresAt) {
        this.expiresAt = expiresAt;
    }

    public Instant getUsedAt() {
        return usedAt;
    }

    public void setUsedAt(Instant usedAt) {
        this.usedAt = usedAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    // Business logic methods

    /**
     * Checks if the token has expired.
     *
     * @return true if current time is after expiration time
     */
    public boolean isExpired() {
        return Instant.now().isAfter(expiresAt);
    }

    /**
     * Checks if the token has already been used.
     *
     * @return true if usedAt is not null
     */
    public boolean isUsed() {
        return usedAt != null;
    }

    /**
     * Checks if the token is valid (not expired and not used).
     *
     * @return true if token is valid
     */
    public boolean isValid() {
        return !isExpired() && !isUsed();
    }

    /**
     * Marks the token as used with the current timestamp.
     */
    public void markAsUsed() {
        this.usedAt = Instant.now();
    }

    // equals and hashCode based on id

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        VerificationToken that = (VerificationToken) o;
        return id != null && Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }

    @Override
    public String toString() {
        return "VerificationToken{" +
                "id=" + id +
                ", userId=" + (user != null ? user.getId() : null) +
                ", tokenType=" + tokenType +
                ", expiresAt=" + expiresAt +
                ", usedAt=" + usedAt +
                ", createdAt=" + createdAt +
                '}';
    }
}
