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
import jakarta.annotation.Resource;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FilenameUtils;
import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.*;

import static org.apache.commons.lang3.StringUtils.isEmpty;
import static org.apache.commons.lang3.StringUtils.join;


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
        return null;
    }

    private BulkUploadResultDTO createInitialResponse(BulkUploadTracker tracker) {

        return null;
    }

    @Override
    public BulkUploadResultDTO getUploadStatus(Long uploadId) {
        return null;
    }

    @Override
    public Resource downloadErrorReport(Long uploadId) {
        return null;
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
    public BulkUploadResultDTO processBulkUpload(MultipartFile file) throws PriceValidationException {
        try {
            List<PriceUploadDTO> prices = parseAndValidateFile(file);
            String uploadId = generateUploadId();
            BulkUploadTracker tracker = createTracker(uploadId, null, null);
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
            exampleRow.createCell(8).setCellValue("2024-01-01 00:00:00"); // Effective From
            exampleRow.createCell(9).setCellValue("2024-12-31 23:59:59"); // Effective To
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
            "Product ID", "MRP", "Base Price", "Selling Price",
            "Currency", "Effective From", "Effective To", "Price Type", "Status"
        );

        for (int i = 0; i < requiredHeaders.size(); i++) {
            Cell cell = headerRow.getCell(i);
            if (cell == null || !cell.getStringCellValue().trim().equalsIgnoreCase(requiredHeaders.get(i))) {
                throw new BulkUploadException("Invalid or missing header: " + requiredHeaders.get(i));
            }
        }
    }

    private PriceUploadDTO parseRow(Row row) {
        PriceUploadDTO price = new PriceUploadDTO();

        // Change from string parsing to direct numeric assignment
        price.setProductId(getCellValueAsLong(row.getCell(0)));
        price.setSellerId(getCellValueAsLong(row.getCell(1)));
        price.setSiteId(getCellValueAsLong(row.getCell(2)));
        price.setMrp(getCellValueAsString(row.getCell(1)));
        price.setBasePrice(getCellValueAsString(row.getCell(2)));
        price.setSellingPrice(getCellValueAsString(row.getCell(3)));
        price.setCurrency(getCellValueAsString(row.getCell(4)));
        price.setEffectiveFrom(getCellValueAsString(row.getCell(5)));
        price.setEffectiveTo(getCellValueAsString(row.getCell(6)));
        price.setPriceType(getCellValueAsString(row.getCell(7)));
        price.setStatus(getCellValueAsString(row.getCell(8)));

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

    private void validatePrice(PriceUploadDTO price) {
        List<String> errors = new ArrayList<>();

        if (price.getProductId() == null) {
            errors.add("Product ID is required");
        }
        if (price.getSellerId() == null) {
            errors.add("Seller ID is required");
        }
        if (price.getSiteId() == null) {
            errors.add("Site ID is required");
        }
        if (isEmpty(price.getMrp())) {
            errors.add("MRP is required");
        }
        if (isEmpty(price.getBasePrice())) {
            errors.add("Base Price is required");
        }
        if (isEmpty(price.getSellingPrice())) {
            errors.add("Selling Price is required");
        }
        if (isEmpty(price.getCurrency())) {
            errors.add("Currency is required");
        }
        if (isEmpty(price.getEffectiveFrom())) {
            errors.add("Effective From date is required");
        }
        if (isEmpty(price.getPriceType())) {
            errors.add("Price Type is required");
        }

        // Validate numeric values
        try {
            if (price.getMrp() != null) {
                new BigDecimal(price.getMrp());
            }
            if (price.getBasePrice() != null) {
                new BigDecimal(price.getBasePrice());
            }
            if (price.getSellingPrice() != null) {
                new BigDecimal(price.getSellingPrice());
            }
        } catch (NumberFormatException e) {
            errors.add("Invalid price format. Prices must be valid numbers");
        }

        // Validate dates
        try {
            if (price.getEffectiveFrom() != null) {
                LocalDateTime.parse(price.getEffectiveFrom());
            }
            if (price.getEffectiveTo() != null) {
                LocalDateTime.parse(price.getEffectiveTo());
            }
        } catch (DateTimeParseException e) {
            errors.add("Invalid date format. Dates must be in ISO format (yyyy-MM-dd'T'HH:mm:ss)");
        }

        if (!errors.isEmpty()) {
            throw new BulkUploadException(join(errors, ", "));
        }
    }
}
