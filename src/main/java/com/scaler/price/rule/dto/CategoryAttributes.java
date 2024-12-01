package com.scaler.price.rule.dto;

import com.scaler.price.core.management.domain.AuditInfo;
import com.scaler.price.rule.domain.constraint.RuleConstraints;
import com.vladmihalcea.hibernate.type.json.JsonBinaryType;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.Type;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.HashSet;
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

    @Type(JsonBinaryType.class)
    @Column(columnDefinition = "jsonb")
    private Set<String> tags = new HashSet<>();

    @Type(JsonBinaryType.class)
    @Column(columnDefinition = "jsonb")
    private Map<String, String> customAttributes = new HashMap<>();

    @ManyToOne
    @JoinColumn(name = "margin_constraint_id")
    private RuleConstraints marginConstraints;

    @ManyToOne
    @JoinColumn(name = "price_constraint_id")
    private RuleConstraints priceConstraints;

    @ManyToOne
    @JoinColumn(name = "time_constraint_id")
    private RuleConstraints timeConstraints;

    @Type(JsonBinaryType.class)
    @Column(columnDefinition = "jsonb")
    @Builder.Default
    private Map<String, PriceAttribute> priceAttributes = new HashMap<>();

    @Type(JsonBinaryType.class)
    @Column(columnDefinition = "jsonb")
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
        
        @Type(JsonBinaryType.class)
        @Column(columnDefinition = "jsonb")
        private Map<String, String> priceModifiers = new HashMap<>();
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
        
        @Type(JsonBinaryType.class)
        @Column(columnDefinition = "jsonb")
        private Map<String, String> parameters = new HashMap<>();
    }

    public enum ValidationSeverity {
        ERROR,
        WARNING,
        INFO
    }
}