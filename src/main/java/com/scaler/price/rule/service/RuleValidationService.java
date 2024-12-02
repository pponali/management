package com.scaler.price.rule.service;

import com.scaler.price.rule.domain.*;

import com.scaler.price.rule.exceptions.RuleValidationException;
import com.scaler.price.validation.services.impl.PriceValidator;
import com.scaler.price.validation.services.util.BasicFieldValidator;
import com.scaler.price.validation.services.util.ConflictValidator;
import com.scaler.price.validation.services.util.DateValidator;
import com.scaler.price.validation.services.util.LimitValidator;
import com.scaler.price.validation.services.util.MappingValidator;

import com.scaler.price.validation.services.*;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;



@Service
@Slf4j
@RequiredArgsConstructor
public class RuleValidationService {

    private final ActionValidator actionValidator;
    private final ConditionValidator conditionValidator;
    private final PriceValidator priceValidator;
    private final BasicFieldValidator basicFieldValidator;
    private final DateValidator dateValidator;
    private final MappingValidator mappingValidator;
    private final LimitValidator limitValidator;
    private final ConflictValidator conflictValidator;

    public void validateRule(PricingRule rule) throws RuleValidationException {
        log.debug("Validating rule: {}", rule.getRuleName());
        try {
            basicFieldValidator.validate(rule);
            dateValidator.validate(rule);
            priceValidator.validatePrices(rule);
            priceValidator.validateMargins(rule);
            mappingValidator.validate(rule);
            conditionValidator.validateConditions(rule.getConditions());
            actionValidator.validateActions(rule.getActions());
            limitValidator.validate(rule);
            conflictValidator.validate(rule);
        } catch (Exception e) {
            log.error("Rule validation failed for rule {}: {}", rule.getRuleName(), e.getMessage());
            throw new RuleValidationException("Rule validation failed: " + e.getMessage(), e);
        }
    }

    public void validateRuleUpdate(PricingRule existingRule, PricingRule updatedRule) throws RuleValidationException {
        validateRule(updatedRule);
        validateUpdateSpecificRules(existingRule, updatedRule);
    }

    private void validateUpdateSpecificRules(PricingRule existingRule, PricingRule updatedRule) throws RuleValidationException {
        if (existingRule == null) {
            throw new RuleValidationException("Existing rule cannot be null", null);
        }

        // Validate immutable fields
        if (!existingRule.getRuleType().equals(updatedRule.getRuleType())) {
            throw new RuleValidationException("Rule type cannot be changed after creation", null);
        }

        // Validate significant changes
        if (hasSignificantChanges(existingRule, updatedRule)) {
            validateSignificantChanges(existingRule, updatedRule);
        }
    }

    private boolean hasSignificantChanges(PricingRule existingRule, PricingRule updatedRule) {
        return !existingRule.getConditions().equals(updatedRule.getConditions()) ||
                !existingRule.getActions().equals(updatedRule.getActions());
    }

    private void validateSignificantChanges(PricingRule existingRule, PricingRule updatedRule) {
        // Add validation logic for significant changes
        // For example, validate that changes don't break existing dependencies
        log.debug("Validating significant changes for rule: {}", existingRule.getRuleName());
    }
}
