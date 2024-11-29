package com.scaler.price.rule.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.scaler.price.rule.domain.ChangeType;
import com.scaler.price.rule.domain.PricingRule;
import com.scaler.price.rule.domain.RuleHistory;
import com.scaler.price.rule.domain.RuleStatus;
import com.scaler.price.rule.repository.RuleHistoryRepository;
import com.scaler.price.rule.repository.RuleRepository;
import com.scaler.price.rule.service.RuleHistoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class RuleHistoryServiceImpl implements RuleHistoryService {

    private final RuleHistoryRepository historyRepository;
    private final RuleRepository ruleRepository;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional
    public RuleHistory recordRuleChange(PricingRule rule, String changeType, String userId, String comment) {
        log.info("Recording rule change: {} for rule: {} by user: {}", changeType, rule.getId(), userId);
        
        try {
            RuleHistory history = RuleHistory.builder()
                    .ruleId(rule.getId())
                    .ruleVersion(getNextVersion(rule.getId()))
                    .changeType(new ChangeType(changeType))
                    .userId(userId)
                    .comment(comment)
                    .timestamp(LocalDateTime.now())
                    .ruleSnapshot(objectMapper.writeValueAsString(rule).getBytes())
                    .build();
            
            return historyRepository.save(history);
        } catch (Exception e) {
            log.error("Error recording rule change: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to record rule change", e);
        }
    }

    @Override
    public Page<RuleHistory> getRuleHistory(Long ruleId, Pageable pageable) {
        log.debug("Retrieving history for rule: {}", ruleId);
        return historyRepository.findByRuleIdOrderByTimestampDesc(ruleId, pageable);
    }

    @Override
    public Page<RuleHistory> getRuleHistory(String ruleId, Pageable pageable) {
        log.debug("Retrieving history for rule: {}", ruleId);
        return historyRepository.findByRuleIdStringOrderByTimestampDesc(ruleId, pageable);
    }

    @Override
    public Optional<PricingRule> getRuleVersion(String ruleId, Integer version) {
        log.debug("Retrieving version {} of rule: {}", version, ruleId);
        
        return historyRepository.findByRuleIdStringAndVersion(ruleId, version)
                .map(history -> {
                    try {
                        return objectMapper.readValue(history.getRuleSnapshot(), PricingRule.class);
                    } catch (Exception e) {
                        log.error("Error deserializing rule snapshot: {}", e.getMessage(), e);
                        return null;
                    }
                });
    }

    @Override
    public List<String> compareVersions(String ruleId, Integer version1, Integer version2) {
        log.debug("Comparing versions {} and {} of rule: {}", version1, version2, ruleId);
        
        Optional<PricingRule> rule1 = getRuleVersion(ruleId, version1);
        Optional<PricingRule> rule2 = getRuleVersion(ruleId, version2);

        if (rule1.isEmpty() || rule2.isEmpty()) {
            throw new IllegalArgumentException("One or both versions not found");
        }

        return compareRules(rule1.get(), rule2.get());
    }

    @Override
    public Page<RuleHistory> getUserChanges(String userId, Pageable pageable) {
        log.debug("Retrieving changes made by user: {}", userId);
        return historyRepository.findByUserIdOrderByTimestampDesc(userId, pageable);
    }

    @Override
    public Page<RuleHistory> getChangesByTimeRange(LocalDateTime startTime, LocalDateTime endTime, Pageable pageable) {
        log.debug("Retrieving changes between {} and {}", startTime, endTime);
        return historyRepository.findByTimestampBetweenOrderByTimestampDesc(startTime, endTime, pageable);
    }

    @Override
    public Page<RuleHistory> getRecentChanges(Pageable pageable) {
        log.debug("Retrieving recent changes");
        return historyRepository.findAllByOrderByTimestampDesc(pageable);
    }

    @Override
    @Transactional
    public PricingRule restoreVersion(String ruleId, Integer version, String userId, String comment) {
        log.info("Restoring rule {} to version {} by user {}", ruleId, version, userId);
        
        Optional<PricingRule> oldVersion = getRuleVersion(ruleId, version);
        if (oldVersion.isEmpty()) {
            throw new IllegalArgumentException("Version not found");
        }

        PricingRule restoredRule = oldVersion.get();
        restoredRule.setLastModifiedBy(userId);
        restoredRule.setLastModifiedAt(LocalDateTime.now());
        
        PricingRule savedRule = ruleRepository.save(restoredRule);
        recordRuleChange(savedRule, "RESTORE", userId, 
                String.format("Restored to version %d. %s", version, comment));
        
        return savedRule;
    }

    @Override
    @Transactional
    public int archiveOldRecords(LocalDateTime cutoffDate) {
        log.info("Archiving records older than {}", cutoffDate);
        return historyRepository.archiveRecordsOlderThan(cutoffDate);
    }

    @Override
    public Page<RuleHistory> getProductRelatedChanges(String productId, Pageable pageable) {
        log.debug("Retrieving changes related to product: {}", productId);
        return historyRepository.findByProductId(productId, pageable);
    }

    @Override
    public List<RuleHistory> getRuleApprovalHistory(String ruleId) {
        log.debug("Retrieving approval history for rule: {}", ruleId);
        return historyRepository.findByRuleIdAndChangeTypeInOrderByTimestampDesc(
                ruleId, 
                Arrays.asList("SUBMIT_FOR_APPROVAL", "APPROVE", "REJECT"));
    }

    @Override
    public List<RuleHistory> getBatchOperationHistory(String batchId) {
        log.debug("Retrieving history for batch operation: {}", batchId);
        return historyRepository.findByBatchIdOrderByTimestampDesc(batchId);
    }

    @Override
    public void logRuleStatusChange(Long ruleId, RuleStatus status) {
        log.info("Logging status change for rule {}: {}", ruleId, status);
        Optional<PricingRule> ruleOpt = ruleRepository.findById(ruleId);
        if (ruleOpt.isPresent()) {
            recordRuleChange(ruleOpt.get(), "STATUS_CHANGE", "SYSTEM", 
                    String.format("Rule status changed to %s", status));
        }
    }

    @Override
    public void logRuleDeletion(Long ruleId) {
        log.info("Logging deletion for rule {}", ruleId);
        Optional<PricingRule> ruleOpt = ruleRepository.findById(ruleId);
        if (ruleOpt.isPresent()) {
            recordRuleChange(ruleOpt.get(), "DELETE", "SYSTEM", "Rule deleted");
        }
    }

    @Override
    public void logRuleUpdate(PricingRule updatedRule) {
        log.info("Logging update for rule {}", updatedRule.getId());
        recordRuleChange(updatedRule, "UPDATE", "SYSTEM", "Rule updated");
    }

    @Override
    public void logRuleCreation(PricingRule newRule) {
        log.info("Logging creation for rule {}", newRule.getId());
        recordRuleChange(newRule, "CREATE", "SYSTEM", "Rule created");
    }

    // Helper methods
    private Long getNextVersion(Long ruleId) {
        return historyRepository.findMaxVersionByRuleId(ruleId)
                .map(version -> version + 1)
                .orElse(1L);
    }

    private List<String> compareRules(PricingRule rule1, PricingRule rule2) {
        List<String> differences = new ArrayList<>();
        
        compareProperty(differences, "Name", rule1.getRuleName(), rule2.getRuleName());
        compareProperty(differences, "Description", rule1.getDescription(), rule2.getDescription());
        compareProperty(differences, "Priority", rule1.getPriority(), rule2.getPriority());
        compareProperty(differences, "Status", rule1.getStatus(), rule2.getStatus());
        
        if (!Objects.equals(rule1.getConditions(), rule2.getConditions())) {
            differences.add("Conditions have been modified");
        }
        
        if (!Objects.equals(rule1.getActions(), rule2.getActions())) {
            differences.add("Actions have been modified");
        }
        
        return differences;
    }

    private void compareProperty(List<String> differences, String propertyName, Object value1, Object value2) {
        if (!Objects.equals(value1, value2)) {
            differences.add(String.format("%s changed from '%s' to '%s'", 
                    propertyName, value1, value2));
        }
    }
}
