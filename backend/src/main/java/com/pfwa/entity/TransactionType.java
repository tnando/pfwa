package com.pfwa.entity;

/**
 * Enum representing the types of financial transactions.
 * Maps to the PostgreSQL enum type 'transaction_type'.
 */
public enum TransactionType {

    /**
     * Income transaction - money coming in.
     * Examples: Salary, freelance payments, investment returns.
     */
    INCOME,

    /**
     * Expense transaction - money going out.
     * Examples: Food, transportation, utilities.
     */
    EXPENSE
}
