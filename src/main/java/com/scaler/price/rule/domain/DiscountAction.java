package com.scaler.price.rule.domain;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@DiscriminatorValue("DISCOUNT")
@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper=false)
public class DiscountAction extends RuleAction {
    
    @Column(precision = 10, scale = 2)
    private BigDecimal discountValue;


    
    @Column(length = 20)
    private String discountType; // PERCENTAGE or FIXED
    
    @Column
    private boolean stackable;
    
    @Column(length = 50)
    private String stackInterval;
    
    @Column
    private int maxStackCount;
    
    @Column
    private boolean seasonalRestrictions;
    
    @Column(length = 50)
    private String seasonType;
    
    @Column
    private BigDecimal minimumPurchaseAmount;
    
    @Column
    private BigDecimal maximumDiscountAmount;

    public DiscountAction(BigDecimal discountValue, String discountType) {
        this.discountValue = discountValue;
        this.discountType = discountType;
        this.stackable = false;
        this.maxStackCount = 1;
        this.seasonalRestrictions = false;
    }

    @PrePersist
    @PreUpdate
    private void validateDiscount() {
        if (discountValue == null || discountValue.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Discount value must be greater than zero");
        }
        
        if (discountType == null || (!discountType.equals("PERCENTAGE") && !discountType.equals("FIXED"))) {
            throw new IllegalArgumentException("Discount type must be either PERCENTAGE or FIXED");
        }
        
        if (discountType.equals("PERCENTAGE") && discountValue.compareTo(new BigDecimal("100")) > 0) {
            throw new IllegalArgumentException("Percentage discount cannot exceed 100%");
        }
        
        if (stackable) {
            if (maxStackCount <= 0) {
                throw new IllegalArgumentException("Maximum stack count must be greater than zero");
            }
            if (stackInterval == null || stackInterval.trim().isEmpty()) {
                throw new IllegalArgumentException("Stack interval is required for stackable discounts");
            }
        }
        
        if (minimumPurchaseAmount != null && minimumPurchaseAmount.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Minimum purchase amount cannot be negative");
        }
        
        if (maximumDiscountAmount != null && maximumDiscountAmount.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Maximum discount amount cannot be negative");
        }
    }

    public BigDecimal calculateDiscount(BigDecimal basePrice) {
        if (basePrice == null || basePrice.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Base price must be greater than zero");
        }
        
        if (minimumPurchaseAmount != null && basePrice.compareTo(minimumPurchaseAmount) < 0) {
            return BigDecimal.ZERO;
        }
        
        BigDecimal discountAmount;
        if ("PERCENTAGE".equals(discountType)) {
            discountAmount = basePrice.multiply(discountValue)
                    .divide(new BigDecimal("100"), 2, BigDecimal.ROUND_HALF_UP);
        } else {
            discountAmount = discountValue;
        }
        
        if (maximumDiscountAmount != null && discountAmount.compareTo(maximumDiscountAmount) > 0) {
            discountAmount = maximumDiscountAmount;
        }
        
        return discountAmount;
    }

    public BigDecimal calculateStackedDiscount(BigDecimal basePrice, int stackCount) {
        if (!stackable) {
            throw new IllegalStateException("Discount is not stackable");
        }
        
        if (stackCount <= 0 || stackCount > maxStackCount) {
            throw new IllegalArgumentException("Invalid stack count: " + stackCount);
        }
        
        BigDecimal totalDiscount = BigDecimal.ZERO;
        for (int i = 0; i < stackCount; i++) {
            totalDiscount = totalDiscount.add(calculateDiscount(basePrice));
        }
        
        return totalDiscount;
    }



    @Column
    public Instant endDate;

    @Column
    public int duration;

    @Column
    public BigDecimal discountPercentage;

    @Column
    public BigDecimal currentPrice;
    
    @Column
    public BigDecimal costPrice;
}
