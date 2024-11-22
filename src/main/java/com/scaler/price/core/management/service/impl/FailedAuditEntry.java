package com.scaler.price.core.management.service.impl;

import com.scaler.price.audit.domain.AuditEntry;
import jakarta.persistence.Entity;
import lombok.Data;


@Data
@Entity

class FailedAuditEntry {
    private AuditEntry audit;
    private Exception error;

    public FailedAuditEntry(AuditEntry audit, Exception error) {
        this.audit = audit;
        this.error = error;
    }

    // Getters and setters
}
