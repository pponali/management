package com.scaler.price.rule.domain.constraint;

import com.scaler.price.core.management.domain.AuditInfo;
import com.scaler.price.rule.dto.CategoryAttributes;
import com.scaler.price.rule.domain.RuleType;
import com.vladmihalcea.hibernate.type.json.JsonBinaryType;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Type;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "categories")
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper=false)
@Builder
public class CategoryConstraints extends RuleConstraints{
    @Id
    private String categoryId;

    @Column(nullable = false)
    private String categoryName;

    private String description;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_category_id")
    private CategoryConstraints parentCategory;

    @OneToMany(mappedBy = "parentCategory")
    private Set<CategoryConstraints> subCategories;

    @Column(nullable = false)
    private Integer level;

    @Column(nullable = false)
    private Boolean isActive;

    @Column(nullable = false)
    private String categoryPath;

    @Type(JsonBinaryType.class)
    @Column(columnDefinition = "jsonb")
    private CategoryAttributes attributes;

    @ElementCollection
    @CollectionTable(
            name = "category_site_mappings",
            joinColumns = @JoinColumn(name = "category_id")
    )
    @Column(name = "site_id")
    private Set<String> siteIds;

    @Column(nullable = false)
    private Integer displayOrder;

    @Embedded
    private AuditInfo auditInfo;

    @Version
    private Long version;

    @Builder(builderMethodName = "categoryConstraintsBuilder")
    public CategoryConstraints(String categoryId, String categoryName, String description,
                             BigDecimal minimumPrice, BigDecimal maximumPrice,
                             BigDecimal minimumMargin, BigDecimal maximumMargin,
                             LocalDateTime effectiveFrom, LocalDateTime effectiveTo,
                             Boolean isActive, Integer priority, RuleType ruleType,
                             Instant startDate, Instant endDate) {
        super(minimumPrice, maximumPrice, minimumMargin, maximumMargin,
              effectiveFrom, effectiveTo, isActive, priority, ruleType,
              startDate, endDate);
        this.categoryId = categoryId;
        this.categoryName = categoryName;
        this.description = description;
    }

    public void addSubCategory(CategoryConstraints subCategory) {
        if (subCategories == null) {
            subCategories = new HashSet<>();
        }
        subCategories.add(subCategory);
        subCategory.setParentCategory(this);
    }

    public void removeSubCategory(CategoryConstraints subCategory) {
        if (subCategories != null) {
            subCategories.remove(subCategory);
        }
        subCategory.setParentCategory(null);
    }

    @PrePersist
    @PreUpdate
    protected void onUpdate() {
        auditInfo.setUpdatedAt(LocalDateTime.now());
        if (auditInfo.getCreatedAt() == null) {
            auditInfo.setCreatedAt(auditInfo.getUpdatedAt());
        }
    }
}
