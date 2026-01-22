package com.pfwa.exception;

/**
 * Exception thrown when a single-use token has already been used.
 */
public class TokenAlreadyUsedException extends RuntimeException {

    public TokenAlreadyUsedException(String message) {
        super(message);
    }
}
