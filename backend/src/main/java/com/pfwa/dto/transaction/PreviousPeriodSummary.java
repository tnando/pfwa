package com.pfwa.dto.transaction;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * DTO containing summary data for the previous comparison period.
 */
public record PreviousPeriodSummary(
        LocalDate startDate,
        LocalDate endDate,
        BigDecimal income,
        BigDecimal expenses,
        BigDecimal net
) {
    /**
     * Creates a PreviousPeriodSummary with calculated net balance.
     *
     * @param startDate the start date of the period
     * @param endDate   the end date of the period
     * @param income    total income
     * @param expenses  total expenses
     * @return the previous period summary
     */
    public static PreviousPeriodSummary of(
            LocalDate startDate,
            LocalDate endDate,
            BigDecimal income,
            BigDecimal expenses) {
        BigDecimal net = income.subtract(expenses);
        return new PreviousPeriodSummary(startDate, endDate, income, expenses, net);
    }
}
