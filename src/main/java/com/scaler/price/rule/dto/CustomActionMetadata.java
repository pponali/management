package com.scaler.price.rule.dto;

import lombok.Builder;
import lombok.Data;

import java.util.Map;
import java.util.Set;

@Data
@Builder
public class CustomActionMetadata {
    private String name;
    private String description;
    private ParameterSchema parameterSchema;
    private Set<String> requiredPermissions;
    private boolean supportsRollback;
    private Map<String, String> validationRules;
}
