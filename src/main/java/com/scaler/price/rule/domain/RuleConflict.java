package com.scaler.price.rule.domain;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class RuleConflict {
    private Long rule1Id;
    private Long rule2Id;
    private String conflictType;
    private String description;
}