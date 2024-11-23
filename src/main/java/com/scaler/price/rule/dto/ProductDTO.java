package com.scaler.price.rule.dto;

import com.scaler.price.rule.domain.ProductStatus;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class ProductDTO {
    private String productId;
    private String productName;
    private String categoryId;
    private String brandId;
    private String sellerId;
    
    @Builder.Default
    private Set<String> siteIds = new HashSet<>();
    private BigDecimal mrp;
    private BigDecimal costPrice;
    private String currency;
    private ProductStatus status;
    
    @Builder.Default
    private Map<String, Object> attributes = new HashMap<>();
}
