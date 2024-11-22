package com.scaler.price.rule.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.scaler.price.rule.actions.CustomActionRegistry;
import com.scaler.price.rule.actions.handler.CustomActionHandler;
import com.scaler.price.rule.domain.RuleAction;
import com.scaler.price.validation.helper.ActionParameters;
import com.scaler.price.rule.dto.RuleEvaluationContext;
import com.scaler.price.rule.dto.RuleEvaluationResult;
import com.scaler.price.rule.exceptions.ActionExecutionException;
import com.scaler.price.rule.exceptions.ActionRegistrationException;
import io.micrometer.common.util.StringUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

@Service
@Slf4j
public class ActionExecutorService {
    private final ObjectMapper objectMapper;
    private final CompetitorPriceService competitorPriceService;
    private final BundleService bundleService;
    private final CustomActionRegistry customActionRegistry;

    public ActionExecutorService(ObjectMapper objectMapper, CompetitorPriceService competitorPriceService, BundleService bundleService, CustomActionRegistry customActionRegistry) {
        this.objectMapper = objectMapper;
        this.competitorPriceService = competitorPriceService;
        this.bundleService = bundleService;
        this.customActionRegistry = customActionRegistry;
    }

    public RuleEvaluationResult executeActions(
            Set<RuleAction> actions,
            RuleEvaluationContext context,
            BigDecimal currentPrice) throws ActionExecutionException, ActionRegistrationException {

        RuleEvaluationResult result = new RuleEvaluationResult();
        result.setAdjustedPrice(currentPrice);

        // Sort actions by sequence
        List<RuleAction> sortedActions = new ArrayList<>(actions);
        sortedActions.sort(Comparator.comparing(RuleAction::getSequence));

        for (RuleAction action : sortedActions) {
            try {
                executeAction(action, context, result);
            } catch (Exception e) {
                log.error("Error executing action: {}", action, e);
                return null;
            }
        }

        return result;
    }

    private void executeAction(
            RuleAction action,
            RuleEvaluationContext context,
            RuleEvaluationResult result) throws ActionExecutionException, ActionRegistrationException {

        switch (action.getType()) {
            case SET_PRICE -> executeSetPrice(action, context, result);
            case APPLY_DISCOUNT_PERCENTAGE -> executeDiscountPercentage(action, context, result);
            case APPLY_DISCOUNT_AMOUNT -> executeDiscountAmount(action, context, result);
            case SET_MARGIN -> executeSetMargin(action, context, result);
            case MATCH_COMPETITOR_PRICE -> executeMatchCompetitor(action, context, result);
            case BEAT_COMPETITOR_PRICE -> executeBeatCompetitor(action, context, result);
            case BUNDLE_DISCOUNT -> executeBundleDiscount(action, context, result);
            case QUANTITY_DISCOUNT -> executeQuantityDiscount(action, context, result);
            case CUSTOM -> executeCustomAction(action, context, result);
            default -> log.warn("Unknown action type: {}", action.getType());
        }
    }

    private void executeSetPrice(
            RuleAction action,
            RuleEvaluationContext context,
            RuleEvaluationResult result) {

        ActionParameters params = parseParameters(action.getParameters());
        BigDecimal newPrice = new BigDecimal(params.getValue());
        result.setAdjustedPrice(newPrice);
        result.setDiscountAmount(context.getCurrentPrice().subtract(newPrice));
    }

    private ActionParameters parseParameters(String parameters) {
         try {
            return objectMapper.readValue(parameters, ActionParameters.class);
        } catch (JsonProcessingException e) {
            log.error("Error parsing parameters: {}", parameters, e);
            throw new IllegalArgumentException("Invalid parameters format", e);
        }
    }

    private void executeDiscountPercentage(
            RuleAction action,
            RuleEvaluationContext context,
            RuleEvaluationResult result) {

        ActionParameters params = parseParameters(action.getParameters());
        BigDecimal discountPercent = new BigDecimal(params.getValue());
        BigDecimal discountAmount = context.getCurrentPrice()
                .multiply(discountPercent)
                .divide(new BigDecimal("100"));

        BigDecimal newPrice = context.getCurrentPrice().subtract(discountAmount);
        result.setAdjustedPrice(newPrice);
        result.setDiscountAmount(discountAmount);
    }

