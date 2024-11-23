package com.scaler.price.core.management.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Embeddable
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AuditInfo {
    
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "created_by")
    private String createdBy;

    @Column(name = "updated_by")
    private String updatedBy;

    @Column(name = "version")
    private Long version = 0L;

    public void setLastModifiedBy(String updatedBy) {
        this.updatedBy = updatedBy;
    }

    public void setLastModifiedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public void setModifiedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
