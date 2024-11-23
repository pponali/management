package com.scaler.price.rule.exceptions;

public class RuleValidationException extends Throwable {
    public RuleValidationException(String actionTypeIsRequired, Exception e) {
        super(actionTypeIsRequired);
    }

    public RuleValidationException(String actionTypeIsRequired) {
        super(actionTypeIsRequired);
    }
}
