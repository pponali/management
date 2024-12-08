package com.scaler.price.rule.domain.constraint;

import com.scaler.price.core.management.domain.AuditInfo;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "regional_adjustments")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class RegionalAdjustment extends AuditInfo {
    
    @Column(name = "region_id")
    private String regionId;
    
    @Column(name = "adjustment_percentage", precision = 5, scale = 2)
    private BigDecimal adjustmentPercentage;
    
    @ElementCollection
    @CollectionTable(name = "regional_adjustment_excluded_categories",
            joinColumns = @JoinColumn(name = "regional_adjustment_id"))
    @Column(name = "category_id")
    private Set<String> excludedCategories = new HashSet<>();
}
