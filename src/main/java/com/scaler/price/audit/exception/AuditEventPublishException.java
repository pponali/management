package com.scaler.price.audit.exception;

public class AuditEventPublishException extends RuntimeException {

    public AuditEventPublishException(String message) {
        super(message);
    }

    public AuditEventPublishException(String message, Throwable cause) {
        super(message, cause);
    }
}