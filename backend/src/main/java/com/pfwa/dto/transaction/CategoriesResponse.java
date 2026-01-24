package com.pfwa.dto.transaction;

import java.util.List;

/**
 * Response DTO containing categories grouped by type.
 */
public record CategoriesResponse(
        List<CategoryResponse> income,
        List<CategoryResponse> expense
) {
    /**
     * Creates a CategoriesResponse with income and expense categories.
     *
     * @param incomeCategories  list of income categories
     * @param expenseCategories list of expense categories
     * @return the categories response
     */
    public static CategoriesResponse of(
            List<CategoryResponse> incomeCategories,
            List<CategoryResponse> expenseCategories) {
        return new CategoriesResponse(incomeCategories, expenseCategories);
    }
}
