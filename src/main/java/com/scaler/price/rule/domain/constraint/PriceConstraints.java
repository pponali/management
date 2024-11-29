package com.scaler.price.rule.domain.constraint;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.Instant;
import java.util.*;

import com.scaler.price.rule.domain.RuleType;

@Entity
@Table(name = "price_constraints")
@Data
@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode(callSuper=false)
public class PriceConstraints extends RuleConstraints{
    
    @Column(name = "max_price_change_percentage", precision = 10, scale = 2)
    private BigDecimal maxPriceChangePercentage;
    
    @Column(name = "max_price_increase_amount", precision = 10, scale = 2)
    private BigDecimal maxPriceIncreaseAmount;
    
    @Column(name = "max_price_decrease_amount", precision = 10, scale = 2)
    private BigDecimal maxPriceDecreaseAmount;
    
    @Column(name = "min_discount_percentage", precision = 10, scale = 2)
    private BigDecimal minDiscountPercentage;
    
    @Column(name = "max_discount_percentage", precision = 10, scale = 2)
    private BigDecimal maxDiscountPercentage;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "rounding_strategy")
    private RoundingStrategy roundingStrategy = RoundingStrategy.NONE;
    
    @Column(name = "rounding_value")
    private Integer roundingValue;
    
    @Column(name = "allow_price_increase")
    private Boolean allowPriceIncrease = true;

    @ElementCollection
    @CollectionTable(
        name = "category_specific_limits",
        joinColumns = @JoinColumn(name = "price_constraints_id")
    )
    @MapKeyColumn(name = "category_id")
    private Map<String, CategoryLimit> categorySpecificLimits = new HashMap<>();

    @Column(name = "minimum_price", precision = 10, scale = 2)
    private BigDecimal minimumPrice;

    @Column(name = "maximum_price", precision = 10, scale = 2)
    private BigDecimal maximumPrice;
    
    @ElementCollection
    @CollectionTable(name = "price_constraint_excluded_categories",
            joinColumns = @JoinColumn(name = "price_constraint_id"))
    @Column(name = "category_id")
    private Set<String> excludedCategories = new HashSet<>();
    
    @ElementCollection
    @CollectionTable(name = "price_constraint_thresholds",
            joinColumns = @JoinColumn(name = "price_constraint_id"))
    @OrderColumn(name = "threshold_order")
    private List<PriceThreshold> priceThresholds = new ArrayList<>();
    
    @ElementCollection
    @CollectionTable(name = "price_constraint_regional_adjustments",
            joinColumns = @JoinColumn(name = "price_constraint_id"))
    @MapKeyColumn(name = "region_id")
    private Map<String, RegionalPriceAdjustment> regionalAdjustments = new HashMap<>();
    
    @Embeddable
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class CategoryLimit {
        @Column(name = "min_price", precision = 10, scale = 2)
        private BigDecimal minPrice;

        @Column(name = "max_price", precision = 10, scale = 2)
        private BigDecimal maxPrice;

        @Column(name = "min_discount_percentage", precision = 5, scale = 2)
        private BigDecimal minDiscountPercentage;

        @Column(name = "max_discount_percentage", precision = 5, scale = 2)
        private BigDecimal maxDiscountPercentage;
    }
    
    @Builder(builderMethodName = "priceConstraintsBuilder")
    public PriceConstraints(BigDecimal minimumPrice, BigDecimal maximumPrice,
                          BigDecimal minimumMargin, BigDecimal maximumMargin,
                          LocalDateTime effectiveFrom, LocalDateTime effectiveTo,
                          Boolean isActive, Integer priority, RuleType ruleType,
                          Instant startDate, Instant endDate,
                          BigDecimal maxPriceChangePercentage,
                          BigDecimal maxPriceIncreaseAmount,
                          BigDecimal maxPriceDecreaseAmount,
                          BigDecimal minDiscountPercentage,
                          BigDecimal maxDiscountPercentage,
                          RoundingStrategy roundingStrategy,
                          Integer roundingValue,
                          Boolean allowPriceIncrease,
                          Set<String> excludedCategories,
                          Map<String, CategoryLimit> categorySpecificLimits,
                          List<PriceThreshold> priceThresholds,
                          Map<String, RegionalPriceAdjustment> regionalAdjustments) {
        this.maxPriceChangePercentage = maxPriceChangePercentage;
        this.maxPriceIncreaseAmount = maxPriceIncreaseAmount;
        this.maxPriceDecreaseAmount = maxPriceDecreaseAmount;
        this.minDiscountPercentage = minDiscountPercentage;
        this.maxDiscountPercentage = maxDiscountPercentage;
        this.roundingStrategy = roundingStrategy;
        this.roundingValue = roundingValue;
        this.allowPriceIncrease = allowPriceIncrease;
        this.excludedCategories = excludedCategories != null ? excludedCategories : new HashSet<>();
        this.categorySpecificLimits = categorySpecificLimits != null ? categorySpecificLimits : new HashMap<>();
        this.priceThresholds = priceThresholds != null ? priceThresholds : new ArrayList<>();
        this.regionalAdjustments = regionalAdjustments != null ? regionalAdjustments : new HashMap<>();
        this.minimumPrice = minimumPrice;
        this.maximumPrice = maximumPrice;
    }
    
    @Embeddable
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PriceThreshold {
        @Column(name = "from_price", precision = 10, scale = 2)
        private BigDecimal fromPrice;
        
        @Column(name = "to_price", precision = 10, scale = 2)
        private BigDecimal toPrice;
        
        @Column(name = "max_change_percentage", precision = 10, scale = 2)
        private BigDecimal maxChangePercentage;
        
        @Column(name = "applicability_rule")
        private String applicabilityRule;
        
        @Enumerated(EnumType.STRING)
        @Column(name = "threshold_type")
        private ThresholdType thresholdType;
        
        @ElementCollection
        @CollectionTable(name = "price_threshold_additional_rules",
                joinColumns = @JoinColumn(name = "price_threshold_id"))
        @MapKeyColumn(name = "rule_id")
        @Column(name = "rule_value")
        private Map<String, Object> additionalRules;
    }
    
    @Embeddable
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RegionalPriceAdjustment {
        @Column(name = "region")
        private String region;
        
        @Column(name = "adjustment_factor", precision = 10, scale = 4)
        private BigDecimal adjustmentFactor;
        
        @Enumerated(EnumType.STRING)
        @Column(name = "adjustment_type")
        private AdjustmentType adjustmentType;
        
        @Column(name = "apply_to_base_price")
        private Boolean applyToBasePrice;
        
        @ElementCollection
        @CollectionTable(name = "regional_price_adjustment_excluded_categories",
                joinColumns = @JoinColumn(name = "regional_price_adjustment_id"))
        @Column(name = "category_id")
        private Set<String> excludedCategories;
    }
    
    public enum RoundingStrategy {
        NONE,
        ROUND_UP,
        ROUND_DOWN,
        ROUND_TO_NEAREST
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
        if (price == null || roundingStrategy == RoundingStrategy.NONE || roundingValue == null || roundingValue <= 0) {
            return price;
        }
        
        BigDecimal factor = new BigDecimal(roundingValue);
        BigDecimal divided = price.divide(factor, 0, getRoundingMode());
        return divided.multiply(factor);
    }
    
    private RoundingMode getRoundingMode() {
        return switch (roundingStrategy) {
            case ROUND_UP -> RoundingMode.CEILING;
            case ROUND_DOWN -> RoundingMode.FLOOR;
            case ROUND_TO_NEAREST -> RoundingMode.HALF_UP;
            default -> RoundingMode.HALF_UP;
        };
    }

}
