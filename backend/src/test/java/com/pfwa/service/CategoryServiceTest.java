package com.pfwa.service;

import com.pfwa.dto.transaction.CategoriesResponse;
import com.pfwa.dto.transaction.CategoryResponse;
import com.pfwa.entity.Category;
import com.pfwa.entity.TransactionType;
import com.pfwa.exception.CategoryNotFoundException;
import com.pfwa.repository.CategoryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for CategoryService.
 * Tests business logic with mocked repository.
 */
@ExtendWith(MockitoExtension.class)
class CategoryServiceTest {

    @Mock
    private CategoryRepository categoryRepository;

    @InjectMocks
    private CategoryService categoryService;

    private Category salaryCategory;
    private Category freelanceCategory;
    private Category foodCategory;
    private Category transportCategory;

    @BeforeEach
    void setUp() {
        salaryCategory = createCategory("Salary", TransactionType.INCOME, "payments", "#4CAF50", 1);
        freelanceCategory = createCategory("Freelance", TransactionType.INCOME, "work", "#8BC34A", 2);
        foodCategory = createCategory("Food & Dining", TransactionType.EXPENSE, "restaurant", "#F44336", 1);
        transportCategory = createCategory("Transportation", TransactionType.EXPENSE, "directions_car", "#E91E63", 2);
    }

    private Category createCategory(String name, TransactionType type, String icon, String color, int displayOrder) {
        Category category = new Category(name, type, icon, color, displayOrder);
        setField(category, "id", UUID.randomUUID());
        category.setActive(true);
        return category;
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
    @DisplayName("getAllCategories")
    class GetAllCategoriesTests {

        @Test
        @DisplayName("Should return all categories grouped by type")
        void getAllCategories_success() {
            // Given
            when(categoryRepository.findAllIncomeCategories())
                    .thenReturn(List.of(salaryCategory, freelanceCategory));
            when(categoryRepository.findAllExpenseCategories())
                    .thenReturn(List.of(foodCategory, transportCategory));

            // When
            CategoriesResponse result = categoryService.getAllCategories();

            // Then
            assertThat(result).isNotNull();
            assertThat(result.income()).hasSize(2);
            assertThat(result.expense()).hasSize(2);
            assertThat(result.income().get(0).name()).isEqualTo("Salary");
            assertThat(result.expense().get(0).name()).isEqualTo("Food & Dining");
        }

        @Test
        @DisplayName("Should return empty lists when no categories exist")
        void getAllCategories_empty() {
            // Given
            when(categoryRepository.findAllIncomeCategories()).thenReturn(Collections.emptyList());
            when(categoryRepository.findAllExpenseCategories()).thenReturn(Collections.emptyList());

            // When
            CategoriesResponse result = categoryService.getAllCategories();

            // Then
            assertThat(result).isNotNull();
            assertThat(result.income()).isEmpty();
            assertThat(result.expense()).isEmpty();
        }

        @Test
        @DisplayName("Should return only income categories when no expense categories exist")
        void getAllCategories_onlyIncome() {
            // Given
            when(categoryRepository.findAllIncomeCategories())
                    .thenReturn(List.of(salaryCategory, freelanceCategory));
            when(categoryRepository.findAllExpenseCategories())
                    .thenReturn(Collections.emptyList());

            // When
            CategoriesResponse result = categoryService.getAllCategories();

            // Then
            assertThat(result.income()).hasSize(2);
            assertThat(result.expense()).isEmpty();
        }

        @Test
        @DisplayName("Should return only expense categories when no income categories exist")
        void getAllCategories_onlyExpense() {
            // Given
            when(categoryRepository.findAllIncomeCategories())
                    .thenReturn(Collections.emptyList());
            when(categoryRepository.findAllExpenseCategories())
                    .thenReturn(List.of(foodCategory, transportCategory));

            // When
            CategoriesResponse result = categoryService.getAllCategories();

            // Then
            assertThat(result.income()).isEmpty();
            assertThat(result.expense()).hasSize(2);
        }
    }

    @Nested
    @DisplayName("getCategoriesByType")
    class GetCategoriesByTypeTests {

        @Test
        @DisplayName("Should return income categories when type is INCOME")
        void getCategoriesByType_income() {
            // Given
            when(categoryRepository.findByTypeAndActiveOrderByDisplayOrder(TransactionType.INCOME))
                    .thenReturn(List.of(salaryCategory, freelanceCategory));

            // When
            CategoriesResponse result = categoryService.getCategoriesByType(TransactionType.INCOME);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.income()).hasSize(2);
            assertThat(result.expense()).isEmpty();
            assertThat(result.income().get(0).type()).isEqualTo(TransactionType.INCOME);
        }

        @Test
        @DisplayName("Should return expense categories when type is EXPENSE")
        void getCategoriesByType_expense() {
            // Given
            when(categoryRepository.findByTypeAndActiveOrderByDisplayOrder(TransactionType.EXPENSE))
                    .thenReturn(List.of(foodCategory, transportCategory));

            // When
            CategoriesResponse result = categoryService.getCategoriesByType(TransactionType.EXPENSE);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.income()).isEmpty();
            assertThat(result.expense()).hasSize(2);
            assertThat(result.expense().get(0).type()).isEqualTo(TransactionType.EXPENSE);
        }

