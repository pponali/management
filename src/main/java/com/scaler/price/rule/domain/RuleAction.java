package com.scaler.price.rule.domain;

import com.fasterxml.jackson.databind.JsonNode;
import com.scaler.price.core.management.domain.AuditInfo;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@Entity
@Table(name = "rule_actions")
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class RuleAction extends AuditInfo {


    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "rule_id", nullable = false)
    private PricingRule rule;

    @Column(nullable = false)
    private ActionType actionType;

    @Column(nullable = false)
    @Convert(converter = JsonNodeConverter.class)
    private JsonNode parameters;

    @Column(nullable = false)
    private Integer sequence;

    @Column
    private String dependsOn;

    public String getDependsOn() {
        return dependsOn;
    }

    public void setDependsOn(String dependsOn) {
        this.dependsOn = dependsOn;
    }

}