package com.scaler.price.validation.helper;

import lombok.Data;

import java.math.BigDecimal;


@Data
public class PriceActionParameters extends ActionParameters {
    private final BigDecimal currentPrice;
    private final BigDecimal price;
    private final BigDecimal costPrice;
    private final BigDecimal priceChangePercentage;
    private final BigDecimal minMarginPercentage;

    public PriceActionParameters(BigDecimal currentPrice, BigDecimal price, BigDecimal costPrice, BigDecimal priceChangePercentage, BigDecimal minMarginPercentage) {
        this.currentPrice = currentPrice;
        this.price = price;
        this.costPrice = costPrice;
        this.priceChangePercentage = priceChangePercentage;
        this.minMarginPercentage = minMarginPercentage;
    }

    public PriceActionParameters(BigDecimal price, BigDecimal currentPrice, BigDecimal costPrice, BigDecimal minMarginPercentage) {
        this.currentPrice = currentPrice;
        this.price = price;
        this.costPrice = costPrice;
        this.minMarginPercentage = minMarginPercentage;
        this.priceChangePercentage = calculatePriceChangePercentage(currentPrice, price);
    }

    public PriceActionParameters(BigDecimal price, BigDecimal currentPrice) {
        this.currentPrice = currentPrice;
        this.price = price;
        this.costPrice = null;
        this.minMarginPercentage = null;
        this.priceChangePercentage = calculatePriceChangePercentage(currentPrice, price);
    }

    private BigDecimal calculatePriceChangePercentage(BigDecimal currentPrice, BigDecimal price) {
        if (currentPrice == null || currentPrice.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }

        return price.subtract(currentPrice)
                .divide(currentPrice, 4, BigDecimal.ROUND_HALF_UP)
                .multiply(new BigDecimal("100"));
    }

}
