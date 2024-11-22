package com.scaler.price.audit.domain;

public enum AuditEventType {
    RULE_CREATED,
    RULE_MODIFIED,
    RULE_DELETED,
    RULE_ACTIVATED,
    RULE_DEACTIVATED,
    PRICE_CALCULATION,
    RULE_VALIDATION,
    RULE_EXECUTION,
    BULK_OPERATION,
    RULE_APPROVAL
}