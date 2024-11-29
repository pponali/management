package com.scaler.price.rule.dto;

import com.scaler.price.core.management.domain.AuditInfo;
import com.scaler.price.rule.domain.constraint.RuleConstraints;
import jakarta.persistence.Entity;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;


@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@Entity
public class CategoryAttributes extends AuditInfo {
    private String displayName;
    private String imageUrl;
    private String metaTitle;
    private String metaDescription;
    private Set<String> tags;
    private Map<String, String> customAttributes;
    private RuleConstraints MarginConstraints;
    private RuleConstraints priceConstraints;
    private RuleConstraints timeConstraints;

    @Builder.Default
    private Map<String, PriceAttribute> priceAttributes = new HashMap<>();

    @Builder.Default
    private Map<String, ValidationRule> validationRules = new HashMap<>();


    @Data
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
    }

    @Data
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
    }

    public enum ValidationSeverity {
        ERROR,
        WARNING,
        INFO
    }
}