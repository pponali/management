package com.scaler.price.rule.domain;

import com.scaler.price.core.management.domain.AuditInfo;
import com.scaler.price.rule.domain.constraint.MarginConstraints;
import com.scaler.price.rule.domain.constraint.RuleConstraints;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;
import java.util.*;

@Entity
@Table(name = "products")
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class Product extends AuditInfo {
    private String displayName;
    private String imageUrl;
    private String metaTitle;
    private String metaDescription;
    private String name;
    private Long categoryId;
    
    
    @ManyToOne
    @JoinColumn(name = "brand_id")
    private Brand brand;
    
    private Long sellerId;
    
    @ElementCollection
    @CollectionTable(name = "product_tags")
    private Set<String> tags;

    @ElementCollection
    @CollectionTable(name = "product_custom_attributes")
    @MapKeyColumn(name = "attribute_key")
    @Column(name = "attribute_value")
    private Map<String, String> customAttributes;

    @ElementCollection
    @CollectionTable(name = "product_site_ids")
    private Set<Long> siteIds = new HashSet<>();
    
    private BigDecimal mrp;
    private BigDecimal costPrice;
    private String currency;
    private ProductStatus status;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "margin_constraint_id")
    private RuleConstraints marginConstraints;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "price_constraint_id")
    private RuleConstraints priceConstraints;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "time_constraint_id")
    private RuleConstraints timeConstraints;

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "product_id")
    private Map<String, PriceAttribute> priceAttributes = new HashMap<>();

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "product_id")
    private Map<String, ValidationRule> validationRules = new HashMap<>();

    private Integer quantity;

    public MarginConstraints getMarginConstraints() {
        return MarginConstraints.marginConstraintsBuilder()
                .siteId("DEFAULT")
                .minMarginPercentage(BigDecimal.ZERO)
                .maxMarginOverride(BigDecimal.valueOf(100))
                .defaultMargin(BigDecimal.valueOf(20))
                .build();
    }

    @Entity
    @Table(name = "price_attributes")
    @Data
    @SuperBuilder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PriceAttribute {
        @Id
        @GeneratedValue(strategy = GenerationType.IDENTITY)
        private Long id;

        @Column(name = "attribute_key")
        private String key;

        private BigDecimal minimumPrice;
        private BigDecimal maximumPrice;
        private BigDecimal minimumMargin;
        private BigDecimal maximumMargin;
        private String priceType;
        private Boolean enforceMinPrice;
        private Boolean enforceMaxPrice;
        
        @ElementCollection
        @CollectionTable(name = "price_attribute_excluded_products")
        @Column(name = "product_id")
        private Set<String> excludedProducts = new HashSet<>();
    }

    @Entity
    @Table(name = "validation_rules")
    @Data
    @SuperBuilder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ValidationRule {
        @Id
        @GeneratedValue(strategy = GenerationType.IDENTITY)
        private Long id;

        @Column(name = "rule_key")
        private String key;

        private String ruleType;
        private String validationMessage;
        private Boolean isActive;
        private Integer priority;
        
        @ElementCollection
        @CollectionTable(name = "validation_rule_parameters")
        @MapKeyColumn(name = "param_key")
        @Column(name = "param_value")
        private Map<String, String> parameters = new HashMap<>();
    }

    public enum ValidationSeverity {
        ERROR,
        WARNING,
        INFO
    }

    public enum ProductStatus {
        DRAFT,
        ACTIVE,
        INACTIVE,
        OUT_OF_STOCK,
        DISCONTINUED
    }
}
