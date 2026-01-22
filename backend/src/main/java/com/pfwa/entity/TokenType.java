package com.pfwa.entity;

/**
 * Enum representing the types of verification tokens.
 * Maps to the PostgreSQL enum type 'token_type'.
 */
public enum TokenType {

    /**
     * Token for email verification after registration.
     * Expires after 24 hours.
     */
    EMAIL_VERIFICATION,

    /**
     * Token for password reset requests.
     * Expires after 1 hour.
     */
    PASSWORD_RESET
}
