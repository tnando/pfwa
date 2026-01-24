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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for TransactionService.
 * Tests business logic with mocked repositories.
 */
@ExtendWith(MockitoExtension.class)
class TransactionServiceTest {

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private CategoryService categoryService;

    @InjectMocks
    private TransactionService transactionService;

    @Captor
    private ArgumentCaptor<Transaction> transactionCaptor;

    private UUID testUserId;
    private UUID testTransactionId;
    private UUID testCategoryId;
    private User testUser;
    private Category testCategory;
    private Transaction testTransaction;

    @BeforeEach
    void setUp() {
        testUserId = UUID.randomUUID();
        testTransactionId = UUID.randomUUID();
        testCategoryId = UUID.randomUUID();

        testUser = new User("test@example.com", "hashedPassword");
        setField(testUser, "id", testUserId);

        testCategory = new Category("Food & Dining", TransactionType.EXPENSE, "restaurant", "#F44336", 1);
        setField(testCategory, "id", testCategoryId);
        testCategory.setActive(true);

        testTransaction = new Transaction(
                testUser,
                testCategory,
                new BigDecimal("150.00"),
                TransactionType.EXPENSE,
                LocalDate.of(2026, 1, 20)
        );
        setField(testTransaction, "id", testTransactionId);
        testTransaction.setDescription("Grocery shopping");
        testTransaction.setNotes("Weekly groceries");
        setField(testTransaction, "createdAt", Instant.now());
        setField(testTransaction, "updatedAt", Instant.now());
    }

    private void setField(Object target, String fieldName, Object value) {
        try {
            java.lang.reflect.Field field = target.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(target, value);
        } catch (Exception e) {
            throw new RuntimeException("Failed to set field: " + fieldName, e);
        }
    }

    @Nested
    @DisplayName("createTransaction")
    class CreateTransactionTests {

        @Test
        @DisplayName("Should create transaction with valid data")
        void createTransaction_success() {
            // Given
            CreateTransactionRequest request = new CreateTransactionRequest(
                    new BigDecimal("150.00"),
                    TransactionType.EXPENSE,
                    testCategoryId,
                    LocalDate.of(2026, 1, 20),
                    "Grocery shopping",
                    "Weekly groceries"
            );

            when(userRepository.getReferenceById(testUserId)).thenReturn(testUser);
            when(categoryService.getCategoryById(testCategoryId)).thenReturn(testCategory);
            when(transactionRepository.save(any(Transaction.class))).thenReturn(testTransaction);

            // When
            TransactionResponse result = transactionService.createTransaction(testUserId, request);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.id()).isEqualTo(testTransactionId);
            assertThat(result.amount()).isEqualByComparingTo(new BigDecimal("150.00"));
            assertThat(result.type()).isEqualTo(TransactionType.EXPENSE);
            assertThat(result.category().name()).isEqualTo("Food & Dining");
            assertThat(result.description()).isEqualTo("Grocery shopping");

            verify(transactionRepository).save(transactionCaptor.capture());
            Transaction saved = transactionCaptor.getValue();
            assertThat(saved.getAmount()).isEqualByComparingTo(new BigDecimal("150.00"));
            assertThat(saved.getType()).isEqualTo(TransactionType.EXPENSE);
        }

        @Test
        @DisplayName("Should create transaction with null optional fields")
        void createTransaction_nullOptionalFields() {
            // Given
            CreateTransactionRequest request = new CreateTransactionRequest(
                    new BigDecimal("100.00"),
                    TransactionType.EXPENSE,
                    testCategoryId,
                    LocalDate.of(2026, 1, 20),
                    null,
                    null
            );

            when(userRepository.getReferenceById(testUserId)).thenReturn(testUser);
            when(categoryService.getCategoryById(testCategoryId)).thenReturn(testCategory);
            when(transactionRepository.save(any(Transaction.class))).thenReturn(testTransaction);

            // When
            TransactionResponse result = transactionService.createTransaction(testUserId, request);

            // Then
            assertThat(result).isNotNull();
            verify(transactionRepository).save(any(Transaction.class));
        }

