package com.scaler.price.core.management.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.Data;
import org.hibernate.validator.constraints.Length;

@Data
@Schema(description = "Data Transfer Object for bulk price upload")
public class PriceUploadDTO {
    @Schema(description = "Product identifier", example = "123456")
    @NotNull(message = "Product ID is required")
    private Long productId;

    @Schema(description = "Seller identifier", example = "789")
    @NotNull(message = "Seller ID is required")
    private Long sellerId;

    @Schema(description = "Site identifier", example = "1")
    @NotNull(message = "Site ID is required")
    private Long siteId;

    @Schema(description = "Maximum Retail Price", example = "999.99")
    @NotNull(message = "MRP is required")
    @DecimalMin(value = "0.0", message = "MRP must be greater than or equal to 0")
    private String mrp;

    @Schema(description = "Base price before any discounts", example = "899.99")
    @NotNull(message = "Base price is required")
    @DecimalMin(value = "0.0", message = "Base price must be greater than or equal to 0")
    private String basePrice;

    @Schema(description = "Final selling price", example = "849.99")
    @NotNull(message = "Selling price is required")
    @DecimalMin(value = "0.0", message = "Selling price must be greater than or equal to 0")
    private String sellingPrice;

    @Schema(description = "Currency code in ISO format", example = "USD")
    @NotBlank(message = "Currency is required")
    @Length(min = 3, max = 3, message = "Currency must be a 3-letter code")
    @Pattern(regexp = "^[A-Z]{3}$", message = "Currency must be a 3-letter uppercase code")
    private String currency;

    @Schema(description = "Price effective start date (ISO format)", example = "2024-01-01T00:00:00Z")
    @NotBlank(message = "Effective from date is required")
    @Pattern(regexp = "^\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}(\\.\\d{1,3})?Z?$", 
            message = "Effective from date must be in ISO format (yyyy-MM-ddTHH:mm:ss)")
    private String effectiveFrom;

    @Schema(description = "Price effective end date (ISO format)", example = "2024-12-31T23:59:59Z")
    @Pattern(regexp = "^\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}(\\.\\d{1,3})?Z?$", 
            message = "Effective to date must be in ISO format (yyyy-MM-ddTHH:mm:ss)")
    private String effectiveTo;

    @Schema(description = "Type of price", example = "REGULAR", allowableValues = {"REGULAR", "PROMOTIONAL", "CLEARANCE"})
    @NotBlank(message = "Price type is required")
    @Pattern(regexp = "^(REGULAR|PROMOTIONAL|CLEARANCE)$", 
            message = "Price type must be one of: REGULAR, PROMOTIONAL, CLEARANCE")
    private String priceType;

    @Schema(description = "Whether the price is active", example = "true")
    private Boolean isActive;

    @Schema(description = "Error message in case of validation failure")
    private String errorMessage;

    @Schema(description = "Status of the price upload")
    private String status;

    @Schema(description = "Row number in the uploaded file")
    private Integer rowNumber;

    // Utility method to convert String ID to Long
    @Schema(hidden = true)
    public static Long parseLongOrNull(String value) {
        try {
            return value != null ? Long.parseLong(value.trim()) : null;
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
