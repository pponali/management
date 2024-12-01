package com.scaler.price.core.management.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;

@Entity
@Table(name = "bulk_upload_tracker")
@Setter
@Getter
@SuperBuilder
public class BulkUploadTracker extends AuditInfo {

    @Column(nullable = false)
    private Long sellerId;

    @Column(nullable = false)
    private Long siteId;

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

}
