package com.scaler.price.rule.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "product_attributes")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductAttribute {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long productId;

    @Column(nullable = false)
    private Long attributeKey;

    @Column(nullable = false)
    private String attributeValue;

    @Column(nullable = false)
    private String attributeType;

    private String category;

    private String subCategory;

    private Boolean isSearchable;

    private Boolean isFilterable;

    private Integer displayOrder;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @Version
    private Long version;

    
}