package com.pfwa.exception;

/**
 * Exception thrown when refresh token reuse is detected (potential security breach).
 */
public class TokenReuseException extends RuntimeException {

    public TokenReuseException(String message) {
        super(message);
    }
}
