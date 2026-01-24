package com.pfwa.service;

import com.pfwa.dto.transaction.*;
import com.pfwa.entity.Category;
import com.pfwa.entity.Transaction;
import com.pfwa.entity.TransactionType;
import com.pfwa.entity.User;
import com.pfwa.exception.CategoryTypeMismatchException;
import com.pfwa.exception.TransactionNotFoundException;
import com.pfwa.repository.CategoryRepository;
import com.pfwa.repository.TransactionRepository;
import com.pfwa.repository.UserRepository;
import com.pfwa.specification.TransactionSpecification;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service for managing transactions.
 * Handles CRUD operations, filtering, search, and summary statistics.
 */
@Service
@Transactional(readOnly = true)
public class TransactionService {

    private static final Logger logger = LoggerFactory.getLogger(TransactionService.class);

    private final TransactionRepository transactionRepository;
    private final CategoryRepository categoryRepository;
    private final UserRepository userRepository;
    private final CategoryService categoryService;

    public TransactionService(
            TransactionRepository transactionRepository,
            CategoryRepository categoryRepository,
            UserRepository userRepository,
            CategoryService categoryService) {
        this.transactionRepository = transactionRepository;
        this.categoryRepository = categoryRepository;
        this.userRepository = userRepository;
        this.categoryService = categoryService;
    }

    /**
     * Creates a new transaction.
     *
     * @param userId  the ID of the user creating the transaction
     * @param request the create transaction request
     * @return the created transaction response
     */
    @Transactional
    public TransactionResponse createTransaction(UUID userId, CreateTransactionRequest request) {
        logger.debug("Creating transaction for user {}", userId);

        User user = userRepository.getReferenceById(userId);
        Category category = validateAndGetCategory(request.categoryId(), request.type());

        Transaction transaction = new Transaction(
                user,
                category,
                request.amount(),
                request.type(),
                request.date()
        );
        transaction.setDescription(request.description());
        transaction.setNotes(request.notes());

        Transaction saved = transactionRepository.save(transaction);
        logger.info("Created transaction {} for user {}", saved.getId(), userId);

        return TransactionResponse.from(saved);
    }

    /**
     * Gets a transaction by ID for a specific user.
     *
     * @param userId        the user ID
     * @param transactionId the transaction ID
     * @return the transaction response
     * @throws TransactionNotFoundException if transaction not found or not owned by user
     */
    public TransactionResponse getTransaction(UUID userId, UUID transactionId) {
        Transaction transaction = transactionRepository.findByIdAndUserId(transactionId, userId)
                .orElseThrow(() -> new TransactionNotFoundException(transactionId));

        return TransactionResponse.from(transaction);
    }

    /**
     * Gets a paginated list of transactions with optional filtering.
     *
     * @param userId   the user ID
     * @param filter   the filter request (optional)
     * @param pageable pagination parameters
     * @return the transaction list response with summary
     */
    public TransactionListResponse getTransactions(
            UUID userId,
            TransactionFilterRequest filter,
            Pageable pageable) {

        logger.debug("Fetching transactions for user {} with filters: {}", userId, filter);

        // Build specification with filters
        Specification<Transaction> spec = TransactionSpecification.fromFilter(userId, filter);

        // Fetch transactions with category eagerly loaded
        spec = spec.and(TransactionSpecification.fetchCategory());

        Page<Transaction> transactionPage = transactionRepository.findAll(spec, pageable);

        // Map to DTOs
        Page<TransactionResponse> responsePage = transactionPage.map(TransactionResponse::from);

        // Calculate summary for filtered transactions
        TransactionListSummary summary = calculateSummaryForFilter(userId, filter);

        // Build applied filters
        AppliedFilters appliedFilters = filter != null ? AppliedFilters.from(filter) :
                AppliedFilters.from(TransactionFilterRequest.empty());

        return TransactionListResponse.from(responsePage, summary, appliedFilters);
    }

    /**
     * Updates an existing transaction.
     *
     * @param userId        the user ID
     * @param transactionId the transaction ID
     * @param request       the update request
     * @return the updated transaction response
     * @throws TransactionNotFoundException if transaction not found or not owned by user
     */
    @Transactional
    public TransactionResponse updateTransaction(
            UUID userId,
            UUID transactionId,
            UpdateTransactionRequest request) {

        logger.debug("Updating transaction {} for user {}", transactionId, userId);

        Transaction transaction = transactionRepository.findByIdAndUserId(transactionId, userId)
                .orElseThrow(() -> new TransactionNotFoundException(transactionId));

        Category category = validateAndGetCategory(request.categoryId(), request.type());

        transaction.setAmount(request.amount());
        transaction.setType(request.type());
        transaction.setCategory(category);
        transaction.setTransactionDate(request.date());
        transaction.setDescription(request.description());
        transaction.setNotes(request.notes());

        Transaction saved = transactionRepository.save(transaction);
        logger.info("Updated transaction {} for user {}", transactionId, userId);

        return TransactionResponse.from(saved);
    }

