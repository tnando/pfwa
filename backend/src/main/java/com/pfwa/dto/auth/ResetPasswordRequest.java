package com.pfwa.dto.auth;

import com.pfwa.validation.PasswordMatch;
import com.pfwa.validation.StrongPassword;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request DTO for completing password reset.
 */
@PasswordMatch(passwordField = "newPassword", confirmPasswordField = "confirmPassword")
public record ResetPasswordRequest(
        @NotBlank(message = "Token is required")
        @Size(min = 32, max = 64, message = "Invalid token format")
        String token,

        @NotBlank(message = "New password is required")
        @StrongPassword
        @Size(min = 8, max = 128, message = "Password must be between 8 and 128 characters")
        String newPassword,

        @NotBlank(message = "Confirm password is required")
        @Size(min = 8, max = 128, message = "Confirm password must be between 8 and 128 characters")
        String confirmPassword
) {
}
