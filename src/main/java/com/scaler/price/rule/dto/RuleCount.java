package com.scaler.price.rule.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class RuleCount {
    private String siteId;
    private Long totalCount;
    private Long activeCount;
}