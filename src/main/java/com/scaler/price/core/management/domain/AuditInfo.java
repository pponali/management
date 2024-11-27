package com.scaler.price.core.management.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.time.LocalDateTime;

import org.springframework.data.annotation.Id;

import io.swagger.v3.oas.annotations.media.Schema;

@Embeddable
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AuditInfo {

    @Schema(description = "Audit Event ID", example = "1")
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
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

        @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
