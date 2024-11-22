// SalesVelocityValue.java
package com.scaler.price.rule.dto;

import lombok.Data;
import lombok.Builder;

@Data
@Builder
public class SalesVelocityValue {
    private Integer timeWindow;
    private Double minVelocity;
    private Double maxVelocity;
}
