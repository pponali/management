package com.scaler.price.rule.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;

@Entity
@Table(name = "seller_limits")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SellerLimits {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "seller_id")
    private Long sellerId;
    
    @Column(name = "limit_value")
    private int limitValue;
    
    @Column(name = "max_rules")
    private long maxRules;
    
    @Column(name = "max_discount")
    private BigDecimal maxDiscount;
    
    @Column(name = "is_active")
    private boolean isActive = true;
    
    public SellerLimits(int limitValue) {
        this.limitValue = limitValue;
    }
    
    public static SellerLimits getDefaultLimits() {
        SellerLimits limits = new SellerLimits(100);
        limits.setMaxRules(10);
        limits.setMaxDiscount(BigDecimal.valueOf(50));
        return limits;
    }
}
