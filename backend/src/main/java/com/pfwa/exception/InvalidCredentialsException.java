package com.pfwa.exception;

/**
 * Exception thrown when login credentials are invalid.
 * Uses generic message to prevent email enumeration.
 */
public class InvalidCredentialsException extends RuntimeException {

    public InvalidCredentialsException() {
        super("Invalid email or password");
    }

    public InvalidCredentialsException(String message) {
        super(message);
    }
}
