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
    private Long productId;

    @NotBlank(message = "Seller ID is required")
    private Long sellerId;

    @NotBlank(message = "Site ID is required")
    private Long siteId;

    private Long categoryId;
    private Long brandId;

    @NotNull(message = "Base price is required")
    private BigDecimal basePrice;

    @NotNull(message = "Cost price is required")
    private BigDecimal costPrice;

    private Map<String, Object> attributes;

    private int quantity = 1; // Default quantity to 1 if not specified

    private String customerSegment;

    public int getQuantity() {
        return quantity;
    }

    public String getCustomerSegment() {
        return customerSegment;
    }
}