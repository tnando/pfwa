package com.pfwa.repository;

import com.pfwa.entity.Transaction;
import com.pfwa.entity.TransactionType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for Transaction entity operations.
 * Provides methods for transaction CRUD, filtering, search, and aggregations.
 * Extends JpaSpecificationExecutor for dynamic query building.
 */
@Repository
public interface TransactionRepository extends JpaRepository<Transaction, UUID>, JpaSpecificationExecutor<Transaction> {

    /**
     * Finds a transaction by ID and user ID.
     * Used to verify ownership before update/delete.
     *
     * @param id the transaction ID
     * @param userId the user ID
     * @return Optional containing the transaction if found and owned by user
     */
    @Query("SELECT t FROM Transaction t WHERE t.id = :id AND t.user.id = :userId")
    Optional<Transaction> findByIdAndUserId(@Param("id") UUID id, @Param("userId") UUID userId);

    /**
     * Finds all transactions for a user with pagination.
     *
     * @param userId the user ID
     * @param pageable pagination parameters
     * @return page of transactions
     */
    @Query("SELECT t FROM Transaction t WHERE t.user.id = :userId")
    Page<Transaction> findByUserId(@Param("userId") UUID userId, Pageable pageable);

    /**
     * Finds all transactions for a user within a date range.
     *
     * @param userId the user ID
     * @param startDate the start date (inclusive)
     * @param endDate the end date (inclusive)
     * @param pageable pagination parameters
     * @return page of transactions within the date range
     */
    @Query("SELECT t FROM Transaction t WHERE t.user.id = :userId " +
           "AND t.transactionDate >= :startDate AND t.transactionDate <= :endDate")
    Page<Transaction> findByUserIdAndDateRange(
            @Param("userId") UUID userId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            Pageable pageable);

    /**
     * Finds all transactions for a user by type.
     *
     * @param userId the user ID
     * @param type the transaction type
     * @param pageable pagination parameters
     * @return page of transactions of the specified type
     */
    @Query("SELECT t FROM Transaction t WHERE t.user.id = :userId AND t.type = :type")
    Page<Transaction> findByUserIdAndType(
            @Param("userId") UUID userId,
            @Param("type") TransactionType type,
            Pageable pageable);

    /**
     * Finds all transactions for a user by category.
     *
     * @param userId the user ID
     * @param categoryId the category ID
     * @param pageable pagination parameters
     * @return page of transactions in the specified category
     */
    @Query("SELECT t FROM Transaction t WHERE t.user.id = :userId AND t.category.id = :categoryId")
    Page<Transaction> findByUserIdAndCategoryId(
            @Param("userId") UUID userId,
            @Param("categoryId") UUID categoryId,
            Pageable pageable);

    /**
     * Searches transactions by description or notes (case-insensitive partial match).
     *
     * @param userId the user ID
     * @param searchTerm the search term
     * @param pageable pagination parameters
     * @return page of matching transactions
     */
    @Query("SELECT t FROM Transaction t WHERE t.user.id = :userId " +
           "AND (LOWER(t.description) LIKE LOWER(CONCAT('%', :searchTerm, '%')) " +
           "OR LOWER(t.notes) LIKE LOWER(CONCAT('%', :searchTerm, '%')))")
    Page<Transaction> searchByDescriptionOrNotes(
            @Param("userId") UUID userId,
            @Param("searchTerm") String searchTerm,
            Pageable pageable);

    /**
     * Calculates the total amount of transactions by type for a user.
     *
     * @param userId the user ID
     * @param type the transaction type
     * @return the sum of amounts (null if no transactions)
     */
    @Query("SELECT COALESCE(SUM(t.amount), 0) FROM Transaction t WHERE t.user.id = :userId AND t.type = :type")
    BigDecimal sumAmountByUserIdAndType(@Param("userId") UUID userId, @Param("type") TransactionType type);

