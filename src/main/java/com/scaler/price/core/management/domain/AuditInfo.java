package com.scaler.price.core.management.domain;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;

@Embeddable
@Setter
@Getter
@SuperBuilder
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
