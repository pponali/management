package com.scaler.price.rule.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RuleEvaluationRequest {
    @NotBlank(message = "Product ID is required")
    private String productId;

    @NotBlank(message = "Seller ID is required")
    private String sellerId;

    @NotBlank(message = "Site ID is required")
    private String siteId;

    private String categoryId;
    private String brandId;

    @NotNull(message = "Base price is required")
    private BigDecimal basePrice;

    @NotNull(message = "Cost price is required")
    private BigDecimal costPrice;

    private Map<String, Object> attributes;
}