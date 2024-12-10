package com.scaler.price.core.management.domain;

import jakarta.persistence.Embeddable;
import lombok.Data;

@Embeddable
@Data
public class SellerAttributes {
    private String sellerName;
    private SellerType sellerType;
    private SellerCategory sellerCategory;
    private FulfilmentType fulfilmentType;
    private String sellerRegion;
    private Boolean isSellerActive;
}