package com.scaler.price.rule.domain.constraint;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PriceConstraints extends RuleConstraints{
    private BigDecimal maxPriceChangePercentage;
    private BigDecimal maxPriceIncreaseAmount;
    private BigDecimal maxPriceDecreaseAmount;
    private RoundingStrategy roundingStrategy;
    private Integer roundingValue;
    private Boolean allowPriceIncrease;
    private Set<String> excludedCategories;
    @Builder.Default
    private Map<String, BigDecimal> categorySpecificLimits = new HashMap<>();

    @Builder.Default
    private List<PriceThreshold> priceThresholds = new ArrayList<>();

    @Builder.Default
    private Map<String, RegionalPriceAdjustment> regionalAdjustments = new HashMap<>();

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PriceThreshold {
        private BigDecimal fromPrice;
        private BigDecimal toPrice;
        private BigDecimal maxChangePercentage;
        private String applicabilityRule;
        private ThresholdType thresholdType;
        private Map<String, Object> additionalRules;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RegionalPriceAdjustment {
        private String region;
        private BigDecimal adjustmentFactor;
        private AdjustmentType adjustmentType;
        private Boolean applyToBasePrice;
        private Set<String> excludedCategories;
    }

    public enum RoundingStrategy {
        NONE,
        ROUND_UP,
        ROUND_DOWN,
        ROUND_TO_NEAREST,
        CUSTOM
    }

    public enum ThresholdType {
        FIXED,
        PERCENTAGE,
        DYNAMIC
    }

    public enum AdjustmentType {
        PERCENTAGE,
        FIXED_AMOUNT,
        MULTIPLIER
    }

    public BigDecimal applyRounding(BigDecimal price) {
        if (roundingStrategy == null || roundingValue == null) {
            return price;
        }

        return switch (roundingStrategy) {
            case ROUND_UP -> roundUp(price);
            case ROUND_DOWN -> roundDown(price);
            case ROUND_TO_NEAREST -> roundToNearest(price);
            case CUSTOM -> applyCustomRounding(price);
            default -> price;
        };
    }

    private BigDecimal roundUp(BigDecimal price) {
        return price.divide(new BigDecimal(roundingValue), 0, RoundingMode.CEILING)
                .multiply(new BigDecimal(roundingValue));
    }

    private BigDecimal roundDown(BigDecimal price) {
        return price.divide(new BigDecimal(roundingValue), 0, RoundingMode.FLOOR)
                .multiply(new BigDecimal(roundingValue));
    }

    private BigDecimal roundToNearest(BigDecimal price) {
        return price.divide(new BigDecimal(roundingValue), 0, RoundingMode.HALF_UP)
                .multiply(new BigDecimal(roundingValue));
    }

    private BigDecimal applyCustomRounding(BigDecimal price) {
        // Implement custom rounding logic
        return price;
    }
}
