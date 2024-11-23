package com.scaler.price.rule.exceptions;

public class CategoryValidationException extends Exception {

    public CategoryValidationException(String ruleExpressionCannotBeEmpty) {
        super(ruleExpressionCannotBeEmpty);
    }   
}
