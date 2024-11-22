package com.scaler.price.core.management.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ConflictSummary {
    private Long ruleId1;
    private Long ruleId2;
    private String ruleName1;
    private String ruleName2;
    private String conflictType;
}