        @Test
        @DisplayName("Should return all categories when type is null")
        void getCategoriesByType_null() {
            // Given
            when(categoryRepository.findAllIncomeCategories())
                    .thenReturn(List.of(salaryCategory));
            when(categoryRepository.findAllExpenseCategories())
                    .thenReturn(List.of(foodCategory));

            // When
            CategoriesResponse result = categoryService.getCategoriesByType(null);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.income()).hasSize(1);
            assertThat(result.expense()).hasSize(1);
        }

        @Test
        @DisplayName("Should return empty list when no categories of type exist")
        void getCategoriesByType_empty() {
            // Given
            when(categoryRepository.findByTypeAndActiveOrderByDisplayOrder(TransactionType.INCOME))
                    .thenReturn(Collections.emptyList());

            // When
            CategoriesResponse result = categoryService.getCategoriesByType(TransactionType.INCOME);

            // Then
            assertThat(result.income()).isEmpty();
            assertThat(result.expense()).isEmpty();
        }
    }

    @Nested
    @DisplayName("getCategoryById")
    class GetCategoryByIdTests {

        @Test
        @DisplayName("Should return category when it exists and is active")
        void getCategoryById_success() {
            // Given
            UUID categoryId = salaryCategory.getId();
            when(categoryRepository.findByIdAndActive(categoryId))
                    .thenReturn(Optional.of(salaryCategory));

            // When
            Category result = categoryService.getCategoryById(categoryId);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getName()).isEqualTo("Salary");
            assertThat(result.getType()).isEqualTo(TransactionType.INCOME);
        }

        @Test
        @DisplayName("Should throw exception when category does not exist")
        void getCategoryById_notFound() {
            // Given
            UUID categoryId = UUID.randomUUID();
            when(categoryRepository.findByIdAndActive(categoryId))
                    .thenReturn(Optional.empty());

            // When/Then
            assertThatThrownBy(() -> categoryService.getCategoryById(categoryId))
                    .isInstanceOf(CategoryNotFoundException.class);
        }

        @Test
        @DisplayName("Should throw exception when category is inactive")
        void getCategoryById_inactive() {
            // Given
            Category inactiveCategory = createCategory("Old Category", TransactionType.EXPENSE, "old", "#000000", 99);
            inactiveCategory.setActive(false);
            UUID categoryId = inactiveCategory.getId();

            when(categoryRepository.findByIdAndActive(categoryId))
                    .thenReturn(Optional.empty()); // Inactive categories are not returned

            // When/Then
            assertThatThrownBy(() -> categoryService.getCategoryById(categoryId))
                    .isInstanceOf(CategoryNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("validateCategoryForType")
    class ValidateCategoryForTypeTests {

        @Test
        @DisplayName("Should return category when type matches")
        void validateCategoryForType_matches() {
            // Given
            UUID categoryId = salaryCategory.getId();
            when(categoryRepository.findByIdAndActive(categoryId))
                    .thenReturn(Optional.of(salaryCategory));

            // When
            Category result = categoryService.validateCategoryForType(categoryId, TransactionType.INCOME);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getType()).isEqualTo(TransactionType.INCOME);
        }

        @Test
        @DisplayName("Should return category even when type does not match (logging only)")
        void validateCategoryForType_mismatch() {
            // Given - The method logs but doesn't throw on mismatch
            UUID categoryId = salaryCategory.getId();
            when(categoryRepository.findByIdAndActive(categoryId))
                    .thenReturn(Optional.of(salaryCategory));

            // When
            Category result = categoryService.validateCategoryForType(categoryId, TransactionType.EXPENSE);

            // Then - It returns the category but logs a debug message
            assertThat(result).isNotNull();
            assertThat(result.getType()).isEqualTo(TransactionType.INCOME);
        }

        @Test
        @DisplayName("Should throw exception when category does not exist")
        void validateCategoryForType_notFound() {
            // Given
            UUID categoryId = UUID.randomUUID();
            when(categoryRepository.findByIdAndActive(categoryId))
                    .thenReturn(Optional.empty());

            // When/Then
            assertThatThrownBy(() -> categoryService.validateCategoryForType(categoryId, TransactionType.INCOME))
                    .isInstanceOf(CategoryNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("Category Response Mapping")
    class CategoryResponseMappingTests {

        @Test
        @DisplayName("Should correctly map category entity to response")
        void mapToResponse() {
            // Given
            when(categoryRepository.findAllIncomeCategories())
                    .thenReturn(List.of(salaryCategory));
            when(categoryRepository.findAllExpenseCategories())
                    .thenReturn(Collections.emptyList());

            // When
            CategoriesResponse result = categoryService.getAllCategories();

            // Then
            CategoryResponse response = result.income().get(0);
            assertThat(response.id()).isEqualTo(salaryCategory.getId());
            assertThat(response.name()).isEqualTo("Salary");
            assertThat(response.type()).isEqualTo(TransactionType.INCOME);
            assertThat(response.icon()).isEqualTo("payments");
            assertThat(response.color()).isEqualTo("#4CAF50");
        }

        @Test
        @DisplayName("Should preserve display order in response")
        void preserveDisplayOrder() {
            // Given - Categories returned in display order
            Category thirdCategory = createCategory("Investments", TransactionType.INCOME, "trending_up", "#66BB6A", 3);
            List<Category> orderedCategories = List.of(salaryCategory, freelanceCategory, thirdCategory);

            when(categoryRepository.findAllIncomeCategories()).thenReturn(orderedCategories);
            when(categoryRepository.findAllExpenseCategories()).thenReturn(Collections.emptyList());

            // When
            CategoriesResponse result = categoryService.getAllCategories();

            // Then
            assertThat(result.income()).hasSize(3);
            assertThat(result.income().get(0).name()).isEqualTo("Salary");
            assertThat(result.income().get(1).name()).isEqualTo("Freelance");
            assertThat(result.income().get(2).name()).isEqualTo("Investments");
        }
    }
}
