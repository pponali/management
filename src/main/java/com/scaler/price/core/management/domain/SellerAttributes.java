package com.scaler.price.core.management.domain;

import jakarta.persistence.Embeddable;
import lombok.Data;

@Embeddable
@Data
public class SellerAttributes {
    private String sellerName;
    private String sellerType; // MARKETPLACE, DIRECT, CONSIGNMENT
    private String sellerCategory; // PREFERRED, REGULAR, NEW
    private String fulfilmentType; // SELLER_FULFILLED, scalerFULFILLED
    private String sellerRegion;
    private Boolean isSellerActive;
}