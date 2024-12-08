package com.scaler.price.core.management.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.scaler.price.core.management.domain.Price;

@Repository
public interface PriceRepository extends JpaRepository<Price, Long> {
    Optional<Price> findByProductIdAndEffectiveFromLessThanEqualAndEffectiveToGreaterThan(
            Long productId, LocalDateTime effectiveFrom, LocalDateTime effectiveTo);

    List<Price> findByProductId(Long productId);

    @Query("""
        SELECT p FROM Price p 
        WHERE p.productId = :productId 
        AND p.sellerId = :sellerId 
        AND p.siteId = :siteId 
        AND p.effectiveFrom <= :date 
        AND (p.effectiveTo IS NULL OR p.effectiveTo >= :date)
        AND p.isActive = true
        ORDER BY p.effectiveFrom DESC
        """)
    Optional<Price> findActivePrice(
            Long productId,
            Long sellerId,
            Long siteId,
            LocalDateTime date
    );

    List<Price> findBySellerIdAndSiteId(Long sellerId, Long siteId);

    @Query("""
        SELECT p FROM Price p 
        WHERE p.sellerId = :sellerId 
        AND p.siteId = :siteId 
        AND p.effectiveFrom >= :startDate 
        AND p.effectiveFrom <= :endDate
        """)
    List<Price> findUpcomingPriceChanges(
            Long sellerId,
            Long siteId,
            LocalDateTime startDate,
            LocalDateTime endDate
    );

    @Query("""
        SELECT p FROM Price p 
        WHERE p.productId = :productId 
        AND p.siteId = :siteId 
        AND p.isActive = true 
        AND p.effectiveFrom <= :currentTime 
        AND (p.effectiveTo IS NULL OR p.effectiveTo >= :currentTime)
        """)
    List<Price> findActiveValidPrices(
        @Param("productId") Long productId,
        @Param("siteId") Long siteId,
        @Param("currentTime") LocalDateTime currentTime
    );
}