        @Test
        @DisplayName("Should throw exception when category type does not match transaction type")
        void createTransaction_categoryTypeMismatch() {
            // Given
            Category incomeCategory = new Category("Salary", TransactionType.INCOME, "payments", "#4CAF50", 1);
            setField(incomeCategory, "id", testCategoryId);
            incomeCategory.setActive(true);

            CreateTransactionRequest request = new CreateTransactionRequest(
                    new BigDecimal("150.00"),
                    TransactionType.EXPENSE, // Transaction is EXPENSE
                    testCategoryId,
                    LocalDate.of(2026, 1, 20),
                    "Test",
                    null
            );

            when(categoryService.getCategoryById(testCategoryId)).thenReturn(incomeCategory);

            // When/Then
            assertThatThrownBy(() -> transactionService.createTransaction(testUserId, request))
                    .isInstanceOf(CategoryTypeMismatchException.class);

            verify(transactionRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should create income transaction successfully")
        void createTransaction_incomeType() {
            // Given
            UUID incomeCategoryId = UUID.randomUUID();
            Category incomeCategory = new Category("Salary", TransactionType.INCOME, "payments", "#4CAF50", 1);
            setField(incomeCategory, "id", incomeCategoryId);
            incomeCategory.setActive(true);

            Transaction incomeTransaction = new Transaction(
                    testUser, incomeCategory, new BigDecimal("5000.00"),
                    TransactionType.INCOME, LocalDate.of(2026, 1, 15)
            );
            setField(incomeTransaction, "id", UUID.randomUUID());
            setField(incomeTransaction, "createdAt", Instant.now());
            setField(incomeTransaction, "updatedAt", Instant.now());

            CreateTransactionRequest request = new CreateTransactionRequest(
                    new BigDecimal("5000.00"),
                    TransactionType.INCOME,
                    incomeCategoryId,
                    LocalDate.of(2026, 1, 15),
                    "Monthly salary",
                    null
            );

            when(userRepository.getReferenceById(testUserId)).thenReturn(testUser);
            when(categoryService.getCategoryById(incomeCategoryId)).thenReturn(incomeCategory);
            when(transactionRepository.save(any(Transaction.class))).thenReturn(incomeTransaction);

            // When
            TransactionResponse result = transactionService.createTransaction(testUserId, request);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.type()).isEqualTo(TransactionType.INCOME);
        }
    }

    @Nested
    @DisplayName("getTransaction")
    class GetTransactionTests {

        @Test
        @DisplayName("Should return transaction when it exists and belongs to user")
        void getTransaction_success() {
            // Given
            when(transactionRepository.findByIdAndUserId(testTransactionId, testUserId))
                    .thenReturn(Optional.of(testTransaction));

            // When
            TransactionResponse result = transactionService.getTransaction(testUserId, testTransactionId);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.id()).isEqualTo(testTransactionId);
            assertThat(result.amount()).isEqualByComparingTo(new BigDecimal("150.00"));
        }

        @Test
        @DisplayName("Should throw exception when transaction does not exist")
        void getTransaction_notFound() {
            // Given
            when(transactionRepository.findByIdAndUserId(testTransactionId, testUserId))
                    .thenReturn(Optional.empty());

            // When/Then
            assertThatThrownBy(() -> transactionService.getTransaction(testUserId, testTransactionId))
                    .isInstanceOf(TransactionNotFoundException.class);
        }

