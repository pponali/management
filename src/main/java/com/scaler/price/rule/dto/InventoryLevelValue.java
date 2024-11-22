// InventoryLevelValue.java
package com.scaler.price.rule.dto;

import lombok.Data;
import lombok.Builder;

@Data
@Builder
public class InventoryLevelValue {
    private Long level;
    private String warehouseId;
}
