package com.pfwa.exception;

/**
 * Exception thrown when a token (JWT, verification, or reset) is invalid.
 */
public class InvalidTokenException extends RuntimeException {

    public InvalidTokenException(String message) {
        super(message);
    }

    public InvalidTokenException(String message, Throwable cause) {
        super(message, cause);
    }
}
