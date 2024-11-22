package com.scaler.price.rule.domain;

import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Embeddable
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PriceAdjustment {
    @Enumerated(EnumType.STRING)
    private AdjustmentType type;

    private BigDecimal value;
    private Boolean applyToBasePrice;
    private String alternateRuleId;

    public enum AdjustmentType {
        PERCENTAGE,
        FIXED_AMOUNT,
        USE_ALTERNATE_RULE
    }
}