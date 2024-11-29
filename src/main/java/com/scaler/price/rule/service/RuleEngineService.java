package com.scaler.price.rule.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.scaler.price.core.management.service.ConfigurationService;

import com.scaler.price.rule.domain.PricingRule;
import com.scaler.price.rule.domain.RuleAction;
import com.scaler.price.rule.domain.RuleCondition;
import com.scaler.price.rule.dto.RuleEvaluationRequest;
import com.scaler.price.rule.dto.RuleEvaluationResult;
import com.scaler.price.rule.exceptions.ActionExecutionException;
import com.scaler.price.rule.exceptions.ActionRegistrationException;
import com.scaler.price.rule.exceptions.ProductFetchException;
import com.scaler.price.rule.dto.RuleEvaluationContext;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Service
@Slf4j
@RequiredArgsConstructor
public class RuleEngineService {

    private final ObjectMapper objectMapper;
    private final ActionExecutorService actionExecutorService;
    private final ConfigurationService configService;


    public RuleEvaluationResult evaluate(
            PricingRule rule,
            RuleEvaluationRequest request,
            BigDecimal currentPrice) throws ActionExecutionException, ActionRegistrationException, ProductFetchException {

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

    private boolean evaluateCondition(JsonNode parameters, RuleEvaluationRequest request) {
        try {
            if (parameters == null || request == null) {
                log.warn("Null parameters or request in condition evaluation");
                return false;
            }

            // Get condition type
            String conditionType = parameters.path("type").asText();
            if (conditionType == null || conditionType.isEmpty()) {
                log.warn("No condition type specified in parameters");
                return false;
            }

            switch (conditionType.toLowerCase()) {
                case "price_range":
                    BigDecimal minPrice = new BigDecimal(parameters.path("minPrice").asText("0"));
                    BigDecimal maxPrice = new BigDecimal(parameters.path("maxPrice").asText("999999999"));
                    return request.getBasePrice().compareTo(minPrice) >= 0 
                           && request.getBasePrice().compareTo(maxPrice) <= 0;

                case "category_match":
                    JsonNode categories = parameters.path("categories");
                    if (categories.isArray()) {
                        for (JsonNode category : categories) {
                            if (category.asText().equals(request.getCategoryId())) {
                                return true;
                            }
                        }
                    }
                    return false;

                case "date_range":
                    LocalDateTime now = LocalDateTime.now();
                    LocalDateTime startDate = LocalDateTime.parse(parameters.path("startDate").asText());
                    LocalDateTime endDate = LocalDateTime.parse(parameters.path("endDate").asText());
                    return now.isAfter(startDate) && now.isBefore(endDate);

                case "quantity_threshold":
                    int minQuantity = parameters.path("minQuantity").asInt(0);
                    int maxQuantity = parameters.path("maxQuantity").asInt(Integer.MAX_VALUE);
                    int requestQuantity = request.getQuantity();
                    return requestQuantity >= minQuantity && requestQuantity <= maxQuantity;

                case "margin_check":
                    BigDecimal minMarginPercent = new BigDecimal(parameters.path("minMarginPercent").asText("0"));
                    BigDecimal currentMargin = request.getBasePrice()
                            .subtract(request.getCostPrice())
                            .divide(request.getBasePrice(), 4, RoundingMode.HALF_UP)
                            .multiply(new BigDecimal("100"));
                    return currentMargin.compareTo(minMarginPercent) >= 0;

                case "customer_segment":
                    String customerSegment = request.getCustomerSegment();
                    JsonNode allowedSegments = parameters.path("segments");
                    if (customerSegment == null || customerSegment.isEmpty()) {
                        return false; 
                    }
                    if (allowedSegments.isArray()) {
                        for (JsonNode segment : allowedSegments) {
                            if (segment.asText().equals(customerSegment)) {
                                return true;
                            }
                        }
                    }
                    return false;

                case "time_of_day":
                    int currentHour = LocalDateTime.now().getHour();
                    int startHour = parameters.path("startHour").asInt(0);
                    int endHour = parameters.path("endHour").asInt(23);
                    return currentHour >= startHour && currentHour <= endHour;

                case "composite":
                    JsonNode conditions = parameters.path("conditions");
                    String operator = parameters.path("operator").asText("AND");
                    if (!conditions.isArray()) {
                        return false;
                    }
                    
                    boolean result = operator.equals("AND");
                    for (JsonNode condition : conditions) {
                        boolean subConditionResult = evaluateCondition(condition, request);
                        if (operator.equals("AND")) {
                            result &= subConditionResult;
                            if (!result) break; // Short circuit for AND
                        } else {
                            result |= subConditionResult;
                            if (result) break; // Short circuit for OR
                        }
                    }
                    return result;

                default:
                    log.warn("Unknown condition type: {}", conditionType);
                    return false;
            }
        } catch (Exception e) {
            log.error("Error evaluating condition: {}", e.getMessage(), e);
            return false;
        }
    }

    public RuleEvaluationResult evaluateRule(RuleAction action, Map<String, Object> context) throws ActionExecutionException, ActionRegistrationException, ProductFetchException {
        RuleEvaluationContext evaluationContext = RuleEvaluationContext.builder()
            .attributes(context)
            .build();
        
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
            BigDecimal currentPrice) throws ActionExecutionException, ActionRegistrationException, ProductFetchException {

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

        BigDecimal minPrice = rule.getMinimumPrice();
        BigDecimal maxPrice = rule.getMaximumPrice();

        if (minPrice != null && price.compareTo(minPrice) < 0) {
            return minPrice;
        }

        if (maxPrice != null && price.compareTo(maxPrice) > 0) {
            return maxPrice;
        }

        return price;
    }
}
