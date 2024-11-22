package com.scaler.price.rule.domain.constraint;

import com.scaler.price.core.management.domain.AuditInfo;
import com.scaler.price.rule.dto.CategoryAttributes;
import com.vladmihalcea.hibernate.type.json.JsonBinaryType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Type;

import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "categories")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
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
    @Builder.Default
    private Set<CategoryConstraints> subCategories = new HashSet<>();

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
    @Builder.Default
    private Set<String> siteIds = new HashSet<>();

    @Column(nullable = false)
    private Integer displayOrder;

    @Embedded
    private AuditInfo auditInfo;

    @Version
    private Long version;

    public void addSubCategory(CategoryConstraints subCategory) {
        subCategories.add(subCategory);
        subCategory.setParentCategory(this);
    }

    public void removeSubCategory(CategoryConstraints subCategory) {
        subCategories.remove(subCategory);
        subCategory.setParentCategory(null);
    }
}
