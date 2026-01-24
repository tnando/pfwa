package com.pfwa.specification;

import com.pfwa.dto.transaction.TransactionFilterRequest;
import com.pfwa.entity.Transaction;
import com.pfwa.entity.TransactionType;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * JPA Specification for building dynamic Transaction queries.
 * Supports filtering by user, date range, type, categories, amount range, and search.
 */
public class TransactionSpecification {

    private TransactionSpecification() {
        // Utility class
    }

    /**
     * Creates a specification for filtering transactions by user ID.
     *
     * @param userId the user ID
     * @return the specification
     */
    public static Specification<Transaction> hasUserId(UUID userId) {
        return (root, query, criteriaBuilder) ->
                criteriaBuilder.equal(root.get("user").get("id"), userId);
    }

    /**
     * Creates a specification for filtering transactions with date on or after start date.
     *
     * @param startDate the start date (inclusive)
     * @return the specification
     */
    public static Specification<Transaction> dateOnOrAfter(LocalDate startDate) {
        return (root, query, criteriaBuilder) ->
                criteriaBuilder.greaterThanOrEqualTo(root.get("transactionDate"), startDate);
    }

    /**
     * Creates a specification for filtering transactions with date on or before end date.
     *
     * @param endDate the end date (inclusive)
     * @return the specification
     */
    public static Specification<Transaction> dateOnOrBefore(LocalDate endDate) {
        return (root, query, criteriaBuilder) ->
                criteriaBuilder.lessThanOrEqualTo(root.get("transactionDate"), endDate);
    }

    /**
     * Creates a specification for filtering transactions by type.
     *
     * @param type the transaction type
     * @return the specification
     */
    public static Specification<Transaction> hasType(TransactionType type) {
        return (root, query, criteriaBuilder) ->
                criteriaBuilder.equal(root.get("type"), type);
    }

    /**
     * Creates a specification for filtering transactions by category IDs.
     *
     * @param categoryIds the list of category IDs
     * @return the specification
     */
    public static Specification<Transaction> hasCategoryIn(List<UUID> categoryIds) {
        return (root, query, criteriaBuilder) ->
                root.get("category").get("id").in(categoryIds);
    }

    /**
     * Creates a specification for filtering transactions with amount >= minAmount.
     *
     * @param minAmount the minimum amount (inclusive)
     * @return the specification
     */
    public static Specification<Transaction> amountGreaterThanOrEqual(BigDecimal minAmount) {
        return (root, query, criteriaBuilder) ->
                criteriaBuilder.greaterThanOrEqualTo(root.get("amount"), minAmount);
    }

    /**
     * Creates a specification for filtering transactions with amount <= maxAmount.
     *
     * @param maxAmount the maximum amount (inclusive)
     * @return the specification
     */
    public static Specification<Transaction> amountLessThanOrEqual(BigDecimal maxAmount) {
        return (root, query, criteriaBuilder) ->
                criteriaBuilder.lessThanOrEqualTo(root.get("amount"), maxAmount);
    }

    /**
     * Creates a specification for searching transactions by description or notes.
     * Case-insensitive partial match.
     *
     * @param searchTerm the search term
     * @return the specification
     */
    public static Specification<Transaction> searchByDescriptionOrNotes(String searchTerm) {
        return (root, query, criteriaBuilder) -> {
            String pattern = "%" + searchTerm.toLowerCase() + "%";
            Predicate descriptionMatch = criteriaBuilder.like(
                    criteriaBuilder.lower(root.get("description")), pattern);
            Predicate notesMatch = criteriaBuilder.like(
                    criteriaBuilder.lower(root.get("notes")), pattern);
            return criteriaBuilder.or(descriptionMatch, notesMatch);
        };
    }

    /**
     * Builds a combined specification from filter request and user ID.
     *
     * @param userId the user ID (required for security)
     * @param filter the filter request
     * @return the combined specification
     */
    public static Specification<Transaction> fromFilter(UUID userId, TransactionFilterRequest filter) {
        Specification<Transaction> spec = hasUserId(userId);

        if (filter == null) {
            return spec;
        }

        if (filter.startDate() != null) {
            spec = spec.and(dateOnOrAfter(filter.startDate()));
        }

        if (filter.endDate() != null) {
            spec = spec.and(dateOnOrBefore(filter.endDate()));
        }

        if (filter.type() != null) {
            spec = spec.and(hasType(filter.type()));
        }

        if (filter.categoryIds() != null && !filter.categoryIds().isEmpty()) {
            spec = spec.and(hasCategoryIn(filter.categoryIds()));
        }

        if (filter.minAmount() != null) {
            spec = spec.and(amountGreaterThanOrEqual(filter.minAmount()));
        }

        if (filter.maxAmount() != null) {
            spec = spec.and(amountLessThanOrEqual(filter.maxAmount()));
        }

        if (filter.search() != null && !filter.search().isBlank()) {
            spec = spec.and(searchByDescriptionOrNotes(filter.search()));
        }

        return spec;
    }

    /**
     * Creates a specification for fetching the category eagerly.
     * Should be used with queries that need category data.
     *
     * @return the specification with category fetch
     */
    public static Specification<Transaction> fetchCategory() {
        return (root, query, criteriaBuilder) -> {
            if (query.getResultType() != Long.class && query.getResultType() != long.class) {
                root.fetch("category");
            }
            return criteriaBuilder.conjunction();
        };
    }
}
