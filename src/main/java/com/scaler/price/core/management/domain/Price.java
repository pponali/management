package com.scaler.price.core.management.domain;


import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
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
@Getter
@Setter
@SuperBuilder(toBuilder = true,builderMethodName = "priceBuilder")
@NoArgsConstructor
@AllArgsConstructor
public class Price extends AuditInfo{

    @Column(name = "product_id", nullable = false)
    private Long productId;

    @Column(name = "seller_id", nullable = false)
    private Long sellerId;

    @Column(name = "site_id", nullable = false)
    private Long siteId;

    @Column(name = "is_seller_active")
    private Boolean isSellerActive;

    @Column(name = "is_site_active")
    private Boolean isSiteActive;


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

    @Enumerated(EnumType.STRING)
    @Column(name = "fulfilment_type")
    private FulfilmentType fulfilmentType;
}