package com.scaler.price.core.management.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.scaler.price.core.management.domain.SellerMetrics;

@Repository
public interface SellerMetricsRepository extends JpaRepository<SellerMetrics, Long> {
    Optional<SellerMetrics> findBySellerId(Long sellerId);
} 