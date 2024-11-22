package com.scaler.price.audit.domain;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.Instant;

@Data
@AllArgsConstructor
public class FailedAuditEntry {
    private AuditEntry auditEntry;
    private Exception error;
    private Instant failedAt = Instant.now();
    private int retryCount = 0;
}
