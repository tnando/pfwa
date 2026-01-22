package com.pfwa.dto.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request DTO for resending verification email.
 */
public record ResendVerificationRequest(
        @NotBlank(message = "Email is required")
        @Email(message = "Invalid email format")
        @Size(max = 255, message = "Email must not exceed 255 characters")
        String email
) {
    /**
     * Returns email in lowercase with trimmed whitespace.
     */
    public String normalizedEmail() {
        return email != null ? email.trim().toLowerCase() : null;
    }
}
