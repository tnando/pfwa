package com.pfwa.dto.transaction;

import java.math.BigDecimal;

/**
 * Summary DTO containing totals for a list of transactions.
 */
public record TransactionListSummary(
        BigDecimal totalIncome,
        BigDecimal totalExpenses,
        BigDecimal netBalance
) {
    /**
     * Creates a summary with the given totals.
     * Net balance is calculated as income minus expenses.
     *
     * @param totalIncome   total income amount
     * @param totalExpenses total expenses amount
     * @return the transaction list summary
     */
    public static TransactionListSummary of(BigDecimal totalIncome, BigDecimal totalExpenses) {
        BigDecimal net = totalIncome.subtract(totalExpenses);
        return new TransactionListSummary(totalIncome, totalExpenses, net);
    }
}
