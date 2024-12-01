package com.scaler.price.rule.domain.constraint;

import com.scaler.price.rule.domain.RuleType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;

@Entity
@DiscriminatorValue("pricing_rule_constraints") 
@Getter
@Setter
@SuperBuilder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class PricingRuleConstraints extends RuleConstraints {
    @Column(name = "minimum_price", insertable = false, updatable = false)
    private BigDecimal minimumPrice;

    @Column(name = "maximum_price", insertable = false, updatable = false)
    private BigDecimal maximumPrice;

    @Column(name = "minimum_margin", insertable = false, updatable = false)
    private BigDecimal minimumMargin;

    @Column(name = "maximum_margin", insertable = false, updatable = false)
    private BigDecimal maximumMargin;

    @Column(name = "effective_from", insertable = false, updatable = false)
    private LocalDateTime effectiveFrom;

    @Column(name = "effective_to", insertable = false, updatable = false)
    private LocalDateTime effectiveTo;

    @Column(name = "rule_type", insertable = false, updatable = false)
    @Enumerated(EnumType.STRING)
    private RuleType ruleType;

    @Column(name = "priority", insertable = false, updatable = false)
    private Integer priority;

    @Column(name = "start_date")
    private Instant startDate;

    @Column(name = "end_date")
    private Instant endDate;

}
