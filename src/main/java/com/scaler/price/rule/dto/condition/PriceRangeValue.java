package com.scaler.price.rule.dto.condition;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Setter
@Getter
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