package com.scaler.price.rule.domain;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import jakarta.persistence.*;

import com.scaler.price.core.management.domain.AuditInfo;

@Entity
@Table(name = "product_attributes")
@Getter
@Setter
@SuperBuilder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class ProductAttribute extends AuditInfo {
    @Column(nullable = false)
    private Long productId;

    @Column(nullable = false)
    private Long attributeKey;

    @Column(nullable = false, columnDefinition = "jsonb")
    private String attributeValue;

    @Column(nullable = false)
    private String attributeType;

    private Long category;

    private String subCategory;

    private Boolean isSearchable;

    private Boolean isFilterable;

    private Integer displayOrder;
}