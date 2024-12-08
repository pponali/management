package com.scaler.price.core.management.service.impl;

import com.scaler.price.core.management.domain.SellerMetrics;
import com.scaler.price.core.management.repository.SellerMetricsRepository;
import com.scaler.price.core.management.service.SellerScoreService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

@Service
@Slf4j
@RequiredArgsConstructor
public class SellerScoreServiceImpl implements SellerScoreService {
    private final SellerMetricsRepository sellerMetricsRepository;
    
    @Override
    @Cacheable(value = "sellerScores", key = "#sellerId")
    public double getSellerRating(Long sellerId) {
        log.debug("Calculating seller score for seller: {}", sellerId);
        
        SellerMetrics metrics = sellerMetricsRepository.findBySellerId(sellerId)
                .orElse(getDefaultMetrics());
                
        return calculateNormalizedScore(metrics);
    }
    
    private double calculateNormalizedScore(SellerMetrics metrics) {
        double score = 0.0;
        
        // Customer Rating (40%)
        score += normalizeRating(metrics.getCustomerRating()) * 0.4;
        
        // Fulfillment Rate (30%)
        score += metrics.getFulfillmentRate() * 0.3;
        
        // Return Rate (20% - inverse as lower is better)
        score += (1 - metrics.getReturnRate()) * 0.2;
        
        // Account Age (10%)
        score += calculateAccountAgeScore(metrics.getAccountCreationDate()) * 0.1;
        
        log.debug("Calculated seller score: {}", score);
        return score;
    }
    
    private double normalizeRating(BigDecimal rating) {
        // Normalize 1-5 rating to 0-1 scale
        return rating.subtract(BigDecimal.ONE)
                .divide(BigDecimal.valueOf(4), 2, RoundingMode.HALF_UP)
                .doubleValue();
    }
    
    private double calculateAccountAgeScore(LocalDateTime creationDate) {
        if (creationDate == null) {
            return 0.0;
        }
        
        long monthsActive = ChronoUnit.MONTHS.between(creationDate, LocalDateTime.now());
        // Max score for accounts 24+ months old
        return Math.min(monthsActive / 24.0, 1.0);
    }
    
    private SellerMetrics getDefaultMetrics() {
        return SellerMetrics.builder()
                .customerRating(BigDecimal.valueOf(3.0))  // Average rating
                .fulfillmentRate(0.7)                     // 70% fulfillment
                .returnRate(0.1)                          // 10% returns
                .accountCreationDate(LocalDateTime.now()) // New account
                .build();
    }
} 