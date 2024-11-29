package com.scaler.price.rule.domain.constraint;


import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;


@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class SpecificRuleConstraints extends RuleConstraints {
    private BigDecimal minimumPrice;
    private BigDecimal maximumPrice;
    private BigDecimal minimumMargin;
    private BigDecimal maximumMargin;


    public boolean isValidPrice(BigDecimal price) {
        if (price == null) {
            return false;
        }
        
        if (minimumPrice != null && price.compareTo(minimumPrice) < 0) {
            return false;
        }
        
        if (maximumPrice != null && price.compareTo(maximumPrice) > 0) {
            return false;
        }
        
        return true;
    }

    public boolean isValidMargin(BigDecimal margin) {
        if (margin == null) {
            return false;
        }
        
        if (minimumMargin != null && margin.compareTo(minimumMargin) < 0) {
            return false;
        }
        
        if (maximumMargin != null && margin.compareTo(maximumMargin) > 0) {
            return false;
        }
        
        return true;
    }
}