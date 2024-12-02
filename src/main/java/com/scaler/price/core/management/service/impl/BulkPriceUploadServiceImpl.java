package com.scaler.price.core.management.service.impl;

import com.scaler.price.core.bulk.services.AsyncPriceProcessor;
import com.scaler.price.core.management.domain.BulkUploadTracker;
import com.scaler.price.core.management.domain.UploadStatus;
import com.scaler.price.core.management.dto.PriceDTO;
import com.scaler.price.core.management.dto.PriceUploadDTO;
import com.scaler.price.core.management.exceptions.BulkUploadException;
import com.scaler.price.core.management.exceptions.PriceValidationException;
import com.scaler.price.core.management.repository.BulkUploadTrackerRepository;
import com.scaler.price.core.management.service.BulkPriceUploadService;
import com.scaler.price.core.management.service.BulkUploadResultDTO;
import com.scaler.price.core.management.service.PriceService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FilenameUtils;
import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.net.MalformedURLException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Implementation of the BulkPriceUploadService interface.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class BulkPriceUploadServiceImpl implements BulkPriceUploadService {
    private final PriceService priceService;
    private final BulkUploadTrackerRepository trackerRepository;
    private final AsyncPriceProcessor asyncPriceProcessor;
    private final FileStorageService fileStorageService;

    @Override
    @Transactional
    public BulkUploadResultDTO uploadPrices(MultipartFile file, Long sellerId, Long siteId) throws PriceValidationException {
        // Generate unique upload ID
        String uploadId = generateUploadId();

        // Create and save tracker
        BulkUploadTracker tracker = createTracker(uploadId, sellerId, siteId);

        try {
            // Parse and validate file
            List<PriceUploadDTO> prices = parseAndValidateFile(file);
            assert tracker != null;
            tracker.setTotalRecords(prices.size());
            trackerRepository.save(tracker);

            // Start async processing
            asyncPriceProcessor.processPrices(uploadId, prices);

            return createInitialResponse(tracker);
        } catch (Exception e) {
            tracker.setStatus(UploadStatus.FAILED);
            tracker.setFailureCount(tracker.getTotalRecords());
            trackerRepository.save(tracker);
            throw new BulkUploadException("Failed to process bulk upload", e);
        }
    }

    private BulkUploadTracker createTracker(String uploadId, Long sellerId, Long siteId) {
        BulkUploadTracker tracker = new BulkUploadTracker();
        tracker.setUploadId(uploadId);
        tracker.setSellerId(sellerId);
        tracker.setSiteId(siteId);
        tracker.setUploadedBy(SecurityContextHolder.getContext().getAuthentication().getName());
        tracker.setUploadedAt(LocalDateTime.now());
        tracker.setStatus(UploadStatus.PENDING);
        tracker.setProcessedRecords(0);
        tracker.setSuccessCount(0);
        tracker.setFailureCount(0);
        return tracker;
    }


    @Override
    public BulkUploadResultDTO getUploadStatus(String uploadId) {
        return null;
    }



    private BulkUploadResultDTO createInitialResponse(BulkUploadTracker tracker) {
        BulkUploadResultDTO result = new BulkUploadResultDTO();
        result.setUploadId(tracker.getUploadId());
        result.setTotalRecords(tracker.getTotalRecords());
        result.setSuccessCount(tracker.getSuccessCount());
        result.setFailureCount(tracker.getFailureCount());
        result.setStatus(tracker.getStatus());
        if (tracker.getErrorFilePath() != null) {
            result.setDownloadUrl("/api/v1/prices/bulk/download/" + tracker.getUploadId());
        }
        return result;
    }

    @Override
    public Resource downloadErrorReport(String uploadId) {
        BulkUploadTracker tracker = trackerRepository.findByUploadId(uploadId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Upload not found with ID: " + uploadId));

        if (tracker.getErrorFilePath() == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "No error report available for upload ID: " + uploadId);
        }

        try {
            Path errorFile = Paths.get(tracker.getErrorFilePath());
            UrlResource resource = new UrlResource(errorFile.toUri());

            if (resource.exists() && resource.isReadable()) {
                return resource;
            } else {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Error report file not found or not readable");
            }
        } catch (MalformedURLException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error accessing error report file", e);
        }
    }

    @Override
    public List<String> validatePrices(List<PriceDTO> prices) {
        return prices.stream()
                .map(price -> {
                    try {
                        priceService.validatePrice(price);
                        return null; // No validation error
                    } catch (PriceValidationException e) {
                        return e.getMessage();
                    }
                })
                .filter(Objects::nonNull)
                .toList();
    }

    @Override
    public BulkUploadResultDTO processBulkUpload(MultipartFile file, Long sellerId, Long siteId) throws PriceValidationException {
        try {
            List<PriceUploadDTO> prices = parseAndValidateFile(file);
            String uploadId = generateUploadId();
            BulkUploadTracker tracker = createTracker(uploadId, sellerId, siteId);
            assert tracker != null;
            tracker.setTotalRecords(prices.size());
            trackerRepository.save(tracker);

            asyncPriceProcessor.processPrices(uploadId, prices);

            return createInitialResponse(tracker);
        } catch (Exception e) {
            throw new BulkUploadException("Failed to process bulk upload", e);
        }
    }

    @Override
    public byte[] generateTemplate() {
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Price Upload Template");

            // Create header row with cell style
            CellStyle headerStyle = workbook.createCellStyle();
            headerStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerStyle.setFont(headerFont);

            Row headerRow = sheet.createRow(0);
            String[] headers = {
                    "Product ID*", "Seller ID*", "Site ID*", "Base Price*", "Selling Price*",
                    "MRP*", "Price Type*", "Currency*", "Effective From*", "Effective To",
                    "Is Active*", "Status"
            };

            // Create headers
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
                sheet.autoSizeColumn(i);
            }

            // Add example row
            Row exampleRow = sheet.createRow(1);
            exampleRow.createCell(0).setCellValue("PROD123"); // Product ID
            exampleRow.createCell(1).setCellValue("SELLER456"); // Seller ID
            exampleRow.createCell(2).setCellValue("SITE789"); // Site ID
            exampleRow.createCell(3).setCellValue("1000.00"); // Base Price
            exampleRow.createCell(4).setCellValue("900.00"); // Selling Price
            exampleRow.createCell(5).setCellValue("1200.00"); // MRP
            exampleRow.createCell(6).setCellValue("REGULAR"); // Price Type
            exampleRow.createCell(7).setCellValue("INR"); // Currency
            exampleRow.createCell(8).setCellValue("1/1/24 0:00"); // Effective From
            exampleRow.createCell(9).setCellValue("12/31/24 23:59"); // Effective To
            exampleRow.createCell(10).setCellValue("TRUE"); // Is Active
            exampleRow.createCell(11).setCellValue("ACTIVE"); // Status

            // Convert workbook to byte array
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            workbook.write(outputStream);
            return outputStream.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException("Failed to generate price upload template", e);
        }
    }

    private String generateUploadId() {
        return String.format("BU_%s_%s",
                System.currentTimeMillis(),
                UUID.randomUUID().toString().substring(0, 8));
    }

    private List<PriceUploadDTO> parseAndValidateFile(MultipartFile file) throws IOException, InvalidFormatException {
        if (file == null || file.isEmpty()) {
            throw new BulkUploadException("Upload file is empty or null");
        }

        String fileExtension = FilenameUtils.getExtension(file.getOriginalFilename());
        assert fileExtension != null;
        if (!Arrays.asList("xlsx", "xls").contains(fileExtension.toLowerCase())) {
            throw new BulkUploadException("Invalid file format. Only Excel files (xlsx, xls) are supported");
        }

        List<PriceUploadDTO> prices = new ArrayList<>();
        try (Workbook workbook = WorkbookFactory.create(file.getInputStream())) {
            Sheet sheet = workbook.getSheetAt(0);
            Row headerRow = sheet.getRow(0);
            validateHeaders(headerRow);

            int rowNum = 1; // Skip header row
            while (rowNum <= sheet.getLastRowNum()) {
                Row row = sheet.getRow(rowNum);
                if (row != null) {
                    try {
                        PriceUploadDTO price = parseRow(row);
                        price.setRowNumber(rowNum + 1); // Excel row numbers are 1-based
                        validatePrice(price);
                        prices.add(price);
                    } catch (Exception e) {
                        PriceUploadDTO errorPrice = new PriceUploadDTO();
                        errorPrice.setRowNumber(rowNum + 1);
                        errorPrice.setErrorMessage(e.getMessage());
                        prices.add(errorPrice);
                    }
                }
                rowNum++;
            }
        }

        if (prices.isEmpty()) {
            throw new BulkUploadException("No valid price records found in the file");
        }

        return prices;
    }

    private void validateHeaders(Row headerRow) {
        if (headerRow == null) {
            throw new BulkUploadException("Excel file is missing header row");
        }

        List<String> requiredHeaders = Arrays.asList(
                "Product ID", "Seller ID", "Site ID", "Base Price", "Selling Price",
                "MRP", "Price Type", "Currency", "Effective From", "Effective To",
                "Is Active", "Status"
        );

        // Get actual headers from the file
        List<String> actualHeaders = new ArrayList<>();
        for (Cell cell : headerRow) {
            if (cell != null) {
                // Remove asterisk and trim whitespace
                String headerValue = cell.getStringCellValue()
                        .replaceAll("\\*", "")
                        .trim();
                actualHeaders.add(headerValue);
            }
        }

        // Check if all required headers are present
        List<String> missingHeaders = new ArrayList<>();
        for (String required : requiredHeaders) {
            boolean found = actualHeaders.stream()
                    .anyMatch(actual -> actual.equalsIgnoreCase(required));
            if (!found) {
                missingHeaders.add(required);
            }
        }

        if (!missingHeaders.isEmpty()) {
            throw new BulkUploadException("Missing required headers: " + String.join(", ", missingHeaders));
        }

        // Check for any unknown headers
        List<String> unknownHeaders = actualHeaders.stream()
                .filter(actual -> !requiredHeaders.stream()
                        .anyMatch(required -> required.equalsIgnoreCase(actual)))
                .collect(Collectors.toList());

        if (!unknownHeaders.isEmpty()) {
            throw new BulkUploadException("Unknown headers found: " + String.join(", ", unknownHeaders));
        }
    }

    private String getCellValueAsString(Cell cell) {
        if (cell == null) {
            return null;
        }

        switch (cell.getCellType()) {
            case STRING:
                return cell.getStringCellValue().trim();
            case NUMERIC:
                if (DateUtil.isCellDateFormatted(cell)) {
                    return cell.getLocalDateTimeCellValue().toString();
                }
                return String.valueOf(cell.getNumericCellValue());
            case BOOLEAN:
                return String.valueOf(cell.getBooleanCellValue());
            case FORMULA:
                try {
                    return String.valueOf(cell.getNumericCellValue());
                } catch (Exception e) {
                    return cell.getStringCellValue();
                }
            default:
                return null;
        }
    }

    private void validateHeaders(Sheet sheet) throws PriceValidationException {
        Row headerRow = sheet.getRow(0);
        if (headerRow == null) {
            throw new PriceValidationException("Excel file is empty or missing headers");
        }

        Map<Integer, String> expectedHeaders = new HashMap<>();
        expectedHeaders.put(0, "Product ID*");
        expectedHeaders.put(1, "Seller ID*");
        expectedHeaders.put(2, "Site ID*");
        expectedHeaders.put(3, "Base Price*");
        expectedHeaders.put(4, "Selling Price*");
        expectedHeaders.put(5, "MRP*");
        expectedHeaders.put(6, "Price Type*");
        expectedHeaders.put(7, "Currency*");
        expectedHeaders.put(8, "Effective From*");
        expectedHeaders.put(9, "Effective To");
        expectedHeaders.put(10, "Is Active*");
        expectedHeaders.put(11, "Status");

        List<String> missingHeaders = new ArrayList<>();

        for (Map.Entry<Integer, String> entry : expectedHeaders.entrySet()) {
            Cell cell = headerRow.getCell(entry.getKey());
            String headerValue = getCellValueAsString(cell);

            if (headerValue == null || !headerValue.trim().equalsIgnoreCase(entry.getValue())) {
                missingHeaders.add(entry.getValue());
            }
        }

        if (!missingHeaders.isEmpty()) {
            throw new PriceValidationException("Missing or invalid headers: " + String.join(", ", missingHeaders));
        }
    }

    private PriceUploadDTO parseRow(Row row) {
        PriceUploadDTO price = new PriceUploadDTO();

        // Parse IDs
        price.setProductId(getCellValueAsLong(row.getCell(0)));  // Product ID*
        price.setSellerId(getCellValueAsLong(row.getCell(1)));   // Seller ID*
        price.setSiteId(getCellValueAsLong(row.getCell(2)));     // Site ID*

        // Parse prices
        price.setBasePrice(getCellValueAsString(row.getCell(3)));    // Base Price*
        price.setSellingPrice(getCellValueAsString(row.getCell(4))); // Selling Price*
        price.setMrp(getCellValueAsString(row.getCell(5)));          // MRP*

        // Parse other fields
        price.setPriceType(getCellValueAsString(row.getCell(6)));     // Price Type*
        price.setCurrency(getCellValueAsString(row.getCell(7)));      // Currency*
        price.setEffectiveFrom(getCellValueAsString(row.getCell(8))); // Effective From*
        price.setEffectiveTo(getCellValueAsString(row.getCell(9)));   // Effective To

        // Parse boolean and status
        String isActiveStr = getCellValueAsString(row.getCell(10));   // Is Active*
        price.setIsActive(isActiveStr != null ? Boolean.parseBoolean(isActiveStr.toLowerCase()) : null);

        price.setStatus(getCellValueAsString(row.getCell(11)));       // Status

        // Set row number for error tracking
        price.setRowNumber(row.getRowNum() + 1);

        return price;
    }

    private Long getCellValueAsLong(Cell cell) {
        if (cell == null) return null;
        try {
            switch (cell.getCellType()) {
                case NUMERIC:
                    return (long) cell.getNumericCellValue();
                case STRING:
                    return Long.parseLong(cell.getStringCellValue().trim());
                default:
                    return null;
            }
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private void validatePrice(PriceUploadDTO price) {
        if (price == null) {
            throw new IllegalArgumentException("Price cannot be null");
        }

        if (price.getProductId() == null) {
            throw new IllegalArgumentException("Product ID is required");
        }
        if (price.getSellerId() == null) {
            throw new IllegalArgumentException("Seller ID is required");
        }
        if (price.getSiteId() == null) {
            throw new IllegalArgumentException("Site ID is required");
        }

        // Add more specific validations based on the annotations in PriceUploadDTO
        validateDecimalValue(price.getMrp(), "MRP");
        validateDecimalValue(price.getBasePrice(), "Base Price");
        validateDecimalValue(price.getSellingPrice(), "Selling Price");

        // Currency validation
        if (price.getCurrency() == null || price.getCurrency().trim().isEmpty()) {
            throw new IllegalArgumentException("Currency is required");
        }
        if (!price.getCurrency().matches("^[A-Z]{3}$")) {
            throw new IllegalArgumentException("Currency must be a 3-letter uppercase code");
        }

        // Date validation for Effective From
        if (price.getEffectiveFrom() == null || price.getEffectiveFrom().trim().isEmpty()) {
            throw new IllegalArgumentException("Effective From date is required");
        }
        if (!price.getEffectiveFrom().matches("^\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}(\\.\\d{1,3})?Z?$")) {
            throw new IllegalArgumentException("Effective From date must be in format: yyyy-MM-ddTHH:mm:ss (e.g.,2024-01-01T00:00:00Z)");
        }

        // Date validation for Effective To
        if (price.getEffectiveTo() == null || price.getEffectiveTo().trim().isEmpty()) {
            throw new IllegalArgumentException("Effective To date is required");
        }
        if (!price.getEffectiveTo().matches("^\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}(\\.\\d{1,3})?Z?$")) {
            throw new IllegalArgumentException("Effective To date must be in format: yyyy-MM-ddTHH:mm:ss (e.g., 2024-01-01T00:00:00Z)");
        }

        // Price type validation
        if (price.getPriceType() == null || price.getPriceType().trim().isEmpty()) {
            throw new IllegalArgumentException("Price Type is required");
        }
        if (!Arrays.asList("REGULAR", "PROMOTIONAL", "CLEARANCE").contains(price.getPriceType())) {
            throw new IllegalArgumentException("Price Type must be one of: REGULAR, PROMOTIONAL, CLEARANCE");
        }

        // Business rule validations
        try {
            BigDecimal mrp = new BigDecimal(price.getMrp().trim());
            BigDecimal basePrice = new BigDecimal(price.getBasePrice().trim());
            BigDecimal sellingPrice = new BigDecimal(price.getSellingPrice().trim());

            if (basePrice.compareTo(mrp) > 0) {
                throw new IllegalArgumentException("Base Price cannot be greater than MRP");
            }
            if (sellingPrice.compareTo(mrp) > 0) {
                throw new IllegalArgumentException("Selling Price cannot be greater than MRP");
            }
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid price format");
        }
    }

    private void validateDecimalValue(String value, String fieldName) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException(fieldName + " is required");
        }
        try {
            BigDecimal decimalValue = new BigDecimal(value.trim());
            if (decimalValue.compareTo(BigDecimal.ZERO) <= 0) {
                throw new IllegalArgumentException(fieldName + " must be greater than zero");
            }
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(fieldName + " must be a valid decimal number");
        }
    }
}
