package com.scaler.price.rule.domain;


import lombok.Data;

import java.math.BigDecimal;

@Data
public class SellerLimits {
    // Define fields for SellerLimits
    private int limitValue; // Example field

    // Constructor
    public SellerLimits(int limitValue) {
        this.limitValue = limitValue;
    }

    // Getter
    public int getLimitValue() {
        return limitValue;
    }

    // Static method to get default limits
    public static SellerLimits getDefaultLimits() {
        return new SellerLimits(100); // Example default value
    }

    public long getMaxRules() {
        return 0;
    }


    public BigDecimal maxDiscount;
}

