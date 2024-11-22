package com.scaler.price.rule.domain;

import com.fasterxml.jackson.databind.JsonNode;
import com.vladmihalcea.hibernate.type.json.JsonBinaryType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Type;

import java.util.Map;

@Entity
@Table(name = "rule_conditions")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RuleCondition {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "rule_id", nullable = false)
    private PricingRule rule;

    @Column(nullable = false)
    private String conditionType;

    @Column(nullable = false)
    @Convert(converter = JsonNodeConverter.class)
    private JsonNode parameters;

    @Column(nullable = false)
    private Integer sequence;

    @Column
    private Boolean isEnabled;

    @Version
    private Long version;

    @Enumerated(EnumType.STRING)
    private ConditionType type;

    @Column(nullable = false)
    private String attribute;

    @Enumerated(EnumType.STRING)
    private Operator operator;

    @Type(JsonBinaryType.class)
    @Column(columnDefinition = "jsonb")
    private String value;

    public boolean evaluate(Map<String, Object> context) {
        Object actualValue = context.get(attribute);
        return ConditionEvaluator.evaluate(actualValue, operator, value);
    }
}