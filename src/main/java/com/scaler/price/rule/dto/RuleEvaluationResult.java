package com.scaler.price.rule.dto;

import com.scaler.price.rule.domain.RuleType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RuleEvaluationResult {
    private Long ruleId;
    private String ruleName;
    private RuleType ruleType;
    private BigDecimal originalPrice;
    private BigDecimal adjustedPrice;
    private BigDecimal discountAmount;
    private BigDecimal marginPercentage;
    private String appliedReason;
    private Map<String, Object> metadata;
    private LocalDateTime evaluatedAt;
    private boolean success;
    private String errorMessage;

    public void addMetadata(String key, Object value) {
        if (metadata == null) {
            metadata = new HashMap<>();
        }
        metadata.put(key, value);
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }
}
