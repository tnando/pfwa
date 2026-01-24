package com.pfwa.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pfwa.IntegrationTestBase;
import com.pfwa.dto.transaction.CreateTransactionRequest;
import com.pfwa.dto.transaction.UpdateTransactionRequest;
import com.pfwa.entity.Category;
import com.pfwa.entity.Transaction;
import com.pfwa.entity.TransactionType;
import com.pfwa.entity.User;
import com.pfwa.repository.CategoryRepository;
import com.pfwa.repository.TransactionRepository;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for Transaction endpoints.
 * Uses TestContainers for database and tests full request/response cycle.
 */
class TransactionControllerIntegrationTest extends IntegrationTestBase {

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    private User testUser;
    private Category foodCategory;
    private Category salaryCategory;
    private String accessToken;

    @BeforeEach
    void setUpTransactions() {
        // Clean up transactions
        transactionRepository.deleteAll();

        // Create verified user and get access token
        testUser = createVerifiedUser(TEST_EMAIL, TEST_PASSWORD);
        accessToken = tokenService.createAccessToken(testUser);

        // Get categories (seeded by Flyway migration)
        foodCategory = categoryRepository.findByNameAndType("Food & Dining", TransactionType.EXPENSE)
                .orElseGet(() -> {
                    Category cat = new Category("Food & Dining", TransactionType.EXPENSE, "restaurant", "#F44336", 1);
                    cat.setActive(true);
                    return categoryRepository.save(cat);
                });

        salaryCategory = categoryRepository.findByNameAndType("Salary", TransactionType.INCOME)
                .orElseGet(() -> {
                    Category cat = new Category("Salary", TransactionType.INCOME, "payments", "#4CAF50", 1);
                    cat.setActive(true);
                    return categoryRepository.save(cat);
                });
    }

    private Transaction createTestTransaction(User user, Category category, BigDecimal amount,
                                               TransactionType type, LocalDate date, String description) {
        Transaction transaction = new Transaction(user, category, amount, type, date);
        transaction.setDescription(description);
        return transactionRepository.save(transaction);
    }

    @Nested
    @DisplayName("POST /api/v1/transactions")
    class CreateTransactionIntegrationTests {

