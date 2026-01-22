package com.pfwa.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * RefreshToken entity representing the refresh_tokens table.
 * Manages JWT refresh tokens with rotation tracking for secure session management.
 */
@Entity
@Table(name = "refresh_tokens")
public class RefreshToken {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "token_hash", nullable = false, unique = true, length = 255)
    private String tokenHash;

    @Column(name = "family_id", nullable = false)
    private UUID familyId;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "device_info", columnDefinition = "jsonb")
    private Map<String, String> deviceInfo;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "revoked_at")
    private Instant revokedAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    // Default constructor required by JPA
    public RefreshToken() {
    }

    // Constructor for creating new tokens
    public RefreshToken(User user, String tokenHash, UUID familyId, Instant expiresAt) {
        this.user = user;
        this.tokenHash = tokenHash;
        this.familyId = familyId;
        this.expiresAt = expiresAt;
    }

    // Full constructor including device info
    public RefreshToken(User user, String tokenHash, UUID familyId, Instant expiresAt,
                        Map<String, String> deviceInfo) {
        this.user = user;
        this.tokenHash = tokenHash;
        this.familyId = familyId;
        this.expiresAt = expiresAt;
        this.deviceInfo = deviceInfo;
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

    public String getTokenHash() {
        return tokenHash;
    }

    public void setTokenHash(String tokenHash) {
        this.tokenHash = tokenHash;
    }

    public UUID getFamilyId() {
        return familyId;
    }

    public void setFamilyId(UUID familyId) {
        this.familyId = familyId;
    }

    public Map<String, String> getDeviceInfo() {
        return deviceInfo;
    }

    public void setDeviceInfo(Map<String, String> deviceInfo) {
        this.deviceInfo = deviceInfo;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(Instant expiresAt) {
        this.expiresAt = expiresAt;
    }

    public Instant getRevokedAt() {
        return revokedAt;
    }

    public void setRevokedAt(Instant revokedAt) {
        this.revokedAt = revokedAt;
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
     * Checks if the token has been revoked.
     *
     * @return true if revokedAt is not null
     */
    public boolean isRevoked() {
        return revokedAt != null;
    }

    /**
     * Checks if the token is active (not expired and not revoked).
     *
     * @return true if token is active
     */
    public boolean isActive() {
        return !isExpired() && !isRevoked();
    }

    /**
     * Revokes the token with the current timestamp.
     */
    public void revoke() {
        this.revokedAt = Instant.now();
    }

    /**
     * Gets the browser from device info.
     *
     * @return browser string or null
     */
    public String getBrowser() {
        return deviceInfo != null ? deviceInfo.get("browser") : null;
    }

    /**
     * Gets the operating system from device info.
     *
     * @return OS string or null
     */
    public String getOs() {
        return deviceInfo != null ? deviceInfo.get("os") : null;
    }

    /**
     * Gets the IP address from device info.
     *
     * @return IP address string or null
     */
    public String getIpAddress() {
        return deviceInfo != null ? deviceInfo.get("ip") : null;
    }

    /**
     * Gets a formatted device description for session display.
     *
     * @return formatted device description
     */
    public String getDeviceDescription() {
        String browser = getBrowser();
        String os = getOs();
        if (browser != null && os != null) {
            return browser + " on " + os;
        }
        if (browser != null) {
            return browser;
        }
        if (os != null) {
            return os;
        }
        return "Unknown device";
    }

    // equals and hashCode based on id

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        RefreshToken that = (RefreshToken) o;
        return id != null && Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }

    @Override
    public String toString() {
        return "RefreshToken{" +
                "id=" + id +
                ", userId=" + (user != null ? user.getId() : null) +
                ", familyId=" + familyId +
                ", expiresAt=" + expiresAt +
                ", revokedAt=" + revokedAt +
                ", createdAt=" + createdAt +
                '}';
    }
}
