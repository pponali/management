package com.scaler.price.core.management.repository;

import com.scaler.price.core.management.domain.Price;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface PriceRepository extends JpaRepository<Price, Long> {
    Optional<Price> findByProductIdAndEffectiveFromLessThanEqualAndEffectiveToGreaterThan(
            String productId, LocalDateTime effectiveFrom, LocalDateTime effectiveTo);

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

    List<Price> findBySellerIdAndSiteId(String sellerId, String siteId);

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
}