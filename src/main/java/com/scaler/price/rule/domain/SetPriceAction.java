package com.scaler.price.rule.domain;


import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import lombok.Data;

import java.math.BigDecimal;

@Entity
@Data
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
