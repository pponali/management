package com.scaler.price.rule.service;

import com.scaler.price.rule.domain.RuleAction;
import com.scaler.price.rule.dto.RuleEvaluationContext;
import com.scaler.price.rule.dto.RuleEvaluationResult;
import com.scaler.price.rule.exceptions.ActionExecutionException;
import com.scaler.price.rule.exceptions.ActionRegistrationException;
import com.scaler.price.rule.exceptions.ProductFetchException;

import java.math.BigDecimal;
import java.util.Set;

/**
 * Service interface for executing pricing rule actions
 */
public interface ActionExecutorService {
    
    /**
     * Executes a set of rule actions in sequence
     *
     * @param actions Set of actions to execute
     * @param context The evaluation context containing product and pricing information
     * @param currentPrice The current price before applying actions
     * @return Result containing the adjusted price after applying all actions
     * @throws ActionExecutionException if any action fails to execute
 * @throws ActionRegistrationException 
     * @throws ProductFetchException 
     */
    RuleEvaluationResult executeActions(
            Set<RuleAction> actions,
            RuleEvaluationContext context,
            BigDecimal currentPrice) throws ActionExecutionException, ActionRegistrationException, ProductFetchException;

    /**
     * Executes a single rule action
     *
     * @param action The action to execute
     * @param context The evaluation context containing product and pricing information
     * @param result The current evaluation result to be modified
     * @return Updated evaluation result after applying the action
     * @throws ActionExecutionException if the action fails to execute
 * @throws ActionRegistrationException 
     * @throws ProductFetchException 
     */
    RuleEvaluationResult executeAction(
            RuleAction action,
            RuleEvaluationContext context,
            RuleEvaluationResult result) throws ActionExecutionException, ActionRegistrationException, ProductFetchException;
}