    private void executeDiscountAmount(
            RuleAction action,
            RuleEvaluationContext context,
            RuleEvaluationResult result) {

        ActionParameters params = parseParameters(action.getParameters());
        BigDecimal discountAmount = new BigDecimal(params.getValue());
        BigDecimal currentPrice = result.getAdjustedPrice();

        // Apply maximum discount if specified
        if (params.getMaxDiscountAmount() != null) {
            BigDecimal maxDiscount = new BigDecimal(String.valueOf(params.getMaxDiscountAmount()));
            discountAmount = discountAmount.min(maxDiscount);
        }

        // Ensure discount doesn't make price negative
        if (discountAmount.compareTo(currentPrice) >= 0) {
            log.warn("Discount amount {} exceeds current price {}", discountAmount, currentPrice);
            discountAmount = currentPrice.multiply(new BigDecimal("0.99")); // 99% discount max
        }

        BigDecimal newPrice = currentPrice.subtract(discountAmount);
        result.setAdjustedPrice(newPrice);
        result.getMetadata().put("appliedDiscountAmount", discountAmount);
    }

    private void executeMatchCompetitor(
        RuleAction action,
        RuleEvaluationContext context,
        RuleEvaluationResult result) {

    ActionParameters params = parseParameters(action.getParameters());

    // Get competitor price
    BigDecimal competitorPrice = competitorPriceService.getCompetitorPrice(
            context.getProductId(),
            params.getCompetitor()
    );

    if (competitorPrice == null) {
        log.warn("No competitor price found for {}", context.getProductId());
        return;
    }

    BigDecimal newPrice = competitorPrice;

    // Ensure minimum margin if specified
    if (params.getMinimumMargin() != null && context.getCostPrice() != null) {
        BigDecimal minMarginPrice = calculateMinimumMarginPrice(
                context.getCostPrice(),
                new BigDecimal(String.valueOf(params.getMinimumMargin()))
        );
        newPrice = newPrice.max(minMarginPrice);
    }

    result.setAdjustedPrice(newPrice);
    result.getMetadata().put("competitorPrice", competitorPrice);
    result.getMetadata().put("competitor", params.getCompetitor());
}

    private void executeSetMargin(
            RuleAction action,
            RuleEvaluationContext context,
            RuleEvaluationResult result) {

        if (context.getCostPrice() == null || context.getCostPrice().equals(BigDecimal.ZERO)) {
            log.warn("Cannot set margin: cost price is null or zero");
            return;
        }

        ActionParameters params = parseParameters(action.getParameters());
        BigDecimal targetMargin = new BigDecimal(params.getValue());

        BigDecimal marginMultiplier = BigDecimal.ONE.add(
                targetMargin.divide(new BigDecimal("100"), 4, RoundingMode.HALF_UP)
        );

        BigDecimal newPrice = context.getCostPrice()
                .multiply(marginMultiplier)
                .setScale(2, RoundingMode.HALF_UP);

        result.setAdjustedPrice(newPrice);
        result.getMetadata().put("targetMargin", targetMargin);
        result.getMetadata().put("costPrice", context.getCostPrice());
    }

    private void executeBeatCompetitor(
            RuleAction action,
            RuleEvaluationContext context,
            RuleEvaluationResult result) {

        ActionParameters params = parseParameters(action.getParameters());

        // Get competitor price
        BigDecimal competitorPrice = competitorPriceService.getCompetitorPrice(
                context.getProductId(),
                params.getCompetitor()
        );

        if (competitorPrice == null) {
            log.warn("No competitor price found for {}", context.getProductId());
            return;
        }
        // Calculate beat percentage
        BigDecimal beatPercentage = new BigDecimal(params.getBeatPercentage().toString());
        BigDecimal multiplier = BigDecimal.ONE.subtract(
                beatPercentage.divide(new BigDecimal("100"), 4, RoundingMode.HALF_UP)
        );

        // Calculate new price
        BigDecimal newPrice = competitorPrice.multiply(multiplier)
                .setScale(2, RoundingMode.HALF_UP);

        // Ensure minimum margin if specified
        if (params.getMinimumMargin() != null && context.getCostPrice() != null) {
            BigDecimal minMarginPrice = calculateMinimumMarginPrice(
                    context.getCostPrice(),
                    new BigDecimal(String.valueOf(params.getMinimumMargin()))
            );
            newPrice = newPrice.max(minMarginPrice);
        }

        result.setAdjustedPrice(newPrice);
        result.getMetadata().put("competitorPrice", competitorPrice);
        result.getMetadata().put("beatPercentage", beatPercentage);
        result.getMetadata().put("competitor", params.getCompetitor());
    }

