package com.scaler.price.rule.domain;

import com.vladmihalcea.hibernate.type.json.JsonBinaryType;
import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import org.hibernate.annotations.Type;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "products")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Product {
    @Id
    private String productId;

    @Column(nullable = false)
    private String productName;

    @Column(nullable = false)
    private String categoryId;

    private String brandId;

    @Column(nullable = false)
    private String sellerId;

    @ElementCollection
    @CollectionTable(
            name = "product_site_mappings",
            joinColumns = @JoinColumn(name = "product_id")
    )
    @Column(name = "site_id")
    private Set<String> siteIds = new HashSet<>();

    @Column(nullable = false)
    private BigDecimal mrp;

    private BigDecimal costPrice;

    @Column(nullable = false)
    private String currency;

    @Enumerated(EnumType.STRING)
    private ProductStatus status;

    @Type(JsonBinaryType.class)
    @Column(columnDefinition = "jsonb")
    private String attributes;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @Version
    private Long version;
}
