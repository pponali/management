package com.scaler.price.core.management.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;

@Entity
@Table(name = "failed_prices")
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class FailedPrice extends AuditInfo {

    @Column(name = "upload_id")
    private String uploadId;

    @Column(name = "product_id")
    private Long productId;

    @Column(name = "seller_id")
    private Long sellerId;

    @Column(name = "site_id")
    private Long siteId;

    @Column(name = "base_price")
    private BigDecimal basePrice;

    @Column(name = "selling_price")
    private BigDecimal sellingPrice;

    @Column(name = "mrp")
    private BigDecimal mrp;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;
}
