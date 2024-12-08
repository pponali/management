package com.scaler.price.core.management.domain;

import com.scaler.price.rule.domain.RuleType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;
import java.time.LocalDateTime;


@Entity
@Table(name = "price_events") // Table only defined here in parent class
@Getter @Setter
@SuperBuilder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class PriceEventEntity extends AuditInfo {

    @Column(name = "event_id")
    private Long eventId;
    
    private String eventType;
    
    @Column(nullable = false)
    private LocalDateTime timestamp;
    
    @Column(name = "price_id")
    private Long priceId;
    
    @Column(name = "rule_id")
    private Long ruleId;
    
    private String ruleName;
    
    @Enumerated(EnumType.STRING)
    private RuleType ruleType;
    
    @Column(precision = 10, scale = 2)
    private BigDecimal basePrice;
    
    @Column(precision = 10, scale = 2)
    private BigDecimal sellingPrice;
    
    @Column(precision = 10, scale = 2)
    private BigDecimal margin;
    
    @Column(precision = 10, scale = 2)
    private BigDecimal markupPercent;
    
    private String currency;
    
    private LocalDateTime effectiveFrom;
    private LocalDateTime effectiveTo;
    private Boolean isActive;
    private String status;
    private Long sellerId;
    private Long siteId;
    private Long productId;
}
