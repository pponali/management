package com.scaler.price.rule.service;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;

@Data
@Builder
public class BundleEligibility {
    private boolean eligible;
    private BigDecimal originalPrice;
    private BigDecimal discountAmount;
    private BigDecimal marginPercentage;
    private Long bundleId;
    private Instant validUntil;
    private String ineligibilityReason;
    private int itemCount;
    private BigDecimal minimumPurchaseAmount;
    private BigDecimal maximumDiscountAmount;

    public boolean isEligible() {
        return eligible;
    }

    public BigDecimal getOriginalPrice() {
        return originalPrice != null ? originalPrice : BigDecimal.ZERO;
    }

    public BigDecimal getDiscountAmount() {
        return discountAmount != null ? discountAmount : BigDecimal.ZERO;
    }

    public BigDecimal getFinalPrice() {
        return getOriginalPrice().subtract(getDiscountAmount())
                .setScale(2, RoundingMode.HALF_UP);
    }

    public BigDecimal getMarginPercentage() {
        return marginPercentage != null ? marginPercentage : BigDecimal.ZERO;
    }

    public BigDecimal getEffectiveDiscount() {
        if (getOriginalPrice().compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }
        return getDiscountAmount()
                .multiply(new BigDecimal("100"))
                .divide(getOriginalPrice(), 2, RoundingMode.HALF_UP);
    }

    public boolean isValidForPurchase() {
        if (!isEligible()) {
            return false;
        }

        if (validUntil != null && validUntil.isBefore(Instant.now())) {
            return false;
        }

        if (minimumPurchaseAmount != null && 
            getOriginalPrice().compareTo(minimumPurchaseAmount) < 0) {
            return false;
        }

        if (maximumDiscountAmount != null && 
            getDiscountAmount().compareTo(maximumDiscountAmount) > 0) {
            return false;
        }

        return getFinalPrice().compareTo(BigDecimal.ZERO) > 0;
    }

    public String getIneligibilityReason() {
        if (isValidForPurchase()) {
            return null;
        }

        if (!isEligible()) {
            return ineligibilityReason != null ? 
                   ineligibilityReason : "Bundle is not eligible";
        }

        if (validUntil != null && validUntil.isBefore(Instant.now())) {
            return "Bundle offer has expired";
        }

        if (minimumPurchaseAmount != null && 
            getOriginalPrice().compareTo(minimumPurchaseAmount) < 0) {
            return String.format("Minimum purchase amount of %s not met", 
                minimumPurchaseAmount);
        }

        if (maximumDiscountAmount != null && 
            getDiscountAmount().compareTo(maximumDiscountAmount) > 0) {
            return String.format("Discount amount exceeds maximum allowed (%s)", 
                maximumDiscountAmount);
        }

        if (getFinalPrice().compareTo(BigDecimal.ZERO) <= 0) {
            return "Final price cannot be zero or negative";
        }

        return "Unknown eligibility issue";
    }
}
