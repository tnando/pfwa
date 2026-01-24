package com.pfwa.exception;

import java.util.UUID;

/**
 * Exception thrown when a category is not found.
 */
public class CategoryNotFoundException extends RuntimeException {

    private final UUID categoryId;

    public CategoryNotFoundException(UUID categoryId) {
        super("Category not found");
        this.categoryId = categoryId;
    }

    public CategoryNotFoundException(String message) {
        super(message);
        this.categoryId = null;
    }

    public UUID getCategoryId() {
        return categoryId;
    }
}
