package com.scaler.price.rule.domain;

import com.scaler.price.core.management.domain.AuditInfo;
import com.scaler.price.rule.domain.constraint.MarginConstraints;
import com.scaler.price.rule.domain.constraint.PriceConstraints;
import com.scaler.price.rule.domain.constraint.RuleConstraints;
import com.scaler.price.rule.domain.constraint.TimeConstraints;
import com.vladmihalcea.hibernate.type.json.JsonBinaryType;

import jakarta.persistence.*;
import lombok.*;

import org.hibernate.annotations.Type;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import lombok.experimental.SuperBuilder;

@Entity
@Table(name = "seller_site_configs")
@Setter
@Getter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class SellerSiteConfig extends AuditInfo {


    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "rule_id", nullable = false)
    private PricingRule rule;

    @Column(nullable = false)
    private String sellerId;

    @Column(nullable = false)
    private String siteId;

    @Type(JsonBinaryType.class)
    @Column(columnDefinition = "jsonb")
    private Set<RuleConstraints> constraints;

    @Column(name = "minimum_price")
    private BigDecimal minimumPrice;

    @Column(name = "maximum_price")
    private BigDecimal maximumPrice;

    @Column(name = "minimum_margin")
    private BigDecimal minimumMargin;

    @Column(name = "maximum_margin")
    private BigDecimal maximumMargin;

    @Column(name = "start_date")
    private Instant startDate;

    @Column(name = "end_date")
    private Instant endDate;

    @ElementCollection
    @CollectionTable(
            name = "config_category_mappings",
            joinColumns = @JoinColumn(name = "config_id")
    )
    @Column(name = "category_id")
    @Builder.Default
    private Set<String> categoryIds = new HashSet<>();

    @ElementCollection
    @CollectionTable(
            name = "config_brand_mappings",
            joinColumns = @JoinColumn(name = "config_id")
    )
    @Column(name = "brand_id")
    @Builder.Default
    private Set<String> brandIds = new HashSet<>();

    @Version
    private Long version;

    @Type(JsonBinaryType.class)
    @Column(columnDefinition = "jsonb")
    private Map<String, Object> metadata;

    @Embedded
    private AuditInfo auditInfo;

    public Set<RuleConstraints> getEffectiveConstraints() {
        Set<RuleConstraints> effectiveConstraints = new HashSet<>();
        if (constraints != null) {
            effectiveConstraints.addAll(constraints);
        }
        if (rule != null && rule.getConstraints() != null) {
            effectiveConstraints.addAll(rule.getConstraints());
        }
        return effectiveConstraints;
    }

    public void setRule(PricingRule rule) {
        if (this.rule != null && this.rule.getSellerSiteConfigs() != null) {
            this.rule.getSellerSiteConfigs().remove(this);
        }
        this.rule = rule;
        if (rule != null && rule.getSellerSiteConfigs() != null) {
            rule.getSellerSiteConfigs().add(this);
        }
    }

    public MarginConstraints getMarginConstraints() {
        return MarginConstraints.builder()
                .minimumMargin(minimumMargin)
                .maximumMargin(maximumMargin)
                .build();
    }

    public Integer getPriority() {
        return (Integer) metadata.getOrDefault("priority", 0);
    }

    public Boolean getIsActive() {
        return (Boolean) metadata.getOrDefault("isActive", true);
    }

    public PriceConstraints getPriceConstraints() {
        for (RuleConstraints constraint : getEffectiveConstraints()) {
            if (constraint instanceof PriceConstraints) {
                return (PriceConstraints) constraint;
            }
        }
        return null;
    }

    public TimeConstraints getTimeConstraints() {
        for (RuleConstraints constraint : getEffectiveConstraints()) {
            if (constraint instanceof TimeConstraints) {
                return (TimeConstraints) constraint;
            }
        }
        return null;
    }
}