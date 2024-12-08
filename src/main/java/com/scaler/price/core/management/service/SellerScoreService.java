package com.scaler.price.core.management.service;

public interface SellerScoreService {
    /**
     * Get the normalized seller rating (0-1 scale)
     * @param sellerId The seller to evaluate
     * @return Normalized rating between 0 and 1
     */
    double getSellerRating(Long sellerId);
} 