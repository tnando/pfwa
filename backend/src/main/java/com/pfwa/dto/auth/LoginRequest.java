package com.pfwa.dto.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request DTO for user login.
 */
public record LoginRequest(
        @NotBlank(message = "Email is required")
        @Email(message = "Invalid email format")
        @Size(max = 255, message = "Email must not exceed 255 characters")
        String email,

        @NotBlank(message = "Password is required")
        @Size(max = 128, message = "Password must not exceed 128 characters")
        String password,

        Boolean rememberMe
) {
    /**
     * Returns whether remember me is enabled, defaulting to false if null.
     */
    public boolean isRememberMe() {
        return rememberMe != null && rememberMe;
    }

    /**
     * Returns email in lowercase with trimmed whitespace.
     */
    public String normalizedEmail() {
        return email != null ? email.trim().toLowerCase() : null;
    }
}
