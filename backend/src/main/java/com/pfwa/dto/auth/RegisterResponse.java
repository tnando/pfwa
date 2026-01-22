package com.pfwa.dto.auth;

import java.util.UUID;

/**
 * Response DTO for successful user registration.
 */
public record RegisterResponse(
        String message,
        UUID userId
) {
    public static RegisterResponse success(UUID userId) {
        return new RegisterResponse(
                "Registration successful. Please check your email to verify your account.",
                userId
        );
    }
}
