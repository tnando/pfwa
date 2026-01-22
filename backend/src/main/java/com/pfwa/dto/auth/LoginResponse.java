package com.pfwa.dto.auth;

/**
 * Response DTO for successful login.
 */
public record LoginResponse(
        UserProfile user,
        int expiresIn
) {
}
