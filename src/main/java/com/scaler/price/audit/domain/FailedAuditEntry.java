package com.scaler.price.audit.domain;

import com.scaler.price.core.management.domain.AuditInfo;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.time.Instant;

@Entity(name = "AuditFailedEntry")
@Table(name = "failed_audit_entries")
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class FailedAuditEntry extends AuditInfo {
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "audit_entry_id", foreignKey = @ForeignKey(name = "fk_failed_audit_entry"))
    private AuditEntry auditEntry;
    
    @Column(columnDefinition = "text")
    private String errorMessage;
    
    @Column(name = "failed_at")
    private Instant failedAt = Instant.now();
    
    @Column(name = "retry_count")
    private int retryCount = 0;
    
    @PrePersist
    @PreUpdate
    protected void onSave() {
        if (auditEntry == null) {
            throw new IllegalStateException("FailedAuditEntry must have an associated AuditEntry");
        }
    }
    
    public FailedAuditEntry(AuditEntry auditEntry, Exception error) {
        this.auditEntry = auditEntry;
        this.errorMessage = error != null ? error.getMessage() : null;
        this.failedAt = Instant.now();
        this.retryCount = 0;
    }
}
