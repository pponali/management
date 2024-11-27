package com.scaler.price.rule.domain;


import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Entity
@Getter
@Setter
@EqualsAndHashCode(callSuper=false)
public class SetPriceAction extends RuleAction{

    @Column
    private BigDecimal price;

    @Column

    private BigDecimal maxAllowedPrice;

    @Column
    private BigDecimal currentPrice;

    @Column
    public BigDecimal minMarginPercentage;

    @Column
    public BigDecimal costPrice;
}
