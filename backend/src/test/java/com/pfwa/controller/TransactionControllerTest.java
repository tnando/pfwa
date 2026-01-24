package com.pfwa.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.pfwa.dto.transaction.*;
import com.pfwa.entity.TransactionType;
import com.pfwa.exception.CategoryNotFoundException;
import com.pfwa.exception.CategoryTypeMismatchException;
import com.pfwa.exception.GlobalExceptionHandler;
import com.pfwa.exception.TransactionNotFoundException;
import com.pfwa.security.UserPrincipal;
import com.pfwa.service.TransactionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Unit tests for TransactionController.
 * Tests controller behavior with mocked service layer.
 */
@ExtendWith(MockitoExtension.class)
class TransactionControllerTest {

    private MockMvc mockMvc;

    @Mock
    private TransactionService transactionService;

    @InjectMocks
    private TransactionController transactionController;

    private ObjectMapper objectMapper;
    private UUID testUserId;
    private UUID testTransactionId;
    private UUID testCategoryId;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());

        mockMvc = MockMvcBuilders.standaloneSetup(transactionController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .setCustomArgumentResolvers(new TestUserPrincipalArgumentResolver())
                .build();

        testUserId = UUID.randomUUID();
        testTransactionId = UUID.randomUUID();
        testCategoryId = UUID.randomUUID();
    }

    private TransactionResponse createTestTransactionResponse() {
        CategoryResponse category = new CategoryResponse(
                testCategoryId,
                "Food & Dining",
                TransactionType.EXPENSE,
                "restaurant",
                "#F44336"
        );
        return new TransactionResponse(
                testTransactionId,
                new BigDecimal("150.00"),
                TransactionType.EXPENSE,
                category,
                LocalDate.of(2026, 1, 20),
                "Grocery shopping",
                "Weekly groceries",
                Instant.now(),
                Instant.now()
        );
    }

    private TransactionListResponse createTestTransactionListResponse() {
        TransactionListSummary summary = TransactionListSummary.of(
                new BigDecimal("5000.00"),
                new BigDecimal("3250.50")
        );
        AppliedFilters filters = AppliedFilters.from(TransactionFilterRequest.empty());
        return TransactionListResponse.of(
                List.of(createTestTransactionResponse()),
                0, 20, 1, 1, true, true,
                summary,
                filters
        );
    }

    @Nested
    @DisplayName("POST /api/v1/transactions")
    class CreateTransactionTests {

        @Test
        @DisplayName("Should return 201 CREATED on successful transaction creation")
        void createTransaction_success() throws Exception {
            // Given
            CreateTransactionRequest request = new CreateTransactionRequest(
                    new BigDecimal("150.00"),
                    TransactionType.EXPENSE,
                    testCategoryId,
                    LocalDate.of(2026, 1, 20),
                    "Grocery shopping",
                    "Weekly groceries"
            );

            TransactionResponse response = createTestTransactionResponse();
            when(transactionService.createTransaction(any(UUID.class), any(CreateTransactionRequest.class)))
                    .thenReturn(response);

            // When/Then
            mockMvc.perform(post("/api/v1/transactions")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id").value(testTransactionId.toString()))
                    .andExpect(jsonPath("$.amount").value(150.00))
                    .andExpect(jsonPath("$.type").value("EXPENSE"))
                    .andExpect(jsonPath("$.category.name").value("Food & Dining"))
                    .andExpect(jsonPath("$.description").value("Grocery shopping"));

            verify(transactionService).createTransaction(any(UUID.class), any(CreateTransactionRequest.class));
        }

        @Test
        @DisplayName("Should return 400 BAD REQUEST when amount is missing")
        void createTransaction_missingAmount() throws Exception {
            // Given
            String requestJson = """
                    {
                        "type": "EXPENSE",
                        "categoryId": "%s",
                        "date": "2026-01-20",
                        "description": "Test"
                    }
                    """.formatted(testCategoryId);

            // When/Then
            mockMvc.perform(post("/api/v1/transactions")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestJson))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"));

            verify(transactionService, never()).createTransaction(any(), any());
        }

        @Test
        @DisplayName("Should return 400 BAD REQUEST when amount is zero")
        void createTransaction_zeroAmount() throws Exception {
            // Given
            CreateTransactionRequest request = new CreateTransactionRequest(
                    BigDecimal.ZERO,
                    TransactionType.EXPENSE,
                    testCategoryId,
                    LocalDate.of(2026, 1, 20),
                    "Test",
                    null
            );

            // When/Then
            mockMvc.perform(post("/api/v1/transactions")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"));

            verify(transactionService, never()).createTransaction(any(), any());
        }

        @Test
        @DisplayName("Should return 400 BAD REQUEST when amount is negative")
        void createTransaction_negativeAmount() throws Exception {
            // Given
            CreateTransactionRequest request = new CreateTransactionRequest(
                    new BigDecimal("-100.00"),
                    TransactionType.EXPENSE,
                    testCategoryId,
                    LocalDate.of(2026, 1, 20),
                    "Test",
                    null
            );

            // When/Then
            mockMvc.perform(post("/api/v1/transactions")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"));
        }

        @Test
        @DisplayName("Should return 400 BAD REQUEST when amount exceeds maximum")
        void createTransaction_amountExceedsMax() throws Exception {
            // Given
            CreateTransactionRequest request = new CreateTransactionRequest(
                    new BigDecimal("10000001"),
                    TransactionType.EXPENSE,
                    testCategoryId,
                    LocalDate.of(2026, 1, 20),
                    "Test",
                    null
            );

            // When/Then
            mockMvc.perform(post("/api/v1/transactions")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"));
        }

        @Test
        @DisplayName("Should return 400 BAD REQUEST when type is missing")
        void createTransaction_missingType() throws Exception {
            // Given
            String requestJson = """
                    {
                        "amount": 150.00,
                        "categoryId": "%s",
                        "date": "2026-01-20"
                    }
                    """.formatted(testCategoryId);

            // When/Then
            mockMvc.perform(post("/api/v1/transactions")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestJson))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"));
        }

        @Test
        @DisplayName("Should return 400 BAD REQUEST when categoryId is missing")
        void createTransaction_missingCategoryId() throws Exception {
            // Given
            String requestJson = """
                    {
                        "amount": 150.00,
                        "type": "EXPENSE",
                        "date": "2026-01-20"
                    }
                    """;

            // When/Then
            mockMvc.perform(post("/api/v1/transactions")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestJson))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"));
        }

        @Test
        @DisplayName("Should return 400 BAD REQUEST when date is missing")
        void createTransaction_missingDate() throws Exception {
            // Given
            String requestJson = """
                    {
                        "amount": 150.00,
                        "type": "EXPENSE",
                        "categoryId": "%s"
                    }
                    """.formatted(testCategoryId);

            // When/Then
            mockMvc.perform(post("/api/v1/transactions")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestJson))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"));
        }

        @Test
        @DisplayName("Should return 400 BAD REQUEST when description exceeds 500 characters")
        void createTransaction_descriptionTooLong() throws Exception {
            // Given
            String longDescription = "a".repeat(501);
            CreateTransactionRequest request = new CreateTransactionRequest(
                    new BigDecimal("150.00"),
                    TransactionType.EXPENSE,
                    testCategoryId,
                    LocalDate.of(2026, 1, 20),
                    longDescription,
                    null
            );

            // When/Then
            mockMvc.perform(post("/api/v1/transactions")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"));
        }

        @Test
        @DisplayName("Should return 404 NOT FOUND when category does not exist")
        void createTransaction_categoryNotFound() throws Exception {
            // Given
            CreateTransactionRequest request = new CreateTransactionRequest(
                    new BigDecimal("150.00"),
                    TransactionType.EXPENSE,
                    testCategoryId,
                    LocalDate.of(2026, 1, 20),
                    "Test",
                    null
            );

            when(transactionService.createTransaction(any(UUID.class), any(CreateTransactionRequest.class)))
                    .thenThrow(new CategoryNotFoundException(testCategoryId));

            // When/Then
            mockMvc.perform(post("/api/v1/transactions")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.error").value("NOT_FOUND"));
        }

        @Test
        @DisplayName("Should return 400 BAD REQUEST when category type mismatches transaction type")
        void createTransaction_categoryTypeMismatch() throws Exception {
            // Given
            CreateTransactionRequest request = new CreateTransactionRequest(
                    new BigDecimal("150.00"),
                    TransactionType.EXPENSE,
                    testCategoryId,
                    LocalDate.of(2026, 1, 20),
                    "Test",
                    null
            );

            when(transactionService.createTransaction(any(UUID.class), any(CreateTransactionRequest.class)))
                    .thenThrow(new CategoryTypeMismatchException(testCategoryId, TransactionType.EXPENSE, TransactionType.INCOME));

            // When/Then
            mockMvc.perform(post("/api/v1/transactions")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"));
        }

        @Test
        @DisplayName("Should accept transaction with null description and notes")
        void createTransaction_nullOptionalFields() throws Exception {
            // Given
            CreateTransactionRequest request = new CreateTransactionRequest(
                    new BigDecimal("150.00"),
                    TransactionType.EXPENSE,
                    testCategoryId,
                    LocalDate.of(2026, 1, 20),
                    null,
                    null
            );

            TransactionResponse response = createTestTransactionResponse();
            when(transactionService.createTransaction(any(UUID.class), any(CreateTransactionRequest.class)))
                    .thenReturn(response);

            // When/Then
            mockMvc.perform(post("/api/v1/transactions")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated());

            verify(transactionService).createTransaction(any(UUID.class), any(CreateTransactionRequest.class));
        }

        @Test
        @DisplayName("Should accept transaction with INCOME type")
        void createTransaction_incomeType() throws Exception {
            // Given
            UUID incomeCategoryId = UUID.randomUUID();
            CreateTransactionRequest request = new CreateTransactionRequest(
                    new BigDecimal("5000.00"),
                    TransactionType.INCOME,
                    incomeCategoryId,
                    LocalDate.of(2026, 1, 15),
                    "Monthly salary",
                    null
            );

            CategoryResponse category = new CategoryResponse(
                    incomeCategoryId, "Salary", TransactionType.INCOME, "payments", "#4CAF50"
            );
            TransactionResponse response = new TransactionResponse(
                    testTransactionId, new BigDecimal("5000.00"), TransactionType.INCOME,
                    category, LocalDate.of(2026, 1, 15), "Monthly salary", null,
                    Instant.now(), Instant.now()
            );
            when(transactionService.createTransaction(any(UUID.class), any(CreateTransactionRequest.class)))
                    .thenReturn(response);

            // When/Then
            mockMvc.perform(post("/api/v1/transactions")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.type").value("INCOME"));
        }
    }

    @Nested
    @DisplayName("GET /api/v1/transactions")
    class GetTransactionsTests {

        @Test
        @DisplayName("Should return paginated transaction list")
        void getTransactions_success() throws Exception {
            // Given
            TransactionListResponse response = createTestTransactionListResponse();
            when(transactionService.getTransactions(any(UUID.class), any(TransactionFilterRequest.class), any(Pageable.class)))
                    .thenReturn(response);

            // When/Then
            mockMvc.perform(get("/api/v1/transactions"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content").isArray())
                    .andExpect(jsonPath("$.content.length()").value(1))
                    .andExpect(jsonPath("$.totalElements").value(1))
                    .andExpect(jsonPath("$.summary.totalIncome").value(5000.00))
                    .andExpect(jsonPath("$.summary.totalExpenses").value(3250.50));

            verify(transactionService).getTransactions(any(UUID.class), any(TransactionFilterRequest.class), any(Pageable.class));
        }

        @Test
        @DisplayName("Should apply pagination parameters")
        void getTransactions_withPagination() throws Exception {
            // Given
            TransactionListResponse response = createTestTransactionListResponse();
            when(transactionService.getTransactions(any(UUID.class), any(TransactionFilterRequest.class), any(Pageable.class)))
                    .thenReturn(response);

            // When/Then
            mockMvc.perform(get("/api/v1/transactions")
                            .param("page", "2")
                            .param("size", "50"))
                    .andExpect(status().isOk());

            verify(transactionService).getTransactions(any(UUID.class), any(TransactionFilterRequest.class), argThat(pageable ->
                    pageable.getPageNumber() == 2 && pageable.getPageSize() == 50
            ));
        }

        @Test
        @DisplayName("Should constrain page size to maximum 100")
        void getTransactions_maxPageSize() throws Exception {
            // Given
            TransactionListResponse response = createTestTransactionListResponse();
            when(transactionService.getTransactions(any(UUID.class), any(TransactionFilterRequest.class), any(Pageable.class)))
                    .thenReturn(response);

            // When/Then
            mockMvc.perform(get("/api/v1/transactions")
                            .param("size", "200"))
                    .andExpect(status().isOk());

            verify(transactionService).getTransactions(any(UUID.class), any(TransactionFilterRequest.class), argThat(pageable ->
                    pageable.getPageSize() == 100
            ));
        }

        @Test
        @DisplayName("Should apply date filter")
        void getTransactions_withDateFilter() throws Exception {
            // Given
            TransactionListResponse response = createTestTransactionListResponse();
            when(transactionService.getTransactions(any(UUID.class), any(TransactionFilterRequest.class), any(Pageable.class)))
                    .thenReturn(response);

            // When/Then
            mockMvc.perform(get("/api/v1/transactions")
                            .param("startDate", "2026-01-01")
                            .param("endDate", "2026-01-31"))
                    .andExpect(status().isOk());

            verify(transactionService).getTransactions(any(UUID.class), argThat(filter ->
                    filter.startDate() != null && filter.endDate() != null
            ), any(Pageable.class));
        }

        @Test
        @DisplayName("Should apply type filter")
        void getTransactions_withTypeFilter() throws Exception {
            // Given
            TransactionListResponse response = createTestTransactionListResponse();
            when(transactionService.getTransactions(any(UUID.class), any(TransactionFilterRequest.class), any(Pageable.class)))
                    .thenReturn(response);

            // When/Then
            mockMvc.perform(get("/api/v1/transactions")
                            .param("type", "EXPENSE"))
                    .andExpect(status().isOk());

            verify(transactionService).getTransactions(any(UUID.class), argThat(filter ->
                    filter.type() == TransactionType.EXPENSE
            ), any(Pageable.class));
        }

        @Test
        @DisplayName("Should apply category filter")
        void getTransactions_withCategoryFilter() throws Exception {
            // Given
            TransactionListResponse response = createTestTransactionListResponse();
            when(transactionService.getTransactions(any(UUID.class), any(TransactionFilterRequest.class), any(Pageable.class)))
                    .thenReturn(response);

            UUID categoryId1 = UUID.randomUUID();
            UUID categoryId2 = UUID.randomUUID();

            // When/Then
            mockMvc.perform(get("/api/v1/transactions")
                            .param("categoryIds", categoryId1.toString(), categoryId2.toString()))
                    .andExpect(status().isOk());

            verify(transactionService).getTransactions(any(UUID.class), argThat(filter ->
                    filter.categoryIds() != null && filter.categoryIds().size() == 2
            ), any(Pageable.class));
        }

        @Test
        @DisplayName("Should apply amount range filter")
        void getTransactions_withAmountFilter() throws Exception {
            // Given
            TransactionListResponse response = createTestTransactionListResponse();
            when(transactionService.getTransactions(any(UUID.class), any(TransactionFilterRequest.class), any(Pageable.class)))
                    .thenReturn(response);

            // When/Then
            mockMvc.perform(get("/api/v1/transactions")
                            .param("minAmount", "50")
                            .param("maxAmount", "200"))
                    .andExpect(status().isOk());

            verify(transactionService).getTransactions(any(UUID.class), argThat(filter ->
                    filter.minAmount() != null && filter.minAmount().compareTo(new BigDecimal("50")) == 0 &&
                    filter.maxAmount() != null && filter.maxAmount().compareTo(new BigDecimal("200")) == 0
            ), any(Pageable.class));
        }

        @Test
        @DisplayName("Should apply search filter")
        void getTransactions_withSearch() throws Exception {
            // Given
            TransactionListResponse response = createTestTransactionListResponse();
            when(transactionService.getTransactions(any(UUID.class), any(TransactionFilterRequest.class), any(Pageable.class)))
                    .thenReturn(response);

            // When/Then
            mockMvc.perform(get("/api/v1/transactions")
                            .param("search", "grocery"))
                    .andExpect(status().isOk());

            verify(transactionService).getTransactions(any(UUID.class), argThat(filter ->
                    "grocery".equals(filter.search())
            ), any(Pageable.class));
        }

        @Test
        @DisplayName("Should apply sort parameters")
        void getTransactions_withSort() throws Exception {
            // Given
            TransactionListResponse response = createTestTransactionListResponse();
            when(transactionService.getTransactions(any(UUID.class), any(TransactionFilterRequest.class), any(Pageable.class)))
                    .thenReturn(response);

            // When/Then
            mockMvc.perform(get("/api/v1/transactions")
                            .param("sort", "amount,asc"))
                    .andExpect(status().isOk());

            verify(transactionService).getTransactions(any(UUID.class), any(TransactionFilterRequest.class), argThat(pageable ->
                    pageable.getSort().getOrderFor("amount") != null &&
                    pageable.getSort().getOrderFor("amount").isAscending()
            ));
        }

        @Test
        @DisplayName("Should return empty list when no transactions")
        void getTransactions_emptyList() throws Exception {
            // Given
            TransactionListSummary summary = TransactionListSummary.of(BigDecimal.ZERO, BigDecimal.ZERO);
            AppliedFilters filters = AppliedFilters.from(TransactionFilterRequest.empty());
            TransactionListResponse response = TransactionListResponse.of(
                    Collections.emptyList(), 0, 20, 0, 0, true, true, summary, filters
            );
            when(transactionService.getTransactions(any(UUID.class), any(TransactionFilterRequest.class), any(Pageable.class)))
                    .thenReturn(response);

            // When/Then
            mockMvc.perform(get("/api/v1/transactions"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content").isEmpty())
                    .andExpect(jsonPath("$.totalElements").value(0));
        }
    }

    @Nested
    @DisplayName("GET /api/v1/transactions/{id}")
    class GetTransactionByIdTests {

        @Test
        @DisplayName("Should return transaction by ID")
        void getTransaction_success() throws Exception {
            // Given
            TransactionResponse response = createTestTransactionResponse();
            when(transactionService.getTransaction(any(UUID.class), any(UUID.class)))
                    .thenReturn(response);

            // When/Then
            mockMvc.perform(get("/api/v1/transactions/{id}", testTransactionId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(testTransactionId.toString()))
                    .andExpect(jsonPath("$.amount").value(150.00));

            verify(transactionService).getTransaction(any(UUID.class), eq(testTransactionId));
        }

        @Test
        @DisplayName("Should return 404 NOT FOUND when transaction does not exist")
        void getTransaction_notFound() throws Exception {
            // Given
            when(transactionService.getTransaction(any(UUID.class), any(UUID.class)))
                    .thenThrow(new TransactionNotFoundException(testTransactionId));

            // When/Then
            mockMvc.perform(get("/api/v1/transactions/{id}", testTransactionId))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.error").value("NOT_FOUND"));
        }

        @Test
        @DisplayName("Should return 400 BAD REQUEST for invalid UUID format")
        void getTransaction_invalidUuid() throws Exception {
            // When/Then
            mockMvc.perform(get("/api/v1/transactions/{id}", "invalid-uuid"))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("PUT /api/v1/transactions/{id}")
    class UpdateTransactionTests {

        @Test
        @DisplayName("Should return 200 OK on successful update")
        void updateTransaction_success() throws Exception {
            // Given
            UpdateTransactionRequest request = new UpdateTransactionRequest(
                    new BigDecimal("175.00"),
                    TransactionType.EXPENSE,
                    testCategoryId,
                    LocalDate.of(2026, 1, 20),
                    "Updated description",
                    "Updated notes"
            );

            TransactionResponse response = createTestTransactionResponse();
            when(transactionService.updateTransaction(any(UUID.class), any(UUID.class), any(UpdateTransactionRequest.class)))
                    .thenReturn(response);

            // When/Then
            mockMvc.perform(put("/api/v1/transactions/{id}", testTransactionId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(testTransactionId.toString()));

            verify(transactionService).updateTransaction(any(UUID.class), eq(testTransactionId), any(UpdateTransactionRequest.class));
        }

        @Test
        @DisplayName("Should return 404 NOT FOUND when updating non-existent transaction")
        void updateTransaction_notFound() throws Exception {
            // Given
            UpdateTransactionRequest request = new UpdateTransactionRequest(
                    new BigDecimal("175.00"),
                    TransactionType.EXPENSE,
                    testCategoryId,
                    LocalDate.of(2026, 1, 20),
                    "Updated",
                    null
            );

            when(transactionService.updateTransaction(any(UUID.class), any(UUID.class), any(UpdateTransactionRequest.class)))
                    .thenThrow(new TransactionNotFoundException(testTransactionId));

            // When/Then
            mockMvc.perform(put("/api/v1/transactions/{id}", testTransactionId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.error").value("NOT_FOUND"));
        }

        @Test
        @DisplayName("Should return 400 BAD REQUEST for invalid update data")
        void updateTransaction_invalidData() throws Exception {
            // Given
            UpdateTransactionRequest request = new UpdateTransactionRequest(
                    new BigDecimal("-100.00"), // Invalid negative amount
                    TransactionType.EXPENSE,
                    testCategoryId,
                    LocalDate.of(2026, 1, 20),
                    "Test",
                    null
            );

            // When/Then
            mockMvc.perform(put("/api/v1/transactions/{id}", testTransactionId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"));

            verify(transactionService, never()).updateTransaction(any(), any(), any());
        }

        @Test
        @DisplayName("Should allow changing transaction type")
        void updateTransaction_changeType() throws Exception {
            // Given
            UUID incomeCategoryId = UUID.randomUUID();
            UpdateTransactionRequest request = new UpdateTransactionRequest(
                    new BigDecimal("100.00"),
                    TransactionType.INCOME,
                    incomeCategoryId,
                    LocalDate.of(2026, 1, 20),
                    "Changed to income",
                    null
            );

            CategoryResponse category = new CategoryResponse(
                    incomeCategoryId, "Refunds", TransactionType.INCOME, "receipt", "#9CCC65"
            );
            TransactionResponse response = new TransactionResponse(
                    testTransactionId, new BigDecimal("100.00"), TransactionType.INCOME,
                    category, LocalDate.of(2026, 1, 20), "Changed to income", null,
                    Instant.now(), Instant.now()
            );
            when(transactionService.updateTransaction(any(UUID.class), any(UUID.class), any(UpdateTransactionRequest.class)))
                    .thenReturn(response);

            // When/Then
            mockMvc.perform(put("/api/v1/transactions/{id}", testTransactionId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.type").value("INCOME"));
        }
    }

    @Nested
    @DisplayName("DELETE /api/v1/transactions/{id}")
    class DeleteTransactionTests {

        @Test
        @DisplayName("Should return 204 NO CONTENT on successful deletion")
        void deleteTransaction_success() throws Exception {
            // Given
            doNothing().when(transactionService).deleteTransaction(any(UUID.class), any(UUID.class));

            // When/Then
            mockMvc.perform(delete("/api/v1/transactions/{id}", testTransactionId))
                    .andExpect(status().isNoContent());

            verify(transactionService).deleteTransaction(any(UUID.class), eq(testTransactionId));
        }

        @Test
        @DisplayName("Should return 404 NOT FOUND when deleting non-existent transaction")
        void deleteTransaction_notFound() throws Exception {
            // Given
            doThrow(new TransactionNotFoundException(testTransactionId))
                    .when(transactionService).deleteTransaction(any(UUID.class), any(UUID.class));

            // When/Then
            mockMvc.perform(delete("/api/v1/transactions/{id}", testTransactionId))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.error").value("NOT_FOUND"));
        }

        @Test
        @DisplayName("Should return 400 BAD REQUEST for invalid UUID format")
        void deleteTransaction_invalidUuid() throws Exception {
            // When/Then
            mockMvc.perform(delete("/api/v1/transactions/{id}", "not-a-uuid"))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("GET /api/v1/transactions/summary")
    class GetTransactionSummaryTests {

        @Test
        @DisplayName("Should return transaction summary")
        void getSummary_success() throws Exception {
            // Given
            SummaryPeriod period = SummaryPeriod.of(LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 31));
            SummaryTotals totals = SummaryTotals.of(new BigDecimal("5000.00"), new BigDecimal("3250.50"), 87);
            CategoryBreakdown breakdown = CategoryBreakdown.of(Collections.emptyList(), Collections.emptyList());
            TransactionSummaryResponse response = TransactionSummaryResponse.of(period, totals, breakdown, null);

            when(transactionService.getTransactionSummary(any(UUID.class), any(), any()))
                    .thenReturn(response);

            // When/Then
            mockMvc.perform(get("/api/v1/transactions/summary")
                            .param("startDate", "2026-01-01")
                            .param("endDate", "2026-01-31"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.totals.income").value(5000.00))
                    .andExpect(jsonPath("$.totals.expenses").value(3250.50))
                    .andExpect(jsonPath("$.totals.transactionCount").value(87));

            verify(transactionService).getTransactionSummary(
                    any(UUID.class),
                    eq(LocalDate.of(2026, 1, 1)),
                    eq(LocalDate.of(2026, 1, 31))
            );
        }

        @Test
        @DisplayName("Should return summary without date parameters")
        void getSummary_noDates() throws Exception {
            // Given
            SummaryPeriod period = SummaryPeriod.of(LocalDate.now().withDayOfMonth(1), LocalDate.now());
            SummaryTotals totals = SummaryTotals.of(BigDecimal.ZERO, BigDecimal.ZERO, 0);
            CategoryBreakdown breakdown = CategoryBreakdown.of(Collections.emptyList(), Collections.emptyList());
            TransactionSummaryResponse response = TransactionSummaryResponse.of(period, totals, breakdown, null);

            when(transactionService.getTransactionSummary(any(UUID.class), isNull(), isNull()))
                    .thenReturn(response);

            // When/Then
            mockMvc.perform(get("/api/v1/transactions/summary"))
                    .andExpect(status().isOk());

            verify(transactionService).getTransactionSummary(any(UUID.class), isNull(), isNull());
        }
    }

    /**
     * Custom argument resolver for tests to inject UserPrincipal
     */
    static class TestUserPrincipalArgumentResolver implements org.springframework.web.method.support.HandlerMethodArgumentResolver {
        @Override
        public boolean supportsParameter(org.springframework.core.MethodParameter parameter) {
            return parameter.getParameterType().equals(UserPrincipal.class);
        }

        @Override
        public Object resolveArgument(org.springframework.core.MethodParameter parameter,
                                       org.springframework.web.method.support.ModelAndViewContainer mavContainer,
                                       org.springframework.web.context.request.NativeWebRequest webRequest,
                                       org.springframework.web.bind.support.WebDataBinderFactory binderFactory) {
            return mock(UserPrincipal.class, invocation -> {
                if (invocation.getMethod().getName().equals("getId")) {
                    return UUID.randomUUID();
                }
                return null;
            });
        }
    }
}
