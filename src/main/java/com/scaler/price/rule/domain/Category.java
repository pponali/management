package com.scaler.price.rule.domain;

import com.scaler.price.core.management.domain.AuditInfo;
import com.scaler.price.rule.domain.constraint.CategoryConstraints;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.util.*;

@Entity
@Table(name = "categories")
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class Category extends AuditInfo {
    
    @Column(unique = true)
    private String name;
    
    private String displayName;
    private String description;
    private String imageUrl;
    private String metaTitle;
    private String metaDescription;
    
    private Long parentCategoryId;
    private Integer level;
    private Integer displayOrder;
    private Boolean isActive;
    
    @ElementCollection
    @CollectionTable(name = "category_site_ids")
    private Set<Long> siteIds = new HashSet<>();
    
    @ElementCollection
    @CollectionTable(name = "category_attributes")
    @MapKeyColumn(name = "attribute_key")
    @Column(name = "attribute_value")
    private Map<String, String> attributes = new HashMap<>();
    
    // Changed to match Product entity's categoryId field
    @OneToMany
    @JoinColumn(name = "categoryId")
    private List<Product> products = new ArrayList<>();
    
    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "category_id")
    private Map<String, ValidationRule> validationRules = new HashMap<>();
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "constraint_id")
    private CategoryConstraints constraints;
    
    @Entity
    @Table(name = "category_validation_rules")
    @Data
    @SuperBuilder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ValidationRule {
        @Id
        @GeneratedValue(strategy = GenerationType.IDENTITY)
        private Long id;
        
        private String ruleType;
        private String ruleValue;
        private Boolean isRequired;
        private String errorMessage;
        private Integer priority;
        
        @ElementCollection
        @CollectionTable(name = "validation_rule_parameters")
        @MapKeyColumn(name = "param_key")
        @Column(name = "param_value")
        private Map<String, String> parameters = new HashMap<>();
    }
}
