package com.scaler.price.rule.dto;

import com.scaler.price.rule.domain.constraint.RuleConstraints;
import lombok.*;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@ToString
@EqualsAndHashCode
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CategoryAttributes {
    private Long id;
    private Long categoryId;
    private String displayName;
    private String imageUrl;
    private String metaTitle;
    private String metaDescription;
    private Set<String> tags = new HashSet<>();
    private Map<String, String> customAttributes = new HashMap<>();
    private Map<String, PriceAttribute> priceAttributes = new HashMap<>();
    private Map<String, ValidationRule> validationRules = new HashMap<>();
    private Boolean isActive;
    private String lastModifiedBy;
    private String modifiedUser;
    private RuleConstraints marginConstraints;
    private RuleConstraints timeConstraints;
    private RuleConstraints priceConstraints;

    public void setPriceConstraints(RuleConstraints priceConstraints) {
        this.priceConstraints = priceConstraints;
    }

    @ToString
    @EqualsAndHashCode
    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PriceAttribute {
        private BigDecimal minimumPrice;
        private BigDecimal maximumPrice;
        private BigDecimal minimumMargin;
        private BigDecimal maximumMargin;
        private String priceType;
        private Boolean enforceMinPrice;
        private Boolean enforceMaxPrice;
        private Set<String> excludedProducts;
        private Map<String, String> priceModifiers = new HashMap<>();
    }

    @ToString
    @EqualsAndHashCode
    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ValidationRule {
        private String ruleName;
        private String ruleType;
        private String ruleExpression;
        private String errorMessage;
        private ValidationSeverity severity;
        private Boolean isActive;
        private Map<String, String> parameters = new HashMap<>();
    }

    public enum ValidationSeverity {
        ERROR,
        WARNING,
        INFO
    }
}