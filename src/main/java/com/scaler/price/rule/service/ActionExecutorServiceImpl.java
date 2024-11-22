package com.scaler.price.rule.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.scaler.price.rule.domain.RuleAction;
import com.scaler.price.validation.helper.ActionParameters;
import com.scaler.price.rule.dto.RuleEvaluationContext;
import com.scaler.price.rule.dto.RuleEvaluationResult;
import com.scaler.price.rule.exceptions.ActionExecutionException;
import lombok.RequiredArgsConstructor;
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
@RequiredArgsConstructor
public class ActionExecutorServiceImpl {
    private final ObjectMapper objectMapper;
    private final CompetitorPriceService competitorPriceService;
    private final BundleService bundleService;

    public RuleEvaluationResult executeActions(
            Set<RuleAction> actions,
            RuleEvaluationContext context) throws ActionExecutionException {

        RuleEvaluationResult result = new RuleEvaluationResult();
        result.setAdjustedPrice(context.getCurrentPrice());

        // Sort actions by sequence
        List<RuleAction> sortedActions = new ArrayList<>(actions);
        sortedActions.sort(Comparator.comparing(RuleAction::getSequence));

        for (RuleAction action : sortedActions) {
            try {
                executeAction(action, context, result);
            } catch (Exception e) {
                log.error("Error executing action: {}", e.getMessage(), e);
                return null;
            }
        }

        return result;
    }

    private void executeAction(
            RuleAction action,
            RuleEvaluationContext context,
            RuleEvaluationResult result) throws ActionExecutionException {

        switch (action.getType()) {
            case SET_PRICE:
                executeSetPrice(action, context, result);
                break;
            case APPLY_DISCOUNT_PERCENTAGE:
                executeDiscountPercentage(action, context, result);
                break;
            case APPLY_DISCOUNT_AMOUNT:
                executeDiscountAmount(action, context, result);
                break;
            case SET_MARGIN:
                executeSetMargin(action, context, result);
                break;
            case MATCH_COMPETITOR_PRICE:
                executeMatchCompetitor(action, context, result);
                break;
            case BEAT_COMPETITOR_PRICE:
                executeBeatCompetitor(action, context, result);
                break;
            case BUNDLE_DISCOUNT:
                executeBundleDiscount(action, context, result);
                break;
            case QUANTITY_DISCOUNT:
                executeQuantityDiscount(action, context, result);
                break;
            default:
                log.warn("Unsupported action type: {}", action.getType());
        }
    }

