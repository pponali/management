package com.scaler.price.core.management.service;

import com.scaler.price.core.management.domain.Price;

import java.util.Optional;

public interface BuyboxService {
    /**
     * Determines the winning seller price for a product based on buybox logic
     * @param productId Product to evaluate
     * @param siteId Site context
     * @return Winning price details or empty if no valid price exists
     */
    Optional<Price> getWinningPrice(Long productId, Long siteId);
} 