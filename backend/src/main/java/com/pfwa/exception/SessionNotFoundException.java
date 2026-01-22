package com.pfwa.exception;

/**
 * Exception thrown when a session is not found.
 */
public class SessionNotFoundException extends RuntimeException {

    public SessionNotFoundException(String message) {
        super(message);
    }
}
