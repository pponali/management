package com.scaler.price.rule.domain.constraint;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Entity
@DiscriminatorValue("PRICE_CONSTRAINTS")
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@SuperBuilder(toBuilder = true, builderMethodName = "priceConstraintsBuilder")
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

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "price_constraints_id")
    private Set<CategoryLimit> categoryLimits = new HashSet<>();
    
    @ElementCollection
    @CollectionTable(name = "price_constraint_excluded_categories",
            joinColumns = @JoinColumn(name = "price_constraint_id"))
    @Column(name = "category_id")
    private Set<String> excludedCategories = new HashSet<>();
    
    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "price_constraint_id")
    private List<PriceThreshold> priceThresholds = new ArrayList<>();
    
    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "price_constraint_id")
    private Set<RegionalAdjustment> regionalAdjustments = new HashSet<>();

    @Column(name = "margin_percentage", precision = 10, scale = 2)
    private BigDecimal marginPercentage;


    public enum RoundingStrategy {
        NONE,
        ROUND_UP,
        ROUND_DOWN,
        ROUND_TO_NEAREST
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
    
    public Map<String, CategoryLimit> getCategorySpecificLimits() {
        return categoryLimits.stream()
            .collect(Collectors.toMap(
                CategoryLimit::getCategoryId, 
                Function.identity(), 
                (v1, v2) -> v1
            ));
    }

    public Boolean getAllowPriceIncrease() {
        return allowPriceIncrease;
    }

    public void setAllowPriceIncrease(Boolean allowPriceIncrease) {
        this.allowPriceIncrease = allowPriceIncrease;
    }

    public RoundingStrategy getRoundingStrategy() {
        return this.roundingStrategy;
    }

    public static BigDecimal calculatePercentage(BigDecimal baseValue, BigDecimal percentage) {
        if (baseValue == null || percentage == null) {
            return BigDecimal.ZERO;
        }
        return baseValue.multiply(percentage).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
    }
}
