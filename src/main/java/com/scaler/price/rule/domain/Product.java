package com.scaler.price.rule.domain;

import com.scaler.price.core.management.domain.AuditInfo;
import com.scaler.price.rule.domain.constraint.MarginConstraints;
import com.scaler.price.rule.domain.constraint.RuleConstraints;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

@Entity
@Table(name = "products")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder
public class Product {
    @Id
    private String id;
    private String displayName;
    private String imageUrl;
    private String metaTitle;
    private String metaDescription;
    private String name;
    private String categoryId;
    private String brandId;
    private String sellerId;
    private String version;
    
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
    private Set<String> siteIds = new HashSet<>();
    
    private BigDecimal mrp;
    private BigDecimal costPrice;
    private String currency;
    private ProductStatus status;
    
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @OneToOne(cascade = CascadeType.ALL)
    private RuleConstraints marginConstraints;

    @OneToOne(cascade = CascadeType.ALL)
    private RuleConstraints priceConstraints;

    @OneToOne(cascade = CascadeType.ALL)
    private RuleConstraints timeConstraints;

    @ElementCollection
    @CollectionTable(name = "product_price_attributes")
    @MapKeyColumn(name = "attribute_key")
    private Map<String, PriceAttribute> priceAttributes = new HashMap<>();

    @ElementCollection
    @CollectionTable(name = "product_validation_rules")
    @MapKeyColumn(name = "rule_key")
    private Map<String, ValidationRule> validationRules = new HashMap<>();

    @Embedded
    private AuditInfo auditInfo;

    private Integer quantity;

    public MarginConstraints getMarginConstraints() {
        return MarginConstraints.builder()
                .siteId("DEFAULT")
                .minMarginPercentage(BigDecimal.ZERO)
                .maxMarginOverride(BigDecimal.valueOf(100))
                .defaultMargin(BigDecimal.valueOf(20))
                .build();
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Embeddable
    public static class PriceAttribute {
        private BigDecimal minimumPrice;
        private BigDecimal maximumPrice;
        private BigDecimal minimumMargin;
        private BigDecimal maximumMargin;
        private String priceType;
        private Boolean enforceMinPrice;
        private Boolean enforceMaxPrice;
        
        @ElementCollection
        @CollectionTable(name = "price_attribute_excluded_products")
        private Set<String> excludedProducts;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Embeddable
    public static class ValidationRule {
        private String ruleName;
        private String ruleType;
        private String ruleExpression;
        private String errorMessage;
        
        @Enumerated(EnumType.STRING)
        private ValidationSeverity severity;
        
        private Boolean isActive;
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