    /**
     * Calculates the total amount by type within a date range.
     *
     * @param userId the user ID
     * @param type the transaction type
     * @param startDate the start date (inclusive)
     * @param endDate the end date (inclusive)
     * @return the sum of amounts
     */
    @Query("SELECT COALESCE(SUM(t.amount), 0) FROM Transaction t WHERE t.user.id = :userId " +
           "AND t.type = :type AND t.transactionDate >= :startDate AND t.transactionDate <= :endDate")
    BigDecimal sumAmountByUserIdAndTypeAndDateRange(
            @Param("userId") UUID userId,
            @Param("type") TransactionType type,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    /**
     * Calculates the total amount by category within a date range.
     *
     * @param userId the user ID
     * @param categoryId the category ID
     * @param startDate the start date (inclusive)
     * @param endDate the end date (inclusive)
     * @return the sum of amounts
     */
    @Query("SELECT COALESCE(SUM(t.amount), 0) FROM Transaction t WHERE t.user.id = :userId " +
           "AND t.category.id = :categoryId AND t.transactionDate >= :startDate AND t.transactionDate <= :endDate")
    BigDecimal sumAmountByUserIdAndCategoryIdAndDateRange(
            @Param("userId") UUID userId,
            @Param("categoryId") UUID categoryId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    /**
     * Counts transactions for a user.
     *
     * @param userId the user ID
     * @return the count of transactions
     */
    @Query("SELECT COUNT(t) FROM Transaction t WHERE t.user.id = :userId")
    long countByUserId(@Param("userId") UUID userId);

    /**
     * Counts transactions by type within a date range.
     *
     * @param userId the user ID
     * @param type the transaction type
     * @param startDate the start date (inclusive)
     * @param endDate the end date (inclusive)
     * @return the count of transactions
     */
    @Query("SELECT COUNT(t) FROM Transaction t WHERE t.user.id = :userId " +
           "AND t.type = :type AND t.transactionDate >= :startDate AND t.transactionDate <= :endDate")
    long countByUserIdAndTypeAndDateRange(
            @Param("userId") UUID userId,
            @Param("type") TransactionType type,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    /**
     * Deletes a transaction by ID and user ID.
     * Returns the number of deleted rows (0 or 1).
     *
     * @param id the transaction ID
     * @param userId the user ID
     * @return number of deleted rows
     */
    @Modifying
    @Query("DELETE FROM Transaction t WHERE t.id = :id AND t.user.id = :userId")
    int deleteByIdAndUserId(@Param("id") UUID id, @Param("userId") UUID userId);

    /**
     * Deletes multiple transactions by IDs and user ID (bulk delete).
     *
     * @param ids the list of transaction IDs
     * @param userId the user ID
     * @return number of deleted rows
     */
    @Modifying
    @Query("DELETE FROM Transaction t WHERE t.id IN :ids AND t.user.id = :userId")
    int deleteByIdsAndUserId(@Param("ids") List<UUID> ids, @Param("userId") UUID userId);

    /**
     * Checks if a transaction exists and belongs to a user.
     *
     * @param id the transaction ID
     * @param userId the user ID
     * @return true if transaction exists and belongs to user
     */
    @Query("SELECT CASE WHEN COUNT(t) > 0 THEN true ELSE false END FROM Transaction t " +
           "WHERE t.id = :id AND t.user.id = :userId")
    boolean existsByIdAndUserId(@Param("id") UUID id, @Param("userId") UUID userId);

    /**
     * Finds transactions with eager loading of category (avoids N+1).
     *
     * @param userId the user ID
     * @param pageable pagination parameters
     * @return page of transactions with categories loaded
     */
    @Query("SELECT t FROM Transaction t JOIN FETCH t.category WHERE t.user.id = :userId")
    Page<Transaction> findByUserIdWithCategory(@Param("userId") UUID userId, Pageable pageable);

    /**
     * Finds all transactions for a user by category IDs.
     *
     * @param userId the user ID
     * @param categoryIds the list of category IDs
     * @param pageable pagination parameters
     * @return page of transactions in the specified categories
     */
    @Query("SELECT t FROM Transaction t WHERE t.user.id = :userId AND t.category.id IN :categoryIds")
    Page<Transaction> findByUserIdAndCategoryIds(
            @Param("userId") UUID userId,
            @Param("categoryIds") List<UUID> categoryIds,
            Pageable pageable);

    /**
     * Finds transactions within an amount range.
     *
     * @param userId the user ID
     * @param minAmount the minimum amount (inclusive)
     * @param maxAmount the maximum amount (inclusive)
     * @param pageable pagination parameters
     * @return page of transactions within the amount range
     */
    @Query("SELECT t FROM Transaction t WHERE t.user.id = :userId " +
           "AND t.amount >= :minAmount AND t.amount <= :maxAmount")
    Page<Transaction> findByUserIdAndAmountRange(
            @Param("userId") UUID userId,
            @Param("minAmount") BigDecimal minAmount,
            @Param("maxAmount") BigDecimal maxAmount,
            Pageable pageable);
}
