package com.scaler.price.rule.domain.constraint;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;

import com.scaler.price.rule.domain.RuleType;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;

@Entity
@AllArgsConstructor
@Table(name = "pricing_rule_constraints")
public class PricingRuleConstraints extends RuleConstraints {
    // You can add any additional fields or methods specific to pricing rules
    
    public PricingRuleConstraints(BigDecimal minimumPrice, BigDecimal maximumPrice, 
                                   BigDecimal minimumMargin, BigDecimal maximumMargin,
                                   LocalDateTime effectiveFrom, LocalDateTime effectiveTo,
                                   RuleType ruleType, Boolean isActive, Integer priority,
                                   Instant startDate, Instant endDate) {
        super(minimumPrice, maximumPrice, minimumMargin, maximumMargin, 
              effectiveFrom, effectiveTo, ruleType, isActive, priority, 
              startDate, endDate);
    }
}