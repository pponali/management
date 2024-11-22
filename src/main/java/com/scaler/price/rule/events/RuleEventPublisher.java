package com.scaler.price.rule.events;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.scaler.price.core.management.utils.PriceServiceMetrics;
import com.scaler.price.rule.domain.FailedEventEntity;
import com.scaler.price.rule.domain.PricingRule;
import com.scaler.price.rule.dto.RuleEvaluationResult;
import com.scaler.price.rule.repository.FailedEventRepository;
import com.scaler.price.rule.service.NotificationService;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryConfig;
import io.github.resilience4j.retry.RetryRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.kafka.common.KafkaException;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.scheduling.annotation.Async;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

import static com.scaler.price.rule.events.RuleEventType.*;

// com.scaler.price.rules.event.RuleEventPublisher.java
@Service
@Slf4j
@RequiredArgsConstructor
public class RuleEventPublisher {
    private final KafkaTemplate<String, RuleEvent> kafkaTemplate;
    private final ObjectMapper objectMapper;
    private final PriceServiceMetrics metricsService;
    private final FailedEventRepository failedEventRepository;
    private final NotificationService notificationService;

    private static final String RULE_EVENTS_TOPIC = "price-rule-events";
    private static final String RULE_AUDIT_TOPIC = "price-rule-audit";

    public void publishRuleCreated(PricingRule rule) {
        publishEvent(buildEvent(rule, CREATED));
    }

    public void publishRuleUpdated(PricingRule rule) {
        publishEvent(buildEvent(rule, UPDATED));
    }

    public void publishRuleDeleted(PricingRule rule) {
        publishEvent(buildEvent(rule, DELETED));
    }

    public void publishRuleActivated(PricingRule rule) {
        publishEvent(buildEvent(rule, RuleEventType.ACTIVATED));
    }

    public void publishRuleDeactivated(PricingRule rule) {
        publishEvent(buildEvent(rule, RuleEventType.DEACTIVATED));
    }

    public void publishRuleEvaluated(PricingRule rule, RuleEvaluationResult result) {
        RuleEvent event = buildEvent(rule, RuleEventType.EVALUATED);
        event.setEvaluationResult(result);
        publishEvent(event);
    }

    public void publishRuleValidationFailed(PricingRule rule, String reason) {
        RuleEvent event = buildEvent(rule, RuleEventType.VALIDATION_FAILED);
        event.setFailureReason(reason);
        publishEvent(event);
    }

    private RuleEvent buildEvent(PricingRule rule, RuleEventType eventType) {
        return RuleEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .eventType(eventType)
                .ruleId(rule.getId())
                .ruleName(rule.getRuleName())
                .ruleType(rule.getRuleType())
                .sellerIds(rule.getSellerIds())
                .siteIds(rule.getSiteIds())
                .version(rule.getVersion())
                .effectiveFrom(rule.getEffectiveFrom())
                .effectiveTo(rule.getEffectiveTo())
                .status(rule.getIsActive() ? "ACTIVE" : "INACTIVE")
                .timestamp(LocalDateTime.now())
                .payload(serializeRule(rule))
                .userId(getCurrentUserId())
                .build();
    }

    private void publishEvent(RuleEvent event) {
        try {
            // Send to main events topic
            kafkaTemplate.send(RULE_EVENTS_TOPIC, event.getRuleId().toString(), event)
                    .whenComplete((result, ex) -> {
                        if (ex == null) {
                            handleSuccess(event, result);
                        } else {
                            handleFailure(event, ex);
                        }
                    });

            // Send to audit topic if it's an auditable event
            if (isAuditableEvent(event.getEventType())) {
                kafkaTemplate.send(RULE_AUDIT_TOPIC, event.getRuleId().toString(), event);
            }

            // Record metrics
            recordEventMetrics(event);

        } catch (Exception e) {
            log.error("Failed to publish rule event: {}", e.getMessage(), e);
            metricsService.incrementEventPublishFailure(event.getEventType());
            handleEventPublishingFailure(event, e);
        }
    }

