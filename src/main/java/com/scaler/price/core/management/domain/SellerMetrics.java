package com.scaler.price.core.management.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;

import jakarta.persistence.Table;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "seller_metrics")
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class SellerMetrics extends AuditInfo{

    @Column(nullable = false)
    private Long sellerId;

    @Column(nullable = false)
    private Long totalSales;

    @Column(nullable = false)
    private BigDecimal totalRevenue;
    
    @Column(nullable = false)
    private BigDecimal customerRating;
    
    @Column(nullable = false)
    private Double fulfillmentRate;
    
    @Column(nullable = false)
    private Double returnRate;
    
    @Column(nullable = false)
    private LocalDateTime accountCreationDate;
    
    // Additional metrics can be added as needed
} 