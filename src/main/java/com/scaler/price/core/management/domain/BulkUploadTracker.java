package com.scaler.price.core.management.domain;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Entity
@Table(name = "bulk_upload_tracker")
@Data
public class BulkUploadTracker {
    @Id
    private String uploadId;

    @Column(nullable = false)
    private String sellerId;

    @Column(nullable = false)
    private String siteId;

    @Column(nullable = false)
    private String uploadedBy;

    @Column(nullable = false)
    private LocalDateTime uploadedAt;

    private Integer totalRecords;
    private Integer processedRecords;
    private Integer successCount;
    private Integer failureCount;

    @Enumerated(EnumType.STRING)
    private UploadStatus status;

    private String errorFilePath;
    private String originalFileName;

    @Version
    private Long version;

    @PrePersist
    public void prePersist() {
        uploadedAt = LocalDateTime.now();
        processedRecords = 0;
        successCount = 0;
        failureCount = 0;
        status = UploadStatus.PENDING;
    }
}
