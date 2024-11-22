package com.scaler.price.rule.dto.action;

import com.scaler.price.rule.domain.DiscountType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.Set;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DiscountActionParameters {
    private BigDecimal discountValue;
    private DiscountType discountType;
    private Boolean stackable;
    private Integer maxStackCount;
    private Duration stackInterval;
    private BigDecimal maxTotalDiscount;
    private DiscountRestrictions restrictions;
    private StackingRules stackingRules;

    @Data
    @Builder
    public static class DiscountRestrictions {
        private BigDecimal minimumPurchaseAmount;
        private Integer minimumQuantity;
        private Set<String> applicableCategories;
        private Set<String> excludedProducts;
        private Set<String> customerSegments;
    }

    @Data
    @Builder
    public static class StackingRules {
        private Set<String> stackableWith;
        private Set<String> notStackableWith;
        private Integer minimumInterval;
        private StackingStrategy strategy;
    }

    public enum StackingStrategy {
        ADDITIVE,           // Add discounts
        MULTIPLICATIVE,     // Multiply discounts
        HIGHEST_WINS,       // Take highest discount
        SEQUENTIAL          // Apply in sequence
    }
}