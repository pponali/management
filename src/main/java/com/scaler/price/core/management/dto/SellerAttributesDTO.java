package com.scaler.price.core.management.dto;


import lombok.Data;

@Data
public class SellerAttributesDTO {
    private String sellerName;
    private String sellerType; // MARKETPLACE, DIRECT, CONSIGNMENT
    private String sellerCategory; // PREFERRED, REGULAR, NEW
    private String fulfilmentType; // SELLER_FULFILLED, scalerFULFILLED
    private String sellerRegion;
    private Boolean isSellerActive;
}