    private void handleSuccess(RuleEvent event, SendResult<String, RuleEvent> result) {
        if (result != null && result.getRecordMetadata() != null) {
            log.debug("Published rule event: {} partition: {} offset: {}",
                    event.getEventId(),
                    result.getRecordMetadata().partition(),
                    result.getRecordMetadata().offset()
            );
            metricsService.incrementEventPublishSuccess(event.getEventType());
        }
    }

    private void handleFailure(RuleEvent event, Throwable ex) {
        log.error("Failed to publish rule event: {} error: {}",
                event.getEventId(),
                ex.getMessage()
        );
        metricsService.incrementEventPublishFailure(event.getEventType());
        handleEventPublishingFailure(event, ex);
    }

    private boolean isAuditableEvent(RuleEventType eventType) {
        return switch (eventType) {
            case CREATED, UPDATED, DELETED, ACTIVATED,
                 DEACTIVATED, VALIDATION_FAILED -> true;
            default -> false;
        };
    }

    private void recordEventMetrics(RuleEvent event) {
        metricsService.recordEventPublished(
                event.getEventType(),
                event.getRuleType(),
                event.getSellerIds().size(),
                event.getSiteIds().size()
        );
    }

    private String serializeRule(PricingRule rule) {
        try {
            return objectMapper.writeValueAsString(rule);
        } catch (JsonProcessingException e) {
            log.error("Error serializing rule: {}", e.getMessage());
            return "{}";
        }
    }

    private String getCurrentUserId() {
        return SecurityContextHolder.getContext()
                .getAuthentication()
                .getName();
    }

    @Async
    protected void handleEventPublishingFailure(RuleEvent event, Throwable ex) {
        try {
            // Store failed event in dead letter queue
            storeFailedEvent(event, ex);

            // Notify appropriate teams
            notifyFailure(event, ex);

            // Attempt recovery if possible
            if (shouldRetryEvent(event)) {
                scheduleEventRetry(event);
            }

        } catch (Exception e) {
            log.error("Error handling event failure: {}", e.getMessage(), e);
        }
    }

    private void storeFailedEvent(RuleEvent event, Throwable ex) {
        FailedEventEntity failedEvent = FailedEventEntity.builder()
                .eventId(event.getEventId())
                .eventType(event.getEventType())
                .ruleId(event.getRuleId())
                .payload(serializeEvent(event))
                .errorMessage(ex.getMessage())
                .stackTrace(getStackTrace(ex))
                .failureTimestamp(LocalDateTime.now())
                .retryCount(0)
                .status("FAILED")
                .build();

        failedEventRepository.save(failedEvent);
    }

    private void notifyFailure(RuleEvent event, Throwable ex) {
        NotificationEvent notification = NotificationEvent.builder()
                .type("RULE_EVENT_FAILURE")
                .severity("HIGH")
                .source("RuleEventPublisher")
                .description("Failed to publish rule event: " + event.getEventId())
                .details(Map.of(
                        "ruleId", event.getRuleId(),
                        "eventType", event.getEventType(),
                        "error", ex.getMessage()
                ))
                .timestamp(LocalDateTime.now())
                .build();

        notificationService.sendNotification(notification);
    }

    private boolean shouldRetryEvent(RuleEvent event) {
        return switch (event.getEventType()) {
            case CREATED, UPDATED, DELETED -> true;
            default -> false;
        };
    }

    @Async
    private void scheduleEventRetry(RuleEvent event) {
        RetryConfig retryConfig = RetryConfig.custom()
                .maxAttempts(3)
                .waitDuration(Duration.ofSeconds(5))
                .retryExceptions(KafkaException.class)
                .build();

        Retry retry = RetryRegistry.of(retryConfig)
                .retry("rule-event-retry");

        retry.executeRunnable(() -> publishEvent(event));
    }

    private String serializeEvent(RuleEvent event) {
        try {
            return objectMapper.writeValueAsString(event);
        } catch (JsonProcessingException e) {
            log.error("Error serializing event: {}", e.getMessage());
            return "{}";
        }
    }

    private String getStackTrace(Throwable ex) {
        return ExceptionUtils.getStackTrace(ex);
    }
}
