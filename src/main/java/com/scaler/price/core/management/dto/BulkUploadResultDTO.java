package com.scaler.price.core.management.dto;

import com.scaler.price.core.management.domain.UploadStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BulkUploadResultDTO {
    private String uploadId;
    private Integer totalRecords;
    private Integer successCount;
    private Integer failureCount;
    private UploadStatus status;
    private List<FailedPriceDTO> failedRecords;
    private String downloadUrl;
    private String message;
}
