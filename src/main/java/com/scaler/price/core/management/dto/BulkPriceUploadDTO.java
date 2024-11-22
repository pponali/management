package com.scaler.price.core.management.dto;

import lombok.Data;
import java.util.List;

@Data
public class BulkPriceUploadDTO {
    private String uploadId;
    private String sellerId;
    private String siteId;
    private List<PriceUploadDTO> prices;
    private String uploadedBy;
    private String status;
}
