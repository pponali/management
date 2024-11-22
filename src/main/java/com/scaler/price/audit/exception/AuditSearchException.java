package com.scaler.price.audit.exception;

public class AuditSearchException extends Throwable {
    public AuditSearchException(String failedToSearchAuditEntries, Exception e) {
        super(failedToSearchAuditEntries, e);
    }
    public AuditSearchException(String failedToSearchAuditEntries) {
        super(failedToSearchAuditEntries);
    }
}
