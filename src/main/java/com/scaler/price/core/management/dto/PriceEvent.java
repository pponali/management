package com.scaler.price.core.management.dto;

import com.scaler.price.rule.domain.RuleType;

import lombok.AllArgsConstructor;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;
import java.time.LocalDateTime;


@Setter
@Getter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class PriceEvent{
    private Long eventId;
    private String eventType;
    private LocalDateTime timestamp;
    private Long priceId;
    private Long ruleId;
    private String ruleName;
    private RuleType ruleType;
    
    // Price data fields
    private Long productId;
    private Long sellerId;
    private Long siteId;
    private BigDecimal basePrice;
    private BigDecimal sellingPrice;
    private BigDecimal mrp;
    private String priceType;
    private String currency;
    private LocalDateTime effectiveFrom;
    private LocalDateTime effectiveTo;
    private Boolean isActive;
    private String status;
    
    private Long version;
    
    // Convert from PriceDTO
    public void setPayloadFromDTO(PriceDTO dto) {
        if (dto != null) {
            this.productId = dto.getProductId();
            this.sellerId = dto.getSellerId();
            this.siteId = dto.getSiteId();
            this.basePrice = dto.getBasePrice();
            this.sellingPrice = dto.getSellingPrice();
            this.mrp = dto.getMrp();
            this.priceType = dto.getPriceType();
            this.currency = dto.getCurrency();
            this.effectiveFrom = dto.getEffectiveFrom();
            this.effectiveTo = dto.getEffectiveTo();
            this.isActive = dto.getIsActive();
            this.status = dto.getStatus();
        }
    }
    
    // Convert to PriceDTO
    public PriceDTO toDTO() {
        return PriceDTO.builder()
                .productId(this.productId)
                .sellerId(this.sellerId)
                .siteId(this.siteId)
                .basePrice(this.basePrice)
                .sellingPrice(this.sellingPrice)
                .mrp(this.mrp)
                .priceType(this.priceType)
                .currency(this.currency)
                .effectiveFrom(this.effectiveFrom)
                .effectiveTo(this.effectiveTo)
                .isActive(this.isActive)
                .status(this.status)
                .build();
    }
}
