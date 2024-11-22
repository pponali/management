package com.scaler.price.core.management.dto;

import lombok.Data;

@Data
public class SiteAttributesDTO {
    private String siteName; // scaler, LUXURY, MARKETPLACE
    private String channel; // WEB, MOBILE, POS
    private String region;
    private String businessUnit;
    private Boolean isSiteActive;
}
