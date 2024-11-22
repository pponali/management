package com.scaler.price.core.management.domain;

import lombok.Data;
import jakarta.persistence.Embeddable;

@Embeddable
@Data
public class SiteAttributes {
    private String siteName; // scaler, LUXURY, MARKETPLACE
    private String channel; // WEB, MOBILE, POS
    private String region;
    private String businessUnit;
    private Boolean isSiteActive;
}
