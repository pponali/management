package com.scaler.price.core.management.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.HashMap;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RuleMetrics {
    private Long totalRules;
    private Long activeRules;
    private Long expiredRules;
    private Long priceRules;
    private Long discountRules;
    private Long priceConditions;
    private Long timeConditions;
    private Double activePercentage;
    @Builder.Default
    private Map<String, Long> ruleTypeCounts = new HashMap<>();
    @Builder.Default
    private Map<String, Long> conditionTypeCounts = new HashMap<>();
}
