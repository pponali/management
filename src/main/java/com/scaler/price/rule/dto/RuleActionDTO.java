package com.scaler.price.rule.dto;

import com.fasterxml.jackson.databind.JsonNode;
import com.scaler.price.rule.domain.ActionType;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class RuleActionDTO {
    private Long id;
    private ActionType type;
    private JsonNode parameters;
    private Integer sequence;
}