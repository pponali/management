package com.scaler.price.rule.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
public class RuleSiteSummary {
    private Long siteId;
    private Long totalRules;
    private Long activeRules;
    private LocalDateTime earliestRule;
    private LocalDateTime latestRule;

    public Double getActivePercentage() {
        if (totalRules == 0) return 0.0;
        return (activeRules * 100.0) / totalRules;
    }
}
