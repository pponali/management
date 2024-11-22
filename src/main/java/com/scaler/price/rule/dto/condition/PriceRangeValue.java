package com.scaler.price.rule.dto.condition;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PriceRangeValue {
    private BigDecimal minPrice;
    private BigDecimal maxPrice;
    private String currency;
    private PriceRangeType rangeType;

    public enum PriceRangeType {
        ABSOLUTE,          // Direct price values
        PERCENTAGE_BASE,   // Percentage of base price
        PERCENTAGE_MRP     // Percentage of MRP
    }
}