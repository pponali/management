package com.scaler.price.core.management.repository;

import com.scaler.price.rule.domain.constraint.PriceConstraints;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface PriceConstraintsRepository extends JpaRepository<PriceConstraints, Long> {

    /**
     * Find price constraints by category ID
     */
    Optional<PriceConstraints> findByCategoryId(String categoryId);

    /**
     * Find price constraints by multiple category IDs
     */
    List<PriceConstraints> findByCategoryIdIn(List<String> categoryIds);

    /**
     * Find price constraints with minimum price greater than specified value
     */
    List<PriceConstraints> findByMinPriceGreaterThan(BigDecimal minPrice);

    /**
     * Find price constraints with maximum price less than specified value
     */
    List<PriceConstraints> findByMaxPriceLessThan(BigDecimal maxPrice);

    /**
     * Find price constraints by discount range
     */
    List<PriceConstraints> findByMinDiscountPercentageBetweenAndMaxDiscountPercentageBetween(
            Double minDiscountLower, Double minDiscountUpper,
            Double maxDiscountLower, Double maxDiscountUpper);

    /**
     * Find active price constraints
     */
    List<PriceConstraints> findByIsActiveTrue();

    /**
     * Find price constraints by category and active status
     */
    Optional<PriceConstraints> findByCategoryIdAndIsActiveTrue(Long categoryId);

    /**
     * Find price constraints modified after specified date
     */
    List<PriceConstraints> findByLastModifiedDateAfter(Instant modifiedDate);

    /**
     * Find price constraints by modified user
     */
    List<PriceConstraints> findByLastModifiedBy(String userId);

    /**
     * Find paginated price constraints
     */
    Page<PriceConstraints> findAll(Pageable pageable);

    /**
     * Find price constraints with specific discount settings
     */
    @Query("SELECT pc FROM PriceConstraints pc WHERE " +
            "pc.minDiscountPercentage >= :minDiscount AND " +
            "pc.maxDiscountPercentage <= :maxDiscount")
    List<PriceConstraints> findByDiscountRange(
            @Param("minDiscount") Double minDiscount,
            @Param("maxDiscount") Double maxDiscount);

    /**
     * Find price constraints by price range
     */
    @Query("SELECT pc FROM PriceConstraints pc WHERE " +
            "pc.minPrice >= :minPrice AND pc.maxPrice <= :maxPrice")
    List<PriceConstraints> findByPriceRange(
            @Param("minPrice") BigDecimal minPrice,
            @Param("maxPrice") BigDecimal maxPrice);

    /**
     * Count constraints by category type
     */
    @Query("SELECT COUNT(pc) FROM PriceConstraints pc " +
            "WHERE pc.categoryType = :categoryType")
    long countByCategoryType(@Param("categoryType") String categoryType);

    /**
     * Find constraints with overlapping price ranges
     */
    @Query("SELECT pc1 FROM PriceConstraints pc1, PriceConstraints pc2 " +
            "WHERE pc1.id <> pc2.id AND " +
            "pc1.minPrice <= pc2.maxPrice AND pc1.maxPrice >= pc2.minPrice")
    List<PriceConstraints> findOverlappingPriceRanges();

    /**
     * Find constraints by complex criteria
     */
    @Query("SELECT pc FROM PriceConstraints pc WHERE " +
            "(:categoryId IS NULL OR pc.categoryId = :categoryId) AND " +
            "(:minPrice IS NULL OR pc.minPrice >= :minPrice) AND " +
            "(:maxPrice IS NULL OR pc.maxPrice <= :maxPrice) AND " +
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

    /**
     * Delete constraints by category ID
     */
    void deleteByCategoryId(Long categoryId);

    /**
     * Delete inactive constraints older than specified date
     */
    @Query("DELETE FROM PriceConstraints pc WHERE " +
            "pc.isActive = false AND pc.lastModifiedDate < :date")
    void deleteInactiveConstraintsOlderThan(@Param("date") Instant date);

    /**
     * Update constraints status by category ID
     */
    @Query("UPDATE PriceConstraints pc SET pc.isActive = :status, " +
            "pc.lastModifiedDate = CURRENT_TIMESTAMP, " +
            "pc.lastModifiedBy = :userId " +
            "WHERE pc.categoryId = :categoryId")
    int updateConstraintStatus(
            @Param("categoryId") Long categoryId,
            @Param("status") boolean status,
            @Param("userId") String userId);

    /**
     * Find constraints needing review (modified recently)
     */
    @Query("SELECT pc FROM PriceConstraints pc WHERE " +
            "pc.lastModifiedDate > :reviewDate AND " +
            "pc.reviewStatus = 'PENDING'")
    List<PriceConstraints> findConstraintsNeedingReview(
            @Param("reviewDate") Instant reviewDate);

    /**
     * Find constraints by validation status
     */
    List<PriceConstraints> findByValidationStatus(String validationStatus);

    /**
     * Find constraints with specific attributes
     */
    @Query("SELECT pc FROM PriceConstraints pc WHERE " +
            "pc.attributes LIKE %:attribute%")
    List<PriceConstraints> findByAttribute(@Param("attribute") String attribute);
}