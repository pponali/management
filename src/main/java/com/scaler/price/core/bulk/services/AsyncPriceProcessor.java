package com.scaler.price.core.bulk.services;

import com.scaler.price.core.management.domain.BulkUploadTracker;
import com.scaler.price.core.management.domain.FailedPrice;
import com.scaler.price.core.management.domain.UploadStatus;
import com.scaler.price.core.management.dto.PriceDTO;
import com.scaler.price.core.management.dto.PriceUploadDTO;
import com.scaler.price.core.management.exceptions.BulkUploadException;
import com.scaler.price.core.management.exceptions.FileStorageException;
import com.scaler.price.core.management.exceptions.PriceValidationException;
import com.scaler.price.core.management.repository.BulkUploadTrackerRepository;
import com.scaler.price.core.management.repository.FailedPricesRepository;
import com.scaler.price.core.management.service.PriceService;
import com.scaler.price.core.management.service.impl.FileStorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class AsyncPriceProcessor {
    
    private final PriceService priceService;
    private final BulkUploadTrackerRepository trackerRepository;
    private final FailedPricesRepository failedPricesRepository;
    private final FileStorageService fileStorageService;

    @Async
    public void processPrices(String uploadId, List<PriceUploadDTO> prices) throws PriceValidationException {
        log.info("Starting bulk price processing for upload ID: {}. Total records: {}", uploadId, prices.size());

        BulkUploadTracker tracker = trackerRepository.findByUploadId(uploadId)
                .orElseThrow(() -> new BulkUploadException("Tracker not found: " + uploadId));

        List<PriceUploadDTO> failedRecords = new ArrayList<>();
        int successCount = 0;
        int currentRecord = 0;

        for (PriceUploadDTO price : prices) {
            currentRecord++;
            try {
                PriceDTO priceDTO = convertToPrice(price, tracker);
                processPrice(priceDTO);
                successCount++;
                log.debug("Successfully processed price for Product ID: {}", price.getProductId());
            } catch (Throwable e) {
                handlePriceProcessingError(price, e, tracker.getUploadId());
                failedRecords.add(price);
            }

            // Update tracker periodically
            if (currentRecord % 100 == 0) {
                updateTrackerProgress(tracker, successCount, failedRecords.size(), currentRecord, prices.size());
            }
        }

        // Final update
        finalizeProcessing(tracker, uploadId, failedRecords, prices, successCount);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    protected void processPrice(PriceDTO priceDTO) throws PriceValidationException {
        priceService.createPrice(priceDTO);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    protected void updateTrackerProgress(BulkUploadTracker tracker, int successCount,
                                         int failureCount, int currentRecord, int totalRecords) {
        try {
            tracker.setSuccessCount(successCount);
            tracker.setFailureCount(failureCount);
            tracker.setProcessedRecords(currentRecord);
            tracker.setStatus(UploadStatus.IN_PROGRESS);
            trackerRepository.save(tracker);
            log.debug("Updated tracker progress: {}/{} records processed", currentRecord, totalRecords);
        } catch (Exception e) {
            log.error("Failed to update tracker progress: {}", e.getMessage());
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    protected void finalizeProcessing(BulkUploadTracker tracker, String uploadId,
                                      List<PriceUploadDTO> failedRecords, List<PriceUploadDTO> prices, int successCount) {
        try {
            if (!failedRecords.isEmpty()) {
                saveFailedRecords(uploadId, failedRecords);
                String errorFilePath = generateErrorReport(uploadId, failedRecords);
                tracker.setErrorFilePath(errorFilePath);
            }

            UploadStatus finalStatus = failedRecords.isEmpty() ?
                    UploadStatus.COMPLETED : UploadStatus.COMPLETED_WITH_ERRORS;

            tracker.setStatus(finalStatus);
            tracker.setSuccessCount(successCount);
            tracker.setFailureCount(failedRecords.size());
            tracker.setProcessedRecords(prices.size());
            trackerRepository.save(tracker);

            log.info("Processing completed. Status: {}, Success: {}, Failed: {}",
                    finalStatus, successCount, failedRecords.size());
        } catch (Exception e) {
            log.error("Error in finalizing processing: {}", e.getMessage());
            tracker.setStatus(UploadStatus.FAILED);
            trackerRepository.save(tracker);
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    protected void saveFailedRecords(String uploadId, List<PriceUploadDTO> failedRecords) {
        for (PriceUploadDTO failedRecord : failedRecords) {
            try {
                FailedPrice failedPrice = FailedPrice.builder()
                        .uploadId(uploadId)
                        .productId(failedRecord.getProductId())
                        .sellerId(failedRecord.getSellerId())
                        .siteId(failedRecord.getSiteId())
                        .basePrice(failedRecord.getBasePrice())
                        .sellingPrice(failedRecord.getSellingPrice())
                        .mrp(failedRecord.getMrp())
                        .errorMessage(failedRecord.getErrorMessage())
                        .build();

                failedPricesRepository.save(failedPrice);
            } catch (Exception e) {
                log.error("Failed to save failed record for Product ID: {}. Error: {}",
                        failedRecord.getProductId(), e.getMessage());
            }
        }
    }

    private void handlePriceProcessingError(PriceUploadDTO price, Throwable e, String uploadId) {
        String errorMessage;
        
        // Get the root cause of the exception
        Throwable rootCause = getRootCause(e);
        String rootMessage = rootCause.getMessage();
        
        if (e instanceof PriceValidationException) {
            errorMessage = e.getMessage();
        } else if (rootMessage != null && rootMessage.contains("already exists")) {
            errorMessage = "Duplicate price entry: A price already exists for this product, seller, site, date and price type combination";
        } else if (rootMessage != null && rootMessage.contains("value too long")) {
            errorMessage = "One or more fields exceed the maximum allowed length";
        } else {
            errorMessage = "Unexpected error: " + e.getMessage();
        }
        
        log.error("Error processing price for Product ID: {}. Error: {}", price.getProductId(), errorMessage, e);
        
        FailedPrice failedPrice = FailedPrice.builder()
                .uploadId(uploadId)
                .productId(price.getProductId())
                .sellerId(price.getSellerId())
                .siteId(price.getSiteId())
                .basePrice(price.getBasePrice())
                .sellingPrice(price.getSellingPrice())
                .mrp(price.getMrp())
                .errorMessage(errorMessage)
                .build();

        failedPricesRepository.save(failedPrice);
    }

    private Throwable getRootCause(Throwable throwable) {
        Throwable rootCause = throwable;
        while (rootCause.getCause() != null && rootCause.getCause() != rootCause) {
            rootCause = rootCause.getCause();
        }
        return rootCause;
    }

    private PriceDTO convertToPrice(PriceUploadDTO price, BulkUploadTracker tracker) {
        return PriceDTO.builder()
                .productId(price.getProductId())
                .sellerId(price.getSellerId())
                .siteId(price.getSiteId())
                .basePrice(price.getBasePrice())
                .sellingPrice(price.getSellingPrice())
                .mrp(price.getMrp())
                .currency(price.getCurrency())
                .effectiveFrom(LocalDateTime.parse(price.getEffectiveFrom()))
                .effectiveTo(price.getEffectiveTo() != null ? LocalDateTime.parse(price.getEffectiveTo()) : null)
                .priceType(price.getPriceType())
                .isActive(price.getIsActive())
                .build();
    }

    private String generateErrorReport(String uploadId, List<PriceUploadDTO> failedRecords) {
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Failed Records");

            // Create headers
            Row headerRow = sheet.createRow(0);
            createHeaders(headerRow);

            // Add failed records
            int rowNum = 1;
            for (PriceUploadDTO record : failedRecords) {
                Row row = sheet.createRow(rowNum++);
                try {
                    populateErrorRow(row, record);
                } catch (Exception e) {
                    log.error("Failed to populate error row for record: {}", record, e);
                }
            }

            // Auto-size columns for better readability
            for (int i = 0; i < 11; i++) {
                sheet.autoSizeColumn(i);
            }

            // Save error report using FileStorageService
            try {
                return fileStorageService.saveErrorReport(workbook, uploadId);
            } catch (FileStorageException e) {
                log.error("Failed to save error report for upload {}: {}", uploadId, e.getMessage());
                throw e;
            }
        } catch (IOException e) {
            log.error("Failed to generate error report for upload {}: {}", uploadId, e.getMessage());
            throw new FileStorageException("Could not generate error report", e);
        }
    }

    private void createHeaders(Row headerRow) {
        String[] headers = {
            "Row Number", "Product ID", "Error Message", "Base Price",
            "Selling Price", "MRP", "Currency", "Effective From",
            "Effective To", "Price Type", "Status"
        };
        
        for (int i = 0; i < headers.length; i++) {
            headerRow.createCell(i).setCellValue(headers[i]);
        }
    }

    private void populateErrorRow(Row row, PriceUploadDTO record) {
        int cellNum = 0;
        row.createCell(cellNum++).setCellValue(record.getRowNumber());
        row.createCell(cellNum++).setCellValue(record.getProductId());
        row.createCell(cellNum++).setCellValue(record.getErrorMessage());

        // Convert BigDecimal to double
        row.createCell(cellNum++).setCellValue(record.getBasePrice() != null ? record.getBasePrice().doubleValue() : 0.0);
        row.createCell(cellNum++).setCellValue(record.getSellingPrice() != null ? record.getSellingPrice().doubleValue() : 0.0);
        row.createCell(cellNum++).setCellValue(record.getMrp() != null ? record.getMrp().doubleValue() : 0.0);

        row.createCell(cellNum++).setCellValue(record.getCurrency());
        row.createCell(cellNum++).setCellValue(record.getEffectiveFrom());
        row.createCell(cellNum++).setCellValue(record.getEffectiveTo());
        row.createCell(cellNum++).setCellValue(record.getPriceType());
        row.createCell(cellNum).setCellValue(record.getStatus());
    }
}
