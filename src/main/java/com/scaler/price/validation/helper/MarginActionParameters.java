package com.scaler.price.validation.helper;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class MarginActionParameters {
    private final BigDecimal margin;
    private final BigDecimal costPrice;
    private final BigDecimal currentPrice;

    public MarginActionParameters(BigDecimal margin, BigDecimal costPrice, BigDecimal currentPrice) {
        this.margin = margin;
        this.costPrice = costPrice;
        this.currentPrice = currentPrice;
    }

}
