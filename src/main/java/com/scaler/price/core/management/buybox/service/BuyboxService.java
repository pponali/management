package com.scaler.price.core.management.buybox.service;

import com.scaler.price.core.management.domain.Price;

import java.util.List;
import java.util.Optional;

/**
 * Service interface for buybox calculations and winner determination
 */
public interface BuyboxService {
    /**
     * Determines the winning seller price for a product based on buybox logic
     *
     * @param productId Product to evaluate
     * @param siteId    Site context
     * @return Winning price details or empty if no valid price exists
     */
    Optional<Price> getWinningPrice(Long productId, Long siteId);


    /**
     * Gets all eligible prices for buybox calculation for a product
     *
     * @param productId Product ID to get prices for
     * @param siteId    Site ID to filter prices
     * @return List of eligible prices
     */
    List<Price> getEligiblePrices(Long productId, Long siteId);
}

