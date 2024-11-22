package com.scaler.price.core.management.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import lombok.Data;

import java.time.LocalDateTime;

@Embeddable
@Data
public class AuditInfo {
    @Column(nullable = false, updatable = false)
    private String createdBy;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    private String modifiedBy;
    private LocalDateTime modifiedAt;
    private String approvedBy;
    private LocalDateTime approvedAt;

    @PrePersist
    public void prePersist() {
        createdAt = LocalDateTime.now();
    }

    @PreUpdate
    public void preUpdate() {
        modifiedAt = LocalDateTime.now();
    }
    private String status; // DRAFT, PENDING_APPROVAL, APPROVED, REJECTED

    public void setLastModifiedBy(String userId) {
        this.modifiedBy = userId;
        this.modifiedAt = LocalDateTime.now();
    }

    public void setLastModifiedAt(LocalDateTime now) {
        this.modifiedAt = now;
    }
}
