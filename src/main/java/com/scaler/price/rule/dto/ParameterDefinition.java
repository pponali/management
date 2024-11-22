package com.scaler.price.rule.dto;

import com.scaler.price.rule.validation.ValidationRules;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ParameterDefinition {
    private String name;
    private String type;
    private String description;
    private Object defaultValue;
    private ValidationRules validationRules;
}