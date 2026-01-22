package com.pfwa.dto.auth;

import jakarta.validation.constraints.NotBlank;

/**
 * Request DTO for email verification.
 */
public record VerifyEmailRequest(
        @NotBlank(message = "Token is required")
        String token
) {
}
