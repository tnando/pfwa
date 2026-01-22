package com.pfwa.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.util.regex.Pattern;

/**
 * Validator for the StrongPassword annotation.
 * Validates password strength according to security requirements.
 */
public class StrongPasswordValidator implements ConstraintValidator<StrongPassword, String> {

    private static final int MIN_LENGTH = 8;
    private static final Pattern UPPERCASE_PATTERN = Pattern.compile("[A-Z]");
    private static final Pattern LOWERCASE_PATTERN = Pattern.compile("[a-z]");
    private static final Pattern DIGIT_PATTERN = Pattern.compile("[0-9]");
    private static final Pattern SPECIAL_CHAR_PATTERN = Pattern.compile("[!@#$%^&*()_+\\-=\\[\\]{}|;:,.<>?]");

    @Override
    public void initialize(StrongPassword constraintAnnotation) {
        // No initialization needed
    }

    @Override
    public boolean isValid(String password, ConstraintValidatorContext context) {
        if (password == null || password.isBlank()) {
            // Let @NotBlank handle null/blank validation
            return true;
        }

        boolean isValid = true;
        context.disableDefaultConstraintViolation();

        if (password.length() < MIN_LENGTH) {
            context.buildConstraintViolationWithTemplate("Password must be at least 8 characters")
                    .addConstraintViolation();
            isValid = false;
        }

        if (!UPPERCASE_PATTERN.matcher(password).find()) {
            context.buildConstraintViolationWithTemplate("Password must contain at least one uppercase letter")
                    .addConstraintViolation();
            isValid = false;
        }

        if (!LOWERCASE_PATTERN.matcher(password).find()) {
            context.buildConstraintViolationWithTemplate("Password must contain at least one lowercase letter")
                    .addConstraintViolation();
            isValid = false;
        }

        if (!DIGIT_PATTERN.matcher(password).find()) {
            context.buildConstraintViolationWithTemplate("Password must contain at least one digit")
                    .addConstraintViolation();
            isValid = false;
        }

        if (!SPECIAL_CHAR_PATTERN.matcher(password).find()) {
            context.buildConstraintViolationWithTemplate("Password must contain at least one special character")
                    .addConstraintViolation();
            isValid = false;
        }

        return isValid;
    }
}
