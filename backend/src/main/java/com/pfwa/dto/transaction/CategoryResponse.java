package com.pfwa.dto.transaction;

import com.pfwa.entity.Category;
import com.pfwa.entity.TransactionType;

import java.util.UUID;

/**
 * Response DTO for category information.
 */
public record CategoryResponse(
        UUID id,
        String name,
        TransactionType type,
        String icon,
        String color
) {
    /**
     * Creates a CategoryResponse from a Category entity.
     *
     * @param category the category entity
     * @return the category response DTO
     */
    public static CategoryResponse from(Category category) {
        return new CategoryResponse(
                category.getId(),
                category.getName(),
                category.getType(),
                category.getIcon(),
                category.getColor()
        );
    }
}
