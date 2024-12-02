package com.scaler.price.rule.domain.constraint;


import com.scaler.price.rule.dto.CategoryAttributes;
import com.vladmihalcea.hibernate.type.json.JsonBinaryType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.Type;

import java.util.HashSet;
import java.util.Set;

@Entity
@DiscriminatorValue("category_constraints")
@Getter
@Setter
@SuperBuilder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class CategoryConstraints extends RuleConstraints{

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
    private Set<Long> siteIds;

    @Column(nullable = false)
    private Integer displayOrder;



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

    public void setParentCategory(CategoryConstraints parentCategory) {
        this.parentCategory = parentCategory;
    }
}
