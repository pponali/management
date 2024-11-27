package com.scaler.price.validation.helper;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Setter
@Getter
public class MarginActionParameters extends ActionParameters {
    private final BigDecimal margin;
    private final BigDecimal costPrice;
    private final BigDecimal currentPrice;

    public MarginActionParameters(BigDecimal margin, BigDecimal costPrice, BigDecimal currentPrice) {
        this.margin = margin;
        this.costPrice = costPrice;
        this.currentPrice = currentPrice;
    }

}
