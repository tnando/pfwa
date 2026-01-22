package com.pfwa.dto.auth;

/**
 * Response DTO for token refresh.
 */
public record TokenRefreshResponse(
        int expiresIn
) {
}
