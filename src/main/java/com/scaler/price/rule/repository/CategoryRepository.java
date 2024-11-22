package com.scaler.price.rule.repository;

import com.scaler.price.rule.domain.CategorySummary;
import com.scaler.price.rule.domain.constraint.CategoryConstraints;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CategoryRepository extends JpaRepository<CategoryConstraints, String> {

    Optional<CategoryConstraints> findByCategoryId(String categoryId);

    List<CategoryConstraints> findByParentCategoryId(String parentCategoryId);

    List<CategoryConstraints> findBySiteIdsContaining(String siteId);

    @Query("""
        SELECT c FROM Category c
        WHERE c.isActive = true
        AND :siteId MEMBER OF c.siteIds
        ORDER BY c.level, c.displayOrder
        """)
    List<CategoryConstraints> findActiveCategoriesBySite(
            @Param("siteId") String siteId
    );

    @Query(countQuery = """
            WITH RECURSIVE CategoryHierarchy AS (
                SELECT c.category_id, c.parent_category_id, 1 as level
                FROM categories c
                WHERE c.category_id = :categoryId
            
                UNION ALL
            
                SELECT c.category_id, c.parent_category_id, ch.level + 1
                FROM categories c
                INNER JOIN CategoryHierarchy ch 
                ON c.category_id = ch.parent_category_id
            )
            SELECT c.* FROM categories c
            INNER JOIN CategoryHierarchy ch 
            ON c.category_id = ch.category_id
            ORDER BY ch.level DESC
            """, nativeQuery = true)
    List<CategoryConstraints> findParentCategories(@Param("categoryId") String categoryId);

    @Query(countQuery = """
            WITH RECURSIVE CategoryHierarchy AS (
                SELECT c.category_id, c.parent_category_id, 1 as level
                FROM categories c
                WHERE c.parent_category_id = :categoryId
            
                UNION ALL
            
                SELECT c.category_id, c.parent_category_id, ch.level + 1
                FROM categories c
                INNER JOIN CategoryHierarchy ch 
                ON c.parent_category_id = ch.category_id
            )
            SELECT c.* FROM categories c
            INNER JOIN CategoryHierarchy ch 
            ON c.category_id = ch.category_id
            ORDER BY ch.level, c.display_order
            """, nativeQuery = true)
    List<CategoryConstraints> findAllSubCategories(@Param("categoryId") String categoryId);

    @Query("""
        SELECT DISTINCT c FROM Category c
        LEFT JOIN FETCH c.subCategories
        WHERE c.isActive = true
        AND c.parentCategory IS NULL
        AND :siteId MEMBER OF c.siteIds
        ORDER BY c.displayOrder
        """)
    List<CategoryConstraints> findRootCategories(@Param("siteId") String siteId);

    @Query("""
        SELECT c FROM Category c
        WHERE c.isActive = true
        AND :siteId MEMBER OF c.siteIds
        AND LOWER(c.categoryName) LIKE LOWER(CONCAT('%', :searchTerm, '%'))
        ORDER BY c.level, c.displayOrder
        """)
    List<CategoryConstraints> searchCategories(
            @Param("siteId") String siteId,
            @Param("searchTerm") String searchTerm
    );

    @Query("""
        SELECT new com.scaler.price.category.dto.CategorySummary(
            c.categoryId,
            c.categoryName,
            COUNT(pc),
            CASE WHEN c.isActive = true THEN 'ACTIVE' ELSE 'INACTIVE' END
        )
        FROM Category c
        LEFT JOIN Category pc ON pc.parentCategory = c
        WHERE :siteId MEMBER OF c.siteIds
        GROUP BY c.categoryId, c.categoryName, c.isActive
        """)
    List<CategorySummary> getCategorySummaries(@Param("siteId") String siteId);

    // Custom query to find categories with price attributes
    @Query("""
        SELECT c FROM Category c
        WHERE c.isActive = true
        AND :siteId MEMBER OF c.siteIds
        AND c.attributes.priceAttributes IS NOT NULL
        AND c.attributes.priceAttributes != '{}'
        """)
    List<CategoryConstraints> findCategoriesWithPriceAttributes(
            @Param("siteId") String siteId
    );

    // Custom query for validation rules
    @Query("""
        SELECT c FROM Category c
        WHERE c.isActive = true
        AND :siteId MEMBER OF c.siteIds
        AND c.attributes.validationRules IS NOT NULL
        AND c.attributes.validationRules != '{}'
        """)
    List<CategoryConstraints> findCategoriesWithValidationRules(
            @Param("siteId") String siteId
    );
}