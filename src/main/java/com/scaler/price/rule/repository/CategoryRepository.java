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
public interface CategoryRepository extends JpaRepository<CategoryConstraints, Long> {

    Optional<CategoryConstraints> findByCategoryId(Long categoryId);

    List<CategoryConstraints> findByParentCategoryId(Long parentCategoryId);

    List<CategoryConstraints> findBySiteIdsContaining(Long siteId);

    @Query("""
        SELECT c FROM CategoryConstraints c
        WHERE c.isActive = true
        AND :siteId MEMBER OF c.siteIds
        ORDER BY c.level, c.displayOrder
        """)
    List<CategoryConstraints> findActiveCategoriesBySite(
            @Param("siteId") Long siteId
    );


    @Query("""
        SELECT c FROM CategoryConstraints c
        WHERE c.isActive = true
        AND :siteId MEMBER OF c.siteIds
        AND LOWER(c.categoryName) LIKE LOWER(CONCAT('%', :searchTerm, '%'))
        ORDER BY c.level, c.displayOrder
        """)
    List<CategoryConstraints> searchCategories(
            @Param("siteId") Long siteId,
            @Param("searchTerm") String searchTerm
    );

    @Query("""
        SELECT new com.scaler.price.rule.domain.CategorySummary(
            c.categoryId,
            c.categoryName,
            SIZE(c.subCategories),
            CASE WHEN c.isActive = true THEN 'ACTIVE' ELSE 'INACTIVE' END
        )
        FROM CategoryConstraints c
        WHERE :siteId MEMBER OF c.siteIds
        ORDER BY c.displayOrder
        """)
    List<CategorySummary> getCategorySummaries(@Param("siteId") Long siteId);


    // Custom query to find categories with price attributes
    @Query(value = """
        SELECT c.* FROM category_constraints c
        CROSS JOIN LATERAL jsonb_object_keys(c.attributes->'priceAttributes') pa
        WHERE c.is_active = true
        AND c.site_ids @> CAST(:siteId AS jsonb)
        GROUP BY c.category_id
        """, nativeQuery = true)
    List<CategoryConstraints> findCategoriesWithPriceAttributes(
            @Param("siteId") Long siteId
    );

    // Custom query for validation rules by site
    @Query(value = """
        SELECT c.* FROM category_constraints c
        CROSS JOIN LATERAL jsonb_object_keys(c.attributes->'validationRules') vr
        WHERE c.is_active = true
        AND c.site_ids @> CAST(:siteId AS jsonb)
        GROUP BY c.category_id
        """, nativeQuery = true)
    List<CategoryConstraints> findCategoriesWithValidationRulesBySite(
            @Param("siteId") Long siteId
    );

    // Custom query for validation rules by type
    @Query(value = """
        SELECT c.* FROM category_constraints c,
        jsonb_each(c.attributes->'validationRules') vr
        WHERE c.is_active = true
        AND vr.value->>'ruleType' = :ruleType
        GROUP BY c.category_id
        """, nativeQuery = true)
    List<CategoryConstraints> findCategoriesWithValidationRulesByType(
            @Param("ruleType") String ruleType
    );


    @Query(value = """
            WITH RECURSIVE CategoryHierarchy AS (
                SELECT c.category_id, c.parent_category_id, 1 as level
                FROM category_constraints c
                WHERE c.category_id = :categoryId
            
                UNION ALL
            
                SELECT c.category_id, c.parent_category_id, ch.level + 1
                FROM category_constraints c
                INNER JOIN CategoryHierarchy ch 
                ON c.category_id = ch.parent_category_id
            )
            SELECT c.* FROM category_constraints c
            INNER JOIN CategoryHierarchy ch 
            ON c.category_id = ch.category_id
            ORDER BY ch.level DESC
            """,
            nativeQuery = true)
    List<CategoryConstraints> findParentCategories(@Param("categoryId") Long categoryId);

    @Query(value = """
            WITH RECURSIVE CategoryHierarchy AS (
                SELECT c.category_id, c.parent_category_id, 1 as level
                FROM category_constraints c
                WHERE c.parent_category_id = :categoryId
            
                UNION ALL
            
                SELECT c.category_id, c.parent_category_id, ch.level + 1
                FROM category_constraints c
                INNER JOIN CategoryHierarchy ch 
                ON c.parent_category_id = ch.category_id
            )
            SELECT c.* FROM category_constraints c
            INNER JOIN CategoryHierarchy ch 
            ON c.category_id = ch.category_id
            ORDER BY ch.level, c.display_order
            """,
            nativeQuery = true)
    List<CategoryConstraints> findAllSubCategories(@Param("categoryId") Long categoryId);

    @Query(value = """
            SELECT c.* FROM category_constraints c
            WHERE c.is_active = true
            AND c.parent_category_id IS NULL
            AND c.site_ids @> CAST(:siteId AS jsonb)
            ORDER BY c.display_order
            """,
            nativeQuery = true)
    List<CategoryConstraints> findRootCategories(@Param("siteId") Long siteId);
}