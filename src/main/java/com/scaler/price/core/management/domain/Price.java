package com.scaler.price.core.management.domain;

import lombok.Data;
import jakarta.persistence.*;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "prices",
        uniqueConstraints = {
                @UniqueConstraint(
                        columnNames = {"product_id", "seller_id", "site_id", "effective_from", "price_type"}
                )
        }
)
@Data
@SuperBuilder
public class Price extends AuditInfo{

    @Column(name = "product_id", nullable = false)
    private String productId;

    @Column(name = "seller_id", nullable = false)
    private String sellerId;

    @Column(name = "site_id", nullable = false)
    private String siteId;

    @Column(nullable = false)
    private BigDecimal basePrice;

    @Column(nullable = false)
    private BigDecimal sellingPrice;

    private BigDecimal mrp;

    @Enumerated(EnumType.STRING)
    @Column(name = "price_type")
    private PriceType priceType;

    @Column(nullable = false)
    private String currency;

    @Column(name = "effective_from", nullable = false)
    private LocalDateTime effectiveFrom;

    @Column(name = "effective_to")
    private LocalDateTime effectiveTo;

    @Column(nullable = false)
    private Boolean isActive;

    @Embedded
    private SellerAttributes sellerAttributes;

    @Embedded
    private SiteAttributes siteAttributes;

    @Embedded
    private AuditInfo auditInfo;

    @Version
    private Long version;
}