package com.scaler.price.audit.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.scaler.price.audit.AuditEventPublisher;
import com.scaler.price.audit.domain.AuditEntry;
import com.scaler.price.audit.domain.AuditEventType;
import com.scaler.price.audit.exception.AuditEventPublishException;
import com.scaler.price.audit.repository.AuditEventRepository;
import com.scaler.price.rule.domain.PricingRule;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuditEventPublisherImpl implements AuditEventPublisher {

    private final ApplicationEventPublisher eventPublisher;
    private final AuditEventRepository auditEventRepository;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional
    public void publishRuleCreatedEvent(PricingRule rule, String userId) {
        log.debug("Publishing rule created event for rule: {} by user: {}", rule.getId(), userId);
        
        Map<String, Object> eventData = createBaseEventData(AuditEventType.RULE_CREATED, userId);
        eventData.put("ruleId", rule.getId());
        eventData.put("ruleName", rule.getRuleName());
        eventData.put("ruleType", rule.getRuleType());
        eventData.put("priority", rule.getPriority());
        
        publishAndPersistEvent(eventData);
    }

    @Override
    @Transactional
    public void publishRuleModifiedEvent(PricingRule rule, String userId, Map<String, String> changes) {
        log.debug("Publishing rule modified event for rule: {} by user: {}", rule.getId(), userId);
        
        Map<String, Object> eventData = createBaseEventData(AuditEventType.RULE_MODIFIED, userId);
        eventData.put("ruleId", rule.getId());
        eventData.put("ruleName", rule.getRuleName());
        eventData.put("changes", changes);
        eventData.put("modificationTime", Instant.now());
        
        publishAndPersistEvent(eventData);
    }

    @Override
    @Transactional
    public void publishRuleDeletedEvent(String ruleId, String userId) {
        log.debug("Publishing rule deleted event for rule: {} by user: {}", ruleId, userId);
        
        Map<String, Object> eventData = createBaseEventData(AuditEventType.RULE_DELETED, userId);
        eventData.put("ruleId", ruleId);
        eventData.put("deletionTime", Instant.now());
        
        publishAndPersistEvent(eventData);
    }

    @Override
    @Transactional
    public void publishRuleActivatedEvent(PricingRule rule, String userId) {
        log.debug("Publishing rule activated event for rule: {} by user: {}", rule.getId(), userId);
        
        Map<String, Object> eventData = createBaseEventData(AuditEventType.RULE_ACTIVATED, userId);
        eventData.put("ruleId", rule.getId());
        eventData.put("ruleName", rule.getRuleName());
        eventData.put("activationTime", Instant.now());
        eventData.put("validFrom", rule.getStartDate());
        eventData.put("validTo", rule.getEndDate());
        
        publishAndPersistEvent(eventData);
    }

    @Override
    @Transactional
    public void publishRuleDeactivatedEvent(PricingRule rule, String userId) {
        log.debug("Publishing rule deactivated event for rule: {} by user: {}", rule.getId(), userId);
        
        Map<String, Object> eventData = createBaseEventData(AuditEventType.RULE_DEACTIVATED, userId);
        eventData.put("ruleId", rule.getId());
        eventData.put("ruleName", rule.getRuleName());
        eventData.put("deactivationTime", Instant.now());
        eventData.put("reason", "User initiated deactivation");
        
        publishAndPersistEvent(eventData);
    }

    @Override
    @Transactional
    public void publishPriceCalculationEvent(String ruleId, String productId, 
                                           double originalPrice, double calculatedPrice, 
                                           String userId) {
        log.debug("Publishing price calculation event for product: {} using rule: {}", 
                 productId, ruleId);
        
        Map<String, Object> eventData = createBaseEventData(AuditEventType.PRICE_CALCULATION, userId);
        eventData.put("ruleId", ruleId);
        eventData.put("productId", productId);
        eventData.put("originalPrice", originalPrice);
        eventData.put("calculatedPrice", calculatedPrice);
        eventData.put("priceDifference", calculatedPrice - originalPrice);
        eventData.put("percentageChange", 
                     ((calculatedPrice - originalPrice) / originalPrice) * 100);
        
        publishAndPersistEvent(eventData);
    }

    @Override
    @Transactional
    public void publishRuleValidationEvent(PricingRule rule, boolean validationResult, 
                                         String userId) {
        log.debug("Publishing rule validation event for rule: {} with result: {}", 
                 rule.getId(), validationResult);
        
        Map<String, Object> eventData = createBaseEventData(AuditEventType.RULE_VALIDATION, userId);
        eventData.put("ruleId", rule.getId());
        eventData.put("ruleName", rule.getRuleName());
        eventData.put("validationResult", validationResult);
        eventData.put("validationTime", Instant.now());
        
        publishAndPersistEvent(eventData);
    }

    @Override
    @Transactional
    public void publishRuleExecutionEvent(PricingRule rule, String executionResult, 
                                        String userId) {
        log.debug("Publishing rule execution event for rule: {} with result: {}", 
                 rule.getId(), executionResult);
        
        Map<String, Object> eventData = createBaseEventData(AuditEventType.RULE_EXECUTION, userId);
        eventData.put("ruleId", rule.getId());
        eventData.put("ruleName", rule.getRuleName());
        eventData.put("executionResult", executionResult);
        eventData.put("executionTime", Instant.now());
        
        publishAndPersistEvent(eventData);
    }

    @Override
    @Transactional
    public void publishBulkOperationEvent(String operationType, int affectedRules, 
                                        String userId) {
        log.debug("Publishing bulk operation event of type: {} affecting {} rules", 
                 operationType, affectedRules);
        
        Map<String, Object> eventData = createBaseEventData(AuditEventType.BULK_OPERATION, userId);
        eventData.put("operationType", operationType);
        eventData.put("affectedRules", affectedRules);
        eventData.put("operationTime", Instant.now());
        
        publishAndPersistEvent(eventData);
    }

    @Override
    @Transactional
    public void publishRuleApprovalEvent(PricingRule rule, String status, 
                                       String approver, String comments) {
        log.debug("Publishing rule approval event for rule: {} with status: {}", 
                 rule.getId(), status);
        
        Map<String, Object> eventData = createBaseEventData(AuditEventType.RULE_APPROVAL, approver);
        eventData.put("ruleId", rule.getId());
        eventData.put("ruleName", rule.getRuleName());
        eventData.put("approvalStatus", status);
        eventData.put("approver", approver);
        eventData.put("comments", comments);
        eventData.put("approvalTime", Instant.now());
        
        publishAndPersistEvent(eventData);
    }

    @Override
    public void publishAuditEvent(AuditEntry audit) {

    }

    private Map<String, Object> createBaseEventData(AuditEventType eventType, String userId) {
        Map<String, Object> eventData = new HashMap<>();
        eventData.put("eventType", eventType.name());
        eventData.put("timestamp", Instant.now());
        eventData.put("userId", userId);
        eventData.put("source", "PRICING_ENGINE");
        return eventData;
    }

    private void publishAndPersistEvent(Map<String, Object> eventData) {
        try {
            // Create and persist audit event
            AuditEntry auditEvent = AuditEntry.builder()
                    .type(AuditEventType.valueOf((String) eventData.get("eventType")))
                    .userId((String) eventData.get("userId"))
                    .data(objectMapper.writeValueAsString(eventData))
                    .source((String) eventData.get("source"))
                    .comment("Auto-generated audit event")
                    .createdAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .eventTime(LocalDateTime.now())
                    .build();
            
            auditEventRepository.save(auditEvent);

            // Publish event for real-time processing
            eventPublisher.publishEvent(new AuditEventMessage(eventData));
            
            log.debug("Successfully published and persisted audit event: {}", eventData);
        } catch (Exception e) {
            log.error("Error publishing audit event: {}", e.getMessage(), e);
            throw new AuditEventPublishException("Failed to publish audit event", e);
        }
    }

    /**
     * Internal message class for audit events
     */
    private static class AuditEventMessage {
        private final Map<String, Object> eventData;

        public AuditEventMessage(Map<String, Object> eventData) {
            this.eventData = eventData;
        }

        public Map<String, Object> getEventData() {
            return eventData;
        }
    }
}
