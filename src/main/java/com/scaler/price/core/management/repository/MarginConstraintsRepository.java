package com.scaler.price.core.management.repository;


import com.scaler.price.rule.domain.constraint.MarginConstraints;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface MarginConstraintsRepository extends JpaRepository<MarginConstraints, Long> {

    /**
     * Find margin constraints by category ID
     */
    Optional<MarginConstraints> findByCategoryId(String categoryId);

    /**
     * Find margin constraints by multiple category IDs
     */
    List<MarginConstraints> findByCategoryIdIn(List<String> categoryIds);

    /**
     * Find margin constraints by minimum margin percentage range
     */
    List<MarginConstraints> findByMinMarginPercentageBetween(Double minMargin, Double maxMargin);

    /**
     * Find margin constraints by maximum margin percentage range
     */
    List<MarginConstraints> findByMaxMarginPercentageBetween(Double minMargin, Double maxMargin);

    /**
     * Find margin constraints by target margin percentage range
     */
    List<MarginConstraints> findByTargetMarginPercentageBetween(Double minTarget, Double maxTarget);

    /**
     * Find active margin constraints
     */
    List<MarginConstraints> findByIsActiveTrue();

    /**
     * Find margin constraints by category and active status
     */
    Optional<MarginConstraints> findByCategoryIdAndIsActiveTrue(Long categoryId);

    /**
     * Find margin constraints modified after specified date
     */
    List<MarginConstraints> findByLastModifiedDateAfter(Instant modifiedDate);

    /**
     * Find margin constraints by modified user
     */
    List<MarginConstraints> findByLastModifiedBy(String userId);

    /**
     * Find paginated margin constraints
     */
    Page<MarginConstraints> findAll(Pageable pageable);

    /**
     * Find constraints with margin violations
     */
    @Query("SELECT mc FROM MarginConstraints mc WHERE " +
            "mc.currentMarginPercentage < mc.minMarginPercentage OR " +
            "mc.currentMarginPercentage > mc.maxMarginPercentage")
    List<MarginConstraints> findMarginViolations();

    /**
     * Find constraints below target margin
     */
    @Query("SELECT mc FROM MarginConstraints mc WHERE " +
            "mc.currentMarginPercentage < mc.targetMarginPercentage")
    List<MarginConstraints> findBelowTargetMargin();

    /**
     * Find constraints by margin efficiency
     */
    @Query("SELECT mc FROM MarginConstraints mc WHERE " +
            "(mc.currentMarginPercentage - mc.targetMarginPercentage) / " +
            "mc.targetMarginPercentage * 100 >= :efficiency")
    List<MarginConstraints> findByMarginEfficiency(@Param("efficiency") Double efficiency);

    /**
     * Find constraints with specific margin ranges
     */
    @Query("SELECT mc FROM MarginConstraints mc WHERE " +
            "mc.minMarginPercentage >= :minMargin AND " +
            "mc.maxMarginPercentage <= :maxMargin")
    List<MarginConstraints> findByMarginRange(
            @Param("minMargin") Double minMargin,
            @Param("maxMargin") Double maxMargin);

    /**
     * Find constraints by complex criteria
     */
    @Query("SELECT mc FROM MarginConstraints mc WHERE " +
            "(:categoryId IS NULL OR mc.categoryId = :categoryId) AND " +
            "(:minMargin IS NULL OR mc.minMarginPercentage >= :minMargin) AND " +
            "(:maxMargin IS NULL OR mc.maxMarginPercentage <= :maxMargin) AND " +
            "(:targetMargin IS NULL OR mc.targetMarginPercentage = :targetMargin) AND " +
            "(:isActive IS NULL OR mc.isActive = :isActive)")
    Page<MarginConstraints> findByComplexCriteria(
            @Param("categoryId") Long categoryId,
            @Param("minMargin") Double minMargin,
            @Param("maxMargin") Double maxMargin,
            @Param("targetMargin") Double targetMargin,
            @Param("isActive") Boolean isActive,
            Pageable pageable);

    /**
     * Update margin status by category ID
     */
    @Query("UPDATE MarginConstraints mc SET mc.isActive = :status, " +
            "mc.lastModifiedDate = CURRENT_TIMESTAMP, " +
            "mc.lastModifiedBy = :userId " +
            "WHERE mc.categoryId = :categoryId")
    int updateMarginStatus(
            @Param("categoryId") Long categoryId,
            @Param("status") boolean status,
            @Param("userId") String userId);

    /**
     * Find constraints needing review
     */
    @Query("SELECT mc FROM MarginConstraints mc WHERE " +
            "mc.lastModifiedDate > :reviewDate AND " +
            "mc.reviewStatus = 'PENDING'")
    List<MarginConstraints> findConstraintsNeedingReview(
            @Param("reviewDate") Instant reviewDate);

    /**
     * Find constraints by validation status
     */
    List<MarginConstraints> findByValidationStatus(String validationStatus);

    /**
     * Delete constraints by category ID
     */
    void deleteByCategoryId(Long categoryId);

    /**
     * Delete inactive constraints older than specified date
     */
    @Query("DELETE FROM MarginConstraints mc WHERE " +
            "mc.isActive = false AND mc.lastModifiedDate < :date")
    void deleteInactiveConstraintsOlderThan(@Param("date") Instant date);

    /**
     * Find constraints with margin alerts
     */
    @Query("SELECT mc FROM MarginConstraints mc WHERE " +
            "mc.currentMarginPercentage < mc.minMarginPercentage * :alertThreshold OR " +
            "mc.currentMarginPercentage > mc.maxMarginPercentage * :alertThreshold")
    List<MarginConstraints> findMarginAlerts(@Param("alertThreshold") Double alertThreshold);

    /**
     * Find constraints by category type
     */
    @Query("SELECT mc FROM MarginConstraints mc WHERE " +
            "mc.categoryType = :categoryType")
    List<MarginConstraints> findByCategoryType(@Param("categoryType") String categoryType);

    /**
     * Find constraints with margin trends
     */
    @Query("SELECT mc FROM MarginConstraints mc WHERE " +
            "mc.marginTrend = :trend AND mc.isActive = true")
    List<MarginConstraints> findByMarginTrend(@Param("trend") String trend);

    /**
     * Calculate average margin by category type
     */
    @Query("SELECT AVG(mc.currentMarginPercentage) FROM MarginConstraints mc " +
            "WHERE mc.categoryType = :categoryType AND mc.isActive = true")
    Double calculateAverageMarginByCategory(@Param("categoryType") String categoryType);

    /**
     * Find constraints requiring optimization
     */
    @Query("SELECT mc FROM MarginConstraints mc WHERE " +
            "ABS(mc.currentMarginPercentage - mc.targetMarginPercentage) > :threshold " +
            "AND mc.isActive = true")
    List<MarginConstraints> findConstraintsNeedingOptimization(
            @Param("threshold") Double threshold);
}