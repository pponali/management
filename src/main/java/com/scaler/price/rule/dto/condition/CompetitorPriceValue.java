package com.scaler.price.rule.dto.condition;

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
public class CompetitorPriceValue {
    private String competitorId;
    private BigDecimal priceThreshold;
    private BigDecimal differencePercentage;
    private ComparisonStrategy strategy;
    private Set<String> excludedProducts;
    private Integer priceValidityHours;

    public enum ComparisonStrategy {
        MATCH,            // Match exactly
        BEAT,             // Price below competitor
        LAG,              // Price above competitor
        WITHIN_RANGE      // Within percentage range
    }
}
