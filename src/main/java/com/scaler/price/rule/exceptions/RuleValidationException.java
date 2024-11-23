package com.scaler.price.rule.exceptions;

public class RuleValidationException extends Exception {
    public RuleValidationException(String message) {
        super(message);
    }

    public RuleValidationException(String message, Throwable cause) {
        super(message, cause);
    }
}
