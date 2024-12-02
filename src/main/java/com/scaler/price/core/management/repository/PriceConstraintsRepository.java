package com.scaler.price.core.management.repository;

import com.scaler.price.rule.domain.RuleType;
import com.scaler.price.rule.domain.constraint.PriceConstraints;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface PriceConstraintsRepository extends JpaRepository<PriceConstraints, Long> {

    // Basic Queries
    Optional<PriceConstraints> findByCategoryId(Long categoryId);
    List<PriceConstraints> findByCategoryIdIn(List<Long> categoryIds);
    Optional<PriceConstraints> findByCategoryIdAndIsActiveTrue(Long categoryId);
    List<PriceConstraints> findByIsActiveTrue();
    Page<PriceConstraints> findByProductId(Long productId, Pageable pageable);

    // Price Range Queries
    @Query("SELECT pc FROM PriceConstraints pc WHERE " +
            "pc.maxPriceIncreaseAmount >= :minPrice AND " +
            "pc.maxPriceDecreaseAmount <= :maxPrice")
    List<PriceConstraints> findByPriceRange(
            @Param("minPrice") BigDecimal minPrice,
            @Param("maxPrice") BigDecimal maxPrice);

    @Query("SELECT pc1 FROM PriceConstraints pc1, PriceConstraints pc2 " +
            "WHERE pc1.id <> pc2.id AND " +
            "pc1.maxPriceIncreaseAmount >= pc2.maxPriceDecreaseAmount AND " +
            "pc1.maxPriceDecreaseAmount <= pc2.maxPriceIncreaseAmount")
    List<PriceConstraints> findOverlappingPriceRanges();

    // Discount Related Queries
    @Query("SELECT pc FROM PriceConstraints pc WHERE " +
            "pc.minDiscountPercentage >= :minDiscount AND " +
            "pc.maxDiscountPercentage <= :maxDiscount")
    List<PriceConstraints> findByDiscountRange(
            @Param("minDiscount") Double minDiscount,
            @Param("maxDiscount") Double maxDiscount);

    // Complex Criteria Queries
    @Query("SELECT pc FROM PriceConstraints pc WHERE " +
            "(:categoryId IS NULL OR pc.categoryId = :categoryId) AND " +
            "(:minPrice IS NULL OR pc.maxPriceIncreaseAmount >= :minPrice) AND " +
            "(:maxPrice IS NULL OR pc.maxPriceDecreaseAmount <= :maxPrice) AND " +
            "(:minDiscount IS NULL OR pc.minDiscountPercentage >= :minDiscount) AND " +
            "(:maxDiscount IS NULL OR pc.maxDiscountPercentage <= :maxDiscount) AND " +
            "(:isActive IS NULL OR pc.isActive = :isActive)")
    Page<PriceConstraints> findByComplexCriteria(
            @Param("categoryId") Long categoryId,
            @Param("minPrice") BigDecimal minPrice,
            @Param("maxPrice") BigDecimal maxPrice,
            @Param("minDiscount") Double minDiscount,
            @Param("maxDiscount") Double maxDiscount,
            @Param("isActive") Boolean isActive,
            Pageable pageable);


    // Attribute Based Queries
    /*@Query("SELECT pc FROM PriceConstraints pc WHERE " +
            "pc.attributes LIKE %:attribute%")
    List<PriceConstraints> findByAttribute(@Param("attribute") String attribute);*/

    // Audit and Review Queries
    List<PriceConstraints> findByUpdatedAtAfter(Instant modifiedDate);
    List<PriceConstraints> findByUpdatedBy(String userId);

    @Query("SELECT pc FROM PriceConstraints pc WHERE " +
            "pc.updatedAt >= :reviewDate OR " +
            "pc.isActive = false")
    List<PriceConstraints> findConstraintsNeedingReview(@Param("reviewDate") Instant reviewDate);

    // Maintenance Queries
    @Modifying
    @Query("UPDATE PriceConstraints pc SET pc.isActive = :status, " +
            "pc.updatedBy = :userId, pc.updatedAt = CURRENT_TIMESTAMP " +
            "WHERE pc.categoryId = :categoryId")
    int updateConstraintStatus(
            @Param("categoryId") Long categoryId,
            @Param("status") boolean status,
            @Param("userId") String userId);

    @Modifying
    @Query("DELETE FROM PriceConstraints pc WHERE " +
            "pc.isActive = false AND pc.updatedAt < :date")
    int deleteInactiveConstraintsOlderThan(@Param("date") Instant date);

    // Remove or comment out the existing findByAttribute method
    // List<PriceConstraints> findByAttribute(@Param("attribute") String attribute);

    /**
     * Find price constraints by category ID and rule type
     * @param categoryId the category ID to search for
     * @param ruleType the rule type to filter by
     * @return List of matching price constraints
     */
    @Query("SELECT pc FROM PriceConstraints pc WHERE pc.categoryId = :categoryId AND pc.ruleType = :ruleType")
    List<PriceConstraints> findByCategoryIdAndRuleType(
            @Param("categoryId") Long categoryId,
            @Param("ruleType") RuleType ruleType);
}