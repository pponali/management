package com.scaler.price.rule.service;

import com.scaler.price.audit.exception.AuditSearchException;
import com.scaler.price.audit.service.AuditService;
import com.scaler.price.rule.service.NotificationService;
import com.scaler.price.rule.domain.PricingRule;
import com.scaler.price.rule.domain.RuleStatus;
import com.scaler.price.rule.dto.StatusChangeNotification;
import com.scaler.price.rule.exceptions.InvalidStatusTransitionException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;
import java.util.Set;

@Service
@Slf4j
@RequiredArgsConstructor
public class RuleStatusManager {
    private static final Map<RuleStatus, Set<RuleStatus>> ALLOWED_TRANSITIONS;

    static {
        ALLOWED_TRANSITIONS = new EnumMap<>(RuleStatus.class);

        // Define valid status transitions
        ALLOWED_TRANSITIONS.put(RuleStatus.DRAFT, Set.of(
                RuleStatus.PENDING_APPROVAL,
                RuleStatus.ARCHIVED
        ));

        ALLOWED_TRANSITIONS.put(RuleStatus.PENDING_APPROVAL, Set.of(
                RuleStatus.APPROVED,
                RuleStatus.REJECTED,
                RuleStatus.DRAFT
        ));

        ALLOWED_TRANSITIONS.put(RuleStatus.APPROVED, Set.of(
                RuleStatus.ACTIVE,
                RuleStatus.SCHEDULED,
                RuleStatus.INACTIVE
        ));

        ALLOWED_TRANSITIONS.put(RuleStatus.ACTIVE, Set.of(
                RuleStatus.INACTIVE,
                RuleStatus.SUSPENDED,
                RuleStatus.EXPIRED
        ));

        ALLOWED_TRANSITIONS.put(RuleStatus.INACTIVE, Set.of(
                RuleStatus.ACTIVE,
                RuleStatus.ARCHIVED
        ));

        ALLOWED_TRANSITIONS.put(RuleStatus.SUSPENDED, Set.of(
                RuleStatus.ACTIVE,
                RuleStatus.INACTIVE,
                RuleStatus.ARCHIVED
        ));

        ALLOWED_TRANSITIONS.put(RuleStatus.SCHEDULED, Set.of(
                RuleStatus.ACTIVE,
                RuleStatus.INACTIVE,
                RuleStatus.DRAFT
        ));

        ALLOWED_TRANSITIONS.put(RuleStatus.REJECTED, Set.of(
                RuleStatus.DRAFT,
                RuleStatus.ARCHIVED
        ));

        ALLOWED_TRANSITIONS.put(RuleStatus.EXPIRED, Set.of(
                RuleStatus.ARCHIVED
        ));

        ALLOWED_TRANSITIONS.put(RuleStatus.ARCHIVED, Collections.emptySet());
    }

    private final AuditService auditService;
    private final NotificationService notificationService;

    
    public void validateStatusTransition(RuleStatus currentStatus, RuleStatus newStatus) throws InvalidStatusTransitionException {
        if (currentStatus == newStatus) {
            return;
        }

        Set<RuleStatus> allowedStatuses = ALLOWED_TRANSITIONS.get(currentStatus);
        if (allowedStatuses == null || !allowedStatuses.contains(newStatus)) {
            throw new InvalidStatusTransitionException(
                    String.format("Cannot transition from %s to %s",
                            currentStatus, newStatus)
            );
        }
    }

    public void updateRuleStatus(PricingRule rule, RuleStatus newStatus, String reason) throws InvalidStatusTransitionException {
        RuleStatus oldStatus = rule.getStatus();
        validateStatusTransition(oldStatus, newStatus);

        rule.setStatus(newStatus);
        rule.setLastModifiedAt(LocalDateTime.now());

        try {
            // Record status change in audit
            auditService.logStatusChange(
                    rule.getId(),
                    oldStatus,
                    newStatus,
                    reason
            );
        } catch (AuditSearchException e) {
            // Log the error and potentially rethrow as a runtime exception
            log.error("Failed to log status change for rule: {}", rule.getId(), e);
            throw new RuntimeException("Audit logging failed", e);
        }

        // Send notifications
        notifyStatusChange(rule, oldStatus, newStatus, reason);
    }

    private void notifyStatusChange(
            PricingRule rule,
            RuleStatus oldStatus,
            RuleStatus newStatus,
            String reason) {
        // Build notification context
        StatusChangeNotification notification = StatusChangeNotification.builder()
                .ruleId(rule.getId())
                .ruleName(rule.getRuleName())
                .oldStatus(oldStatus)
                .newStatus(newStatus)
                .reason(reason)
                .timestamp(LocalDateTime.now())
                .build();

        // Send notifications based on status change
        switch (newStatus) {
            case PENDING_APPROVAL -> notifyApprovalRequired(notification);
            case APPROVED -> notifyRuleApproved(notification);
            case REJECTED -> notifyRuleRejected(notification);
            case ACTIVE -> notifyRuleActivated(notification);
            case SUSPENDED -> notifyRuleSuspended(notification);
            case EXPIRED -> notifyRuleExpired(notification);
        }
    }

    private void notifyApprovalRequired(StatusChangeNotification notification) {
        notificationService.notifyApprovers(
                "Rule Approval Required",
                String.format("Rule %s requires approval", notification.getRuleName()),
                notification
        );
    }

    private void notifyRuleApproved(StatusChangeNotification notification) {
        notificationService.notifyOwners(
                "Rule Approved",
                String.format("Rule %s has been approved", notification.getRuleName()),
                notification
        );
    }

    private void notifyRuleRejected(StatusChangeNotification notification) {
        notificationService.notifyOwners(
                "Rule Rejected",
                String.format("Rule %s has been rejected: %s",
                        notification.getRuleName(),
                        notification.getReason()
                ),
                notification
        );
    }

    private void notifyRuleActivated(StatusChangeNotification notification) {
        notificationService.notifyStakeholders(
                "Rule Activated",
                String.format("Rule %s is now active", notification.getRuleName()),
                notification
        );
    }

    private void notifyRuleSuspended(StatusChangeNotification notification) {
        notificationService.notifyStakeholders(
                "Rule Suspended",
                String.format("Rule %s has been suspended: %s",
                        notification.getRuleName(),
                        notification.getReason()
                ),
                notification
        );
    }

    private void notifyRuleExpired(StatusChangeNotification notification) {
        notificationService.notifyOwners(
                "Rule Expired",
                String.format("Rule %s has expired", notification.getRuleName()),
                notification
        );
    }
}