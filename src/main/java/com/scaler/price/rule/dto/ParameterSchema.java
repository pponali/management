package com.scaler.price.rule.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
@Builder
public class ParameterSchema {
    private Map<String, ParameterDefinition> parameters;
    private List<String> requiredParameters;
    private List<String> optionalParameters;
    private Map<String, List<String>> dependencyRules;

}
