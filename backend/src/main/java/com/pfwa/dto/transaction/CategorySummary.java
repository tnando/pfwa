package com.pfwa.dto.transaction;

import com.pfwa.entity.Category;

import java.util.UUID;

/**
 * DTO for category information in summary breakdown.
 */
public record CategorySummary(
        UUID id,
        String name,
        String icon,
        String color
) {
    /**
     * Creates a CategorySummary from a Category entity.
     *
     * @param category the category entity
     * @return the category summary
     */
    public static CategorySummary from(Category category) {
        return new CategorySummary(
                category.getId(),
                category.getName(),
                category.getIcon(),
                category.getColor()
        );
    }
}
