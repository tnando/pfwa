package com.pfwa.dto.auth;

import com.pfwa.validation.PasswordMatch;
import com.pfwa.validation.StrongPassword;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request DTO for user registration.
 */
@PasswordMatch(passwordField = "password", confirmPasswordField = "confirmPassword")
public record RegisterRequest(
        @NotBlank(message = "Email is required")
        @Email(message = "Invalid email format")
        @Size(min = 5, max = 255, message = "Email must be between 5 and 255 characters")
        String email,

        @NotBlank(message = "Password is required")
        @StrongPassword
        @Size(min = 8, max = 128, message = "Password must be between 8 and 128 characters")
        String password,

        @NotBlank(message = "Confirm password is required")
        @Size(min = 8, max = 128, message = "Confirm password must be between 8 and 128 characters")
        String confirmPassword
) {
    /**
     * Returns email in lowercase with trimmed whitespace.
     */
    public String normalizedEmail() {
        return email != null ? email.trim().toLowerCase() : null;
    }
}