    /**
     * Deletes a transaction.
     *
     * @param userId        the user ID
     * @param transactionId the transaction ID
     * @throws TransactionNotFoundException if transaction not found or not owned by user
     */
    @Transactional
    public void deleteTransaction(UUID userId, UUID transactionId) {
        logger.debug("Deleting transaction {} for user {}", transactionId, userId);

        int deleted = transactionRepository.deleteByIdAndUserId(transactionId, userId);
        if (deleted == 0) {
            throw new TransactionNotFoundException(transactionId);
        }

        logger.info("Deleted transaction {} for user {}", transactionId, userId);
    }

    /**
     * Gets transaction summary and statistics.
     *
     * @param userId    the user ID
     * @param startDate the start date (optional)
     * @param endDate   the end date (optional)
     * @return the transaction summary response
     */
    public TransactionSummaryResponse getTransactionSummary(
            UUID userId,
            LocalDate startDate,
            LocalDate endDate) {

        logger.debug("Getting summary for user {} from {} to {}", userId, startDate, endDate);

        // Default to current month if no dates provided
        LocalDate effectiveStartDate = startDate != null ? startDate : LocalDate.now().withDayOfMonth(1);
        LocalDate effectiveEndDate = endDate != null ? endDate : LocalDate.now();

        // Build period
        SummaryPeriod period = SummaryPeriod.of(effectiveStartDate, effectiveEndDate);

        // Calculate totals
        BigDecimal totalIncome = transactionRepository.sumAmountByUserIdAndTypeAndDateRange(
                userId, TransactionType.INCOME, effectiveStartDate, effectiveEndDate);
        BigDecimal totalExpenses = transactionRepository.sumAmountByUserIdAndTypeAndDateRange(
                userId, TransactionType.EXPENSE, effectiveStartDate, effectiveEndDate);

        int transactionCount = (int) countTransactionsInPeriod(userId, effectiveStartDate, effectiveEndDate);
        SummaryTotals totals = SummaryTotals.of(totalIncome, totalExpenses, transactionCount);

        // Calculate category breakdown
        CategoryBreakdown categoryBreakdown = calculateCategoryBreakdown(
                userId, effectiveStartDate, effectiveEndDate, totalIncome, totalExpenses);

        // Calculate comparison with previous period
        PeriodComparison comparison = calculatePeriodComparison(
                userId, effectiveStartDate, effectiveEndDate, totalIncome, totalExpenses);

        return TransactionSummaryResponse.of(period, totals, categoryBreakdown, comparison);
    }

    // Private helper methods

    private Category validateAndGetCategory(UUID categoryId, TransactionType expectedType) {
        Category category = categoryService.getCategoryById(categoryId);

        if (category.getType() != expectedType) {
            throw new CategoryTypeMismatchException(categoryId, expectedType, category.getType());
        }

        return category;
    }

