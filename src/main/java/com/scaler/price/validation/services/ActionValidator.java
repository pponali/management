package com.scaler.price.validation.services;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.scaler.price.core.management.utils.PriceServiceMetrics;
import com.scaler.price.rule.config.ConfigurationService;
import com.scaler.price.rule.domain.ActionType;
import com.scaler.price.rule.domain.RuleAction;
import com.scaler.price.rule.exceptions.RuleValidationException;
import io.micrometer.common.util.StringUtils;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
@Slf4j
@RequiredArgsConstructor
public class ActionValidator {
    private final PriceValidator priceValidator;
    private final DiscountValidator discountValidator;
    private final ConfigurationService configService;
    private final ObjectMapper objectMapper;
    private final PriceServiceMetrics metricsService;

    private static final int MAX_ACTIONS_PER_RULE = 5;

    @SneakyThrows
    public void validateActions(Set<RuleAction> actions) {
        try {
            validateBasicActionRules(actions);
            validateActionSequences(actions);
            validateActionDependencies(actions);
            validateActionConflicts(actions);
            actions.forEach(this::validateAction);

            metricsService.recordActionValidation(actions.size());
        } catch (Exception e) {
            metricsService.recordActionValidationFailure();
            throw e;
        }
    }

    private void validateBasicActionRules(Set<RuleAction> actions) throws RuleValidationException {
        if (actions == null || actions.isEmpty()) {
            throw new RuleValidationException("At least one action is required");
        }

        if (actions.size() > configService.getMaxActionsPerRule()) {
            throw new RuleValidationException(
                    "Number of actions exceeds maximum allowed: " +
                            configService.getMaxActionsPerRule()
            );
        }
    }

    private void validateAction(RuleAction action) throws RuleValidationException {
        if (action.getType() == null) {
            throw new RuleValidationException("Action type is required");
        }

        if (action.getSequence() == null || action.getSequence() < 0) {
            throw new RuleValidationException("Valid action sequence is required");
        }

        validateActionParameters(action);

        switch (action.getType()) {
            case SET_PRICE -> priceValidator.validateSetPriceAction(action);
            case APPLY_DISCOUNT -> discountValidator.validateDiscountAction(action);
            case CUSTOM -> validateCustomAction(action);
            case SET_MARGIN -> priceValidator.validateMarginAction(action);
            case MATCH_COMPETITOR_PRICE, BEAT_COMPETITOR_PRICE-> validateCompetitorAction(action);
            case BUNDLE_DISCOUNT -> validateBundleAction(action);
            case QUANTITY_DISCOUNT -> validateQuantityAction(action);
            default -> throw new RuleValidationException("Unsupported action type: " + action.getType());
        }
    }

    private void validateActionSequences(Set<RuleAction> actions) throws RuleValidationException {
        Set<Integer> sequences = new HashSet<>();
        for (RuleAction action : actions) {
            if (!sequences.add(action.getSequence())) {
                throw new RuleValidationException(
                        "Duplicate action sequence: " + action.getSequence()
                );
            }
        }

        validateActionOrder(actions);
    }

    private void validateActionOrder(List<RuleAction> actions) throws RuleValidationException {
        for (int i = 0; i < actions.size() - 1; i++) {
            RuleAction current = actions.get(i);
            RuleAction next = actions.get(i + 1);

            if (!isValidActionOrder(current, next)) {
                throw new RuleValidationException(
                        "Invalid action order: " + current.getType() +
                                " cannot be followed by " + next.getType()
                );
            }
        }
    }

    private boolean isValidActionOrder(RuleAction first, RuleAction second) {
        // Define valid action sequences
        return switch (first.getType()) {
            case SET_PRICE -> !isPriceAction(second.getType());
            case APPLY_DISCOUNT -> !isDiscountAction(second.getType());
            case SET_MARGIN -> !isMarginAction(second.getType());
            default -> true;
        };
    }

    private void validateActionDependencies(Set<RuleAction> actions) throws RuleValidationException {
        Map<String, RuleAction> actionMap = new HashMap<>();
        actions.forEach(action -> actionMap.put(action.getId(), action));

        for (RuleAction action : actions) {
            if (action.getDependsOn() != null) {
                validateDependency(action, actionMap);
            }
        }
    }

    private void validateDependency(RuleAction action, Map<String, RuleAction> actionMap) throws RuleValidationException {
        String dependencyId = action.getDependsOn();
        RuleAction dependency = actionMap.get(dependencyId);

        if (dependency == null) {
            throw new RuleValidationException(
                    "Action depends on non-existent action: " + dependencyId
            );
        }

        if (dependency.getSequence() >= action.getSequence()) {
            throw new RuleValidationException(
                    "Action dependency must come before the action in sequence"
            );
        }
    }

    private void validateActionConflicts(Set<RuleAction> actions) throws RuleValidationException {
        long priceActionCount = actions.stream()
                .filter(a -> isPriceAction(a.getType()))
                .count();

        if (priceActionCount > 1) {
            throw new RuleValidationException("Multiple price-related actions are not allowed");
        }

        validateDiscountConflicts(actions);
        validateMarginConflicts(actions);
    }

    private void validateCustomAction(RuleAction action) {
        CustomActionParameters params = parseCustomActionParams(action);
        validateCustomActionScript(params.getScript());
        validateCustomActionParameters(params.getParameters());
    }

    private void validateCustomActionScript(String script) throws RuleValidationException {
        if (StringUtils.isBlank(script)) {
            throw new RuleValidationException("Custom action script cannot be empty");
        }

        validateScriptSecurity(script);
        validateScriptSyntax(script);
    }

    private void validateScriptSecurity(String script) throws RuleValidationException {
        List<String> forbiddenKeywords = Arrays.asList("System.", "Runtime.", "Process");
        for (String keyword : forbiddenKeywords) {
            if (script.contains(keyword)) {
                throw new RuleValidationException("Script contains forbidden keyword: " + keyword);
            }
        }
    }

    private void validateActionParameters(RuleAction action) throws RuleValidationException {
        try {
            String params = action.getParameters();
            if (StringUtils.isBlank(params)) {
                throw new RuleValidationException("Action parameters are required");
            }

            JsonNode paramsNode = objectMapper.readTree(params);
            validateParameterTypes(paramsNode, action.getType());
            validateParameterRanges(paramsNode, action.getType());
            validateRequiredParameters(paramsNode, action.getType());

        } catch (JsonProcessingException e) {
            throw new RuleValidationException("Invalid action parameters format: " + e.getMessage());
        }
    }

    private boolean isPriceAction(ActionType type) {
        return type == ActionType.SET_PRICE ||
                type == ActionType.APPLY_DISCOUNT ||
                type == ActionType.SET_MARGIN;
    }
}

