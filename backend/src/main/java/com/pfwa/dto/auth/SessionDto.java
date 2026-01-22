package com.pfwa.dto.auth;

import com.pfwa.entity.RefreshToken;

import java.time.Instant;
import java.util.UUID;

/**
 * DTO representing an active user session.
 */
public record SessionDto(
        UUID id,
        String deviceType,
        String location,
        String ipAddress,
        Instant lastActive,
        Instant createdAt,
        boolean isCurrent
) {
    /**
     * Creates a SessionDto from a RefreshToken entity.
     */
    public static SessionDto fromEntity(RefreshToken token, UUID currentSessionId) {
        return new SessionDto(
                token.getId(),
                token.getDeviceDescription(),
                token.getDeviceInfo() != null ? token.getDeviceInfo().get("location") : null,
                maskIpAddress(token.getIpAddress()),
                token.getCreatedAt(), // Using createdAt as lastActive for simplicity
                token.getCreatedAt(),
                token.getId().equals(currentSessionId)
        );
    }

    /**
     * Masks IP address for privacy (e.g., 192.168.1.100 -> 192.168.x.x).
     */
    private static String maskIpAddress(String ipAddress) {
        if (ipAddress == null) {
            return null;
        }
        String[] parts = ipAddress.split("\\.");
        if (parts.length == 4) {
            return parts[0] + "." + parts[1] + ".x.x";
        }
        // Handle IPv6 or other formats by returning a generic mask
        return "x.x.x.x";
    }
}
