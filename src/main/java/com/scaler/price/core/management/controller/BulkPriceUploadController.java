
package com.scaler.price.core.management.controller;

import com.scaler.price.core.management.dto.PriceDTO;
import com.scaler.price.core.management.exceptions.PriceValidationException;
import com.scaler.price.core.management.service.BulkPriceUploadService;
import com.scaler.price.core.management.service.BulkUploadResultDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * Controller for handling bulk price upload operations.
 * Provides endpoints for uploading price files, downloading templates,
 * checking upload status, and validating prices.
 */
@RestController
@RequestMapping("/api/v1/prices/bulk")
public class BulkPriceUploadController {

    private final BulkPriceUploadService bulkUploadService;

    @Autowired
    public BulkPriceUploadController(BulkPriceUploadService bulkUploadService) {
        this.bulkUploadService = bulkUploadService;
    }

    /**
     * Handles bulk upload of prices through a file upload.
     * Accepts Excel/CSV files in the specified template format.
     *
     * @param file The multipart file containing price data
     * @return BulkUploadResult containing upload status and any errors
     */
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<BulkUploadResultDTO> uploadPrices(@RequestParam("file") MultipartFile file) {
        BulkUploadResultDTO result = null;
        try {
            result = bulkUploadService.processBulkUpload(file);
        } catch (PriceValidationException e) {
            throw new RuntimeException(e);
        }
        return ResponseEntity.ok(result);
    }

    /**
     * Provides a downloadable template for bulk price uploads.
     * Template includes required columns and example data.
     *
     * @return byte array containing the template file
     */
    @GetMapping("/template")
    public ResponseEntity<byte[]> downloadTemplate() {
        byte[] template = bulkUploadService.generateTemplate();
        return ResponseEntity.ok()
                .header("Content-Disposition", "attachment; filename=price_upload_template.xlsx")
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(template);
    }

    /**
     * Retrieves the status of a bulk upload operation.
     *
     * @param batchId The unique identifier for the upload batch
     * @return BulkUploadResult containing current status and any errors
     */
    @GetMapping("/status/{batchId}")
    public ResponseEntity<BulkUploadResultDTO> getUploadStatus(@PathVariable Long batchId) {
        BulkUploadResultDTO status = bulkUploadService.getUploadStatus(batchId);
        return ResponseEntity.ok(status);
    }

    /**
     * Validates a list of prices before actual upload.
     * Useful for client-side validation before committing to a bulk upload.
     *
     * @param prices List of PriceDTO objects to validate
     * @return List of validation error messages, empty if all valid
     */
    @PostMapping("/validate")
    public ResponseEntity<List<String>> validatePrices(@RequestBody List<PriceDTO> prices) {
        List<String> validationErrors = bulkUploadService.validatePrices(prices);
        return ResponseEntity.ok(validationErrors);
    }
}

