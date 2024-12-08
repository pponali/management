package com.scaler.price.rule.domain.constraint;

import com.scaler.price.core.management.domain.AuditInfo;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;

import java.math.BigDecimal;

@Entity
@Table(name = "price_thresholds")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PriceThreshold extends AuditInfo {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "threshold_value", precision = 10, scale = 2)
    private BigDecimal thresholdValue;
    
    @Column(name = "adjustment_percentage", precision = 5, scale = 2)
    private BigDecimal adjustmentPercentage;

    public BigDecimal getMaxChangePercentage() {
        return thresholdValue != null && adjustmentPercentage != null
                ? thresholdValue.multiply(adjustmentPercentage).divide(BigDecimal.valueOf(100))
                : BigDecimal.ZERO;
    }

    public BigDecimal getFromPrice() {
        return thresholdValue != null
                ? thresholdValue.subtract(getMaxChangePercentage())
                : BigDecimal.ZERO;
    }

    public BigDecimal getToPrice() {
        return thresholdValue != null
                ? thresholdValue.add(getMaxChangePercentage())
                : BigDecimal.ZERO;
    }
}