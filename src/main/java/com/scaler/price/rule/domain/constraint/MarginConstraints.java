package com.scaler.price.rule.domain.constraint;

import com.scaler.price.rule.domain.RuleType;
import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMin;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

/**
 * MarginConstraints class represents the margin constraints for a pricing rule.
 * It extends the RuleConstraints class and adds additional fields and methods specific to margin constraints.
 */
@Entity
@DiscriminatorValue("PRICING")
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder(toBuilder = true, builderMethodName = "marginConstraintsBuilder")
@Getter
@Setter
public class MarginConstraints extends RuleConstraints {

    /**
     * The calculation type for the margin.
     */
    @Column(name = "calculation_type", nullable = false)
    @Enumerated(EnumType.STRING)
    private MarginCalculationType calculationType = MarginCalculationType.PERCENTAGE;

    /**
     * The default margin.
     */
    @Column(name = "default_margin", precision = 10, scale = 2)
    private BigDecimal defaultMargin;

    /**
     * The target margin percentage to be achieved.
     * This represents the desired profit margin as a percentage of the cost.
     */
    @Column(name = "target_margin_percentage", precision = 10, scale = 2)
    @DecimalMin(value = "0.0", message = "Target margin percentage must be non-negative")
    private BigDecimal targetMarginPercentage;

    /**
     * The minimum margin override.
     */
    @Column(name = "min_margin_override", precision = 10, scale = 2)
    private BigDecimal minMarginOverride;

    /**
     * The maximum margin override.
     */
    @Column(name = "max_margin_override", precision = 10, scale = 2)
    private BigDecimal maxMarginOverride;

    /**
     * Whether to enforce the minimum margin.
     */
    @Column(name = "enforce_min_margin")
    private Boolean enforceMinMargin = false;

    /**
     * Whether to enforce the maximum margin.
     */
    @Column(name = "enforce_max_margin")
    private Boolean enforceMaxMargin = false;

    /**
     * The minimum margin percentage.
     */
    @Column(name = "min_margin_percentage", precision = 10, scale = 2)
    private BigDecimal minMarginPercentage;

    /**
     * The maximum margin percentage.
     */
    @Column(name = "max_margin_percentage", precision = 10, scale = 2)
    private BigDecimal maxMarginPercentage;

    /**
     * The site ID.
     */
    @Column(name = "site_id")
    private String siteId;

     /**
     * The margin Trend.
     */
    @Column(name = "margin_trend")
    private String marginTrend;


    /**
     * The rule type.
     */
    @Column(name = "rule_type")
    @Enumerated(EnumType.STRING)
    private RuleType ruleType;

    /**
     * The minimum margin.
     */
    @Column(name = "min_margin")
    @DecimalMin(value = "0.0")
    private BigDecimal minMargin;

    /**
     * The maximum margin.
     */
    @Column(name = "max_margin")
    @DecimalMin(value = "0.0")
    private BigDecimal maxMargin;

    /**
     * The target margin.
     */
    @Column(name = "target_margin")
    @DecimalMin(value = "0.0")
    private BigDecimal targetMargin;


    /**
     * Calculates the margin based on the base price and cost price.
     *
     * @param basePrice  the base price
     * @param costPrice  the cost price
     * @return the calculated margin
     */
    public BigDecimal calculateMargin(BigDecimal basePrice, BigDecimal costPrice) {
        if (basePrice == null || costPrice == null || costPrice.compareTo(BigDecimal.ZERO) == 0) {
            return defaultMargin != null ? defaultMargin : BigDecimal.ZERO;
        }

        BigDecimal margin;
        if (calculationType == MarginCalculationType.PERCENTAGE) {
            margin = basePrice.subtract(costPrice)
                    .multiply(new BigDecimal("100"))
                    .divide(costPrice, 2, RoundingMode.HALF_UP);
        } else {
            margin = basePrice.subtract(costPrice);
        }

        return validateMargin(margin);
    }

    /**
     * Validates the margin.
     *
     * @param margin the margin to validate
     * @return the validated margin
     */
    private BigDecimal validateMargin(BigDecimal margin) {
        if (margin == null) {
            return defaultMargin != null ? defaultMargin : BigDecimal.ZERO;
        }

        if (enforceMinMargin && minMarginOverride != null && margin.compareTo(minMarginOverride) < 0) {
            return minMarginOverride;
        }

        if (enforceMaxMargin && maxMarginOverride != null && margin.compareTo(maxMarginOverride) > 0) {
            return maxMarginOverride;
        }

        return margin;
    }

    /**
     * Enum for margin calculation types.
     */
    public enum MarginCalculationType {
        PERCENTAGE,
        FIXED_AMOUNT
    }

