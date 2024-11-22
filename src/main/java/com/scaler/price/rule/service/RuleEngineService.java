package com.scaler.price.rule.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.scaler.price.rule.domain.PricingRule;
import com.scaler.price.rule.domain.RuleAction;
import com.scaler.price.rule.domain.RuleCondition;
import com.scaler.price.rule.dto.RuleEvaluationRequest;
import com.scaler.price.rule.dto.RuleEvaluationResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.Map;

@Service
@Slf4j
public class RuleEngineService {

    private final ObjectMapper objectMapper;
    private final ActionExecutorService actionExecutorService;

    public RuleEngineService(ObjectMapper objectMapper, ActionExecutorService actionExecutorService) {
        this.objectMapper = objectMapper;
        this.actionExecutorService = actionExecutorService;
    }

    public RuleEvaluationResult evaluate(
            PricingRule rule,
            RuleEvaluationRequest request,
            BigDecimal currentPrice) {

        if (!evaluateConditions(rule, request)) {
            return null;
        }

        return executeRuleActions(rule, request, currentPrice);
    }

    private boolean evaluateConditions(PricingRule rule, RuleEvaluationRequest request) {
        try {
            for (RuleCondition condition : rule.getConditions()) {
                if (!evaluateCondition(condition.getParameters(), request)) {
                    return false;
                }
            }
            return true;
        } catch (Exception e) {
            log.error("Error evaluating conditions for rule {}: {}", 
                    rule.getId(), e.getMessage());
            return false;
        }
    }

    private boolean evaluateCondition(JsonNode condition, RuleEvaluationRequest request) {
        // Implement condition evaluation logic here
        return true; // Placeholder implementation
    }

    private RuleEvaluationResult executeRuleActions(
            PricingRule rule,
            RuleEvaluationRequest request,
            BigDecimal currentPrice) {

        RuleEvaluationResult result = new RuleEvaluationResult();
        result.setRuleId(rule.getId());
        result.setRuleName(rule.getRuleName());

        try {
            Map<String, Object> executionContext = new HashMap<>();
            executionContext.put("currentPrice", currentPrice);
            executionContext.put("basePrice", request.getBasePrice());
            executionContext.put("costPrice", request.getCostPrice());

            BigDecimal adjustedPrice = currentPrice;
            for (RuleAction action : rule.getActions()) {
                adjustedPrice = actionExecutorService.executeAction(action, executionContext);
            }

            // Validate price bounds
            adjustedPrice = validatePriceBounds(adjustedPrice, rule, request.getCostPrice());

            result.setAdjustedPrice(adjustedPrice);
            result.setDiscountAmount(currentPrice.subtract(adjustedPrice));
            result.setMarginPercentage(calculateMargin(adjustedPrice, request.getCostPrice()));

            return result;
        } catch (Exception e) {
            log.error("Error executing actions for rule {}: {}", 
                    rule.getId(), e.getMessage());
            return null;
        }
    }

    private BigDecimal validatePriceBounds(
            BigDecimal price,
            PricingRule rule,
            BigDecimal costPrice) {

        // Check minimum price
        if (rule.getMinimumPrice() != null &&
                price.compareTo(rule.getMinimumPrice()) < 0) {
            return rule.getMinimumPrice();
        }

        // Check maximum price
        if (rule.getMaximumPrice() != null &&
                price.compareTo(rule.getMaximumPrice()) > 0) {
            return rule.getMaximumPrice();
        }

        // Check minimum margin
        if (rule.getMarginPercentage() != null && costPrice != null) {
            BigDecimal currentMargin = calculateMargin(price, costPrice);
            if (currentMargin.compareTo(rule.getMarginPercentage()) < 0) {
                return calculatePriceWithMinimumMargin(
                        costPrice, rule.getMarginPercentage());
            }
        }

        return price;
    }

    private BigDecimal calculateMargin(BigDecimal price, BigDecimal costPrice) {
        if (price == null || costPrice == null || costPrice.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }
        return price.subtract(costPrice)
                .divide(costPrice, 4, RoundingMode.HALF_UP)
                .multiply(new BigDecimal("100"));
    }

    private BigDecimal calculatePriceWithMinimumMargin(BigDecimal costPrice, BigDecimal marginPercentage) {
        if (costPrice == null || marginPercentage == null) {
            return costPrice;
        }
        BigDecimal marginMultiplier = BigDecimal.ONE.add(
                marginPercentage.divide(new BigDecimal("100"), 4, RoundingMode.HALF_UP));
        return costPrice.multiply(marginMultiplier).setScale(2, RoundingMode.HALF_UP);
    }
}
