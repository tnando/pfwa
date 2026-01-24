package com.pfwa.controller;

import com.pfwa.dto.transaction.*;
import com.pfwa.entity.TransactionType;
import com.pfwa.security.UserPrincipal;
import com.pfwa.service.TransactionService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.math.BigDecimal;
import java.net.URI;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * REST controller for managing transactions.
 */
@RestController
@RequestMapping("/api/v1/transactions")
public class TransactionController {

    private static final Logger logger = LoggerFactory.getLogger(TransactionController.class);

    private static final int DEFAULT_PAGE_SIZE = 20;
    private static final int MAX_PAGE_SIZE = 100;

    private final TransactionService transactionService;

    public TransactionController(TransactionService transactionService) {
        this.transactionService = transactionService;
    }

    /**
     * Creates a new transaction.
     *
     * @param principal the authenticated user
     * @param request   the create transaction request
     * @return the created transaction with 201 status
     */
    @PostMapping
    public ResponseEntity<TransactionResponse> createTransaction(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody CreateTransactionRequest request) {

        logger.debug("POST /api/v1/transactions - user: {}", principal.getId());

        TransactionResponse response = transactionService.createTransaction(principal.getId(), request);

        URI location = ServletUriComponentsBuilder
                .fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(response.id())
                .toUri();

        return ResponseEntity.created(location).body(response);
    }

    /**
     * Gets a list of transactions with optional filtering, pagination, and sorting.
     *
     * @param principal   the authenticated user
     * @param page        page number (0-indexed)
     * @param size        page size (max 100)
     * @param sort        sort field and direction (e.g., "date,desc")
     * @param startDate   filter by start date (inclusive)
     * @param endDate     filter by end date (inclusive)
     * @param type        filter by transaction type
     * @param categoryIds filter by category IDs
     * @param minAmount   filter by minimum amount
     * @param maxAmount   filter by maximum amount
     * @param search      search in description and notes
     * @return paginated list of transactions with summary
     */
    @GetMapping
    public ResponseEntity<TransactionListResponse> getTransactions(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "" + DEFAULT_PAGE_SIZE) int size,
            @RequestParam(defaultValue = "transactionDate,desc") String sort,
            @RequestParam(required = false) LocalDate startDate,
            @RequestParam(required = false) LocalDate endDate,
            @RequestParam(required = false) TransactionType type,
            @RequestParam(required = false) List<UUID> categoryIds,
            @RequestParam(required = false) BigDecimal minAmount,
            @RequestParam(required = false) BigDecimal maxAmount,
            @RequestParam(required = false) String search) {

        logger.debug("GET /api/v1/transactions - user: {}, page: {}, size: {}",
                principal.getId(), page, size);

        // Validate and constrain page size
        int constrainedSize = Math.min(Math.max(size, 1), MAX_PAGE_SIZE);

        // Parse sort parameter
        Pageable pageable = buildPageable(page, constrainedSize, sort);

        // Build filter
        TransactionFilterRequest filter = new TransactionFilterRequest(
                startDate, endDate, type, categoryIds, minAmount, maxAmount, search);

        TransactionListResponse response = transactionService.getTransactions(
                principal.getId(), filter, pageable);

        return ResponseEntity.ok(response);
    }

    /**
     * Gets a single transaction by ID.
     *
     * @param principal the authenticated user
     * @param id        the transaction ID
     * @return the transaction
     */
    @GetMapping("/{id}")
    public ResponseEntity<TransactionResponse> getTransaction(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID id) {

        logger.debug("GET /api/v1/transactions/{} - user: {}", id, principal.getId());

        TransactionResponse response = transactionService.getTransaction(principal.getId(), id);
        return ResponseEntity.ok(response);
    }

    /**
     * Updates an existing transaction.
     *
     * @param principal the authenticated user
     * @param id        the transaction ID
     * @param request   the update request
     * @return the updated transaction
     */
    @PutMapping("/{id}")
    public ResponseEntity<TransactionResponse> updateTransaction(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID id,
            @Valid @RequestBody UpdateTransactionRequest request) {

        logger.debug("PUT /api/v1/transactions/{} - user: {}", id, principal.getId());

        TransactionResponse response = transactionService.updateTransaction(principal.getId(), id, request);
        return ResponseEntity.ok(response);
    }

    /**
     * Deletes a transaction.
     *
     * @param principal the authenticated user
     * @param id        the transaction ID
     * @return 204 No Content on success
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTransaction(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID id) {

        logger.debug("DELETE /api/v1/transactions/{} - user: {}", id, principal.getId());

        transactionService.deleteTransaction(principal.getId(), id);
        return ResponseEntity.noContent().build();
    }

    /**
     * Gets transaction summary and statistics.
     *
     * @param principal the authenticated user
     * @param startDate the start date (optional)
     * @param endDate   the end date (optional)
     * @return the transaction summary
     */
    @GetMapping("/summary")
    public ResponseEntity<TransactionSummaryResponse> getTransactionSummary(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(required = false) LocalDate startDate,
            @RequestParam(required = false) LocalDate endDate) {

        logger.debug("GET /api/v1/transactions/summary - user: {}, start: {}, end: {}",
                principal.getId(), startDate, endDate);

        TransactionSummaryResponse response = transactionService.getTransactionSummary(
                principal.getId(), startDate, endDate);

        return ResponseEntity.ok(response);
    }

    /**
     * Builds a Pageable object from the sort parameter.
     * Supports format: "field,direction" (e.g., "date,desc" or "amount,asc")
     */
    private Pageable buildPageable(int page, int size, String sort) {
        if (sort == null || sort.isBlank()) {
            return PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "transactionDate"));
        }

        String[] parts = sort.split(",");
        String field = mapSortField(parts[0].trim());
        Sort.Direction direction = parts.length > 1 && "asc".equalsIgnoreCase(parts[1].trim()) ?
                Sort.Direction.ASC : Sort.Direction.DESC;

        return PageRequest.of(page, size, Sort.by(direction, field));
    }

    /**
     * Maps API sort field names to entity field names.
     */
    private String mapSortField(String field) {
        return switch (field.toLowerCase()) {
            case "date", "transactiondate" -> "transactionDate";
            case "amount" -> "amount";
            case "category" -> "category.name";
            case "createdat" -> "createdAt";
            case "type" -> "type";
            default -> "transactionDate";
        };
    }
}
