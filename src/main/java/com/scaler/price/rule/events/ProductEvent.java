package com.scaler.price.rule.events;

import com.scaler.price.rule.domain.Product;
import com.scaler.price.rule.domain.ProductEventType;
import com.scaler.price.rule.domain.RuleType;
import com.scaler.price.rule.dto.RuleEvaluationResult;
import lombok.Builder;

import java.time.LocalDateTime;
import java.util.Set;
@Builder
public class ProductEvent{


    public Throwable getEventId() {
        return null;
    }

    private String eventId;
    private ProductEventType eventType;
    private Long ruleId;
    private String ruleName;
    private RuleType ruleType;
    private Set<String> sellerIds;
    private Long productId;
    private Set<String> siteIds;
    private Long version;
    private LocalDateTime effectiveFrom;
    private LocalDateTime effectiveTo;
    private String status;
    private LocalDateTime timestamp;
    private Product payload;
    private String userId;
    private RuleEvaluationResult evaluationResult;
    private String failureReason;
}
