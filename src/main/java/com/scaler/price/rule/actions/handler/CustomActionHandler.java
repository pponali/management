package com.scaler.price.rule.actions.handler;

import com.scaler.price.rule.domain.RuleAction;
import com.scaler.price.validation.helper.ActionParameters;
import com.scaler.price.rule.dto.RuleEvaluationContext;
import com.scaler.price.rule.dto.RuleEvaluationResult;
import com.scaler.price.rule.exceptions.ActionExecutionException;
import com.scaler.price.rule.exceptions.ActionValidationException;

public interface CustomActionHandler {
    RuleEvaluationResult execute(
            RuleAction action,
            RuleEvaluationContext context,
            RuleEvaluationResult currentResult
    ) throws ActionExecutionException;

    void validate(ActionParameters parameters)
            throws ActionValidationException;
}