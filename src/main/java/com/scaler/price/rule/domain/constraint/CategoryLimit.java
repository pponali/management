package com.scaler.price.rule.domain.constraint;

import com.scaler.price.core.management.domain.AuditInfo;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Entity
@Table(name = "category_limits")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CategoryLimit extends AuditInfo {

    @Column(name = "category_id")
    private String categoryId;

    @Column(name = "category_name")
    private String categoryName;
    
    @Column(name = "min_price", precision = 10, scale = 2)
    private BigDecimal minPrice;

    @Column(name = "max_price", precision = 10, scale = 2)
    private BigDecimal maxPrice;

    @Column(name = "min_discount_percentage", precision = 5, scale = 2)
    private BigDecimal minDiscountPercentage;

    @Column(name = "max_discount_percentage", precision = 5, scale = 2)
    private BigDecimal maxDiscountPercentage;
}
