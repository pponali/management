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

@Data
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class ProductDTO {
    private String id;
    private String name;
    private String categoryId;
    private String brandId;
    private String sellerId;
    private boolean active;
    
    @Builder.Default
    private Set<String> siteIds = new HashSet<>();
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
