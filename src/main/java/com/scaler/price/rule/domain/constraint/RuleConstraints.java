package com.scaler.price.rule.domain.constraint;

import com.scaler.price.rule.domain.RuleType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RuleConstraints {
    private BigDecimal minimumPrice;
    private BigDecimal maximumPrice;
    private BigDecimal minimumMargin;
    private BigDecimal maximumMargin;
    private LocalDateTime effectiveFrom;
    private LocalDateTime effectiveTo;
    private Boolean isActive;
    private Integer priority;
    private RuleType ruleType;
    private Instant startDate;
    private Instant endDate;

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