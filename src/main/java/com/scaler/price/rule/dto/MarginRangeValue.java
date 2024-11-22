package com.scaler.price.rule.dto;

import lombok.Data;
import lombok.Builder;
import java.math.BigDecimal;

@Data
@Builder
public class MarginRangeValue {
    private BigDecimal minMargin;
    private BigDecimal maxMargin;
}
