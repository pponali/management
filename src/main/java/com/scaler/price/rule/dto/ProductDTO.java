package com.scaler.price.rule.dto;

import com.scaler.price.rule.domain.ProductStatus;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Set;

@Data
@Builder
public class ProductDTO {
    private String productId;
    private String productName;
    private String categoryId;
    private String brandId;
    private String sellerId;
    private Set<String> siteIds;
    private BigDecimal mrp;
    private BigDecimal costPrice;
    private String currency;
    private ProductStatus status;
    private Map<String, Object> attributes;
}
