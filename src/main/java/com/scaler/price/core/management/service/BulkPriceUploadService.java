package com.scaler.price.core.management.service;


import com.scaler.price.core.management.dto.PriceDTO;
import com.scaler.price.core.management.exceptions.PriceValidationException;
import jakarta.annotation.Resource;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface BulkPriceUploadService {
    BulkUploadResultDTO uploadPrices(MultipartFile file, Long sellerId, Long siteId) throws PriceValidationException;
    BulkUploadResultDTO getUploadStatus(Long uploadId);
    Resource downloadErrorReport(Long uploadId);

    List<String> validatePrices(List<PriceDTO> prices);

    BulkUploadResultDTO processBulkUpload(MultipartFile file) throws PriceValidationException;

    byte[] generateTemplate();
}