package com.scaler.price.validation.services;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.scaler.price.core.management.utils.PriceServiceMetrics;
import com.scaler.price.core.management.service.ConfigurationService;

import com.scaler.price.rule.domain.ActionType;
import com.scaler.price.rule.domain.DiscountAction;
import com.scaler.price.rule.domain.RuleAction;
import com.scaler.price.rule.exceptions.RuleValidationException;
import com.scaler.price.validation.helper.CustomActionParameters;
import io.micrometer.common.util.StringUtils;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;

import static com.scaler.price.rule.domain.ActionType.*;

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

    public void validateActions(Set<RuleAction> actions) throws RuleValidationException {
        if (actions == null || actions.isEmpty()) {
            throw new RuleValidationException("Actions set cannot be null or empty");
        }

        if (actions.size() > MAX_ACTIONS_PER_RULE) {
            throw new RuleValidationException("Exceeded maximum number of actions per rule: " + MAX_ACTIONS_PER_RULE);
        }

        try {
            validateBasicActionRules(actions);
            validateActionSequences(actions);
            validateActionDependencies(actions);
            validateActionConflicts(actions);
            
            for (RuleAction action : actions) {
                try {
                    validateAction(action);
                } catch (RuleValidationException e) {
                    // Log specific action validation failure
                    System.err.println("Action validation failed for action: " + action + ". Reason: " + e.getMessage());
                    throw e;
                }
            }
            
            metricsService.recordActionValidation(actions.size());
        } catch (JsonProcessingException e) {
            metricsService.recordActionValidationFailure();
            throw new RuleValidationException("Error processing JSON during action validation: " + e.getMessage(), e);
        } catch (IllegalArgumentException e) {
            metricsService.recordActionValidationFailure();
            throw new RuleValidationException("Invalid argument during action validation: " + e.getMessage(), e);
        } catch (Exception e) {
            metricsService.recordActionValidationFailure();
            throw new RuleValidationException("Unexpected error during action validation: " + e.getMessage(), e);
        }
    }

    private void validateBasicActionRules(Set<RuleAction> actions) throws RuleValidationException {
        if (actions == null || actions.isEmpty()) {
            throw new RuleValidationException("At least one action is required", null);
        }

        if (actions.size() > configService.getMaxActionsPerRule()) {
            throw new RuleValidationException(
                    "Number of actions exceeds maximum allowed: " +
                            configService.getMaxActionsPerRule()
            );
        }
    }

    private void validateAction(RuleAction action) throws RuleValidationException, JsonProcessingException, IllegalArgumentException {
        if (action.getActionType() == null) {
            throw new RuleValidationException("Action type is required");
        }

        if (action.getSequence() == null || action.getSequence() < 0) {
            throw new RuleValidationException("Valid action sequence is required");
        }

        validateActionParameters(action);

        switch (action.getActionType()) {
            case SET_PRICE -> priceValidator.validateSetPriceAction(action);
            case APPLY_DISCOUNT -> {
                DiscountAction discountAction = objectMapper.treeToValue(
                    action.getParameters(), 
                    DiscountAction.class
                );
                discountValidator.validateDiscountAction(discountAction);
            }
            case CUSTOM -> validateCustomAction(action);
            case SET_MARGIN -> priceValidator.validateMarginAction(action);
            case MATCH_COMPETITOR_PRICE, BEAT_COMPETITOR_PRICE-> validateCompetitorAction(action);
            case BUNDLE_DISCOUNT -> validateBundleAction(action);
            case QUANTITY_DISCOUNT -> validateQuantityAction(action);
            default -> throw new RuleValidationException("Unsupported action type: " + action.getActionType());
        }
    }

    private void validateCompetitorAction(RuleAction action) throws RuleValidationException {
        try {
            CustomActionParameters params = objectMapper.treeToValue(
                    action.getParameters(),
                    CustomActionParameters.class
            );

            if (params.getCompetitorId() == null) {
                throw new RuleValidationException("Competitor ID is required for competitor actions");
            }
        } catch (JsonProcessingException e) {
            throw new RuleValidationException("Invalid action parameters: " + e.getMessage());
        }
    }

    private void validateQuantityAction(RuleAction action) throws RuleValidationException {
        try {
            CustomActionParameters params = objectMapper.treeToValue(
                    action.getParameters(),
                    CustomActionParameters.class
            );

            if (params.getQuantity() == null || params.getQuantity() <= 0) {
                throw new RuleValidationException("Quantity must be greater than zero");
            }
        } catch (JsonProcessingException e) {
            throw new RuleValidationException("Invalid action parameters: " + e.getMessage());
        }
    }

    private void validateBundleAction(RuleAction action) throws RuleValidationException {
        try {
            CustomActionParameters params = objectMapper.treeToValue(
                    action.getParameters(),
                    CustomActionParameters.class
            );

            if (params.getBundleQuantity() == null || params.getBundleQuantity() <= 0) {
                throw new RuleValidationException("Bundle quantity must be greater than zero");
            }
        } catch (JsonProcessingException e) {
            throw new RuleValidationException("Invalid action parameters: " + e.getMessage());
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

    private void validateActionOrder(Set<RuleAction> actions) throws RuleValidationException {
        List<RuleAction> actionList = new ArrayList<>(actions);
        for (int i = 0; i < actionList.size() - 1; i++) {
            RuleAction current = actionList.get(i);
            RuleAction next = actionList.get(i + 1);
    
            if (!isValidActionOrder(current, next)) {
                throw new RuleValidationException(
                        "Invalid action order: " + current.getActionType() +
                                " cannot be followed by " + next.getActionType()
                );
            }
        }
    }

    private boolean isValidActionOrder(RuleAction first, RuleAction second) {
        // Define valid action sequences
        return switch (first.getActionType()) {
            case SET_PRICE -> !isPriceAction(second.getActionType());
            case APPLY_DISCOUNT -> !isDiscountAction(second.getActionType());
            case SET_MARGIN -> !isMarginAction(second.getActionType());
            default -> true;
        };
    }

    private void validateActionDependencies(Set<RuleAction> actions) throws RuleValidationException {
        Map<Long, RuleAction> actionMap = new HashMap<>();
        actions.forEach(action -> actionMap.put(action.getId(), action));
        for (RuleAction action : actions) {
            if (action.getDependsOn() != null) {
                validateDependency(action, actionMap);
            }
        }
    }

    private void validateDependency(RuleAction action, Map<Long, RuleAction> actionMap) throws RuleValidationException {
        String dependencyId = action.getDependsOn();
        RuleAction dependency = actionMap.get(dependencyId);
    
        if (dependency == null) {
            throw new RuleValidationException(
                    "Action depends on non-existent action: " + dependencyId
            );
        }
    
        // Change the comparison to use Integer comparison
        if (dependency.getSequence() >= action.getSequence()) {
            throw new RuleValidationException(
                    "Action dependency must come before the action in sequence"
            );
        }
    }

    private void validateActionConflicts(Set<RuleAction> actions) throws RuleValidationException {
        long priceActionCount = actions.stream()
                .filter(a -> isPriceAction(a.getActionType()))
                .count();

        if (priceActionCount > 1) {
            throw new RuleValidationException("Multiple price-related actions are not allowed");
        }

        validateDiscountConflicts(actions);
        validateMarginConflicts(actions);
    }

    private void validateMarginConflicts(Set<RuleAction> actions) throws RuleValidationException {
        long marginActionCount = actions.stream()
                .filter(a -> a.getActionType() == SET_MARGIN)
                .count();

        if (marginActionCount > 1) {
            throw new RuleValidationException("Multiple margin-related actions are not allowed");
        }
    }

    private void validateDiscountConflicts(Set<RuleAction> actions) throws RuleValidationException {
        long discountActionCount = actions.stream()
                .filter(a -> a.getActionType() == APPLY_DISCOUNT)
                .count();

        if (discountActionCount > 1) {
            throw new RuleValidationException("Multiple discount-related actions are not allowed");
        }
    }

    public void validateCustomAction(RuleAction action) throws RuleValidationException {
        CustomActionParameters params = parseCustomActionParams(action);
        validateCustomActionScript(params.getScript());
        validateCustomActionParameters(params.getParameters());
    }

    private void validateCustomActionParameters(Map<String, Object> parameters) throws RuleValidationException {
        // Check if parameters are null or empty
        if (parameters == null || parameters.isEmpty()) {
            throw new RuleValidationException("Custom action parameters cannot be null or empty");
        }
    
        // Validate required parameters
        if (!parameters.containsKey("actionType")) {
            throw new RuleValidationException("Action type is required for custom action");
        }
    
        if (!parameters.containsKey("actionValue")) {
            throw new RuleValidationException("Action value is required for custom action");
        }
    
        // Optional additional validations based on your specific requirements
        String actionType = (String) parameters.get("actionType");
        String actionValue = (String) parameters.get("actionValue");
    
        // Example validations (adjust as per your specific business logic)
        if (StringUtils.isBlank(actionType)) {
            throw new RuleValidationException("Action type cannot be blank");
        }
    
        if (StringUtils.isBlank(actionValue)) {
            throw new RuleValidationException("Action value cannot be blank");
        }
    
        // If quantity is present, validate it
        if (parameters.containsKey("quantity")) {
            Integer quantity = (Integer) parameters.get("quantity");
            if (quantity != null && quantity <= 0) {
                throw new RuleValidationException("Quantity must be a positive number");
            }
        }
    
        // If bundleQuantity is present, validate it
        if (parameters.containsKey("bundleQuantity")) {
            Integer bundleQuantity = (Integer) parameters.get("bundleQuantity");
            if (bundleQuantity != null && bundleQuantity <= 0) {
                throw new RuleValidationException("Bundle quantity must be a positive number");
            }
        }
    }

    private CustomActionParameters parseCustomActionParams(RuleAction action) throws RuleValidationException {
        try {
            return objectMapper.readValue(
                objectMapper.writeValueAsString(action.getParameters()), 
                CustomActionParameters.class
            );
        } catch (JsonProcessingException e) {
            throw new RuleValidationException("Invalid custom action parameters format: " + e.getMessage());
        }
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

    private void validateScriptSyntax(String script) throws RuleValidationException {
        // Basic syntax validation
        if (script == null) {
            throw new RuleValidationException("Script cannot be null");
        }

        // Check for balanced parentheses
        long openParenCount = script.chars().filter(ch -> ch == '(').count();
        long closeParenCount = script.chars().filter(ch -> ch == ')').count();
        if (openParenCount != closeParenCount) {
            throw new RuleValidationException("Unbalanced parentheses in script");
        }

        // Check for balanced curly braces
        long openBraceCount = script.chars().filter(ch -> ch == '{').count();
        long closeBraceCount = script.chars().filter(ch -> ch == '}').count();
        if (openBraceCount != closeBraceCount) {
            throw new RuleValidationException("Unbalanced curly braces in script");
        }

        // Optional: Add more sophisticated syntax validation if needed
        // For example, you could use a scripting language parser or implement more complex checks
    }

    private void validateActionParameters(RuleAction action) throws RuleValidationException {
        try {
            JsonNode paramsNode;
            Object parameters = action.getParameters();
            
            if (parameters == null) {
                throw new RuleValidationException("Action parameters cannot be null");
            }
            
            if (parameters instanceof JsonNode) {
                paramsNode = (JsonNode) parameters;
            } else if (parameters instanceof String) {
                paramsNode = objectMapper.readTree((String) parameters);
            } else {
                // Convert any other type to JsonNode
                paramsNode = objectMapper.valueToTree(parameters);
            }
            
            validateParameterTypes(paramsNode, action.getActionType());
            validateParameterRanges(paramsNode, action.getActionType());
            validateRequiredParameters(paramsNode, action.getActionType());
        } catch (JsonProcessingException e) {
            throw new RuleValidationException("Invalid parameter format: " + e.getMessage(), e);
        }
    }

    private void validateParameterTypes(JsonNode paramsNode, ActionType actionType) throws RuleValidationException {
        if (paramsNode == null) {
            throw new RuleValidationException("Parameters cannot be null");
        }
        
        switch (actionType) {
            case SET_PRICE:
                // Example: Validate price-specific parameter types
                if (!paramsNode.has("price") || !paramsNode.get("price").isNumber()) {
                    throw new RuleValidationException("Price must be a valid number");
                }
                break;
            case APPLY_DISCOUNT:
                // Example: Validate discount-specific parameter types
                if (!paramsNode.has("discountPercentage") || !paramsNode.get("discountPercentage").isNumber()) {
                    throw new RuleValidationException("Discount percentage must be a valid number");
                }
                break;
            // Add more cases for other action types
            default:
                // Optional: Handle unknown action types
                break;
        }
    }

    private void validateParameterRanges(JsonNode paramsNode, ActionType actionType) throws RuleValidationException {
        if (paramsNode == null) {
            throw new RuleValidationException("Parameters cannot be null");
        }
        paramsNode.fields().forEachRemaining(entry -> {
            String fieldName = entry.getKey();
            JsonNode fieldValue = entry.getValue();

            if (fieldValue.isNumber()) {
                double value = fieldValue.asDouble();
                if (value < 0) {
                    try {
                        throw new RuleValidationException("Parameter " + fieldName + " cannot be negative");
                    } catch (RuleValidationException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                }
                if (value > 1000000) {
                    try {
                        throw new RuleValidationException("Parameter " + fieldName + " exceeds maximum allowed value");
                    } catch (RuleValidationException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                }
            }
        });
    }

    private void validateRequiredParameters(JsonNode paramsNode, ActionType actionType) throws RuleValidationException {
        if (paramsNode == null) {
            throw new RuleValidationException("Parameters cannot be null");
        }
        List<String> requiredParams = getRequiredParameters(actionType);
        for (String param : requiredParams) {
            if (paramsNode.get(param) == null) {
                throw new RuleValidationException("Missing required parameter: " + param);
            }
        }
    }

    private List<String> getRequiredParameters(ActionType actionType) {
        // Implement logic to get required parameters based on actionType
        return new ArrayList<>();
    }

    private boolean isPriceAction(ActionType type) {
        return type == ActionType.SET_PRICE;
    }

    private boolean isDiscountAction(ActionType type) {
        return type == ActionType.APPLY_DISCOUNT;
    }

    private boolean isMarginAction(ActionType type) {
        return type == ActionType.SET_MARGIN;
    }
}
