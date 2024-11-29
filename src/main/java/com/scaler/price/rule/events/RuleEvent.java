package com.scaler.price.rule.events;

import com.scaler.price.rule.domain.RuleType;
import com.scaler.price.rule.dto.RuleEvaluationResult;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;


import java.time.LocalDateTime;
import java.util.Set;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RuleEvent {
    private String eventId;
    private RuleEventType eventType;
    private Long ruleId;
    private String ruleName;
    private RuleType ruleType;
    private Set<Long> sellerIds;
    private Set<Long> siteIds;
    private Long version;
    private LocalDateTime effectiveFrom;
    private LocalDateTime effectiveTo;
    private String status;
    private LocalDateTime timestamp;
    private String payload;
    private String userId;
    private RuleEvaluationResult evaluationResult;
    private String failureReason;
}