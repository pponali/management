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
@RequiredArgsConstructor
public class RuleEngineService {

    private final ObjectMapper objectMapper;
    private final ActionExecutorService actionExecutorService;
    private final ConfigurationService configService;

    public RuleEngineService(ObjectMapper objectMapper, ActionExecutorService actionExecutorService, ConfigurationService configService) {
        this.objectMapper = objectMapper;
        this.actionExecutorService = actionExecutorService;
        this.configService = configService;
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

    public RuleEvaluationResult evaluateRule(RuleAction action, Map<String, Object> context) {
        RuleEvaluationContext evaluationContext = new RuleEvaluationContext();
        evaluationContext.setParameters(context);
        
        RuleEvaluationResult result = new RuleEvaluationResult();
        result.setSuccess(true);
        
        try {
            actionExecutorService.executeAction(action, evaluationContext, result);
        } catch (Exception e) {
            log.error("Error executing rule action", e);
            result.setSuccess(false);
            result.setErrorMessage(e.getMessage());
        }
        
        return result;
    }

    private RuleEvaluationResult executeRuleActions(
            PricingRule rule,
            RuleEvaluationRequest request,
            BigDecimal currentPrice) {

        RuleEvaluationResult result = new RuleEvaluationResult();
        result.setRuleId(rule.getId());
        result.setOriginalPrice(currentPrice);

        try {
            Map<String, Object> context = new HashMap<>();
            context.put("currentPrice", currentPrice);
            context.put("costPrice", request.getCostPrice());
            context.put("basePrice", request.getBasePrice());
            context.put("productId", request.getProductId());
            context.put("categoryId", request.getCategoryId());

            BigDecimal adjustedPrice = currentPrice;
            for (RuleAction action : rule.getActions()) {
                RuleEvaluationResult actionResult = evaluateRule(action, context);
                if (!actionResult.isSuccess()) {
                    return actionResult;
                }
                if (actionResult.getAdjustedPrice() != null) {
                    adjustedPrice = actionResult.getAdjustedPrice();
                }
            }

            adjustedPrice = validatePriceBounds(adjustedPrice, rule, request.getCostPrice());
            BigDecimal margin = calculateMargin(rule, request.getBasePrice());

            result.setSuccess(true);
            result.setAdjustedPrice(adjustedPrice);
            result.setDiscountAmount(currentPrice.subtract(adjustedPrice));
            result.setMarginPercentage(margin);

            return result;
        } catch (Exception e) {
            log.error("Error executing actions for rule {}: {}", rule.getId(), e.getMessage());
            result.setSuccess(false);
            result.setErrorMessage(e.getMessage());
            return result;
        }
    }

    public BigDecimal calculateMargin(PricingRule rule, BigDecimal basePrice) {
        if (rule == null || basePrice == null) {
            return BigDecimal.ZERO;
        }

        BigDecimal marginPercentage = rule.getMarginPercentage();
        if (marginPercentage == null) {
            marginPercentage = configService.getDefaultMargin();
        }

        if (marginPercentage.compareTo(configService.getMinimumMargin()) < 0) {
            marginPercentage = configService.getMinimumMargin();
        } else if (marginPercentage.compareTo(configService.getMaximumMargin()) > 0) {
            marginPercentage = configService.getMaximumMargin();
        }

        return basePrice.multiply(marginPercentage)
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
    }

    private BigDecimal validatePriceBounds(BigDecimal price, PricingRule rule, BigDecimal costPrice) {
        if (price == null) {
            return costPrice;
        }

        BigDecimal minPrice = rule.getMinPrice();
        BigDecimal maxPrice = rule.getMaxPrice();

        if (minPrice != null && price.compareTo(minPrice) < 0) {
            return minPrice;
        }

        if (maxPrice != null && price.compareTo(maxPrice) > 0) {
            return maxPrice;
        }

        return price;
    }
}
