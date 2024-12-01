package com.scaler.price.rule.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.scaler.price.rule.domain.Product.ProductStatus;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductDTO {
    private Long id;
    private String name;
    private Long categoryId;
    private Long brandId;
    private Long sellerId;
    private boolean active;

    private BrandDTO brand;
    
    @Builder.Default
    private Set<Long> siteIds = new HashSet<>();
    private BigDecimal mrp;
    private BigDecimal costPrice;
    private String currency;
    private ProductStatus status;
    private BigDecimal basePrice;
    private BigDecimal sellingPrice;
    
    @Builder.Default
    private Map<String, Object> attributes = new HashMap<>();

    public boolean hasAttribute(String attributeName) {
        return attributes.containsKey(attributeName);
    }
}
