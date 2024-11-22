package com.scaler.price.rule.service;

import com.scaler.price.rule.domain.PricingRule;
import com.scaler.price.rule.domain.RuleHistory;
import com.scaler.price.rule.domain.RuleStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Service interface for managing pricing rule history.
 * Tracks changes, modifications, and state transitions of pricing rules.
 */
public interface RuleHistoryService {

    /**
     * Records a new version of a pricing rule in history.
     *
     * @param rule The pricing rule being modified
     * @param changeType The type of change (CREATE, UPDATE, DELETE, ACTIVATE, DEACTIVATE)
     * @param userId The ID of the user making the change
     * @param comment Optional comment describing the change
     * @return The created history record
     */
    RuleHistory recordRuleChange(PricingRule rule, String changeType, String userId, String comment);

    /**
     * Retrieves the complete history of a specific pricing rule.
     *
     * @param ruleId The ID of the pricing rule
     * @param pageable Pagination parameters
     * @return Page of history records for the rule
     */
    Page<RuleHistory> getRuleHistory(Long ruleId, Pageable pageable);

    Page<RuleHistory> getRuleHistory(String ruleId, Pageable pageable);

    /**
     * Retrieves a specific version of a pricing rule.
     *
     * @param ruleId The ID of the pricing rule
     * @param version The version number to retrieve
     * @return Optional containing the rule version if found
     */
    Optional<PricingRule> getRuleVersion(String ruleId, Integer version);

    /**
     * Compares two versions of a pricing rule and returns the differences.
     *
     * @param ruleId The ID of the pricing rule
     * @param version1 First version number
     * @param version2 Second version number
     * @return List of differences between the versions
     */
    List<String> compareVersions(String ruleId, Integer version1, Integer version2);

    /**
     * Retrieves all changes made by a specific user.
     *
     * @param userId The ID of the user
     * @param pageable Pagination parameters
     * @return Page of history records for the user
     */
    Page<RuleHistory> getUserChanges(String userId, Pageable pageable);

    /**
     * Retrieves all changes made within a specific time period.
     *
     * @param startTime Start of the time period
     * @param endTime End of the time period
     * @param pageable Pagination parameters
     * @return Page of history records within the time period
     */
    Page<RuleHistory> getChangesByTimeRange(LocalDateTime startTime, LocalDateTime endTime, Pageable pageable);

    /**
     * Retrieves the most recent changes across all rules.
     *
     * @param pageable Pagination parameters
     * @return Page of recent history records
     */
    Page<RuleHistory> getRecentChanges(Pageable pageable);

    /**
     * Restores a pricing rule to a previous version.
     *
     * @param ruleId The ID of the pricing rule
     * @param version The version to restore to
     * @param userId The ID of the user performing the restore
     * @param comment Optional comment describing the restore
     * @return The restored rule
     */
    PricingRule restoreVersion(String ruleId, Integer version, String userId, String comment);

    /**
     * Archives history records older than the specified date.
     *
     * @param cutoffDate Date before which records should be archived
     * @return Number of records archived
     */
    int archiveOldRecords(LocalDateTime cutoffDate);

    /**
     * Retrieves all changes related to a specific product.
     *
     * @param productId The ID of the product
     * @param pageable Pagination parameters
     * @return Page of history records for the product
     */
    Page<RuleHistory> getProductRelatedChanges(String productId, Pageable pageable);

    /**
     * Retrieves audit trail for rule approval process.
     *
     * @param ruleId The ID of the pricing rule
     * @return List of approval-related history records
     */
    List<RuleHistory> getRuleApprovalHistory(String ruleId);

    /**
     * Retrieves changes made as part of a specific batch operation.
     *
     * @param batchId The ID of the batch operation
     * @return List of history records for the batch
     */
    List<RuleHistory> getBatchOperationHistory(String batchId);

    void logRuleStatusChange(Long ruleId, RuleStatus status);

    void logRuleDeletion(Long ruleId);

    void logRuleUpdate(PricingRule updatedRule);

    void logRuleCreation(PricingRule newRule);
}
