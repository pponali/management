package com.scaler.price.rule.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.scaler.price.rule.actions.CustomActionRegistry;
import com.scaler.price.rule.actions.handler.CustomActionHandler;
import com.scaler.price.rule.domain.RuleAction;
import com.scaler.price.validation.helper.ActionParameters;
import com.scaler.price.rule.dto.RuleEvaluationContext;
import com.scaler.price.rule.dto.RuleEvaluationResult;
import com.scaler.price.rule.exceptions.ActionExecutionException;
import com.scaler.price.rule.exceptions.ActionRegistrationException;
import com.scaler.price.rule.exceptions.ProductFetchException;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

@Service
@Slf4j

@AllArgsConstructor
public class ActionExecutorServiceImpl implements ActionExecutorService {
    private final ObjectMapper objectMapper;
    private final CompetitorPriceService competitorPriceService;
    private final BundleService bundleService;
    private final CustomActionRegistry customActionRegistry;

    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);
    private static final int PRICE_SCALE = 2;

    @Override
    public RuleEvaluationResult executeActions(
            Set<RuleAction> actions,
            RuleEvaluationContext context,
            BigDecimal currentPrice) throws ActionExecutionException, ActionRegistrationException, ProductFetchException {
        if (actions == null || actions.isEmpty()) {
            log.warn("No actions to execute");
            RuleEvaluationResult result = new RuleEvaluationResult();
            result.setAdjustedPrice(currentPrice);
            return result;
        }

        RuleEvaluationResult result = new RuleEvaluationResult();
        result.setAdjustedPrice(currentPrice);

        List<RuleAction> sortedActions = new ArrayList<>(actions);
        sortedActions.sort(Comparator.comparing(RuleAction::getSequence));

        for (RuleAction action : sortedActions) {
            try {
                executeAction(action, context, result);
                log.debug("Successfully executed action: {} with result: {}", action, result);
            } catch (Exception e) {
                log.error("Error executing action {}: {}", action, e.getMessage(), e);
                throw new ActionExecutionException("Failed to execute action: " + action.getActionType(), e);
            }
        }

        return result;
    }

    @Override
    public RuleEvaluationResult executeAction(
            RuleAction action,
            RuleEvaluationContext context,
            RuleEvaluationResult result) throws ActionExecutionException, ActionRegistrationException, ProductFetchException {
        validateActionParameters(action, context, result);

        log.debug("Executing action: {}", action);

        try {
            switch (action.getActionType()) {
                case SET_PRICE:
                    executeSetPrice(action, context, result);
                    break;
                case DISCOUNT_PERCENTAGE:
                    executeDiscountPercentage(action, context, result);
                    break;
                case DISCOUNT_AMOUNT:
                    executeDiscountAmount(action, context, result);
                    break;
                case MATCH_COMPETITOR_PRICE:
                    executeMatchCompetitor(action, context, result);
                    break;
                case SET_MARGIN:
                    executeSetMargin(action, context, result);
                    break;
                case BEAT_COMPETITOR:
                    executeBeatCompetitor(action, context, result);
                    break;
                case BUNDLE_DISCOUNT:
                    executeBundleDiscount(action, context, result);
                    break;
                case QUANTITY_DISCOUNT:
                    executeQuantityDiscount(action, context, result);
                    break;
                case CUSTOM:
                    executeCustomAction(action, context, result);
                    break;
                default:
                    throw new ActionExecutionException("Unsupported action type: " + action.getActionType());
            }
        } catch (Exception e) {
            log.error("Error executing action {}: {}", action, e.getMessage(), e);
            throw new ActionExecutionException("Failed to execute action: " + action.getActionType(), e);
        }

        validateResult(result);
        log.debug("Action execution completed successfully: {}", result);

        return result;
    }

    private void validateActionParameters(RuleAction action, RuleEvaluationContext context, RuleEvaluationResult result)
            throws ActionExecutionException {
        if (action == null) {
            throw new ActionExecutionException("Action cannot be null");
        }
        if (context == null) {
            throw new ActionExecutionException("Context cannot be null");
        }
        if (result == null) {
            throw new ActionExecutionException("Result cannot be null");
        }

        if (action.getParameters() == null || action.getParameters().isEmpty()) {
            log.warn("Action {} has no parameters", action.getActionType());
        }
    }

    private ActionParameters parseParameters(JsonNode parameters) throws ActionExecutionException {
        try {
            return objectMapper.treeToValue(parameters, ActionParameters.class);
        } catch (JsonProcessingException e) {
            throw new ActionExecutionException("Failed to parse action parameters", e);
        }
    }

    private void executeSetPrice(
            RuleAction action,
            RuleEvaluationContext context,
            RuleEvaluationResult result) throws ActionExecutionException {
        ActionParameters params = parseParameters(action.getParameters());
        String priceValue = params.getValue();
        BigDecimal newPrice = priceValue != null ? new BigDecimal(priceValue) : null;
        if (newPrice == null) {
            throw new ActionExecutionException("Price parameter cannot be null for SET_PRICE action");
        }
        result.setAdjustedPrice(newPrice.setScale(PRICE_SCALE, RoundingMode.HALF_UP));
    }

    private void executeDiscountPercentage(
            RuleAction action,
            RuleEvaluationContext context,
            RuleEvaluationResult result) throws ActionExecutionException {
        ActionParameters params = parseParameters(action.getParameters());
        BigDecimal discountPercentage = params.getDiscountPercentage();
        if (discountPercentage == null) {
            throw new ActionExecutionException("Discount percentage cannot be null");
        }
        if (discountPercentage.compareTo(HUNDRED) > 0) {
            log.warn("Discount percentage {} is greater than 100%, capping at 100%", discountPercentage);
            discountPercentage = HUNDRED;
        }
        BigDecimal currentPrice = result.getAdjustedPrice();
        BigDecimal discountAmount = currentPrice.multiply(discountPercentage)
                .divide(HUNDRED, PRICE_SCALE, RoundingMode.HALF_UP);
        result.setAdjustedPrice(currentPrice.subtract(discountAmount));
    }

    private void executeDiscountAmount(
            RuleAction action,
            RuleEvaluationContext context,
            RuleEvaluationResult result) throws ActionExecutionException {
        ActionParameters params = parseParameters(action.getParameters());
        BigDecimal discountAmount = params.getDiscountAmount();
        if (discountAmount == null) {
            throw new ActionExecutionException("Discount amount cannot be null");
        }
        if (discountAmount.compareTo(BigDecimal.ZERO) < 0) {
            throw new ActionExecutionException("Discount amount cannot be negative");
        }
        BigDecimal currentPrice = result.getAdjustedPrice();
        result.setAdjustedPrice(currentPrice.subtract(discountAmount).setScale(PRICE_SCALE, RoundingMode.HALF_UP));
    }

    private void executeMatchCompetitor(
            RuleAction action,
            RuleEvaluationContext context,
            RuleEvaluationResult result) throws ActionExecutionException {
        ActionParameters params = parseParameters(action.getParameters());
        String competitorId = params.getCompetitorId();
        if (competitorId == null || competitorId.trim().isEmpty()) {
            throw new ActionExecutionException("Competitor ID cannot be null or empty");
        }
        BigDecimal competitorPrice = competitorPriceService.getCompetitorPrice(
                competitorId,
                context.getProductId());
        if (competitorPrice == null) {
            throw new ActionExecutionException("Failed to get competitor price for competitor: " + competitorId);
        }
        result.setAdjustedPrice(competitorPrice.setScale(PRICE_SCALE, RoundingMode.HALF_UP));
    }

    private void executeSetMargin(
            RuleAction action,
            RuleEvaluationContext context,
            RuleEvaluationResult result) throws ActionExecutionException {
        ActionParameters params = parseParameters(action.getParameters());
        BigDecimal margin = params.getMinimumMargin();
        BigDecimal costPrice = context.getCostPrice();
        BigDecimal newPrice = calculateMinimumMarginPrice(costPrice, margin);
        result.setAdjustedPrice(newPrice.setScale(PRICE_SCALE, RoundingMode.HALF_UP));
    }

    private void executeBeatCompetitor(
            RuleAction action,
            RuleEvaluationContext context,
            RuleEvaluationResult result) throws ActionExecutionException {
        ActionParameters params = parseParameters(action.getParameters());
        BigDecimal competitorPrice = competitorPriceService.getCompetitorPrice(
                params.getCompetitor(),
                context.getProductId());
        BigDecimal beatAmount = competitorPrice.multiply(params.getBeatPercentage())
                .divide(HUNDRED, PRICE_SCALE, RoundingMode.HALF_UP);
        result.setAdjustedPrice(competitorPrice.subtract(beatAmount).setScale(PRICE_SCALE, RoundingMode.HALF_UP));
    }

    private void executeBundleDiscount(
            RuleAction action,
            RuleEvaluationContext context,
            RuleEvaluationResult result) throws ProductFetchException, ActionExecutionException {
        ActionParameters params = parseParameters(action.getParameters());
        BundleEligibility eligibility = bundleService.checkEligibility(params.getBundleId(), context.getProductId(), null);
        if (eligibility.isEligible()) {
            BigDecimal currentPrice = result.getAdjustedPrice();
            BigDecimal discountAmount = currentPrice.multiply(params.getBundleDiscountPercentage())
                    .divide(HUNDRED, PRICE_SCALE, RoundingMode.HALF_UP);
            result.setAdjustedPrice(currentPrice.subtract(discountAmount));
        }
    }

    private void executeQuantityDiscount(
            RuleAction action,
            RuleEvaluationContext context,
            RuleEvaluationResult result) throws ActionExecutionException {
        try {
            ActionParameters params = parseParameters(action.getParameters());

            // Validate params
            if (params == null) {
                throw new IllegalArgumentException("Action parameters cannot be null");
            }

            Integer quantity = context.getQuantity();

            // Safely parse minimum quantity with default value
            Integer minimumQuantity = 0;
            if (params.getMinQuantity() != null) {
                try {
                    minimumQuantity = Integer.parseInt(params.getMinQuantity());
                } catch (NumberFormatException e) {
                    // Log the error and use default minimum quantity
                    log.warn("Invalid minimum quantity: {}. Using default of 0.", params.getMinQuantity());
                }
            }

            if (quantity == null) {
                log.warn("Quantity is null. Skipping quantity discount.");
                return;
            }

            if (quantity >= minimumQuantity) {
                BigDecimal discountPercentage = calculateTieredDiscount(
                        quantity,
                        minimumQuantity,
                        params.getBaseDiscount(),
                        params.getTierIncrement());

                BigDecimal currentPrice = result.getAdjustedPrice();
                if (currentPrice == null) {
                    log.warn("Current price is null. Skipping quantity discount.");
                    return;
                }

                BigDecimal discountAmount = currentPrice.multiply(discountPercentage)
                        .divide(HUNDRED, PRICE_SCALE, RoundingMode.HALF_UP);
                result.setAdjustedPrice(currentPrice.subtract(discountAmount));
            }
        } catch (Exception e) {
            log.error("Error executing quantity discount", e);
            throw new ActionExecutionException("Failed to execute quantity discount", e);
        }
    }

    private void executeCustomAction(
            RuleAction action,
            RuleEvaluationContext context,
            RuleEvaluationResult result) throws ActionRegistrationException, ActionExecutionException {
        CustomActionHandler handler = customActionRegistry.getHandler(action.getActionType().name());
        if (handler == null) {
            throw new ActionRegistrationException("No handler registered for custom action type: " + action.getActionType());
        }
        handler.execute(action, context, result);
    }

    private ActionParameters parseParameters(String parameters) throws ActionExecutionException {
        try {
            return objectMapper.readValue(parameters, ActionParameters.class);
        } catch (JsonProcessingException e) {
            throw new ActionExecutionException("Failed to parse action parameters", e);
        }
    }

    private BigDecimal calculateMinimumMarginPrice(
            BigDecimal costPrice,
            BigDecimal minimumMargin) {
        return costPrice.multiply(BigDecimal.ONE.add(
                minimumMargin.divide(HUNDRED, PRICE_SCALE, RoundingMode.HALF_UP)));
    }

    private BigDecimal calculateTieredDiscount(
            Integer quantity,
            Integer minimumQuantity,
            BigDecimal baseDiscount,
            BigDecimal tierIncrement) {
        int tiers = (quantity - minimumQuantity) / minimumQuantity;
        return baseDiscount.add(tierIncrement.multiply(BigDecimal.valueOf(tiers)));
    }

    private void validateResult(RuleEvaluationResult result) throws ActionExecutionException {
        Assert.notNull(result, "Result cannot be null");
        if (result.getAdjustedPrice() == null) {
            throw new ActionExecutionException("Action execution resulted in null price");
        }
        if (result.getAdjustedPrice().compareTo(BigDecimal.ZERO) < 0) {
            log.warn("Action execution resulted in negative price: {}, setting to zero", result.getAdjustedPrice());
            result.setAdjustedPrice(BigDecimal.ZERO.setScale(PRICE_SCALE, RoundingMode.HALF_UP));
        }
    }

    private void validateContext(RuleEvaluationContext context) throws ActionExecutionException {
        Assert.notNull(context, "Context cannot be null");
        Assert.notNull(context.getProductId(), "Product ID cannot be null");
        if (context.getCostPrice() != null && context.getCostPrice().compareTo(BigDecimal.ZERO) < 0) {
            throw new ActionExecutionException("Cost price cannot be negative");
        }
    }
}
