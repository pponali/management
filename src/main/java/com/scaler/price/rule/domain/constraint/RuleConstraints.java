package com.scaler.price.rule.domain.constraint;

import com.scaler.price.core.management.domain.AuditInfo;
import com.scaler.price.rule.domain.RuleType;
import jakarta.persistence.Column;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.MappedSuperclass;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;

@MappedSuperclass
@NoArgsConstructor
@SuperBuilder
@Getter
@Setter
public class RuleConstraints extends AuditInfo{
    
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

    @Column(name = "category_id")
    private Long categoryId;

    public RuleConstraints(BigDecimal minimumPrice, BigDecimal maximumPrice,
                         BigDecimal minimumMargin, BigDecimal maximumMargin,
                         LocalDateTime effectiveFrom, LocalDateTime effectiveTo,
                         RuleType ruleType, Boolean isActive, Integer priority,
                         Instant startDate, Instant endDate, Long categoryId) {
        this.minimumPrice = minimumPrice;
        this.maximumPrice = maximumPrice;
        this.minimumMargin = minimumMargin;
        this.maximumMargin = maximumMargin;
        this.effectiveFrom = effectiveFrom;
        this.effectiveTo = effectiveTo;
        this.ruleType = ruleType;
        this.active = isActive;
        this.priority = priority;
        this.startDate = startDate;
        this.endDate = endDate;
        this.categoryId = categoryId;
    }
    
}
