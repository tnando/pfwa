package com.pfwa.exception;

import com.pfwa.entity.TransactionType;

import java.util.UUID;

/**
 * Exception thrown when category type does not match transaction type.
 */
public class CategoryTypeMismatchException extends RuntimeException {

    private final UUID categoryId;
    private final TransactionType expectedType;
    private final TransactionType actualType;

    public CategoryTypeMismatchException(UUID categoryId, TransactionType expectedType, TransactionType actualType) {
        super("Category type must match transaction type");
        this.categoryId = categoryId;
        this.expectedType = expectedType;
        this.actualType = actualType;
    }

    public UUID getCategoryId() {
        return categoryId;
    }

    public TransactionType getExpectedType() {
        return expectedType;
    }

    public TransactionType getActualType() {
        return actualType;
    }
}