    private void executeBundleDiscount(
            RuleAction action,
            RuleEvaluationContext context,
            RuleEvaluationResult result) {

        ActionParameters params = parseParameters(action.getParameters());
        String bundleId = params.getBundleId();

        // Verify bundle eligibility
        BundleEligibility eligibility = bundleService.checkEligibility(
                context.getProductId(),
                bundleId,
                context.getAttributes()
        );

        if (!eligibility.isEligible()) {
            log.debug("Product not eligible for bundle: {}", bundleId);
            return;
        }

        BigDecimal currentPrice = result.getAdjustedPrice();
        BigDecimal discountValue = new BigDecimal(params.getValue());

        BigDecimal newPrice;
        if ("PERCENTAGE".equals(params.getDiscountType())) {
            BigDecimal multiplier = BigDecimal.ONE.subtract(
                    discountValue.divide(new BigDecimal("100"), 4, RoundingMode.HALF_UP)
            );
            newPrice = currentPrice.multiply(multiplier);
        } else {
            newPrice = currentPrice.subtract(discountValue);
        }

        result.setAdjustedPrice(newPrice.setScale(2, RoundingMode.HALF_UP));
        result.getMetadata().put("bundleId", bundleId);
        result.getMetadata().put("bundleDiscount", discountValue);
        result.getMetadata().put("discountType", params.getDiscountType());
    }

    private void executeQuantityDiscount(
            RuleAction action,
            RuleEvaluationContext context,
            RuleEvaluationResult result) {

        if (context.getQuantity() == null || context.getQuantity() <= 0) {
            log.debug("No quantity specified for quantity discount");
            return;
        }

        ActionParameters params = parseParameters(action.getParameters());
        Integer minimumQuantity = Integer.valueOf(params.getMinQuantity());

        if (context.getQuantity() < minimumQuantity) {
            log.debug("Quantity {} less than minimum required: {}",
                    context.getQuantity(), minimumQuantity);
            return;
        }

        BigDecimal currentPrice = result.getAdjustedPrice();
        BigDecimal discountValue = new BigDecimal(params.getValue());

        BigDecimal newPrice;
        if ("PERCENTAGE".equals(params.getDiscountType())) {
            // Calculate tiered discount percentage based on quantity
            BigDecimal effectiveDiscount = calculateTieredDiscount(
                    context.getQuantity(),
                    minimumQuantity,
                    discountValue,
                    params.getTierIncrement()
            );

            BigDecimal multiplier = BigDecimal.ONE.subtract(
                    effectiveDiscount.divide(new BigDecimal("100"), 4, RoundingMode.HALF_UP)
            );
            newPrice = currentPrice.multiply(multiplier);
        } else {
            // Fixed amount discount per unit above minimum
            int extraUnits = context.getQuantity() - minimumQuantity + 1;
            BigDecimal totalDiscount = discountValue.multiply(new BigDecimal(extraUnits));
            newPrice = currentPrice.subtract(totalDiscount);
        }

        result.setAdjustedPrice(newPrice.setScale(2, RoundingMode.HALF_UP));
        result.getMetadata().put("quantity", context.getQuantity());
        result.getMetadata().put("minimumQuantity", minimumQuantity);
        result.getMetadata().put("appliedDiscount", discountValue);
    }

    private void executeCustomAction(
            RuleAction action,
            RuleEvaluationContext context,
            RuleEvaluationResult result) throws ActionExecutionException, ActionRegistrationException {

        ActionParameters params = parseParameters(action.getParameters());
        String actionHandler = params.getActionHandler();

        if (StringUtils.isEmpty(actionHandler)) {
            throw new ActionExecutionException("Custom action handler not specified");
        }

        CustomActionHandler handler = customActionRegistry.getHandler(actionHandler);
        if (handler == null) {
            throw new ActionExecutionException("Custom action handler not found: " + actionHandler);
        }

        try {
            handler.execute(action, context, result);
        } catch (Exception e) {
            throw new ActionExecutionException(
                    "Error executing custom action: " + actionHandler, e);
        }
    }

    private BigDecimal calculateMinimumMarginPrice(
            BigDecimal costPrice,
            BigDecimal minimumMargin) {
        BigDecimal marginMultiplier = BigDecimal.ONE.add(
                minimumMargin.divide(new BigDecimal("100"), 4, RoundingMode.HALF_UP)
        );
        return costPrice.multiply(marginMultiplier).setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal calculateTieredDiscount(
            Integer quantity,
            Integer minimumQuantity,
            BigDecimal baseDiscount,
            BigDecimal tierIncrement) {

        if (tierIncrement == null || tierIncrement.equals(BigDecimal.ZERO)) {
            return baseDiscount;
        }

        int tiers = (quantity - minimumQuantity) / minimumQuantity;
        return baseDiscount.add(tierIncrement.multiply(new BigDecimal(tiers)))
                .min(new BigDecimal("90")); // Maximum 90% discount
    }
}
