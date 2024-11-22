package com.scaler.price.rule.domain;
public class ConditionEvaluator {
    
    public static boolean evaluate(Object actualValue, Operator operator, String value) {
        // Example implementation
        if (operator == Operator.EQUALS) {
            return actualValue.equals(value);
        }
        // Add more conditions as needed
        return false;
    }
}
