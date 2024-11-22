package com.scaler.price.audit;

import com.scaler.price.audit.domain.AuditEntry;
import com.scaler.price.rule.domain.PricingRule;
import java.util.Map;

/**
 * Interface for publishing audit events related to pricing rules and actions
 */
public interface AuditEventPublisher {

    /**
     * Publishes an audit event for rule creation
     *
     * @param rule The created rule
     * @param userId User who created the rule
     */
    void publishRuleCreatedEvent(PricingRule rule, String userId);

    /**
     * Publishes an audit event for rule modification
     *
     * @param rule The modified rule
     * @param userId User who modified the rule
     * @param changes Map of changed fields and their values
     */
    void publishRuleModifiedEvent(PricingRule rule, String userId, Map<String, String> changes);

    /**
     * Publishes an audit event for rule deletion
     *
     * @param ruleId ID of the deleted rule
     * @param userId User who deleted the rule
     */
    void publishRuleDeletedEvent(String ruleId, String userId);

    /**
     * Publishes an audit event for rule activation
     *
     * @param rule The activated rule
     * @param userId User who activated the rule
     */
    void publishRuleActivatedEvent(PricingRule rule, String userId);

    /**
     * Publishes an audit event for rule deactivation
     *
     * @param rule The deactivated rule
     * @param userId User who deactivated the rule
     */
    void publishRuleDeactivatedEvent(PricingRule rule, String userId);

    /**
     * Publishes an audit event for price calculation
     *
     * @param ruleId Rule ID used for calculation
     * @param productId Product ID
     * @param originalPrice Original price
     * @param calculatedPrice Calculated price
     * @param userId User who triggered the calculation
     */
    void publishPriceCalculationEvent(String ruleId, String productId,
                                      double originalPrice, double calculatedPrice,
                                      String userId);

    /**
     * Publishes an audit event for rule validation
     *
     * @param rule The validated rule
     * @param validationResult Result of validation
     * @param userId User who triggered validation
     */
    void publishRuleValidationEvent(PricingRule rule, boolean validationResult,
                                    String userId);

    /**
     * Publishes an audit event for rule execution
     *
     * @param rule The executed rule
     * @param executionResult Result of execution
     * @param userId User who triggered execution
     */
    void publishRuleExecutionEvent(PricingRule rule, String executionResult,
                                   String userId);

    /**
     * Publishes an audit event for bulk rule operations
     *
     * @param operationType Type of bulk operation
     * @param affectedRules Number of rules affected
     * @param userId User who triggered the operation
     */
    void publishBulkOperationEvent(String operationType, int affectedRules,
                                   String userId);

    /**
     * Publishes an audit event for rule approval workflow
     *
     * @param rule The rule in approval workflow
     * @param status Approval status
     * @param approver User who approved/rejected
     * @param comments Approval comments
     */
    void publishRuleApprovalEvent(PricingRule rule, String status,
                                  String approver, String comments);

    void publishAuditEvent(AuditEntry audit);
}