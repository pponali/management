package com.scaler.price.rule.dto;

import com.scaler.price.rule.domain.RuleStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StatusChangeNotification {
    private Long ruleId;
    private String ruleName;
    private RuleStatus oldStatus;
    private RuleStatus newStatus;
    private String reason;
    private LocalDateTime timestamp;
    private Map<String, Object> additionalData;
}
