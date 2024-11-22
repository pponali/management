package com.scaler.price.rule.dto.validation;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ValidationResult {
    private boolean valid;
    private List<ValidationError> errors;
    private List<ValidationWarning> warnings;
    private ValidationMetadata metadata;

    @Data
    @Builder
    public static class ValidationError {
        private String code;
        private String message;
        private String field;
        private ErrorSeverity severity;
        private Map<String, Object> context;
    }

    @Data
    @Builder
    public static class ValidationWarning {
        private String code;
        private String message;
        private String recommendation;
        private WarningSeverity severity;
    }

    @Data
    @Builder
    public static class ValidationMetadata {
        private LocalDateTime validatedAt;
        private String validatedBy;
        private Long executionTimeMs;
        private Set<String> validationsPerformed;
        private Map<String, Object> additionalInfo;
    }

    public enum ErrorSeverity {
        FATAL,
        ERROR,
        VIOLATION
    }

    public enum WarningSeverity {
        HIGH,
        MEDIUM,
        LOW
    }
}
