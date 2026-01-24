package com.pfwa.repository;

import com.pfwa.entity.Category;
import com.pfwa.entity.TransactionType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for Category entity operations.
 * Provides methods for category lookup and filtering.
 */
@Repository
public interface CategoryRepository extends JpaRepository<Category, UUID> {

    /**
     * Finds all active categories ordered by display order.
     *
     * @return list of active categories sorted by display order
     */
    @Query("SELECT c FROM Category c WHERE c.active = true ORDER BY c.displayOrder")
    List<Category> findAllActiveOrderByDisplayOrder();

    /**
     * Finds all active categories of a specific type ordered by display order.
     *
     * @param type the transaction type (INCOME or EXPENSE)
     * @return list of active categories of the specified type
     */
    @Query("SELECT c FROM Category c WHERE c.type = :type AND c.active = true ORDER BY c.displayOrder")
    List<Category> findByTypeAndActiveOrderByDisplayOrder(@Param("type") TransactionType type);

    /**
     * Finds a category by name and type.
     *
     * @param name the category name
     * @param type the transaction type
     * @return Optional containing the category if found
     */
    Optional<Category> findByNameAndType(String name, TransactionType type);

    /**
     * Finds all income categories ordered by display order.
     *
     * @return list of active income categories
     */
    default List<Category> findAllIncomeCategories() {
        return findByTypeAndActiveOrderByDisplayOrder(TransactionType.INCOME);
    }

    /**
     * Finds all expense categories ordered by display order.
     *
     * @return list of active expense categories
     */
    default List<Category> findAllExpenseCategories() {
        return findByTypeAndActiveOrderByDisplayOrder(TransactionType.EXPENSE);
    }

    /**
     * Checks if a category exists and is active.
     *
     * @param id the category ID
     * @return true if category exists and is active
     */
    @Query("SELECT CASE WHEN COUNT(c) > 0 THEN true ELSE false END FROM Category c WHERE c.id = :id AND c.active = true")
    boolean existsByIdAndActive(@Param("id") UUID id);

    /**
     * Finds a category by ID only if it is active.
     *
     * @param id the category ID
     * @return Optional containing the active category if found
     */
    @Query("SELECT c FROM Category c WHERE c.id = :id AND c.active = true")
    Optional<Category> findByIdAndActive(@Param("id") UUID id);

    /**
     * Finds categories by a list of IDs.
     *
     * @param ids the list of category IDs
     * @return list of categories matching the IDs
     */
    @Query("SELECT c FROM Category c WHERE c.id IN :ids AND c.active = true")
    List<Category> findByIdsAndActive(@Param("ids") List<UUID> ids);
}
