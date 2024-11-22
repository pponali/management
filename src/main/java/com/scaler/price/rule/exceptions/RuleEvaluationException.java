package com.scaler.price.rule.exceptions;

public class RuleEvaluationException extends RuntimeException {
    
    public RuleEvaluationException(String message) {
        super(message);
    }

    public RuleEvaluationException(String message, Throwable cause) {
        super(message, cause);
    }

    public RuleEvaluationException(String message, Object... args) {
        super(String.format(message, args));
    }

    public RuleEvaluationException(Throwable cause, String message, Object... args) {
        super(String.format(message, args), cause);
    }
}
