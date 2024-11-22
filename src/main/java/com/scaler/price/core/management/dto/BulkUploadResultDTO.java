package com.scaler.price.core.management.dto;

import lombok.Data;

import java.util.List;

@Data
public class BulkUploadResultDTO {
    private String uploadId;
    private Integer totalRecords;
    private Integer successCount;
    private Integer failureCount;
    private String status;
    private List<PriceUploadDTO> failedRecords;
    private String downloadUrl;
}
