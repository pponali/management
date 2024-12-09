package com.scaler.price.core.management.service.impl;

import com.scaler.price.core.bulk.services.AsyncPriceProcessor;
import com.scaler.price.core.management.domain.BulkUploadTracker;
import com.scaler.price.core.management.domain.UploadStatus;
import com.scaler.price.core.management.dto.BulkUploadResultDTO;
import com.scaler.price.core.management.dto.FailedPriceDTO;
import com.scaler.price.core.management.dto.PriceDTO;
import com.scaler.price.core.management.dto.PriceUploadDTO;
import com.scaler.price.core.management.exceptions.BulkUploadException;
import com.scaler.price.core.management.exceptions.PriceValidationException;
import com.scaler.price.core.management.mapper.FailedPriceMapper;
import com.scaler.price.core.management.mappers.BulkUploadTrackerMapper;
import com.scaler.price.core.management.repository.BulkUploadTrackerRepository;
import com.scaler.price.core.management.repository.FailedPricesRepository;
import com.scaler.price.core.management.service.BulkPriceUploadService;
import com.scaler.price.core.management.service.PriceService;
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
    private final BulkUploadTrackerMapper bulkUploadTrackerMapper;
    private final BulkUploadTrackerRepository trackerRepository;
    private final AsyncPriceProcessor asyncPriceProcessor;
    private final FileStorageService fileStorageService;
    private final FailedPricesRepository failedPricesRepository;
    private final FailedPriceMapper failedPriceMapper;

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
        if (uploadId == null || uploadId.trim().isEmpty()) {
            return BulkUploadResultDTO.builder()
                    .status(UploadStatus.NOT_FOUND)
                    .failureCount(0)
                    .successCount(0)
                    .totalRecords(0)
                    .build();
        }

        Optional<BulkUploadTracker> bulkUploadTracker = trackerRepository.findByUploadId(uploadId);
        if (!bulkUploadTracker.isPresent()) {
            return BulkUploadResultDTO.builder()
                    .uploadId(uploadId)
                    .status(UploadStatus.FAILED)
                    .failureCount(0)
                    .successCount(0)
                    .totalRecords(0)
                    .build();
        }

        BulkUploadTracker tracker = bulkUploadTracker.get();
        List<FailedPriceDTO> failedRecords = failedPricesRepository.findByUploadId(uploadId)
                .stream()
                .map(failedPriceMapper::toDTO)
                .collect(Collectors.toList());

        return BulkUploadResultDTO.builder()
                .uploadId(uploadId)
                .status(tracker.getStatus())
                .failureCount(tracker.getFailureCount())
                .successCount(tracker.getSuccessCount())
                .totalRecords(tracker.getProcessedRecords())
                .failedRecords(failedRecords)
                .downloadUrl(tracker.getErrorFilePath() != null ? "/api/v1/prices/bulk/download/" + tracker.getUploadId() : null)
                .build();
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

            // Filter out invalid records
            List<PriceUploadDTO> validPrices = prices.stream()
                .filter(price -> {
                    validatePrice(price);
                    return true;
                })
                .collect(Collectors.toList());

            if (validPrices.isEmpty()) {
                throw new BulkUploadException("No valid records found in the file");
            }

            tracker.setTotalRecords(prices.size());
            tracker.setFailureCount(prices.size() - validPrices.size());
            trackerRepository.save(tracker);

            // Only process valid prices
            asyncPriceProcessor.processPrices(uploadId, validPrices);

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

    private List<PriceUploadDTO> parseAndValidateFile(MultipartFile file) throws IOException, InvalidFormatException, PriceValidationException {
        log.info("Starting to parse and validate file: {}", file.getOriginalFilename());

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

        log.info("File parsing and validation completed successfully for file: {}", file.getOriginalFilename());
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

    @Override
    public BulkUploadResultDTO uploadPrices(MultipartFile file, Long sellerId, Long siteId) throws PriceValidationException {
        // Generate unique upload ID
        String uploadId = generateUploadId();

        // Create and save tracker
        BulkUploadTracker tracker = createTracker(uploadId, sellerId, siteId);
        trackerRepository.save(tracker);  // Save tracker before async processing

        try {
            // Parse and validate file
            List<PriceUploadDTO> prices = parseAndValidateFile(file);
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

    private BigDecimal getCellValueAsBigDecimal(Cell cell) throws PriceValidationException {
        if (cell == null) {
            return null;
        }

        try {
            switch (cell.getCellType()) {
                case NUMERIC:
                    return BigDecimal.valueOf(cell.getNumericCellValue());
                case STRING:
                    String stringValue = cell.getStringCellValue().trim();
                    return stringValue.isEmpty() ? null : new BigDecimal(stringValue);
                case BLANK:
                    return null;
                default:
                    throw new PriceValidationException("Invalid cell type for price value at column: " + cell.getColumnIndex());
            }
        } catch (NumberFormatException e) {
            throw new PriceValidationException("Invalid price value at column: " + cell.getColumnIndex());
        }
    }

    private PriceUploadDTO parseRow(Row row) throws PriceValidationException {
        log.debug("Parsing row number: {}", row.getRowNum() + 1);

        PriceUploadDTO price = new PriceUploadDTO();

        try {
            // Parse IDs
            price.setProductId(getCellValueAsLong(row.getCell(0)));      // Product ID*
            log.debug("Parsed Product ID: {}", price.getProductId());

            price.setSellerId(getCellValueAsLong(row.getCell(1)));       // Seller ID*
            log.debug("Parsed Seller ID: {}", price.getSellerId());

            price.setSiteId(getCellValueAsLong(row.getCell(2)));         // Site ID*
            log.debug("Parsed Site ID: {}", price.getSiteId());

            // Parse prices
            price.setBasePrice(getCellValueAsBigDecimal(row.getCell(3)));    // Base Price*
            log.debug("Parsed Base Price: {}", price.getBasePrice());

            price.setSellingPrice(getCellValueAsBigDecimal(row.getCell(4))); // Selling Price*
            log.debug("Parsed Selling Price: {}", price.getSellingPrice());

            price.setMrp(getCellValueAsBigDecimal(row.getCell(5)));          // MRP*
            log.debug("Parsed MRP: {}", price.getMrp());

            // Parse other fields
            price.setPriceType(getCellValueAsString(row.getCell(6)));     // Price Type*
            log.debug("Parsed Price Type: {}", price.getPriceType());

            price.setCurrency(getCellValueAsString(row.getCell(7)));      // Currency*
            log.debug("Parsed Currency: {}", price.getCurrency());

            price.setEffectiveFrom(getCellValueAsString(row.getCell(8))); // Effective From*
            log.debug("Parsed Effective From: {}", price.getEffectiveFrom());

            price.setEffectiveTo(getCellValueAsString(row.getCell(9)));   // Effective To
            log.debug("Parsed Effective To: {}", price.getEffectiveTo());

            // Parse boolean and status
            String isActiveStr = getCellValueAsString(row.getCell(10));   // Is Active*
            price.setIsActive(isActiveStr != null ? Boolean.parseBoolean(isActiveStr.toLowerCase()) : null);
            log.debug("Parsed Is Active: {}", price.getIsActive());

            price.setStatus(getCellValueAsString(row.getCell(11)));       // Status
            log.debug("Parsed Status: {}", price.getStatus());

            // Set row number for error tracking
            price.setRowNumber(row.getRowNum() + 1);
            log.debug("Set row number for error tracking: {}", price.getRowNumber());

        } catch (Exception e) {
            log.error("Error parsing row number: {}. Error: {}", row.getRowNum() + 1, e.getMessage(), e);
            throw new BulkUploadException("Error parsing row: " + (row.getRowNum() + 1), e);
        }

        return price;
    }

    private Long getCellValueAsLong(Cell cell) throws PriceValidationException {
        if (cell == null) {
            return null;
        }

        try {
            switch (cell.getCellType()) {
                case NUMERIC:
                    return (long) cell.getNumericCellValue();
                case STRING:
                    String stringValue = cell.getStringCellValue().trim();
                    return stringValue.isEmpty() ? null : Long.parseLong(stringValue);
                case BLANK:
                    return null;
                default:
                    throw new PriceValidationException("Invalid cell type for numeric value at column: " + cell.getColumnIndex());
            }
        } catch (NumberFormatException e) {
            throw new PriceValidationException("Invalid numeric value at column: " + cell.getColumnIndex());
        }
    }

    private void validatePrice(PriceUploadDTO price) {
        List<String> errors = new ArrayList<>();

        // Basic null checks
        if (price.getProductId() == null) {
            errors.add("Product ID is required");
        }
        if (price.getSellerId() == null) {
            errors.add("Seller ID is required");
        }
        if (price.getSiteId() == null) {
            errors.add("Site ID is required");
        }

        // Price validations
        try {
            BigDecimal basePrice = price.getBasePrice();
            BigDecimal sellingPrice = price.getSellingPrice();
            BigDecimal mrp = price.getMrp();

            // Validate price relationships
            if (basePrice.compareTo(mrp) > 0) {
                errors.add("Base Price cannot be greater than MRP");
            }
            if (sellingPrice.compareTo(mrp) > 0) {
                errors.add("Selling Price cannot be greater than MRP");
            }
            if (sellingPrice.compareTo(basePrice) > 0) {
                errors.add("Selling price cannot be greater than base price");
            }

            // Validate positive values
            if (basePrice.compareTo(BigDecimal.ZERO) <= 0) {
                errors.add("Base Price must be greater than zero");
            }
            if (sellingPrice.compareTo(BigDecimal.ZERO) <= 0) {
                errors.add("Selling Price must be greater than zero");
            }
            if (mrp.compareTo(BigDecimal.ZERO) <= 0) {
                errors.add("MRP must be greater than zero");
            }
        } catch (NullPointerException e) {
            errors.add("Invalid price format");
        }

        // If any validation errors exist, throw exception
        if (!errors.isEmpty()) {
            price.setStatus("FAILED");
            price.setErrorMessage(String.join("; ", errors));
        }
    }
}
