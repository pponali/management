package com.scaler.price.core.bulk.services;

import com.scaler.price.core.management.domain.BulkUploadTracker;
import com.scaler.price.core.management.domain.UploadStatus;
import com.scaler.price.core.management.dto.PriceDTO;
import com.scaler.price.core.management.dto.PriceUploadDTO;
import com.scaler.price.core.management.exceptions.BulkUploadException;
import com.scaler.price.core.management.exceptions.FileStorageException;
import com.scaler.price.core.management.exceptions.PriceValidationException;
import com.scaler.price.core.management.repository.BulkUploadTrackerRepository;
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
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class AsyncPriceProcessor {
    
    private final PriceService priceService;
    private final BulkUploadTrackerRepository trackerRepository;
    private final FileStorageService fileStorageService;

    @Async
    @Transactional
    public void processPrices(String uploadId, List<PriceUploadDTO> prices) {
        log.info("Starting bulk price processing for upload ID: {}. Total records: {}", uploadId, prices.size());
        
        for (PriceUploadDTO price : prices) {
            log.debug("Processing PriceUploadDTO: Product ID: {}", price.getProductId());
        }

        BulkUploadTracker tracker = trackerRepository.findByUploadId(uploadId)
                .orElseThrow(() -> new BulkUploadException("Tracker not found: " + uploadId));
        log.debug("Found tracker for upload ID: {}. Initial status: {}", uploadId, tracker.getStatus());

        List<PriceUploadDTO> failedRecords = new ArrayList<>();
        int successCount = 0;
        int currentRecord = 0;

        for (PriceUploadDTO price : prices) {
            currentRecord++;
            log.debug("Processing record {}/{}: Product ID: {}, Price Type: {}", 
                    currentRecord, prices.size(), price.getProductId(), price.getPriceType());

            try {
                if (price.getErrorMessage() != null) {
                    price.setStatus("FAILED");
                    if (price.getErrorMessage() == null) {
                        price.setErrorMessage("Product ID cannot be null or empty");
                    }
                    failedRecords.add(price);
                    log.error("Invalid record - Product ID is null or empty at row: {}", price.getRowNumber());
                    continue;
                }

                if ("FAILED".equals(price.getStatus())) {
                    log.warn("Skipping previously failed record for Product ID: {}", price.getProductId());
                    failedRecords.add(price);
                    continue;
                }

                log.debug("Converting price data for Product ID: {} to DTO", price.getProductId());
                PriceDTO priceDTO = convertToPrice(price, tracker);
                
                try {
                    log.debug("Attempting to create price for Product ID: {}", price.getProductId());
                    priceService.createPrice(priceDTO);
                    successCount++;
                    log.info("Successfully processed price for Product ID: {}. Success count: {}", 
                            price.getProductId(), successCount);
                    
                } catch (PriceValidationException e) {
                    price.setStatus("FAILED");
                    price.setErrorMessage(e.getMessage());
                    failedRecords.add(price);
                    log.error("Validation failed for Product ID: {}. Error: {}", 
                            price.getProductId(), e.getMessage());
                }

                // Update tracker periodically
                if ((successCount + failedRecords.size()) % 100 == 0) {
                    log.info("Progress update - Processed: {}/{}, Success: {}, Failed: {}", 
                            currentRecord, prices.size(), successCount, failedRecords.size());
                    updateTracker(tracker, successCount, failedRecords.size());
                }
            } catch (Exception e) {
                price.setStatus("FAILED");
                price.setErrorMessage(e.getMessage());
                failedRecords.add(price);
                log.error("Unexpected error processing Product ID: {}. Error: {}", 
                        price.getProductId(), e.getMessage(), e);
            }
        }
        // Generate error report if needed
        if (!failedRecords.isEmpty()) {
            log.info("Processing completed with failures. Total failed records: {}", failedRecords.size());
            try {
                log.debug("Generating error report for {} failed records", failedRecords.size());
                String errorFilePath = generateErrorReport(uploadId, failedRecords);
                tracker.setErrorFilePath(errorFilePath);
                log.info("Error report generated successfully at: {}", errorFilePath);
            } catch (Exception e) {
                log.error("Failed to generate error report for upload {}. Error: {}",
                        uploadId, e.getMessage(), e);
            }
        } else {
            log.info("Processing completed successfully. All {} records processed without errors",
                    prices.size());
        }

        UploadStatus finalStatus = failedRecords.isEmpty() ?
                UploadStatus.COMPLETED : UploadStatus.COMPLETED_WITH_ERRORS;
        log.info("Updating final status for upload {}. Status: {}, Success: {}, Failed: {}",
                uploadId, finalStatus, successCount, failedRecords.size());
        updateTracker(tracker, finalStatus, prices.size() - failedRecords.size(), failedRecords, prices);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)  // Add this to create a new transaction
    private void updateTracker(BulkUploadTracker tracker, UploadStatus finalStatus, int successCount, List<?> failedRecords, List<?> prices) {
        try {
            BulkUploadTracker freshTracker = trackerRepository.findByUploadId(tracker.getUploadId())
                    .orElseThrow(() -> new RuntimeException("Tracker not found"));

            freshTracker.setStatus(finalStatus);
            freshTracker.setSuccessCount(successCount);
            freshTracker.setFailureCount(failedRecords.size());
            freshTracker.setProcessedRecords(prices.size());

            trackerRepository.saveAndFlush(freshTracker);
            log.info("Tracker updated successfully: {}", freshTracker);
        } catch (Exception e) {
            log.error("Failed to update tracker: {}", e.getMessage(), e);
            throw e;
        }
    }

    private void updateTracker(BulkUploadTracker tracker, int successCount, int failureCount) {
        log.debug("Updating tracker - Upload ID: {}, Success: {}, Failed: {}",
                tracker.getUploadId(), successCount, failureCount);
        tracker.setSuccessCount(successCount);
        tracker.setFailureCount(failureCount);
        tracker.setProcessedRecords(successCount + failureCount);
        tracker.setStatus(UploadStatus.IN_PROGRESS);
        trackerRepository.save(tracker);
        log.debug("Tracker updated successfully");
    }

    private PriceDTO convertToPrice(PriceUploadDTO price, BulkUploadTracker tracker) {
        return PriceDTO.builder()
                .productId(price.getProductId())
                .sellerId(price.getSellerId())
                .siteId(price.getSiteId())
                .basePrice(new BigDecimal(price.getBasePrice()))
                .sellingPrice(new BigDecimal(price.getSellingPrice()))
                .mrp(new BigDecimal(price.getMrp()))
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
        row.createCell(cellNum++).setCellValue(record.getBasePrice());
        row.createCell(cellNum++).setCellValue(record.getSellingPrice());
        row.createCell(cellNum++).setCellValue(record.getMrp());
        row.createCell(cellNum++).setCellValue(record.getCurrency());
        row.createCell(cellNum++).setCellValue(record.getEffectiveFrom());
        row.createCell(cellNum++).setCellValue(record.getEffectiveTo());
        row.createCell(cellNum++).setCellValue(record.getPriceType());
        row.createCell(cellNum).setCellValue(record.getStatus());
    }
}
