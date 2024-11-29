package com.scaler.price.rule.domain.constraint;

import com.scaler.price.rule.domain.RuleType;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;

public class PricingRuleConstraints extends RuleConstraints {
    private final BigDecimal minimumPrice;
    private final BigDecimal maximumPrice;
    private final BigDecimal minimumMargin;
    private final BigDecimal maximumMargin;
    private final LocalDateTime effectiveFrom;
    private final LocalDateTime effectiveTo;
    private final RuleType ruleType;
    private final Boolean isActive;
    private final Integer priority;
    private final Instant startDate;
    private final Instant endDate;

    public PricingRuleConstraints(
            BigDecimal minimumPrice,
            BigDecimal maximumPrice,
            BigDecimal minimumMargin,
            BigDecimal maximumMargin,
            LocalDateTime effectiveFrom,
            LocalDateTime effectiveTo,
            RuleType ruleType,
            Boolean isActive,
            Integer priority,
            Instant startDate,
            Instant endDate
    ) {
        super(minimumPrice, maximumPrice, minimumMargin, maximumMargin, 
              effectiveFrom, effectiveTo, ruleType, isActive, priority, 
              startDate, endDate, null); // Added null for categoryId
        this.minimumPrice = minimumPrice;
        this.maximumPrice = maximumPrice;
        this.minimumMargin = minimumMargin;
        this.maximumMargin = maximumMargin;
        this.effectiveFrom = effectiveFrom;
        this.effectiveTo = effectiveTo;
        this.ruleType = ruleType;
        this.isActive = isActive;
        this.priority = priority;
        this.startDate = startDate;
        this.endDate = endDate;
    }


    public boolean isValid() {
        // Add validation logic if needed
        return getActive() != null && getActive();
    }
}
