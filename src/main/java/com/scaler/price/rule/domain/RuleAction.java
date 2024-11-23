package com.scaler.price.rule.domain;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "rule_actions")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RuleAction {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

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
    private Boolean isEnabled;

    @Column
    private String dependsOn;

    public String getDependsOn() {
        return dependsOn;
    }

    public void setDependsOn(String dependsOn) {
        this.dependsOn = dependsOn;
    }

    @Version
    private Long version;

}