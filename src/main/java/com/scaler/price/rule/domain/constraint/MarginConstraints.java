package com.scaler.price.rule.domain.constraint;

import com.scaler.price.rule.domain.RuleType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MarginConstraints extends RuleConstraints {
    private MarginCalculationType calculationType;
    private BigDecimal defaultMargin;
    private BigDecimal minMarginOverride;
    private BigDecimal maxMarginOverride;
    private Boolean enforceMinMargin;
    private Boolean enforceMaxMargin;
    private BigDecimal minMarginPercentage;
    private BigDecimal maxMarginPercentage;
    private RoundingMode roundingMode;
    private Integer roundingPrecision;
    private Map<String, Object> additionalRules;
    private ValidationBehavior validationBehavior;
    private BigDecimal targetMarginPercentage;


    @Builder.Default
    private Map<String, CategoryMargin> categoryMargins = new HashMap<>();

    @Builder.Default
    private List<MarginTier> marginTiers = new ArrayList<>();

    @Builder.Default
    private Map<String, SellerMargin> sellerMargins = new HashMap<>();

    public MarginConstraints(BigDecimal minimumPrice, BigDecimal maximumPrice, BigDecimal minimumMargin, BigDecimal maximumMargin, LocalDateTime effectiveFrom, LocalDateTime effectiveTo, Boolean isActive, Integer priority, RuleType ruleType, Instant startDate, Instant endDate) {
        super(minimumPrice, maximumPrice, minimumMargin, maximumMargin, effectiveFrom, effectiveTo, isActive, priority, ruleType, startDate, endDate);
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CategoryMargin {
        private String categoryId;
        private BigDecimal minMargin;
        private BigDecimal maxMargin;
        private BigDecimal targetMargin;
        private Boolean enforceStrict;
        private Set<String> excludedProducts;
        private Map<String, Object> additionalRules;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MarginTier {
        private BigDecimal fromPrice;
        private BigDecimal toPrice;
        private BigDecimal marginPercentage;
        private String applicabilityRule;
        private TierType tierType;
        private Map<String, Object> conditions;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SellerMargin {
        private String sellerId;
        private BigDecimal defaultMargin;
        private Map<String, BigDecimal> categoryMargins;
        private Boolean overrideGlobal;
        private ValidationBehavior validationBehavior;
    }

    public enum MarginCalculationType {
        COST_BASED,
        SELLING_PRICE_BASED,
        MRP_BASED,
        CUSTOM
    }

    public enum TierType {
        PRICE_BASED,
        QUANTITY_BASED,
        DATE_BASED
    }

    public enum ValidationBehavior {
        STRICT,
        WARN_ONLY,
        FLEXIBLE
    }

    public BigDecimal calculateMargin(BigDecimal price, BigDecimal cost) {
        return switch (calculationType) {
            case COST_BASED -> calculateCostBasedMargin(price, cost);
            case SELLING_PRICE_BASED -> calculateSellingPriceBasedMargin(price, cost);
            case MRP_BASED -> calculateMRPBasedMargin(price, cost);
            case CUSTOM -> calculateCustomMargin(price, cost);
        };
    }

    private BigDecimal calculateCostBasedMargin(BigDecimal price, BigDecimal cost) {
        return price.subtract(cost)
                .divide(cost, 4, RoundingMode.HALF_UP)
                .multiply(new BigDecimal("100"));
    }

    private BigDecimal calculateSellingPriceBasedMargin(BigDecimal price, BigDecimal cost) {
        return price.subtract(cost)
                .divide(price, 4, RoundingMode.HALF_UP)
                .multiply(new BigDecimal("100"));
    }

    private BigDecimal calculateMRPBasedMargin(BigDecimal price, BigDecimal cost) {
        // Implement MRP-based margin calculation
        return BigDecimal.ZERO;
    }

    private BigDecimal calculateCustomMargin(BigDecimal price, BigDecimal cost) {
        // Implement custom margin calculation
        return BigDecimal.ZERO;
    }
}