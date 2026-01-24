package com.pfwa.service;

import com.pfwa.dto.transaction.CategoriesResponse;
import com.pfwa.dto.transaction.CategoryResponse;
import com.pfwa.entity.Category;
import com.pfwa.entity.TransactionType;
import com.pfwa.exception.CategoryNotFoundException;
import com.pfwa.repository.CategoryRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Service for managing transaction categories.
 */
@Service
@Transactional(readOnly = true)
public class CategoryService {

    private static final Logger logger = LoggerFactory.getLogger(CategoryService.class);

    private final CategoryRepository categoryRepository;

    public CategoryService(CategoryRepository categoryRepository) {
        this.categoryRepository = categoryRepository;
    }

    /**
     * Gets all categories grouped by type.
     *
     * @return categories response with income and expense categories
     */
    public CategoriesResponse getAllCategories() {
        logger.debug("Fetching all categories");

        List<CategoryResponse> incomeCategories = categoryRepository.findAllIncomeCategories()
                .stream()
                .map(CategoryResponse::from)
                .toList();

        List<CategoryResponse> expenseCategories = categoryRepository.findAllExpenseCategories()
                .stream()
                .map(CategoryResponse::from)
                .toList();

        return CategoriesResponse.of(incomeCategories, expenseCategories);
    }

    /**
     * Gets categories filtered by type.
     *
     * @param type the transaction type (optional, if null returns all)
     * @return categories response
     */
    public CategoriesResponse getCategoriesByType(TransactionType type) {
        logger.debug("Fetching categories by type: {}", type);

        if (type == null) {
            return getAllCategories();
        }

        List<CategoryResponse> categories = categoryRepository
                .findByTypeAndActiveOrderByDisplayOrder(type)
                .stream()
                .map(CategoryResponse::from)
                .toList();

        if (type == TransactionType.INCOME) {
            return CategoriesResponse.of(categories, List.of());
        } else {
            return CategoriesResponse.of(List.of(), categories);
        }
    }

    /**
     * Gets a category by ID.
     *
     * @param categoryId the category ID
     * @return the category entity
     * @throws CategoryNotFoundException if category not found or inactive
     */
    public Category getCategoryById(UUID categoryId) {
        return categoryRepository.findByIdAndActive(categoryId)
                .orElseThrow(() -> new CategoryNotFoundException(categoryId));
    }

    /**
     * Validates that a category exists, is active, and matches the expected type.
     *
     * @param categoryId   the category ID
     * @param expectedType the expected transaction type
     * @return the category entity
     * @throws CategoryNotFoundException if category not found or inactive
     */
    public Category validateCategoryForType(UUID categoryId, TransactionType expectedType) {
        Category category = getCategoryById(categoryId);

        if (category.getType() != expectedType) {
            logger.debug("Category {} has type {} but expected {}",
                    categoryId, category.getType(), expectedType);
        }

        return category;
    }
}
