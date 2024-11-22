package com.scaler.price.rule.dto;

import com.scaler.price.rule.domain.RuleAction;
import com.scaler.price.rule.domain.RuleCondition;
import com.scaler.price.rule.domain.RuleType;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.Set;

@Data
@Builder
public class RuleDTO {
    private Long id;
    private String name;
    private String description;
    private RuleType type;
    private String sellerId;
    private String siteId;
    private String categoryId;
    private String brandId;
    private Set<RuleCondition> conditions;
    private Set<RuleAction> actions;
    private Integer priority;
    private LocalDateTime effectiveFrom;
    private LocalDateTime effectiveTo;
    private Boolean isActive;
}
