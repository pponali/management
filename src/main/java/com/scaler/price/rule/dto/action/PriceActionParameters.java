package com.scaler.price.rule.dto.action;

import com.scaler.price.rule.domain.PriceActionType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.Set;

@Setter
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PriceActionParameters {
    private BigDecimal price;
    private BigDecimal currentPrice;
    private BigDecimal costPrice;
    private BigDecimal minMarginPercentage;
    private String currency;
    private PriceActionType actionType;
    private RoundingStrategy roundingStrategy;
    private PriceRestrictions restrictions;

    @Getter
    @Setter
    @Builder
    public static class PriceRestrictions {
        private BigDecimal minimumPrice;
        private BigDecimal maximumPrice;
        private BigDecimal maximumChangePercentage;
        private Boolean allowPriceIncrease;
        private Set<String> excludedCategories;
    }

    public enum RoundingStrategy {
        NONE,
        ROUND_UP,
        ROUND_DOWN,
        ROUND_TO_NEAREST_5,
        ROUND_TO_NEAREST_10,
        ROUND_TO_NEAREST_100
    }
}