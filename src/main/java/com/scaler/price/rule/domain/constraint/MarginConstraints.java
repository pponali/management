package com.scaler.price.rule.domain.constraint;

import com.scaler.price.core.management.domain.AuditInfo;
import com.scaler.price.rule.domain.RuleType;
import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMin;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.*;

@Entity
@Table(name = "margin_constraints")
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper=false)
@Getter
@Setter
@Builder
public class MarginConstraints extends RuleConstraints {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "calculation_type", nullable = false)
    @Enumerated(EnumType.STRING)
    private MarginCalculationType calculationType = MarginCalculationType.PERCENTAGE;

    @Column(name = "default_margin", precision = 10, scale = 2)
    private BigDecimal defaultMargin;

    @Column(name = "min_margin_override", precision = 10, scale = 2)
    private BigDecimal minMarginOverride;

    @Column(name = "max_margin_override", precision = 10, scale = 2)
    private BigDecimal maxMarginOverride;

    @Column(name = "enforce_min_margin")
    private Boolean enforceMinMargin = false;

    @Column(name = "enforce_max_margin")
    private Boolean enforceMaxMargin = false;

    @Column(name = "min_margin_percentage", precision = 10, scale = 2)
    private BigDecimal minMarginPercentage;

    @Column(name = "max_margin_percentage", precision = 10, scale = 2)
    private BigDecimal maxMarginPercentage;

    @Column(name = "last_modified_by")
    private String lastModifiedBy;

    @Column(name = "site_id")
    private String siteId;

    @Column(name = "rule_type")
    @Enumerated(EnumType.STRING)
    private RuleType ruleType;

    @Column(name = "min_margin")
    @DecimalMin(value = "0.0")
    private BigDecimal minMargin;

    @Column(name = "max_margin")
    @DecimalMin(value = "0.0")
    private BigDecimal maxMargin;

    @Column(name = "target_margin")
    @DecimalMin(value = "0.0")
    private BigDecimal targetMargin;

    @Column(name = "is_active")
    private boolean active;

    @Embedded
    private AuditInfo auditInfo;

    @PrePersist
    @PreUpdate
    protected void onUpdate() {
        if (auditInfo == null) {
            auditInfo = new AuditInfo();
        }
        auditInfo.setUpdatedAt(LocalDateTime.now());
        if (auditInfo.getCreatedAt() == null) {
            auditInfo.setCreatedAt(auditInfo.getUpdatedAt());
        }
    }


    public MarginConstraints(Long id, MarginCalculationType calculationType,
                           BigDecimal defaultMargin, BigDecimal minMarginOverride,
                           BigDecimal maxMarginOverride, Boolean enforceMinMargin,
                           Boolean enforceMaxMargin, BigDecimal minMarginPercentage,
                           BigDecimal maxMarginPercentage, String lastModifiedBy,
                           BigDecimal minimumPrice,
                           BigDecimal maximumPrice, BigDecimal minimumMargin,
                           BigDecimal maximumMargin, LocalDateTime effectiveFrom,
                           LocalDateTime effectiveTo, Boolean isActive,
                           Integer priority, RuleType ruleType,
                           Instant startDate, Instant endDate, String siteId, BigDecimal minMargin, BigDecimal maxMargin, BigDecimal targetMargin, boolean active, AuditInfo auditInfo) {
                        
        this.id = id;
        this.calculationType = calculationType;
        this.defaultMargin = defaultMargin;
        this.minMarginOverride = minMarginOverride;
        this.maxMarginOverride = maxMarginOverride;
        this.enforceMinMargin = enforceMinMargin;
        this.enforceMaxMargin = enforceMaxMargin;
        this.minMarginPercentage = minMarginPercentage;
        this.maxMarginPercentage = maxMarginPercentage;
        this.lastModifiedBy = lastModifiedBy;
        this.siteId = siteId;
        this.ruleType = ruleType;
        this.minMargin = minMargin;
        this.maxMargin = maxMargin;
        this.targetMargin = targetMargin;
        this.active = active;
        this.auditInfo = auditInfo;
    }

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

    public enum MarginCalculationType {
        PERCENTAGE,
        FIXED_AMOUNT
    }

    
    @ElementCollection
    @CollectionTable(name = "category_margins", joinColumns = @JoinColumn(name = "margin_constraint_id"))
    private Map<String, CategoryMargin> categoryMargins = new HashMap<>();

  
    @ElementCollection
    @CollectionTable(name = "margin_tiers", joinColumns = @JoinColumn(name = "margin_constraint_id"))
    private List<MarginTier> marginTiers = new ArrayList<>();


    @ElementCollection
    @CollectionTable(name = "seller_margins", joinColumns = @JoinColumn(name = "margin_constraint_id"))
    private Map<String, SellerMargin> sellerMargins = new HashMap<>();

    @Embeddable
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CategoryMargin {
        @Column(name = "category_id")
        private String categoryId;
        @Column(name = "min_margin")
        private BigDecimal minMargin;
        @Column(name = "max_margin")
        private BigDecimal maxMargin;
        @Column(name = "target_margin")
        private BigDecimal targetMargin;
        @Column(name = "enforce_strict")
        private Boolean enforceStrict;
        @ElementCollection
        @CollectionTable(name = "excluded_products", joinColumns = @JoinColumn(name = "category_margin_id"))
        private Set<String> excludedProducts;
        @Column(name = "additional_rules")
        private Map<String, Object> additionalRules;

        public String getCategoryId() {
            return categoryId;
        }

        public BigDecimal getMinMargin() {
            return minMargin;
        }

        public BigDecimal getMaxMargin() {
            return maxMargin;
        }

        public BigDecimal getTargetMargin() {
            return targetMargin;
        }

        public Boolean getEnforceStrict() {
            return enforceStrict;
        }

        public Set<String> getExcludedProducts() {
            return excludedProducts;
        }

        public Map<String, Object> getAdditionalRules() {
            return additionalRules;
        }
    }

    @Embeddable
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MarginTier {
        @Column(name = "from_price")
        private BigDecimal fromPrice;
        @Column(name = "to_price")
        private BigDecimal toPrice;
        @Column(name = "margin_percentage")
        private BigDecimal marginPercentage;
        @Column(name = "applicability_rule")
        private String applicabilityRule;
        @Column(name = "tier_type")
        private TierType tierType;
        @Column(name = "conditions")
        private Map<String, Object> conditions;

        public BigDecimal getFromPrice() {
            return fromPrice;
        }

        public BigDecimal getToPrice() {
            return toPrice;
        }

        public BigDecimal getMarginPercentage() {
            return marginPercentage;
        }

        public String getApplicabilityRule() {
            return applicabilityRule;
        }

        public TierType getTierType() {
            return tierType;
        }

        public Map<String, Object> getConditions() {
            return conditions;
        }
    }

    @Embeddable
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SellerMargin {
        @Column(name = "seller_id")
        private String sellerId;
        @Column(name = "default_margin")
        private BigDecimal defaultMargin;
        @ElementCollection
        @CollectionTable(name = "category_margins", joinColumns = @JoinColumn(name = "seller_margin_id"))
        private Map<String, BigDecimal> categoryMargins;
        @Column(name = "override_global")
        private Boolean overrideGlobal;
        @Column(name = "validation_behavior")
        private ValidationBehavior validationBehavior;

        public String getSellerId() {
            return sellerId;
        }

        public BigDecimal getDefaultMargin() {
            return defaultMargin;
        }

        public Map<String, BigDecimal> getCategoryMargins() {
            return categoryMargins;
        }

        public Boolean getOverrideGlobal() {
            return overrideGlobal;
        }

        public ValidationBehavior getValidationBehavior() {
            return validationBehavior;
        }
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

    public BigDecimal getTargetMarginPercentage() {
        return targetMargin;
    }


    public void setTargetMarginPercentage(BigDecimal targetMarginPercentage) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'setTargetMarginPercentage'");
    }
}