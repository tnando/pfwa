package com.pfwa.exception;

import java.util.UUID;

/**
 * Exception thrown when a user attempts to access a transaction they do not own.
 */
public class TransactionAccessDeniedException extends RuntimeException {

    private final UUID transactionId;
    private final UUID userId;

    public TransactionAccessDeniedException(UUID transactionId, UUID userId) {
        super("You do not have permission to access this transaction");
        this.transactionId = transactionId;
        this.userId = userId;
    }

    public UUID getTransactionId() {
        return transactionId;
    }

    public UUID getUserId() {
        return userId;
    }
}