        @Test
        @DisplayName("Should create transaction with valid data")
        void createTransaction_success() throws Exception {
            // Given
            CreateTransactionRequest request = new CreateTransactionRequest(
                    new BigDecimal("150.00"),
                    TransactionType.EXPENSE,
                    foodCategory.getId(),
                    LocalDate.of(2026, 1, 20),
                    "Grocery shopping at Whole Foods",
                    "Weekly groceries"
            );

            // When/Then
            mockMvc.perform(post("/api/v1/transactions")
                            .header("Authorization", "Bearer " + accessToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id").isNotEmpty())
                    .andExpect(jsonPath("$.amount").value(150.00))
                    .andExpect(jsonPath("$.type").value("EXPENSE"))
                    .andExpect(jsonPath("$.category.name").value("Food & Dining"))
                    .andExpect(jsonPath("$.description").value("Grocery shopping at Whole Foods"))
                    .andExpect(jsonPath("$.createdAt").isNotEmpty())
                    .andExpect(jsonPath("$.updatedAt").isNotEmpty());

            // Verify in database
            List<Transaction> transactions = transactionRepository.findAll();
            assertThat(transactions).hasSize(1);
            assertThat(transactions.get(0).getAmount()).isEqualByComparingTo(new BigDecimal("150.00"));
        }

        @Test
        @DisplayName("Should create income transaction")
        void createTransaction_income() throws Exception {
            // Given
            CreateTransactionRequest request = new CreateTransactionRequest(
                    new BigDecimal("5000.00"),
                    TransactionType.INCOME,
                    salaryCategory.getId(),
                    LocalDate.of(2026, 1, 15),
                    "Monthly salary",
                    null
            );

            // When/Then
            mockMvc.perform(post("/api/v1/transactions")
                            .header("Authorization", "Bearer " + accessToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.type").value("INCOME"))
                    .andExpect(jsonPath("$.category.type").value("INCOME"));
        }

        @Test
        @DisplayName("Should reject transaction with invalid amount")
        void createTransaction_invalidAmount() throws Exception {
            // Given
            CreateTransactionRequest request = new CreateTransactionRequest(
                    new BigDecimal("-100.00"),
                    TransactionType.EXPENSE,
                    foodCategory.getId(),
                    LocalDate.of(2026, 1, 20),
                    "Test",
                    null
            );

            // When/Then
            mockMvc.perform(post("/api/v1/transactions")
                            .header("Authorization", "Bearer " + accessToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"))
                    .andExpect(jsonPath("$.fieldErrors").isNotEmpty());
        }

        @Test
        @DisplayName("Should reject transaction with mismatched category type")
        void createTransaction_categoryTypeMismatch() throws Exception {
            // Given - Using income category for expense transaction
            CreateTransactionRequest request = new CreateTransactionRequest(
                    new BigDecimal("100.00"),
                    TransactionType.EXPENSE,
                    salaryCategory.getId(), // Income category
                    LocalDate.of(2026, 1, 20),
                    "Test",
                    null
            );

            // When/Then
            mockMvc.perform(post("/api/v1/transactions")
                            .header("Authorization", "Bearer " + accessToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"));
        }

        @Test
        @DisplayName("Should reject unauthenticated request")
        void createTransaction_unauthorized() throws Exception {
            // Given
            CreateTransactionRequest request = new CreateTransactionRequest(
                    new BigDecimal("100.00"),
                    TransactionType.EXPENSE,
                    foodCategory.getId(),
                    LocalDate.of(2026, 1, 20),
                    "Test",
                    null
            );

            // When/Then
            mockMvc.perform(post("/api/v1/transactions")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isUnauthorized());
        }
    }

    @Nested
    @DisplayName("GET /api/v1/transactions")
    class GetTransactionsIntegrationTests {

        @Test
        @DisplayName("Should return paginated transaction list")
        void getTransactions_success() throws Exception {
            // Given
            createTestTransaction(testUser, foodCategory, new BigDecimal("50.00"),
                    TransactionType.EXPENSE, LocalDate.of(2026, 1, 18), "Lunch");
            createTestTransaction(testUser, foodCategory, new BigDecimal("100.00"),
                    TransactionType.EXPENSE, LocalDate.of(2026, 1, 19), "Dinner");
            createTestTransaction(testUser, salaryCategory, new BigDecimal("5000.00"),
                    TransactionType.INCOME, LocalDate.of(2026, 1, 15), "Salary");

            // When/Then
            mockMvc.perform(get("/api/v1/transactions")
                            .header("Authorization", "Bearer " + accessToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content").isArray())
                    .andExpect(jsonPath("$.content", hasSize(3)))
                    .andExpect(jsonPath("$.totalElements").value(3))
                    .andExpect(jsonPath("$.summary.totalIncome").value(5000.00))
                    .andExpect(jsonPath("$.summary.totalExpenses").value(150.00));
        }

        @Test
        @DisplayName("Should return empty list when no transactions exist")
        void getTransactions_empty() throws Exception {
            // When/Then
            mockMvc.perform(get("/api/v1/transactions")
                            .header("Authorization", "Bearer " + accessToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content").isEmpty())
                    .andExpect(jsonPath("$.totalElements").value(0));
        }

        @Test
        @DisplayName("Should filter by date range")
        void getTransactions_filterByDateRange() throws Exception {
            // Given
            createTestTransaction(testUser, foodCategory, new BigDecimal("50.00"),
                    TransactionType.EXPENSE, LocalDate.of(2026, 1, 10), "Early month");
            createTestTransaction(testUser, foodCategory, new BigDecimal("100.00"),
                    TransactionType.EXPENSE, LocalDate.of(2026, 1, 20), "Mid month");
            createTestTransaction(testUser, foodCategory, new BigDecimal("75.00"),
                    TransactionType.EXPENSE, LocalDate.of(2026, 1, 25), "Late month");

            // When/Then
            mockMvc.perform(get("/api/v1/transactions")
                            .header("Authorization", "Bearer " + accessToken)
                            .param("startDate", "2026-01-15")
                            .param("endDate", "2026-01-22"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content", hasSize(1)))
                    .andExpect(jsonPath("$.content[0].description").value("Mid month"));
        }

        @Test
        @DisplayName("Should filter by type")
        void getTransactions_filterByType() throws Exception {
            // Given
            createTestTransaction(testUser, foodCategory, new BigDecimal("50.00"),
                    TransactionType.EXPENSE, LocalDate.of(2026, 1, 18), "Expense");
            createTestTransaction(testUser, salaryCategory, new BigDecimal("5000.00"),
                    TransactionType.INCOME, LocalDate.of(2026, 1, 15), "Income");

            // When/Then
            mockMvc.perform(get("/api/v1/transactions")
                            .header("Authorization", "Bearer " + accessToken)
                            .param("type", "EXPENSE"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content", hasSize(1)))
                    .andExpect(jsonPath("$.content[0].type").value("EXPENSE"));
        }

        @Test
        @DisplayName("Should filter by category")
        void getTransactions_filterByCategory() throws Exception {
            // Given
            createTestTransaction(testUser, foodCategory, new BigDecimal("50.00"),
                    TransactionType.EXPENSE, LocalDate.of(2026, 1, 18), "Food expense");
            createTestTransaction(testUser, salaryCategory, new BigDecimal("5000.00"),
                    TransactionType.INCOME, LocalDate.of(2026, 1, 15), "Salary income");

            // When/Then
            mockMvc.perform(get("/api/v1/transactions")
                            .header("Authorization", "Bearer " + accessToken)
                            .param("categoryIds", foodCategory.getId().toString()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content", hasSize(1)))
                    .andExpect(jsonPath("$.content[0].category.name").value("Food & Dining"));
        }

        @Test
        @DisplayName("Should filter by amount range")
        void getTransactions_filterByAmountRange() throws Exception {
            // Given
            createTestTransaction(testUser, foodCategory, new BigDecimal("25.00"),
                    TransactionType.EXPENSE, LocalDate.of(2026, 1, 18), "Small");
            createTestTransaction(testUser, foodCategory, new BigDecimal("100.00"),
                    TransactionType.EXPENSE, LocalDate.of(2026, 1, 19), "Medium");
            createTestTransaction(testUser, foodCategory, new BigDecimal("500.00"),
                    TransactionType.EXPENSE, LocalDate.of(2026, 1, 20), "Large");

            // When/Then
            mockMvc.perform(get("/api/v1/transactions")
                            .header("Authorization", "Bearer " + accessToken)
                            .param("minAmount", "50")
                            .param("maxAmount", "200"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content", hasSize(1)))
                    .andExpect(jsonPath("$.content[0].amount").value(100.00));
        }

        @Test
        @DisplayName("Should search by description")
        void getTransactions_search() throws Exception {
            // Given
            createTestTransaction(testUser, foodCategory, new BigDecimal("50.00"),
                    TransactionType.EXPENSE, LocalDate.of(2026, 1, 18), "Grocery shopping");
            createTestTransaction(testUser, foodCategory, new BigDecimal("75.00"),
                    TransactionType.EXPENSE, LocalDate.of(2026, 1, 19), "Restaurant dinner");
            createTestTransaction(testUser, foodCategory, new BigDecimal("30.00"),
                    TransactionType.EXPENSE, LocalDate.of(2026, 1, 20), "Groceries at Costco");

            // When/Then
            mockMvc.perform(get("/api/v1/transactions")
                            .header("Authorization", "Bearer " + accessToken)
                            .param("search", "grocery"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content", hasSize(2)));
        }

        @Test
        @DisplayName("Should sort by amount descending")
        void getTransactions_sortByAmount() throws Exception {
            // Given
            createTestTransaction(testUser, foodCategory, new BigDecimal("50.00"),
                    TransactionType.EXPENSE, LocalDate.of(2026, 1, 18), "Small");
            createTestTransaction(testUser, foodCategory, new BigDecimal("200.00"),
                    TransactionType.EXPENSE, LocalDate.of(2026, 1, 19), "Large");
            createTestTransaction(testUser, foodCategory, new BigDecimal("100.00"),
                    TransactionType.EXPENSE, LocalDate.of(2026, 1, 20), "Medium");

            // When/Then
            mockMvc.perform(get("/api/v1/transactions")
                            .header("Authorization", "Bearer " + accessToken)
                            .param("sort", "amount,desc"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content[0].amount").value(200.00))
                    .andExpect(jsonPath("$.content[1].amount").value(100.00))
                    .andExpect(jsonPath("$.content[2].amount").value(50.00));
        }

        @Test
        @DisplayName("Should paginate results")
        void getTransactions_pagination() throws Exception {
            // Given - Create 25 transactions
            for (int i = 0; i < 25; i++) {
                createTestTransaction(testUser, foodCategory, new BigDecimal(10 + i),
                        TransactionType.EXPENSE, LocalDate.of(2026, 1, 1).plusDays(i), "Transaction " + i);
            }

            // When/Then - First page
            mockMvc.perform(get("/api/v1/transactions")
                            .header("Authorization", "Bearer " + accessToken)
                            .param("page", "0")
                            .param("size", "10"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content", hasSize(10)))
                    .andExpect(jsonPath("$.totalElements").value(25))
                    .andExpect(jsonPath("$.totalPages").value(3));
        }

        @Test
        @DisplayName("Should not return transactions of other users")
        void getTransactions_userIsolation() throws Exception {
            // Given
            User otherUser = createVerifiedUser("other@example.com", TEST_PASSWORD);
            createTestTransaction(testUser, foodCategory, new BigDecimal("100.00"),
                    TransactionType.EXPENSE, LocalDate.of(2026, 1, 18), "My transaction");
            createTestTransaction(otherUser, foodCategory, new BigDecimal("200.00"),
                    TransactionType.EXPENSE, LocalDate.of(2026, 1, 19), "Other user transaction");

            // When/Then
            mockMvc.perform(get("/api/v1/transactions")
                            .header("Authorization", "Bearer " + accessToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content", hasSize(1)))
                    .andExpect(jsonPath("$.content[0].description").value("My transaction"));
        }
    }

    @Nested
    @DisplayName("GET /api/v1/transactions/{id}")
    class GetTransactionByIdIntegrationTests {

        @Test
        @DisplayName("Should return transaction by ID")
        void getTransaction_success() throws Exception {
            // Given
            Transaction transaction = createTestTransaction(testUser, foodCategory, new BigDecimal("150.00"),
                    TransactionType.EXPENSE, LocalDate.of(2026, 1, 20), "Test transaction");

            // When/Then
            mockMvc.perform(get("/api/v1/transactions/{id}", transaction.getId())
                            .header("Authorization", "Bearer " + accessToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(transaction.getId().toString()))
                    .andExpect(jsonPath("$.amount").value(150.00))
                    .andExpect(jsonPath("$.description").value("Test transaction"));
        }

        @Test
        @DisplayName("Should return 404 for non-existent transaction")
        void getTransaction_notFound() throws Exception {
            // When/Then
            mockMvc.perform(get("/api/v1/transactions/{id}", UUID.randomUUID())
                            .header("Authorization", "Bearer " + accessToken))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.error").value("NOT_FOUND"));
        }

        @Test
        @DisplayName("Should return 404 when accessing other user's transaction")
        void getTransaction_otherUser() throws Exception {
            // Given
            User otherUser = createVerifiedUser("other@example.com", TEST_PASSWORD);
            Transaction otherTransaction = createTestTransaction(otherUser, foodCategory, new BigDecimal("100.00"),
                    TransactionType.EXPENSE, LocalDate.of(2026, 1, 18), "Other user transaction");

            // When/Then - Current user cannot access other user's transaction
            mockMvc.perform(get("/api/v1/transactions/{id}", otherTransaction.getId())
                            .header("Authorization", "Bearer " + accessToken))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("PUT /api/v1/transactions/{id}")
    class UpdateTransactionIntegrationTests {

        @Test
        @DisplayName("Should update transaction successfully")
        void updateTransaction_success() throws Exception {
            // Given
            Transaction transaction = createTestTransaction(testUser, foodCategory, new BigDecimal("100.00"),
                    TransactionType.EXPENSE, LocalDate.of(2026, 1, 18), "Original description");

            UpdateTransactionRequest request = new UpdateTransactionRequest(
                    new BigDecimal("150.00"),
                    TransactionType.EXPENSE,
                    foodCategory.getId(),
                    LocalDate.of(2026, 1, 19),
                    "Updated description",
                    "Updated notes"
            );

            // When/Then
            mockMvc.perform(put("/api/v1/transactions/{id}", transaction.getId())
                            .header("Authorization", "Bearer " + accessToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.amount").value(150.00))
                    .andExpect(jsonPath("$.description").value("Updated description"))
                    .andExpect(jsonPath("$.notes").value("Updated notes"));

            // Verify in database
            Transaction updated = transactionRepository.findById(transaction.getId()).orElseThrow();
            assertThat(updated.getAmount()).isEqualByComparingTo(new BigDecimal("150.00"));
            assertThat(updated.getDescription()).isEqualTo("Updated description");
        }

        @Test
        @DisplayName("Should return 404 when updating non-existent transaction")
        void updateTransaction_notFound() throws Exception {
            // Given
            UpdateTransactionRequest request = new UpdateTransactionRequest(
                    new BigDecimal("150.00"),
                    TransactionType.EXPENSE,
                    foodCategory.getId(),
                    LocalDate.of(2026, 1, 19),
                    "Updated",
                    null
            );

            // When/Then
            mockMvc.perform(put("/api/v1/transactions/{id}", UUID.randomUUID())
                            .header("Authorization", "Bearer " + accessToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("Should not allow updating other user's transaction")
        void updateTransaction_otherUser() throws Exception {
            // Given
            User otherUser = createVerifiedUser("other@example.com", TEST_PASSWORD);
            Transaction otherTransaction = createTestTransaction(otherUser, foodCategory, new BigDecimal("100.00"),
                    TransactionType.EXPENSE, LocalDate.of(2026, 1, 18), "Other transaction");

            UpdateTransactionRequest request = new UpdateTransactionRequest(
                    new BigDecimal("999.00"),
                    TransactionType.EXPENSE,
                    foodCategory.getId(),
                    LocalDate.of(2026, 1, 19),
                    "Hacked",
                    null
            );

            // When/Then
            mockMvc.perform(put("/api/v1/transactions/{id}", otherTransaction.getId())
                            .header("Authorization", "Bearer " + accessToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isNotFound());

            // Verify original is unchanged
            Transaction unchanged = transactionRepository.findById(otherTransaction.getId()).orElseThrow();
            assertThat(unchanged.getAmount()).isEqualByComparingTo(new BigDecimal("100.00"));
        }
    }

    @Nested
    @DisplayName("DELETE /api/v1/transactions/{id}")
    class DeleteTransactionIntegrationTests {

        @Test
        @DisplayName("Should delete transaction successfully")
        void deleteTransaction_success() throws Exception {
            // Given
            Transaction transaction = createTestTransaction(testUser, foodCategory, new BigDecimal("100.00"),
                    TransactionType.EXPENSE, LocalDate.of(2026, 1, 18), "To be deleted");

            // When/Then
            mockMvc.perform(delete("/api/v1/transactions/{id}", transaction.getId())
                            .header("Authorization", "Bearer " + accessToken))
                    .andExpect(status().isNoContent());

            // Verify deletion
            assertThat(transactionRepository.findById(transaction.getId())).isEmpty();
        }

        @Test
        @DisplayName("Should return 404 when deleting non-existent transaction")
        void deleteTransaction_notFound() throws Exception {
            // When/Then
            mockMvc.perform(delete("/api/v1/transactions/{id}", UUID.randomUUID())
                            .header("Authorization", "Bearer " + accessToken))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("Should not allow deleting other user's transaction")
        void deleteTransaction_otherUser() throws Exception {
            // Given
            User otherUser = createVerifiedUser("other@example.com", TEST_PASSWORD);
            Transaction otherTransaction = createTestTransaction(otherUser, foodCategory, new BigDecimal("100.00"),
                    TransactionType.EXPENSE, LocalDate.of(2026, 1, 18), "Other transaction");

            // When/Then
            mockMvc.perform(delete("/api/v1/transactions/{id}", otherTransaction.getId())
                            .header("Authorization", "Bearer " + accessToken))
                    .andExpect(status().isNotFound());

            // Verify not deleted
            assertThat(transactionRepository.findById(otherTransaction.getId())).isPresent();
        }
    }

    @Nested
    @DisplayName("GET /api/v1/transactions/summary")
    class GetSummaryIntegrationTests {

        @Test
        @DisplayName("Should return transaction summary")
        void getSummary_success() throws Exception {
            // Given
            createTestTransaction(testUser, salaryCategory, new BigDecimal("5000.00"),
                    TransactionType.INCOME, LocalDate.of(2026, 1, 15), "Salary");
            createTestTransaction(testUser, foodCategory, new BigDecimal("500.00"),
                    TransactionType.EXPENSE, LocalDate.of(2026, 1, 18), "Food");
            createTestTransaction(testUser, foodCategory, new BigDecimal("200.00"),
                    TransactionType.EXPENSE, LocalDate.of(2026, 1, 20), "More food");

            // When/Then
            mockMvc.perform(get("/api/v1/transactions/summary")
                            .header("Authorization", "Bearer " + accessToken)
                            .param("startDate", "2026-01-01")
                            .param("endDate", "2026-01-31"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.totals.income").value(5000.00))
                    .andExpect(jsonPath("$.totals.expenses").value(700.00))
                    .andExpect(jsonPath("$.totals.net").value(4300.00))
                    .andExpect(jsonPath("$.totals.transactionCount").value(3));
        }

        @Test
        @DisplayName("Should return zero totals when no transactions")
        void getSummary_noTransactions() throws Exception {
            // When/Then
            mockMvc.perform(get("/api/v1/transactions/summary")
                            .header("Authorization", "Bearer " + accessToken)
                            .param("startDate", "2026-01-01")
                            .param("endDate", "2026-01-31"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.totals.income").value(0))
                    .andExpect(jsonPath("$.totals.expenses").value(0))
                    .andExpect(jsonPath("$.totals.net").value(0));
        }
    }
}
