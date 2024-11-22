package com.scaler.price.core.management.dto;

import lombok.Data;

@Data
public class PriceUploadDTO {
    private String productId;
    private String mrp;
    private String basePrice;
    private String sellingPrice;
    private String currency;
    private String effectiveFrom;
    private String effectiveTo;
    private String priceType;
    private String status;
    private String errorMessage;
    private Integer rowNumber;
}
