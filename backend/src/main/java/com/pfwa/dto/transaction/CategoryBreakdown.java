package com.pfwa.dto.transaction;

import java.util.List;

/**
 * DTO containing category breakdowns for income and expenses.
 */
public record CategoryBreakdown(
        List<CategoryBreakdownItem> income,
        List<CategoryBreakdownItem> expense
) {
    /**
     * Creates a CategoryBreakdown with income and expense breakdowns.
     *
     * @param income  list of income category breakdowns
     * @param expense list of expense category breakdowns
     * @return the category breakdown
     */
    public static CategoryBreakdown of(
            List<CategoryBreakdownItem> income,
            List<CategoryBreakdownItem> expense) {
        return new CategoryBreakdown(income, expense);
    }
}