    private void executeQuantityDiscount(RuleAction action, RuleEvaluationContext context, RuleEvaluationResult result) throws ActionExecutionException {
        ActionParameters params = parseParameters(action.getParameters());
        Integer minQuantity = Integer.valueOf(params.getMinQuantity());
        Integer maxQuantity = Integer.valueOf(params.getMaxQuantity());
        Integer discountPercentage = params.getDiscountPercentage();

        if (context.getQuantity() >= minQuantity && context.getQuantity() <= maxQuantity) {
            BigDecimal discountAmount = result.getAdjustedPrice()
                    .multiply(new BigDecimal(discountPercentage))
                    .divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);

            BigDecimal newPrice = result.getAdjustedPrice().subtract(discountAmount);
            result.setAdjustedPrice(newPrice);
            result.getMetadata().put("appliedQuantityDiscount", discountAmount);
        }

    }

    private void executeBundleDiscount(RuleAction action, RuleEvaluationContext context, RuleEvaluationResult result) {
        ActionParameters params = parseParameters(action.getParameters());
        String bundleId = params.getBundleId();
        BigDecimal discount = new BigDecimal(params.getValue());

        BigDecimal bundleDiscount = bundleService.getBundleDiscount(bundleId, context.getProductId());
        if (bundleDiscount != null) {
            BigDecimal discountAmount = result.getAdjustedPrice()
                    .multiply(bundleDiscount)
                    .divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);

            BigDecimal newPrice = result.getAdjustedPrice().subtract(discountAmount);
            result.setAdjustedPrice(newPrice);
            result.getMetadata().put("appliedBundleDiscount", discountAmount);
        }
    }

    private void executeBeatCompetitor(RuleAction action, RuleEvaluationContext context, RuleEvaluationResult result) {
        ActionParameters params = parseParameters(action.getParameters());
        String competitor = params.getCompetitor();
        BigDecimal beatBy = new BigDecimal(params.getValue());

        BigDecimal competitorPrice = competitorPriceService.getCompetitorPrice(context.getProductId(), competitor);
        if (competitorPrice != null && result.getAdjustedPrice().compareTo(competitorPrice) > 0) {
            BigDecimal discountAmount = result.getAdjustedPrice()
                    .subtract(competitorPrice)
                    .multiply(beatBy)
                    .divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);

            BigDecimal newPrice = result.getAdjustedPrice().subtract(discountAmount);
            result.setAdjustedPrice(newPrice);
            result.getMetadata().put("beatCompetitorPrice", discountAmount);
        }
    }

    private void executeSetMargin(RuleAction action, RuleEvaluationContext context, RuleEvaluationResult result) throws ActionExecutionException {
        ActionParameters params = parseParameters(action.getParameters());
        BigDecimal minimumMargin = new BigDecimal(String.valueOf(params.getMinimumMargin()));
        BigDecimal maximumMargin = new BigDecimal(String.valueOf(params.getMaximumMargin()));

        BigDecimal currentPrice = result.getAdjustedPrice();
        BigDecimal margin = currentPrice
                .multiply(minimumMargin)
                .divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);

        BigDecimal newPrice = currentPrice.add(margin);
        result.setAdjustedPrice(newPrice);
    }

    private void executeDiscountAmount(RuleAction action, RuleEvaluationContext context, RuleEvaluationResult result) throws ActionExecutionException {
        ActionParameters params = parseParameters(action.getParameters());
        BigDecimal discountAmount = new BigDecimal(params.getValue());

        BigDecimal currentPrice = result.getAdjustedPrice();
        BigDecimal newPrice = currentPrice.subtract(discountAmount);
        result.setAdjustedPrice(newPrice);
    }

    private void executeSetPrice(
            RuleAction action,
            RuleEvaluationContext context,
            RuleEvaluationResult result) throws ActionExecutionException {

        ActionParameters params = parseParameters(action.getParameters());
        BigDecimal newPrice = new BigDecimal(params.getValue());
        result.setAdjustedPrice(newPrice);
    }

    private void executeDiscountPercentage(
            RuleAction action,
            RuleEvaluationContext context,
            RuleEvaluationResult result) throws ActionExecutionException {

        ActionParameters params = parseParameters(action.getParameters());
        BigDecimal discountPercent = new BigDecimal(params.getValue());

        BigDecimal currentPrice = result.getAdjustedPrice();
        BigDecimal discountAmount = currentPrice
                .multiply(discountPercent)
                .divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);

        result.setAdjustedPrice(currentPrice.subtract(discountAmount));
    }

    private void executeMatchCompetitor(
            RuleAction action,
            RuleEvaluationContext context,
            RuleEvaluationResult result) throws ActionExecutionException {

        ActionParameters params = parseParameters(action.getParameters());
        BigDecimal competitorPrice = competitorPriceService
                .getCompetitorPrice(context.getProductId(), params.getCompetitor());

        if (competitorPrice != null) {
            result.setAdjustedPrice(competitorPrice);
            result.getMetadata().put("matchedCompetitor", params.getCompetitor());
        }
    }

    private ActionParameters parseParameters(String parameters) throws ActionExecutionException {
        try {
            return objectMapper.readValue(parameters, ActionParameters.class);
        } catch (Exception e) {
            throw new ActionExecutionException("Invalid action parameters", e);
        }
    }
}
