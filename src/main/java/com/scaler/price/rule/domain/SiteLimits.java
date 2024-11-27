package com.scaler.price.rule.domain;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class SiteLimits {
    private int maxPrice;
    private int minPrice;

    // Constructor
    public SiteLimits(int maxPrice, int minPrice) {
        this.maxPrice = maxPrice;
        this.minPrice = minPrice;
    }

    // Getters
    public int getMaxPrice() {
        return maxPrice;
    }

    public int getMinPrice() {
        return minPrice;
    }

    // Static method to return default limits
    public static SiteLimits getDefaultLimits() {
        return new SiteLimits(1000, 100); // Example default values
    }

    public long maxRules;

    public BigDecimal maxDiscount;
}
