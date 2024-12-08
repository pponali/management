package com.scaler.price.core.management.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@NoArgsConstructor
@SuperBuilder
@Table(name = "bulk_upload_tracker")
public class BulkUploadTracker extends AuditInfo {

    @Column(nullable = false, unique = true, name = "upload_id")
    private String uploadId;

    @Column(nullable = false, name = "seller_id")
    private Long sellerId;

    @Column(nullable = false, name = "site_id")
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