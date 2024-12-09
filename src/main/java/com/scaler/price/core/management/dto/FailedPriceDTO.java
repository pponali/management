package com.scaler.price.core.management.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FailedPriceDTO {
    private Long id;
    private String uploadId;
    private Long productId;
    private BigDecimal basePrice;
    private BigDecimal sellingPrice;
    private String errorMessage;
    private LocalDateTime createdAt;
}
