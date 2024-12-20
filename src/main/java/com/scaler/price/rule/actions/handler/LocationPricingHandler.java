package com.scaler.price.rule.actions.handler;

import com.scaler.price.rule.domain.RuleAction;
import com.scaler.price.rule.dto.RuleEvaluationContext;
import com.scaler.price.rule.dto.RuleEvaluationResult;
import com.scaler.price.rule.exceptions.ActionExecutionException;
import com.scaler.price.rule.exceptions.ActionValidationException;
import com.scaler.price.validation.helper.ActionParameters;

public class LocationPricingHandler implements CustomActionHandler {

    @Override
    public RuleEvaluationResult execute(RuleAction action, RuleEvaluationContext context, RuleEvaluationResult currentResult) throws ActionExecutionException {
        // If no changes are needed, return the current result
        if (currentResult == null) {
            currentResult = new RuleEvaluationResult();
        }
        
        // Implement location-based pricing logic here
        // For now, we'll just return the current result without modifications
        return currentResult;
    }

    @Override
    public void validate(ActionParameters parameters) throws ActionValidationException {

    }
}
