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

    private void updateTracker(BulkUploadTracker tracker, int successCount, int size) {
    }

    private PriceDTO convertToPrice(PriceUploadDTO price, BulkUploadTracker tracker) {
        // Implement conversion logic

        return null;
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

    }

    private void populateErrorRow(Row row, PriceUploadDTO record) {
        // Implement logic to populate row with error data
        // ...
        // Example:
        row.createCell(0).setCellValue(record.getProductId());
        row.createCell(1).setCellValue(record.getErrorMessage());


    }
}
