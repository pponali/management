package com.scaler.price.audit.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.scaler.price.audit.AuditEventPublisher;
import com.scaler.price.audit.domain.AuditEntry;
import com.scaler.price.audit.domain.AuditEventType;
import com.scaler.price.audit.exception.AuditSearchException;
import com.scaler.price.audit.repository.AuditEventRepository;
import com.scaler.price.core.management.dto.PriceEvent;
import com.scaler.price.core.management.service.SecurityService;
import com.scaler.price.rule.domain.AuditAction;
import com.scaler.price.rule.domain.ChangeDiff;
import com.scaler.price.rule.domain.PricingRule;
import com.scaler.price.rule.domain.RuleStatus;
import com.scaler.price.rule.domain.SellerSiteConfig;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuditService {
    private final AuditEventRepository auditRepository;
    private final SecurityService securityService;
    private final ObjectMapper objectMapper;
    private final AuditEventPublisher eventPublisher;

    @Transactional
    public void auditRuleCreation(PricingRule rule) throws AuditSearchException {
        log.debug("Auditing rule creation for rule: {}", rule.getId());
        try {
            AuditEntry audit = AuditEntry.builder()
                    .ruleId(rule.getId())
                    .ruleName(rule.getRuleName())
                    .ruleType(rule.getRuleType())
                    .action(AuditAction.CREATED)
                    .timestamp(Instant.now())
                    .userId(securityService.getCurrentUserId())
                    .build();

            saveAndPublish(audit);
        } catch (Exception e) {
            log.error("Error auditing rule creation for rule: {}", rule.getId(), e);
            throw new AuditSearchException("Failed to audit rule creation", e);
        }
    }

    @Transactional
    public void auditRuleUpdate(PricingRule newRule, PricingRule oldRule) throws AuditSearchException {
        log.debug("Auditing rule update for rule: {}", newRule.getId());
        try {
            Map<String, ChangeDiff> changes = detectChanges(oldRule, newRule);

            AuditEntry audit = AuditEntry.builder()
                    .ruleId(newRule.getId())
                    .ruleName(newRule.getRuleName())
                    .ruleType(newRule.getRuleType())
                    .action(AuditAction.UPDATED)
                    .changes(changes)
                    .snapshot(serializeRule(newRule))
                    .timestamp(Instant.now())
                    .userId(securityService.getCurrentUserId())
                    .build();

            saveAndPublish(audit);
        } catch (Exception e) {
            log.error("Error auditing rule update for rule: {}", newRule.getId(), e);
            throw new AuditSearchException("Failed to audit rule update", e);
        }
    }

    @Transactional
    public void auditRuleActivation(PricingRule rule, String reason) throws AuditSearchException {
        log.debug("Auditing rule activation for rule: {}", rule.getId());
        try {
            AuditEntry audit = AuditEntry.builder()
                    .ruleId(rule.getId())
                    .ruleName(rule.getRuleName())
                    .ruleType(rule.getRuleType())
                    .action(AuditAction.ACTIVATED)
                    .changes(Map.of("reason", new ChangeDiff(null, reason)))
                    .snapshot(serializeRule(rule))
                    .timestamp(Instant.now())
                    .userId(securityService.getCurrentUserId())
                    .build();

            saveAndPublish(audit);
        } catch (Exception e) {
            log.error("Error auditing rule activation for rule: {}", rule.getId(), e);
            throw new AuditSearchException("Failed to audit rule activation", e);
        }
    }

    @Transactional
    public void auditRuleDeactivation(PricingRule rule, String reason) throws AuditSearchException {
        log.debug("Auditing rule deactivation for rule: {}", rule.getId());
        try {
            AuditEntry audit = AuditEntry.builder()
                    .ruleId(rule.getId())
                    .ruleName(rule.getRuleName())
                    .ruleType(rule.getRuleType())
                    .action(AuditAction.DEACTIVATED)
                    .changes(Map.of("reason", new ChangeDiff(null, reason)))
                    .snapshot(serializeRule(rule))
                    .timestamp(Instant.now())
                    .userId(securityService.getCurrentUserId())
                    .build();

            saveAndPublish(audit);
        } catch (Exception e) {
            log.error("Error auditing rule deactivation for rule: {}", rule.getId(), e);
            throw new AuditSearchException("Failed to audit rule deactivation", e);
        }
    }

    @Transactional
    public void auditPriceOverride(
            PricingRule rule,
            BigDecimal oldPrice,
            BigDecimal newPrice,
            String reason) throws AuditSearchException {
        log.debug("Auditing price override for rule: {}", rule.getId());
        try {
            Map<String, ChangeDiff> changes = Map.of(
                    "price", new ChangeDiff(
                            oldPrice.toString(),
                            newPrice.toString()
                    ),
                    "reason", new ChangeDiff(null, reason)
            );

            AuditEntry audit = AuditEntry.builder()
                    .ruleId(rule.getId())
                    .ruleName(rule.getRuleName())
                    .ruleType(rule.getRuleType())
                    .action(AuditAction.PRICE_OVERRIDE)
                    .changes(changes)
                    .snapshot(serializeRule(rule))
                    .timestamp(Instant.now())
                    .userId(securityService.getCurrentUserId())
                    .build();

            saveAndPublish(audit);
        } catch (Exception e) {
            log.error("Error auditing price override for rule: {}", rule.getId(), e);
            throw new AuditSearchException("Failed to audit price override", e);
        }
    }

    @Transactional
    public void auditSellerSiteUpdate(
            PricingRule rule,
            Set<String> addedSellers,
            Set<String> removedSellers,
            Set<String> addedSites,
            Set<String> removedSites) throws AuditSearchException {
        log.debug("Auditing seller-site update for rule: {}", rule.getId());
        try {
            Map<String, ChangeDiff> changes = new HashMap<>();

            if (!addedSellers.isEmpty() || !removedSellers.isEmpty()) {
                changes.put("sellers", new ChangeDiff(
                        serializeSet(removedSellers),
                        serializeSet(addedSellers)
                ));
            }

            if (!addedSites.isEmpty() || !removedSites.isEmpty()) {
                changes.put("sites", new ChangeDiff(
                        serializeSet(removedSites),
                        serializeSet(addedSites)
                ));
            }

            AuditEntry audit = AuditEntry.builder()
                    .ruleId(rule.getId())
                    .ruleName(rule.getRuleName())
                    .ruleType(rule.getRuleType())
                    .action(AuditAction.SELLER_SITE_UPDATE)
                    .changes(changes)
                    .snapshot(serializeRule(rule))
                    .timestamp(Instant.now())
                    .userId(securityService.getCurrentUserId())
                    .build();

            saveAndPublish(audit);
        } catch (Exception e) {
            log.error("Error auditing seller-site update for rule: {}", rule.getId(), e);
            throw new AuditSearchException("Failed to audit seller-site update", e);
        }
    }

    @Transactional
    public void logPriceCreation(PriceEvent priceEvent) {
        log.debug("Auditing price creation event: {}", priceEvent);
        try {
            AuditEntry audit = AuditEntry.builder()
                    .ruleId(Long.parseLong(priceEvent.getRuleId()))
                    .ruleName(priceEvent.getRuleName())
                    .ruleType(priceEvent.getRuleType())
                    .action(AuditAction.PRICE_CREATED)
                    .timestamp(Instant.now())
                    .userId(securityService.getCurrentUserId())
                    .eventData(objectMapper.writeValueAsString(priceEvent))
                    .build();

            saveAndPublish(audit);
        } catch (Exception e) {
            log.error("Error auditing price creation event: {}", priceEvent, e);
            throw new RuntimeException("Failed to audit price creation", e);
        }
    }

    @Transactional
    public void logStatusChange(Long ruleId, RuleStatus oldStatus, RuleStatus newStatus, String reason) throws AuditSearchException {
        log.debug("Logging status change for rule: {} from {} to {}", ruleId, oldStatus, newStatus);
        try {
            Map<String, ChangeDiff> changes = new HashMap<>();
            changes.put("status", new ChangeDiff(
                    oldStatus != null ? oldStatus.name() : null,
                    newStatus != null ? newStatus.name() : null
            ));

            AuditEntry audit = AuditEntry.builder()
                    .ruleId(ruleId)
                    .action(AuditAction.STATUS_CHANGE)
                    .changes(changes)
                    .timestamp(Instant.now())
                    .userId(securityService.getCurrentUserId())
                    .comment(reason)
                    .build();

            saveAndPublish(audit);
        } catch (Exception e) {
            log.error("Error logging status change for rule: {}", ruleId, e);
            throw new AuditSearchException("Failed to log status change", e);
        }
    }

    private Map<String, ChangeDiff> detectChanges(
            PricingRule oldRule,
            PricingRule newRule) {
        Map<String, ChangeDiff> changes = new HashMap<>();

        // Basic fields
        detectChange(changes, "name", oldRule.getRuleName(), newRule.getRuleName());
        detectChange(changes, "description", oldRule.getDescription(), newRule.getDescription());
        detectChange(changes, "type", oldRule.getRuleType(), newRule.getRuleType());
        detectChange(changes, "priority", oldRule.getPriority(), newRule.getPriority());

        // Price bounds
        detectChange(changes, "minPrice", oldRule.getMinimumPrice(), newRule.getMinimumPrice());
        detectChange(changes, "maxPrice", oldRule.getMaximumPrice(), newRule.getMaximumPrice());
        detectChange(changes, "minMargin", oldRule.getMinimumMargin(), newRule.getMinimumMargin());
        detectChange(changes, "maxMargin", oldRule.getMaximumMargin(), newRule.getMaximumMargin());

        // Dates
        detectChange(changes, "effectiveFrom", oldRule.getEffectiveFrom(), newRule.getEffectiveFrom());
        detectChange(changes, "effectiveTo", oldRule.getEffectiveTo(), newRule.getEffectiveTo());

        // Collections
        detectCollectionChanges(changes, "sellers", oldRule.getSellerIds(), newRule.getSellerIds());
        detectCollectionChanges(changes, "sites", oldRule.getSiteIds(), newRule.getSiteIds());

        // Configurations
        detectConfigChanges(changes, oldRule.getSellerSiteConfigs(), newRule.getSellerSiteConfigs());

        return changes;
    }

    private void detectChange(
            Map<String, ChangeDiff> changes,
            String field,
            Object oldValue,
            Object newValue) {
        if (!Objects.equals(oldValue, newValue)) {
            changes.put(field, new ChangeDiff(
                    oldValue != null ? oldValue.toString() : null,
                    newValue != null ? newValue.toString() : null
            ));
        }
    }

    private void detectCollectionChanges(
            Map<String, ChangeDiff> changes,
            String field,
            Collection<?> oldValues,
            Collection<?> newValues) {
        if (!Objects.equals(oldValues, newValues)) {
            Set<?> removed = new HashSet<>(oldValues);
            removed.removeAll(newValues);

            Set<?> added = new HashSet<>(newValues);
            added.removeAll(oldValues);

            if (!removed.isEmpty() || !added.isEmpty()) {
                changes.put(field, new ChangeDiff(
                        serializeSet(removed),
                        serializeSet(added)
                ));
            }
        }
    }

    private void detectConfigChanges(
            Map<String, ChangeDiff> changes,
            Set<SellerSiteConfig> oldConfigs,
            Set<SellerSiteConfig> newConfigs) {
        Map<String, SellerSiteConfig> oldConfigMap = oldConfigs.stream()
                .collect(Collectors.toMap(
                        config -> config.getSellerId() + "-" + config.getSiteId(),
                        config -> config
                ));

        Map<String, SellerSiteConfig> newConfigMap = newConfigs.stream()
                .collect(Collectors.toMap(
                        config -> config.getSellerId() + "-" + config.getSiteId(),
                        config -> config
                ));

        Set<String> allKeys = new HashSet<>();
        allKeys.addAll(oldConfigMap.keySet());
        allKeys.addAll(newConfigMap.keySet());

        for (String key : allKeys) {
            SellerSiteConfig oldConfig = oldConfigMap.get(key);
            SellerSiteConfig newConfig = newConfigMap.get(key);

            if (oldConfig == null || newConfig == null || !oldConfig.equals(newConfig)) {
                changes.put("config-" + key, new ChangeDiff(
                        serializeConfig(oldConfig),
                        serializeConfig(newConfig)
                ));
            }
        }
    }

    private String serializeRule(PricingRule rule) {
        try {
            return objectMapper.writeValueAsString(rule);
        } catch (JsonProcessingException e) {
            log.error("Error serializing rule: {}", e.getMessage());
            return "{}";
        }
    }

    private String serializeSet(Collection<?> set) {
        if (set == null || set.isEmpty()) return "[]";
        try {
            return objectMapper.writeValueAsString(set);
        } catch (JsonProcessingException e) {
            log.error("Error serializing set: {}", e.getMessage());
            return "[]";
        }
    }

    private String serializeConfig(SellerSiteConfig config) {
        if (config == null) return null;
        try {
            return objectMapper.writeValueAsString(config);
        } catch (JsonProcessingException e) {
            log.error("Error serializing config: {}", e.getMessage());
            return "{}";
        }
    }

    private void saveAndPublish(AuditEntry audit) {
        try {
            auditRepository.save(audit);
            eventPublisher.publishAuditEvent(audit);
        } catch (Exception e) {
            log.error("Error saving audit entry: {}", e.getMessage(), e);
            // Consider adding retry logic or storing failed audits
        }
    }

    public void auditRuleUpdate(PricingRule savedRule) {


    }

    public List<AuditEntry> findByEventType(AuditEventType eventType) {
        return auditRepository.findByEventType(eventType);
    }

    public List<AuditEntry> findByUserId(String userId) {
        return auditRepository.findByUserId(userId);
    }

    public Map<String, Object> getEventStatistics(Instant startTime, Instant endTime) {
        List<AuditEntry> events = auditRepository.findByTimestampBetween(startTime, endTime);
        Map<String, Object> stats = new HashMap<>();

        // Initialize counters
        Map<AuditEventType, Long> eventTypeCounts = new HashMap<>();
        for (AuditEventType eventType : AuditEventType.values()) {
            eventTypeCounts.put(eventType, 0L);
        }

        // Count events by type
        for (AuditEntry event : events) {
            AuditEventType eventType = event.getEventType();
            eventTypeCounts.put(eventType, eventTypeCounts.get(eventType) + 1);
        }

        // Calculate total events
        long totalEvents = events.size();

        // Add counts and percentages to stats
        for (AuditEventType eventType : AuditEventType.values()) {
            long count = eventTypeCounts.get(eventType);
            double percentage = (totalEvents > 0) ? (count * 100.0) / totalEvents : 0.0;
            stats.put(eventType.name(), Map.of(
                    "count", count,
                    "percentage", percentage
            ));
        }

        return stats;
    }

    /**
     * Find audit entries based on complex criteria with pagination
     *
     * @param userId    Optional user ID to filter by
     * @param eventType Optional event type to filter by
     * @param startTime Optional start time for time range filter
     * @param endTime   Optional end time for time range filter
     * @param pageable  Pagination information
     * @return Page of audit entries matching the criteria
     */
    public Page<AuditEntry> findByComplexCriteria(String userId, AuditEventType eventType,
                                                  Instant startTime, Instant endTime, Pageable pageable) throws AuditSearchException {
        try {
            log.debug("Searching audit entries with criteria - userId: {}, eventType: {}, startTime: {}, endTime: {}",
                    userId, eventType, startTime, endTime);

            // Validate time range if both are provided
            if (startTime != null && endTime != null && startTime.isAfter(endTime)) {
                throw new IllegalArgumentException("Start time must be before end time");
            }

            // If no end time is provided but start time is, set end time to now
            if (startTime != null && endTime == null) {
                endTime = Instant.now();
            }

            // Use the repository method to fetch the data
            Page<AuditEntry> results = auditRepository.findByComplexCriteria(
                    userId, eventType, startTime, endTime, pageable);

            log.debug("Found {} audit entries matching criteria", results.getTotalElements());

            return results;
        } catch (IllegalArgumentException e) {
            log.error("Invalid criteria provided for audit search: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("Error searching audit entries: {}", e.getMessage(), e);
            throw new AuditSearchException("Failed to search audit entries", e);
        }
    }
}