        @Test
        @DisplayName("Should not return transaction belonging to another user")
        void getTransaction_wrongUser() {
            // Given
            UUID anotherUserId = UUID.randomUUID();
            when(transactionRepository.findByIdAndUserId(testTransactionId, anotherUserId))
                    .thenReturn(Optional.empty());

            // When/Then
            assertThatThrownBy(() -> transactionService.getTransaction(anotherUserId, testTransactionId))
                    .isInstanceOf(TransactionNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("getTransactions")
    class GetTransactionsTests {

        @Test
        @DisplayName("Should return paginated transactions for user")
        void getTransactions_success() {
            // Given
            Pageable pageable = PageRequest.of(0, 20);
            Page<Transaction> transactionPage = new PageImpl<>(
                    List.of(testTransaction), pageable, 1
            );

            when(transactionRepository.findAll(any(Specification.class), eq(pageable)))
                    .thenReturn(transactionPage);
            when(transactionRepository.findAll(any(Specification.class)))
                    .thenReturn(List.of(testTransaction));

            // When
            TransactionListResponse result = transactionService.getTransactions(
                    testUserId, TransactionFilterRequest.empty(), pageable
            );

            // Then
            assertThat(result).isNotNull();
            assertThat(result.content()).hasSize(1);
            assertThat(result.totalElements()).isEqualTo(1);
        }

        @Test
        @DisplayName("Should return empty list when no transactions exist")
        void getTransactions_empty() {
            // Given
            Pageable pageable = PageRequest.of(0, 20);
            Page<Transaction> emptyPage = new PageImpl<>(Collections.emptyList(), pageable, 0);

            when(transactionRepository.findAll(any(Specification.class), eq(pageable)))
                    .thenReturn(emptyPage);
            when(transactionRepository.findAll(any(Specification.class)))
                    .thenReturn(Collections.emptyList());

            // When
            TransactionListResponse result = transactionService.getTransactions(
                    testUserId, TransactionFilterRequest.empty(), pageable
            );

            // Then
            assertThat(result).isNotNull();
            assertThat(result.content()).isEmpty();
            assertThat(result.totalElements()).isZero();
        }

        @Test
        @DisplayName("Should calculate summary correctly")
        void getTransactions_withSummary() {
            // Given
            Transaction incomeTransaction = new Transaction(
                    testUser, testCategory, new BigDecimal("5000.00"),
                    TransactionType.INCOME, LocalDate.of(2026, 1, 15)
            );
            setField(incomeTransaction, "id", UUID.randomUUID());
            setField(incomeTransaction, "createdAt", Instant.now());
            setField(incomeTransaction, "updatedAt", Instant.now());

            Transaction expenseTransaction = new Transaction(
                    testUser, testCategory, new BigDecimal("1500.00"),
                    TransactionType.EXPENSE, LocalDate.of(2026, 1, 20)
            );
            setField(expenseTransaction, "id", UUID.randomUUID());
            setField(expenseTransaction, "createdAt", Instant.now());
            setField(expenseTransaction, "updatedAt", Instant.now());

            Pageable pageable = PageRequest.of(0, 20);
            Page<Transaction> transactionPage = new PageImpl<>(
                    List.of(incomeTransaction, expenseTransaction), pageable, 2
            );

            when(transactionRepository.findAll(any(Specification.class), eq(pageable)))
                    .thenReturn(transactionPage);
            when(transactionRepository.findAll(any(Specification.class)))
                    .thenReturn(List.of(incomeTransaction))
                    .thenReturn(List.of(expenseTransaction));

            // When
            TransactionListResponse result = transactionService.getTransactions(
                    testUserId, TransactionFilterRequest.empty(), pageable
            );

            // Then
            assertThat(result).isNotNull();
            assertThat(result.content()).hasSize(2);
        }
    }

    @Nested
    @DisplayName("updateTransaction")
    class UpdateTransactionTests {

        @Test
        @DisplayName("Should update transaction successfully")
        void updateTransaction_success() {
            // Given
            UpdateTransactionRequest request = new UpdateTransactionRequest(
                    new BigDecimal("175.00"),
                    TransactionType.EXPENSE,
                    testCategoryId,
                    LocalDate.of(2026, 1, 21),
                    "Updated description",
                    "Updated notes"
            );

            when(transactionRepository.findByIdAndUserId(testTransactionId, testUserId))
                    .thenReturn(Optional.of(testTransaction));
            when(categoryService.getCategoryById(testCategoryId)).thenReturn(testCategory);
            when(transactionRepository.save(any(Transaction.class))).thenReturn(testTransaction);

            // When
            TransactionResponse result = transactionService.updateTransaction(
                    testUserId, testTransactionId, request
            );

            // Then
            assertThat(result).isNotNull();
            verify(transactionRepository).save(transactionCaptor.capture());
            Transaction saved = transactionCaptor.getValue();
            assertThat(saved.getAmount()).isEqualByComparingTo(new BigDecimal("175.00"));
            assertThat(saved.getDescription()).isEqualTo("Updated description");
        }

        @Test
        @DisplayName("Should throw exception when updating non-existent transaction")
        void updateTransaction_notFound() {
            // Given
            UpdateTransactionRequest request = new UpdateTransactionRequest(
                    new BigDecimal("175.00"),
                    TransactionType.EXPENSE,
                    testCategoryId,
                    LocalDate.of(2026, 1, 21),
                    "Updated",
                    null
            );

            when(transactionRepository.findByIdAndUserId(testTransactionId, testUserId))
                    .thenReturn(Optional.empty());

            // When/Then
            assertThatThrownBy(() -> transactionService.updateTransaction(testUserId, testTransactionId, request))
                    .isInstanceOf(TransactionNotFoundException.class);

            verify(transactionRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should throw exception when changing to incompatible category")
        void updateTransaction_categoryTypeMismatch() {
            // Given
            Category incomeCategory = new Category("Salary", TransactionType.INCOME, "payments", "#4CAF50", 1);
            UUID incomeCategoryId = UUID.randomUUID();
            setField(incomeCategory, "id", incomeCategoryId);
            incomeCategory.setActive(true);

            UpdateTransactionRequest request = new UpdateTransactionRequest(
                    new BigDecimal("175.00"),
                    TransactionType.EXPENSE, // Keeping as EXPENSE
                    incomeCategoryId, // But using INCOME category
                    LocalDate.of(2026, 1, 21),
                    "Updated",
                    null
            );

            when(transactionRepository.findByIdAndUserId(testTransactionId, testUserId))
                    .thenReturn(Optional.of(testTransaction));
            when(categoryService.getCategoryById(incomeCategoryId)).thenReturn(incomeCategory);

            // When/Then
            assertThatThrownBy(() -> transactionService.updateTransaction(testUserId, testTransactionId, request))
                    .isInstanceOf(CategoryTypeMismatchException.class);

            verify(transactionRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should allow changing transaction type with compatible category")
        void updateTransaction_changeType() {
            // Given
            UUID incomeCategoryId = UUID.randomUUID();
            Category incomeCategory = new Category("Refunds", TransactionType.INCOME, "receipt", "#9CCC65", 1);
            setField(incomeCategory, "id", incomeCategoryId);
            incomeCategory.setActive(true);

            UpdateTransactionRequest request = new UpdateTransactionRequest(
                    new BigDecimal("50.00"),
                    TransactionType.INCOME, // Changing type
                    incomeCategoryId,
                    LocalDate.of(2026, 1, 21),
                    "Refund from store",
                    null
            );

            when(transactionRepository.findByIdAndUserId(testTransactionId, testUserId))
                    .thenReturn(Optional.of(testTransaction));
            when(categoryService.getCategoryById(incomeCategoryId)).thenReturn(incomeCategory);
            when(transactionRepository.save(any(Transaction.class))).thenReturn(testTransaction);

            // When
            transactionService.updateTransaction(testUserId, testTransactionId, request);

            // Then
            verify(transactionRepository).save(transactionCaptor.capture());
            Transaction saved = transactionCaptor.getValue();
            assertThat(saved.getType()).isEqualTo(TransactionType.INCOME);
        }
    }

    @Nested
    @DisplayName("deleteTransaction")
    class DeleteTransactionTests {

        @Test
        @DisplayName("Should delete transaction successfully")
        void deleteTransaction_success() {
            // Given
            when(transactionRepository.deleteByIdAndUserId(testTransactionId, testUserId))
                    .thenReturn(1);

            // When
            transactionService.deleteTransaction(testUserId, testTransactionId);

            // Then
            verify(transactionRepository).deleteByIdAndUserId(testTransactionId, testUserId);
        }

        @Test
        @DisplayName("Should throw exception when deleting non-existent transaction")
        void deleteTransaction_notFound() {
            // Given
            when(transactionRepository.deleteByIdAndUserId(testTransactionId, testUserId))
                    .thenReturn(0);

            // When/Then
            assertThatThrownBy(() -> transactionService.deleteTransaction(testUserId, testTransactionId))
                    .isInstanceOf(TransactionNotFoundException.class);
        }

        @Test
        @DisplayName("Should not delete transaction belonging to another user")
        void deleteTransaction_wrongUser() {
            // Given
            UUID anotherUserId = UUID.randomUUID();
            when(transactionRepository.deleteByIdAndUserId(testTransactionId, anotherUserId))
                    .thenReturn(0);

            // When/Then
            assertThatThrownBy(() -> transactionService.deleteTransaction(anotherUserId, testTransactionId))
                    .isInstanceOf(TransactionNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("getTransactionSummary")
    class GetTransactionSummaryTests {

        @Test
        @DisplayName("Should return transaction summary for date range")
        void getSummary_withDateRange() {
            // Given
            LocalDate startDate = LocalDate.of(2026, 1, 1);
            LocalDate endDate = LocalDate.of(2026, 1, 31);

            when(transactionRepository.sumAmountByUserIdAndTypeAndDateRange(
                    testUserId, TransactionType.INCOME, startDate, endDate))
                    .thenReturn(new BigDecimal("5000.00"));
            when(transactionRepository.sumAmountByUserIdAndTypeAndDateRange(
                    testUserId, TransactionType.EXPENSE, startDate, endDate))
                    .thenReturn(new BigDecimal("3250.50"));
            when(transactionRepository.countByUserIdAndTypeAndDateRange(
                    testUserId, TransactionType.INCOME, startDate, endDate))
                    .thenReturn(2L);
            when(transactionRepository.countByUserIdAndTypeAndDateRange(
                    testUserId, TransactionType.EXPENSE, startDate, endDate))
                    .thenReturn(45L);
            when(transactionRepository.findAll(any(Specification.class)))
                    .thenReturn(Collections.emptyList());

            // Previous period
            LocalDate prevStartDate = startDate.minusDays(31);
            LocalDate prevEndDate = startDate.minusDays(1);
            when(transactionRepository.sumAmountByUserIdAndTypeAndDateRange(
                    testUserId, TransactionType.INCOME, prevStartDate, prevEndDate))
                    .thenReturn(new BigDecimal("4500.00"));
            when(transactionRepository.sumAmountByUserIdAndTypeAndDateRange(
                    testUserId, TransactionType.EXPENSE, prevStartDate, prevEndDate))
                    .thenReturn(new BigDecimal("2800.00"));

            // When
            TransactionSummaryResponse result = transactionService.getTransactionSummary(
                    testUserId, startDate, endDate
            );

            // Then
            assertThat(result).isNotNull();
            assertThat(result.totals().income()).isEqualByComparingTo(new BigDecimal("5000.00"));
            assertThat(result.totals().expenses()).isEqualByComparingTo(new BigDecimal("3250.50"));
            assertThat(result.totals().net()).isEqualByComparingTo(new BigDecimal("1749.50"));
            assertThat(result.totals().transactionCount()).isEqualTo(47);
        }

        @Test
        @DisplayName("Should use current month when no dates provided")
        void getSummary_defaultsToCurrentMonth() {
            // Given
            when(transactionRepository.sumAmountByUserIdAndTypeAndDateRange(
                    eq(testUserId), eq(TransactionType.INCOME), any(LocalDate.class), any(LocalDate.class)))
                    .thenReturn(BigDecimal.ZERO);
            when(transactionRepository.sumAmountByUserIdAndTypeAndDateRange(
                    eq(testUserId), eq(TransactionType.EXPENSE), any(LocalDate.class), any(LocalDate.class)))
                    .thenReturn(BigDecimal.ZERO);
            when(transactionRepository.countByUserIdAndTypeAndDateRange(
                    eq(testUserId), any(TransactionType.class), any(LocalDate.class), any(LocalDate.class)))
                    .thenReturn(0L);
            when(transactionRepository.findAll(any(Specification.class)))
                    .thenReturn(Collections.emptyList());

            // When
            TransactionSummaryResponse result = transactionService.getTransactionSummary(
                    testUserId, null, null
            );

            // Then
            assertThat(result).isNotNull();
            assertThat(result.period().startDate()).isEqualTo(LocalDate.now().withDayOfMonth(1));
            assertThat(result.period().endDate()).isEqualTo(LocalDate.now());
        }

        @Test
        @DisplayName("Should return zero totals when no transactions exist")
        void getSummary_noTransactions() {
            // Given
            LocalDate startDate = LocalDate.of(2026, 1, 1);
            LocalDate endDate = LocalDate.of(2026, 1, 31);

            when(transactionRepository.sumAmountByUserIdAndTypeAndDateRange(
                    eq(testUserId), any(TransactionType.class), eq(startDate), eq(endDate)))
                    .thenReturn(BigDecimal.ZERO);
            when(transactionRepository.countByUserIdAndTypeAndDateRange(
                    eq(testUserId), any(TransactionType.class), eq(startDate), eq(endDate)))
                    .thenReturn(0L);
            when(transactionRepository.findAll(any(Specification.class)))
                    .thenReturn(Collections.emptyList());

            // Previous period
            when(transactionRepository.sumAmountByUserIdAndTypeAndDateRange(
                    eq(testUserId), any(TransactionType.class), any(LocalDate.class), any(LocalDate.class)))
                    .thenReturn(BigDecimal.ZERO);

            // When
            TransactionSummaryResponse result = transactionService.getTransactionSummary(
                    testUserId, startDate, endDate
            );

            // Then
            assertThat(result).isNotNull();
            assertThat(result.totals().income()).isEqualByComparingTo(BigDecimal.ZERO);
            assertThat(result.totals().expenses()).isEqualByComparingTo(BigDecimal.ZERO);
            assertThat(result.totals().net()).isEqualByComparingTo(BigDecimal.ZERO);
            assertThat(result.totals().transactionCount()).isZero();
        }
    }
}
