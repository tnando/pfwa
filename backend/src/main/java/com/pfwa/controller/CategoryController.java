package com.pfwa.controller;

import com.pfwa.dto.transaction.CategoriesResponse;
import com.pfwa.entity.TransactionType;
import com.pfwa.service.CategoryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.concurrent.TimeUnit;

/**
 * REST controller for transaction categories.
 */
@RestController
@RequestMapping("/categories")
public class CategoryController {

    private static final Logger logger = LoggerFactory.getLogger(CategoryController.class);

    private final CategoryService categoryService;

    public CategoryController(CategoryService categoryService) {
        this.categoryService = categoryService;
    }

    /**
     * Gets all transaction categories grouped by type.
     * Results can be cached by clients as categories rarely change.
     *
     * @param type optional filter by transaction type
     * @return categories grouped by income and expense
     */
    @GetMapping
    public ResponseEntity<CategoriesResponse> getCategories(
            @RequestParam(required = false) TransactionType type) {

        logger.debug("GET /api/v1/categories - type: {}", type);

        CategoriesResponse response = type != null ?
                categoryService.getCategoriesByType(type) :
                categoryService.getAllCategories();

        return ResponseEntity.ok()
                .cacheControl(CacheControl.maxAge(1, TimeUnit.HOURS))
                .body(response);
    }
}