    /**
     * The category margins.
     */
    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "margin_constraint_id")
    private Map<String, CategoryMargin> categoryMargins = new HashMap<>();

    /**
     * Gets the category margins.
     *
     * @return the category margins
     */
    public Map<String, CategoryMargin> getCategoryMargins() {
        return this.categoryMargins;
    }

    /**
     * Sets the category margins.
     *
     * @param categoryMargins the category margins to set
     */
    public void setCategoryMargins(Map<String, CategoryMargin> categoryMargins) {
        this.categoryMargins = categoryMargins;
    }

    /**
     * The margin tiers.
     */
    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "margin_constraint_id")
    private List<MarginTier> marginTiers = new ArrayList<>();

    /**
     * The seller margins.
     */
    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "margin_constraint_id")
    private Map<String, SellerMargin> sellerMargins = new HashMap<>();

    /**
     * Entity for category margins.
     */
    @Entity
    @Table(name = "category_margins")
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CategoryMargin {
        /**
         * The ID.
         */
        @Id
        @GeneratedValue(strategy = GenerationType.IDENTITY)
        private Long id;

        /**
         * The category ID.
         */
        @Column(name = "category_id")
        private String categoryId;

        /**
         * The minimum margin.
         */
        @Column(name = "min_margin")
        private BigDecimal minMargin;

        /**
         * The maximum margin.
         */
        @Column(name = "max_margin")
        private BigDecimal maxMargin;

        /**
         * The target margin.
         */
        @Column(name = "target_margin")
        private BigDecimal targetMargin;

        /**
         * Whether to enforce strict margin.
         */
        @Column(name = "enforce_strict")
        private Boolean enforceStrict;

        /**
         * The excluded products.
         */
        @ElementCollection
        @CollectionTable(name = "excluded_products",
                joinColumns = @JoinColumn(name = "category_margin_id"))
        private Set<String> excludedProducts = new HashSet<>();

        /**
         * The additional rules.
         */
        @ElementCollection
        @CollectionTable(name = "additional_rules",
                joinColumns = @JoinColumn(name = "category_margin_id"))
        @MapKeyColumn(name = "rule_key")
        @Column(name = "rule_value")
        private Map<String, String> additionalRules = new HashMap<>();
    }

    /**
     * Entity for margin tiers.
     */
    @Entity
    @Table(name = "margin_tiers")
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MarginTier {
        /**
         * The ID.
         */
        @Id
        @GeneratedValue(strategy = GenerationType.IDENTITY)
        private Long id;

        /**
         * The from price.
         */
        @Column(name = "from_price")
        private BigDecimal fromPrice;

        /**
         * The to price.
         */
        @Column(name = "to_price")
        private BigDecimal toPrice;

        /**
         * The margin percentage.
         */
        @Column(name = "margin_percentage")
        private BigDecimal marginPercentage;

        /**
         * The applicability rule.
         */
        @Column(name = "applicability_rule")
        private String applicabilityRule;

        /**
         * The tier type.
         */
        @Column(name = "tier_type")
        @Enumerated(EnumType.STRING)
        private TierType tierType;

        /**
         * The conditions.
         */
        @ElementCollection
        @CollectionTable(name = "tier_conditions",
                joinColumns = @JoinColumn(name = "margin_tier_id"))
        @MapKeyColumn(name = "condition_key")
        @Column(name = "condition_value")
        private Map<String, String> conditions = new HashMap<>();
    }

    /**
     * Entity for seller margins.
     */
    @Entity
    @Table(name = "seller_margins")
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SellerMargin {
        /**
         * The ID.
         */
        @Id
        @GeneratedValue(strategy = GenerationType.IDENTITY)
        private Long id;

        /**
         * The seller ID.
         */
        @Column(name = "seller_id")
        private String sellerId;

        /**
         * The default margin.
         */
        @Column(name = "default_margin")
        private BigDecimal defaultMargin;

        /**
         * The category margins.
         */
        @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
        @JoinColumn(name = "seller_margin_id")
        private Map<String, CategoryMargin> categoryMargins = new HashMap<>();

        /**
         * Whether to override global margin.
         */
        @Column(name = "override_global")
        private Boolean overrideGlobal;

        /**
         * The validation behavior.
         */
        @Column(name = "validation_behavior")
        @Enumerated(EnumType.STRING)
        private ValidationBehavior validationBehavior;
    }

    /**
     * Enum for validation behaviors.
     */
    public enum ValidationBehavior {
        STRICT,
        LENIENT,
        IGNORE
    }

    /**
     * Enum for tier types.
     */
    public enum TierType {
        PRICE_BASED,
        QUANTITY_BASED,
        CUSTOM
    }

    /**
     * Gets the target margin percentage.
     *
     * @return the target margin percentage
     */
    public BigDecimal getTargetMarginPercentage() {
        return targetMargin;
    }

    /**
     * Sets the target margin percentage.
     *
     * @param targetMarginPercentage the target margin percentage to set
     */
    public void setTargetMarginPercentage(BigDecimal targetMarginPercentage) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'setTargetMarginPercentage'");
    }
}