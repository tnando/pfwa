package com.pfwa.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pfwa.dto.transaction.CategoriesResponse;
import com.pfwa.dto.transaction.CategoryResponse;
import com.pfwa.entity.TransactionType;
import com.pfwa.exception.GlobalExceptionHandler;
import com.pfwa.service.CategoryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Unit tests for CategoryController.
 * Tests controller behavior with mocked service layer.
 */
@ExtendWith(MockitoExtension.class)
class CategoryControllerTest {

    private MockMvc mockMvc;

    @Mock
    private CategoryService categoryService;

    @InjectMocks
    private CategoryController categoryController;

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        mockMvc = MockMvcBuilders.standaloneSetup(categoryController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    private List<CategoryResponse> createIncomeCategories() {
        return List.of(
                new CategoryResponse(UUID.randomUUID(), "Salary", TransactionType.INCOME, "payments", "#4CAF50"),
                new CategoryResponse(UUID.randomUUID(), "Freelance", TransactionType.INCOME, "work", "#8BC34A"),
                new CategoryResponse(UUID.randomUUID(), "Investments", TransactionType.INCOME, "trending_up", "#66BB6A")
        );
    }

    private List<CategoryResponse> createExpenseCategories() {
        return List.of(
                new CategoryResponse(UUID.randomUUID(), "Food & Dining", TransactionType.EXPENSE, "restaurant", "#F44336"),
                new CategoryResponse(UUID.randomUUID(), "Transportation", TransactionType.EXPENSE, "directions_car", "#E91E63"),
                new CategoryResponse(UUID.randomUUID(), "Housing", TransactionType.EXPENSE, "home", "#9C27B0"),
                new CategoryResponse(UUID.randomUUID(), "Utilities", TransactionType.EXPENSE, "bolt", "#673AB7"),
                new CategoryResponse(UUID.randomUUID(), "Entertainment", TransactionType.EXPENSE, "movie", "#3F51B5")
        );
    }

    @Nested
    @DisplayName("GET /api/v1/categories")
    class GetCategoriesTests {

        @Test
        @DisplayName("Should return all categories grouped by type")
        void getCategories_all() throws Exception {
            // Given
            List<CategoryResponse> incomeCategories = createIncomeCategories();
            List<CategoryResponse> expenseCategories = createExpenseCategories();
            CategoriesResponse response = CategoriesResponse.of(incomeCategories, expenseCategories);

            when(categoryService.getAllCategories()).thenReturn(response);

            // When/Then
            mockMvc.perform(get("/api/v1/categories"))
                    .andExpect(status().isOk())
                    .andExpect(header().string("Cache-Control", "max-age=3600"))
                    .andExpect(jsonPath("$.income").isArray())
                    .andExpect(jsonPath("$.income.length()").value(3))
                    .andExpect(jsonPath("$.expense").isArray())
                    .andExpect(jsonPath("$.expense.length()").value(5))
                    .andExpect(jsonPath("$.income[0].name").value("Salary"))
                    .andExpect(jsonPath("$.income[0].type").value("INCOME"))
                    .andExpect(jsonPath("$.expense[0].name").value("Food & Dining"))
                    .andExpect(jsonPath("$.expense[0].type").value("EXPENSE"));

            verify(categoryService).getAllCategories();
            verify(categoryService, never()).getCategoriesByType(any());
        }

        @Test
        @DisplayName("Should return only income categories when type=INCOME")
        void getCategories_incomeOnly() throws Exception {
            // Given
            List<CategoryResponse> incomeCategories = createIncomeCategories();
            CategoriesResponse response = CategoriesResponse.of(incomeCategories, Collections.emptyList());

            when(categoryService.getCategoriesByType(TransactionType.INCOME)).thenReturn(response);

            // When/Then
            mockMvc.perform(get("/api/v1/categories")
                            .param("type", "INCOME"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.income").isArray())
                    .andExpect(jsonPath("$.income.length()").value(3))
                    .andExpect(jsonPath("$.expense").isArray())
                    .andExpect(jsonPath("$.expense").isEmpty());

            verify(categoryService).getCategoriesByType(TransactionType.INCOME);
            verify(categoryService, never()).getAllCategories();
        }

        @Test
        @DisplayName("Should return only expense categories when type=EXPENSE")
        void getCategories_expenseOnly() throws Exception {
            // Given
            List<CategoryResponse> expenseCategories = createExpenseCategories();
            CategoriesResponse response = CategoriesResponse.of(Collections.emptyList(), expenseCategories);

            when(categoryService.getCategoriesByType(TransactionType.EXPENSE)).thenReturn(response);

            // When/Then
            mockMvc.perform(get("/api/v1/categories")
                            .param("type", "EXPENSE"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.income").isArray())
                    .andExpect(jsonPath("$.income").isEmpty())
                    .andExpect(jsonPath("$.expense").isArray())
                    .andExpect(jsonPath("$.expense.length()").value(5));

            verify(categoryService).getCategoriesByType(TransactionType.EXPENSE);
        }

        @Test
        @DisplayName("Should return empty lists when no categories exist")
        void getCategories_empty() throws Exception {
            // Given
            CategoriesResponse response = CategoriesResponse.of(Collections.emptyList(), Collections.emptyList());
            when(categoryService.getAllCategories()).thenReturn(response);

            // When/Then
            mockMvc.perform(get("/api/v1/categories"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.income").isEmpty())
                    .andExpect(jsonPath("$.expense").isEmpty());
        }

        @Test
        @DisplayName("Should include category details in response")
        void getCategories_includesDetails() throws Exception {
            // Given
            UUID categoryId = UUID.randomUUID();
            List<CategoryResponse> incomeCategories = List.of(
                    new CategoryResponse(categoryId, "Salary", TransactionType.INCOME, "payments", "#4CAF50")
            );
            CategoriesResponse response = CategoriesResponse.of(incomeCategories, Collections.emptyList());

            when(categoryService.getAllCategories()).thenReturn(response);

            // When/Then
            mockMvc.perform(get("/api/v1/categories"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.income[0].id").value(categoryId.toString()))
                    .andExpect(jsonPath("$.income[0].name").value("Salary"))
                    .andExpect(jsonPath("$.income[0].type").value("INCOME"))
                    .andExpect(jsonPath("$.income[0].icon").value("payments"))
                    .andExpect(jsonPath("$.income[0].color").value("#4CAF50"));
        }

        @Test
        @DisplayName("Should set cache control header for 1 hour")
        void getCategories_cacheControl() throws Exception {
            // Given
            CategoriesResponse response = CategoriesResponse.of(Collections.emptyList(), Collections.emptyList());
            when(categoryService.getAllCategories()).thenReturn(response);

            // When/Then
            mockMvc.perform(get("/api/v1/categories"))
                    .andExpect(status().isOk())
                    .andExpect(header().exists("Cache-Control"))
                    .andExpect(header().string("Cache-Control", "max-age=3600"));
        }

        @Test
        @DisplayName("Should return 400 BAD REQUEST for invalid type parameter")
        void getCategories_invalidType() throws Exception {
            // When/Then
            mockMvc.perform(get("/api/v1/categories")
                            .param("type", "INVALID_TYPE"))
                    .andExpect(status().isBadRequest());

            verify(categoryService, never()).getAllCategories();
            verify(categoryService, never()).getCategoriesByType(any());
        }

        @Test
        @DisplayName("Should return categories ordered by display order")
        void getCategories_orderedByDisplayOrder() throws Exception {
            // Given - Categories should be ordered as returned from service
            List<CategoryResponse> incomeCategories = List.of(
                    new CategoryResponse(UUID.randomUUID(), "Salary", TransactionType.INCOME, "payments", "#4CAF50"),
                    new CategoryResponse(UUID.randomUUID(), "Freelance", TransactionType.INCOME, "work", "#8BC34A"),
                    new CategoryResponse(UUID.randomUUID(), "Investments", TransactionType.INCOME, "trending_up", "#66BB6A")
            );
            CategoriesResponse response = CategoriesResponse.of(incomeCategories, Collections.emptyList());

            when(categoryService.getAllCategories()).thenReturn(response);

            // When/Then
            mockMvc.perform(get("/api/v1/categories"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.income[0].name").value("Salary"))
                    .andExpect(jsonPath("$.income[1].name").value("Freelance"))
                    .andExpect(jsonPath("$.income[2].name").value("Investments"));
        }

        @Test
        @DisplayName("Should handle mixed expense categories")
        void getCategories_expenseVariety() throws Exception {
            // Given
            List<CategoryResponse> expenseCategories = List.of(
                    new CategoryResponse(UUID.randomUUID(), "Food & Dining", TransactionType.EXPENSE, "restaurant", "#F44336"),
                    new CategoryResponse(UUID.randomUUID(), "Transportation", TransactionType.EXPENSE, "directions_car", "#E91E63"),
                    new CategoryResponse(UUID.randomUUID(), "Housing", TransactionType.EXPENSE, "home", "#9C27B0"),
                    new CategoryResponse(UUID.randomUUID(), "Utilities", TransactionType.EXPENSE, "bolt", "#673AB7"),
                    new CategoryResponse(UUID.randomUUID(), "Entertainment", TransactionType.EXPENSE, "movie", "#3F51B5"),
                    new CategoryResponse(UUID.randomUUID(), "Healthcare", TransactionType.EXPENSE, "local_hospital", "#2196F3"),
                    new CategoryResponse(UUID.randomUUID(), "Shopping", TransactionType.EXPENSE, "shopping_bag", "#03A9F4"),
                    new CategoryResponse(UUID.randomUUID(), "Education", TransactionType.EXPENSE, "school", "#00BCD4"),
                    new CategoryResponse(UUID.randomUUID(), "Travel", TransactionType.EXPENSE, "flight", "#009688"),
                    new CategoryResponse(UUID.randomUUID(), "Personal Care", TransactionType.EXPENSE, "spa", "#4CAF50"),
                    new CategoryResponse(UUID.randomUUID(), "Subscriptions", TransactionType.EXPENSE, "subscriptions", "#8BC34A"),
                    new CategoryResponse(UUID.randomUUID(), "Other", TransactionType.EXPENSE, "more_horiz", "#CDDC39")
            );
            CategoriesResponse response = CategoriesResponse.of(Collections.emptyList(), expenseCategories);

            when(categoryService.getAllCategories()).thenReturn(response);

            // When/Then
            mockMvc.perform(get("/api/v1/categories"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.expense.length()").value(12))
                    .andExpect(jsonPath("$.expense[11].name").value("Other"));
        }
    }
}
