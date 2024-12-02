package com.scaler.price.core.management.service;

import com.scaler.price.core.management.domain.UploadStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BulkUploadResultDTO {
    private String uploadId;
    private UploadStatus status;
    private Integer totalRecords;
    private Integer successCount;
    private Integer failureCount;
    private String message;
    private String errorFileUrl;
}
