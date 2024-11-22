package com.scaler.price.rule.dto;

import com.fasterxml.jackson.databind.JsonNode;
import com.scaler.price.rule.domain.ConditionType;
import com.scaler.price.rule.domain.Operator;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class RuleConditionDTO {
    private Long id;
    private ConditionType type;
    private String attributeName;
    private Operator operator;
    private JsonNode value;
}