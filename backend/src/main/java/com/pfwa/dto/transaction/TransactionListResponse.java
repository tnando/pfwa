package com.pfwa.dto.transaction;

import org.springframework.data.domain.Page;

import java.util.List;

/**
 * Response DTO for paginated list of transactions with summary.
 */
public record TransactionListResponse(
        List<TransactionResponse> content,
        int page,
        int size,
        long totalElements,
        int totalPages,
        boolean first,
        boolean last,
        TransactionListSummary summary,
        AppliedFilters appliedFilters
) {
    /**
     * Creates a TransactionListResponse from a Page of transactions.
     *
     * @param transactionPage the page of transaction responses
     * @param summary         the summary statistics
     * @param appliedFilters  the filters that were applied
     * @return the transaction list response
     */
    public static TransactionListResponse from(
            Page<TransactionResponse> transactionPage,
            TransactionListSummary summary,
            AppliedFilters appliedFilters) {
        return new TransactionListResponse(
                transactionPage.getContent(),
                transactionPage.getNumber(),
                transactionPage.getSize(),
                transactionPage.getTotalElements(),
                transactionPage.getTotalPages(),
                transactionPage.isFirst(),
                transactionPage.isLast(),
                summary,
                appliedFilters
        );
    }
}