    private TransactionListSummary calculateSummaryForFilter(UUID userId, TransactionFilterRequest filter) {
        // Build specification for sum calculations
        Specification<Transaction> spec = TransactionSpecification.hasUserId(userId);

        if (filter != null) {
            if (filter.startDate() != null) {
                spec = spec.and(TransactionSpecification.dateOnOrAfter(filter.startDate()));
            }
            if (filter.endDate() != null) {
                spec = spec.and(TransactionSpecification.dateOnOrBefore(filter.endDate()));
            }
            if (filter.categoryIds() != null && !filter.categoryIds().isEmpty()) {
                spec = spec.and(TransactionSpecification.hasCategoryIn(filter.categoryIds()));
            }
            if (filter.minAmount() != null) {
                spec = spec.and(TransactionSpecification.amountGreaterThanOrEqual(filter.minAmount()));
            }
            if (filter.maxAmount() != null) {
                spec = spec.and(TransactionSpecification.amountLessThanOrEqual(filter.maxAmount()));
            }
            if (filter.search() != null && !filter.search().isBlank()) {
                spec = spec.and(TransactionSpecification.searchByDescriptionOrNotes(filter.search()));
            }
        }

        // Calculate income for filter
        Specification<Transaction> incomeSpec = spec.and(TransactionSpecification.hasType(TransactionType.INCOME));
        BigDecimal totalIncome = transactionRepository.findAll(incomeSpec)
                .stream()
                .map(Transaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Calculate expenses for filter
        Specification<Transaction> expenseSpec = spec.and(TransactionSpecification.hasType(TransactionType.EXPENSE));
        BigDecimal totalExpenses = transactionRepository.findAll(expenseSpec)
                .stream()
                .map(Transaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return TransactionListSummary.of(totalIncome, totalExpenses);
    }

    private long countTransactionsInPeriod(UUID userId, LocalDate startDate, LocalDate endDate) {
        long incomeCount = transactionRepository.countByUserIdAndTypeAndDateRange(
                userId, TransactionType.INCOME, startDate, endDate);
        long expenseCount = transactionRepository.countByUserIdAndTypeAndDateRange(
                userId, TransactionType.EXPENSE, startDate, endDate);
        return incomeCount + expenseCount;
    }

    private CategoryBreakdown calculateCategoryBreakdown(
            UUID userId,
            LocalDate startDate,
            LocalDate endDate,
            BigDecimal totalIncome,
            BigDecimal totalExpenses) {

        // Get all transactions in period grouped by category
        Specification<Transaction> spec = TransactionSpecification.hasUserId(userId)
                .and(TransactionSpecification.dateOnOrAfter(startDate))
                .and(TransactionSpecification.dateOnOrBefore(endDate))
                .and(TransactionSpecification.fetchCategory());

        List<Transaction> transactions = transactionRepository.findAll(spec);

        // Group by category and type
        Map<UUID, List<Transaction>> incomeByCategory = transactions.stream()
                .filter(Transaction::isIncome)
                .collect(Collectors.groupingBy(Transaction::getCategoryId));

        Map<UUID, List<Transaction>> expenseByCategory = transactions.stream()
                .filter(Transaction::isExpense)
                .collect(Collectors.groupingBy(Transaction::getCategoryId));

        // Build income breakdown
        List<CategoryBreakdownItem> incomeBreakdown = buildBreakdownItems(
                incomeByCategory, totalIncome);

        // Build expense breakdown
        List<CategoryBreakdownItem> expenseBreakdown = buildBreakdownItems(
                expenseByCategory, totalExpenses);

        return CategoryBreakdown.of(incomeBreakdown, expenseBreakdown);
    }

    private List<CategoryBreakdownItem> buildBreakdownItems(
            Map<UUID, List<Transaction>> transactionsByCategory,
            BigDecimal total) {

        return transactionsByCategory.entrySet().stream()
                .map(entry -> {
                    List<Transaction> txns = entry.getValue();
                    Category category = txns.get(0).getCategory();
                    BigDecimal categoryTotal = txns.stream()
                            .map(Transaction::getAmount)
                            .reduce(BigDecimal.ZERO, BigDecimal::add);

                    BigDecimal percentage = total.compareTo(BigDecimal.ZERO) == 0 ?
                            BigDecimal.ZERO :
                            categoryTotal.divide(total, 4, RoundingMode.HALF_UP)
                                    .multiply(BigDecimal.valueOf(100))
                                    .setScale(2, RoundingMode.HALF_UP);

                    return CategoryBreakdownItem.of(
                            CategorySummary.from(category),
                            categoryTotal,
                            percentage,
                            txns.size()
                    );
                })
                .sorted((a, b) -> b.total().compareTo(a.total())) // Sort by total descending
                .toList();
    }

    private PeriodComparison calculatePeriodComparison(
            UUID userId,
            LocalDate startDate,
            LocalDate endDate,
            BigDecimal currentIncome,
            BigDecimal currentExpenses) {

        // Calculate previous period (same length)
        long daysBetween = ChronoUnit.DAYS.between(startDate, endDate) + 1;
        LocalDate prevEndDate = startDate.minusDays(1);
        LocalDate prevStartDate = prevEndDate.minusDays(daysBetween - 1);

        // Get previous period totals
        BigDecimal prevIncome = transactionRepository.sumAmountByUserIdAndTypeAndDateRange(
                userId, TransactionType.INCOME, prevStartDate, prevEndDate);
        BigDecimal prevExpenses = transactionRepository.sumAmountByUserIdAndTypeAndDateRange(
                userId, TransactionType.EXPENSE, prevStartDate, prevEndDate);

        PreviousPeriodSummary previousPeriod = PreviousPeriodSummary.of(
                prevStartDate, prevEndDate, prevIncome, prevExpenses);

        // Calculate percentage changes
        BigDecimal currentNet = currentIncome.subtract(currentExpenses);
        BigDecimal prevNet = prevIncome.subtract(prevExpenses);

        PeriodChange change = PeriodChange.calculate(
                currentIncome, prevIncome,
                currentExpenses, prevExpenses,
                currentNet, prevNet);

        return PeriodComparison.of(previousPeriod, change);
    }
}
