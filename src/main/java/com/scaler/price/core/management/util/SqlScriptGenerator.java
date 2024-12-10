package com.scaler.price.core.management.util;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Component
public class SqlScriptGenerator {

    private static final String TEMPLATE_PATH = "uploads/prices/price_upload_template.xlsx";
    private static final String SQL_OUTPUT_PATH = "src/main/resources/db/migration/V2__Insert_Sample_Prices.sql";
    
    public void generateSqlScript() {
        try (InputStream is = new ClassPathResource(TEMPLATE_PATH).getInputStream();
             Workbook workbook = new XSSFWorkbook(is);
             FileOutputStream fos = new FileOutputStream(SQL_OUTPUT_PATH)) {
            
            Sheet sheet = workbook.getSheetAt(0);
            List<String> sqlStatements = new ArrayList<>();
            
            // Skip header row
            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null) continue;
                
                String sql = generateInsertStatement(row);
                if (sql != null) {
                    sqlStatements.add(sql);
                }
            }
            
            // Write SQL statements to file
            String fullSql = String.join("\\n", sqlStatements);
            fos.write(fullSql.getBytes());
            
        } catch (IOException e) {
            throw new RuntimeException("Failed to generate SQL script", e);
        }
    }
    
    private String generateInsertStatement(Row row) {
        try {
            Long productId = getNumericCellValue(row.getCell(0));
            Long sellerId = getNumericCellValue(row.getCell(1));
            Long siteId = getNumericCellValue(row.getCell(2));
            BigDecimal basePrice = getBigDecimalCellValue(row.getCell(3));
            BigDecimal sellingPrice = getBigDecimalCellValue(row.getCell(4));
            BigDecimal mrp = getBigDecimalCellValue(row.getCell(5));
            LocalDateTime effectiveFrom = getDateCellValue(row.getCell(6));
            LocalDateTime effectiveTo = getDateCellValue(row.getCell(7));
            String currency = getStringCellValue(row.getCell(8));
            
            return String.format("""
                INSERT INTO prices (
                    product_id, seller_id, site_id, base_price, selling_price, mrp,
                    effective_from, effective_to, currency, is_active, is_seller_active,
                    is_site_active, created_at, updated_at, version
                ) VALUES (
                    %d, %d, %d, %s, %s, %s, '%s', %s, '%s', true, true, true,
                    CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0
                );""",
                productId, sellerId, siteId,
                basePrice, sellingPrice, mrp,
                effectiveFrom,
                effectiveTo == null ? "NULL" : "'" + effectiveTo + "'",
                currency
            );
            
        } catch (Exception e) {
            return null;
        }
    }
    
    private Long getNumericCellValue(Cell cell) {
        if (cell == null) return null;
        return (long) cell.getNumericCellValue();
    }
    
    private BigDecimal getBigDecimalCellValue(Cell cell) {
        if (cell == null) return null;
        return BigDecimal.valueOf(cell.getNumericCellValue());
    }
    
    private String getStringCellValue(Cell cell) {
        if (cell == null) return null;
        return cell.getStringCellValue();
    }
    
    private LocalDateTime getDateCellValue(Cell cell) {
        if (cell == null) return null;
        return cell.getLocalDateTimeCellValue();
    }
}
