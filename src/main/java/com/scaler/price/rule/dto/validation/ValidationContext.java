package com.scaler.price.rule.dto.validation;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ValidationContext {
    private String ruleId;
    private String sellerId;
    private String siteId;
    private LocalDateTime validationTime;
    private Map<String, Object> attributes;
    private ValidationLevel validationLevel;
    private Set<String> enabledValidations;
    private ValidationMode validationMode;

    @Builder.Default
    private Map<String, Object> cache = new HashMap<>();

    public enum ValidationLevel {
        BASIC,
        STANDARD,
        STRICT
    }

    public enum ValidationMode {
        FAIL_FAST,
        COLLECT_ALL_ERRORS,
        WARNING_ONLY
    }
}