package com.pfwa.dto.transaction;

import java.math.BigDecimal;

/**
 * DTO containing total amounts for a summary period.
 */
public record SummaryTotals(
        BigDecimal income,
        BigDecimal expenses,
        BigDecimal net,
        int transactionCount
) {
    /**
     * Creates summary totals with calculated net balance.
     *
     * @param income           total income
     * @param expenses         total expenses
     * @param transactionCount total number of transactions
     * @return the summary totals
     */
    public static SummaryTotals of(BigDecimal income, BigDecimal expenses, int transactionCount) {
        BigDecimal net = income.subtract(expenses);
        return new SummaryTotals(income, expenses, net, transactionCount);
    }
}
