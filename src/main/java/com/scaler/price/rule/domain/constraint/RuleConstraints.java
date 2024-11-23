package com.scaler.price.rule.domain.constraint;

import com.scaler.price.rule.domain.RuleType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.UUID;

@MappedSuperclass
@NoArgsConstructor
@AllArgsConstructor
public abstract class RuleConstraints {
    
    @Column(name = "minimum_price")
    private BigDecimal minimumPrice;

    @Column(name = "maximum_price")
    private BigDecimal maximumPrice;

    @Column(name = "minimum_margin")
    private BigDecimal minimumMargin;

    @Column(name = "maximum_margin")
    private BigDecimal maximumMargin;

    @Column(name = "effective_from")
    private LocalDateTime effectiveFrom;

    @Column(name = "effective_to")
    private LocalDateTime effectiveTo;

    @Column(name = "rule_type")
    @Enumerated(EnumType.STRING)
    private RuleType ruleType;

    @Column(name = "is_active")
    private Boolean active = true;

    @Column(name = "priority")
    private Integer priority;

    @Column(name = "start_date")
    private Instant startDate;

    @Column(name = "end_date")
    private Instant endDate;

    @Column(name = "description")
    private String description;

    public RuleConstraints(BigDecimal minimumPrice, BigDecimal maximumPrice,
                         BigDecimal minimumMargin, BigDecimal maximumMargin,
                         LocalDateTime effectiveFrom, LocalDateTime effectiveTo,
                         Boolean active, Integer priority, RuleType ruleType,
                         Instant startDate, Instant endDate) {
        this.minimumPrice = minimumPrice;
        this.maximumPrice = maximumPrice;
        this.minimumMargin = minimumMargin;
        this.maximumMargin = maximumMargin;
        this.effectiveFrom = effectiveFrom;
        this.effectiveTo = effectiveTo;
        this.active = active;
        this.priority = priority;
        this.ruleType = ruleType;
        this.startDate = startDate;
        this.endDate = endDate;
    }
    
}
