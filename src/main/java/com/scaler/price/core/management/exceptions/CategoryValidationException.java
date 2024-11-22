package com.scaler.price.core.management.exceptions;

public class CategoryValidationException extends RuntimeException {
    public CategoryValidationException(String ruleExpressionCannotBeEmpty) {
        super(ruleExpressionCannotBeEmpty);
    }
}
