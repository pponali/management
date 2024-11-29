package com.scaler.price.rule.domain;

import com.scaler.price.core.management.domain.AuditInfo;
import com.scaler.price.rule.domain.constraint.PriceConstraints;
import com.scaler.price.rule.domain.constraint.PricingRuleConstraints;
import com.scaler.price.rule.domain.constraint.RuleConstraints;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;

@Entity
@Table(name = "pricing_rules")
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class PricingRule extends AuditInfo { 

    @Column
    private Set<RuleConstraints> constraints;

    @Column(nullable = false)
    private String ruleName;

    @Column(nullable = false)
    private String description;

    @Enumerated(EnumType.STRING)
    private RuleType ruleType;

    @Enumerated(EnumType.STRING)
    @Column(name = "rule_status")
    private RuleStatus status = RuleStatus.DRAFT;

    @Builder.Default
    private Set<Long> sellerIds = new HashSet<>();

    @ElementCollection
    @CollectionTable(
            name = "rule_site_mappings",
            joinColumns = @JoinColumn(name = "rule_id")
    )
    @Column(name = "site_id")
    @Builder.Default
    private Set<Long> siteIds = new HashSet<>();

    @ElementCollection
    @CollectionTable(
            name = "rule_category_mappings",
            joinColumns = @JoinColumn(name = "rule_id")
    )
    @Column(name = "category_id")
    @Builder.Default
    private Set<Long> categoryIds = new HashSet<>();

    @ElementCollection
    @CollectionTable(
            name = "rule_brand_mappings",
            joinColumns = @JoinColumn(name = "rule_id")
    )
    @Column(name = "brand_id")
    @Builder.Default
    private Set<String> brandIds = new HashSet<>();

    @Column(name = "minimum_price")
    private BigDecimal minimumPrice;

    @Column(name = "maximum_price")
    private BigDecimal maximumPrice;

    @Column(name = "minimum_margin")
    private BigDecimal minimumMargin;

    @Column(name = "maximum_margin")
    private BigDecimal maximumMargin;

    @Column(name = "margin_percentage")
    private BigDecimal marginPercentage;

    @Column(name = "priority")
    private Integer priority;

    @OneToMany(
            mappedBy = "rule",
            cascade = CascadeType.ALL,
            orphanRemoval = true
    )
    @Builder.Default
    private Set<RuleCondition> conditions = new HashSet<>();

    @OneToMany(
            mappedBy = "rule",
            cascade = CascadeType.ALL,
            orphanRemoval = true
    )
    @Builder.Default
    private Set<RuleAction> actions = new HashSet<>();

    @Column(name = "effective_from")
    private LocalDateTime effectiveFrom;

    @Column(name = "effective_to")
    private LocalDateTime effectiveTo;

    @Column(name = "is_active")
    private Boolean isActive;

    @Version
    @Column(name = "version")
    private Long version;


    @Column(name = "start_date")
    private Instant startDate;

    @Column(name = "end_date")
    private Instant endDate;



    @OneToMany(
            mappedBy = "rule",
            cascade = CascadeType.ALL,
            orphanRemoval = true,
            fetch = FetchType.LAZY
    )
    @Builder.Default
    private Set<SellerSiteConfig> sellerSiteConfigs = new HashSet<>();

    public void addSellerSiteConfig(SellerSiteConfig config) {
        sellerSiteConfigs.add(config);
        config.setRule(this);
    }

    public void removeSellerSiteConfig(SellerSiteConfig config) {
        sellerSiteConfigs.remove(config);
        config.setRule(null);
    }

    public Set<SellerSiteConfig> getSellerSiteConfigs() {
        return Collections.unmodifiableSet(sellerSiteConfigs);
    }

    public Set<Long> getAllowedCategories() {
        return Collections.unmodifiableSet(categoryIds);
    }

    public Set<Long> getAllowedCustomerSegments() {
        return Collections.unmodifiableSet(sellerIds);
    }

    public Set<Long> getAllowedChannels() {
        return Collections.unmodifiableSet(siteIds);
    }

    public Set<String> getAllowedRegions() {
        return Collections.unmodifiableSet(brandIds);
    }

    public boolean isValidTime(Instant startDate, Instant endDate, LocalDateTime now) {
        return now.isAfter(LocalDateTime.ofInstant(startDate, ZoneId.systemDefault())) &&
                now.isBefore(LocalDateTime.ofInstant(endDate, ZoneId.systemDefault()));
    }

    public List<RuleConstraints> getConstraints() {
        List<RuleConstraints> constraints = new ArrayList<>();
        constraints.add(new PricingRuleConstraints(
            minimumPrice,
            maximumPrice,
            minimumMargin,
            maximumMargin,
            effectiveFrom,
            effectiveTo,
            ruleType,
            isActive,
            priority,
            startDate,
            endDate
        ));
        return constraints;
    }

    public BigDecimal getMarginPercentage() {
        return marginPercentage;
    }

    public void setMarginPercentage(BigDecimal marginPercentage) {
        this.marginPercentage = marginPercentage;
    }

    public RuleStatus getStatus() {
        return status;
    }

    public void setStatus(RuleStatus status) {
        this.status = status;
    }
}