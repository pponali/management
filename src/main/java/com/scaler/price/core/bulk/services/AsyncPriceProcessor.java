package com.scaler.price.core.bulk.services;

import com.scaler.price.core.management.domain.BulkUploadTracker;
import com.scaler.price.core.management.domain.UploadStatus;
import com.scaler.price.core.management.dto.PriceDTO;
import com.scaler.price.core.management.dto.PriceUploadDTO;
import com.scaler.price.core.management.exceptions.BulkUploadException;
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
    public void processPrices(String uploadId, List<PriceUploadDTO> prices) throws PriceValidationException {
        BulkUploadTracker tracker = trackerRepository.findById(uploadId)
                .orElseThrow(() -> new BulkUploadException("Tracker not found: " + uploadId));

        List<PriceUploadDTO> failedRecords = new ArrayList<>();
        int successCount = 0;

        for (PriceUploadDTO price : prices) {
            try {
                if ("FAILED".equals(price.getStatus())) {
                    failedRecords.add(price);
                    continue;
                }

                PriceDTO priceDTO = convertToPrice(price, tracker);
                priceService.createPrice(priceDTO);
                successCount++;

                // Update tracker periodically
                if (successCount % 100 == 0) {
                    updateTracker(tracker, successCount, failedRecords.size());
                }
            } catch (Exception e) {
                price.setStatus("FAILED");
                price.setErrorMessage(e.getMessage());
                failedRecords.add(price);
            }
        }

        // Generate error report if needed
        if (!failedRecords.isEmpty()) {
            String errorFilePath = generateErrorReport(uploadId, failedRecords);
            tracker.setErrorFilePath(errorFilePath);
        }

        // Update final status
        tracker.setStatus(failedRecords.isEmpty() ? UploadStatus.COMPLETED : UploadStatus.COMPLETED_WITH_ERRORS);
        tracker.setSuccessCount(successCount);
        tracker.setFailureCount(failedRecords.size());
        tracker.setProcessedRecords(prices.size());
        trackerRepository.save(tracker);
    }

    private void updateTracker(BulkUploadTracker tracker, int successCount, int failureCount) {
        tracker.setSuccessCount(successCount);
        tracker.setFailureCount(failureCount);
        tracker.setProcessedRecords(successCount + failureCount);
        tracker.setStatus(UploadStatus.IN_PROGRESS);
        trackerRepository.save(tracker);
    }

    private PriceDTO convertToPrice(PriceUploadDTO price, BulkUploadTracker tracker) {
        return PriceDTO.builder()
                .productId(Long.parseLong(price.getProductId()))
                .sellerId(tracker.getSellerId())
                .siteId(tracker.getSiteId())
                .basePrice(new BigDecimal(price.getBasePrice()))
                .sellingPrice(new BigDecimal(price.getSellingPrice()))
                .mrp(new BigDecimal(price.getMrp()))
                .currency(price.getCurrency())
                .effectiveFrom(LocalDateTime.parse(price.getEffectiveFrom()))
                .effectiveTo(price.getEffectiveTo() != null ? LocalDateTime.parse(price.getEffectiveTo()) : null)
                .priceType(price.getPriceType())
                .status(price.getStatus())
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
                populateErrorRow(row, record);
            }

            // Save file
            String filePath = "errors/" + uploadId + "_errors.xlsx";
            fileStorageService.saveWorkbook(workbook, filePath);
            return filePath;
        } catch (IOException e) {
            throw new RuntimeException(e);
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
