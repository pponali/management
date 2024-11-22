package com.scaler.price.rule.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.scaler.price.rule.domain.Operator;
import com.scaler.price.rule.domain.RuleCondition;
import com.scaler.price.rule.dto.RuleEvaluationContext;
import com.scaler.price.rule.exceptions.RuleEvaluationException;
import com.scaler.price.core.management.service.InventoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@Slf4j
@RequiredArgsConstructor
public class ConditionEvaluatorService {
    private final ObjectMapper objectMapper;
    private final CompetitorPriceService competitorPriceService;
    private final InventoryService inventoryService;
    private final ProductAttributeService productAttributeService;

    public boolean evaluateConditions(Set<RuleCondition> conditions, RuleEvaluationContext context) throws RuleEvaluationException {
        try {
            return conditions.stream().allMatch(condition -> evaluateCondition(condition, context));
        } catch (Exception e) {
            log.error("Error evaluating conditions for product {}: {}",
                    context.getProductId(), e.getMessage());
            throw new RuleEvaluationException("Condition evaluation failed", e);
        }
    }

    private boolean evaluateCondition(RuleCondition condition, RuleEvaluationContext context) {
        try {
            return switch (condition.getType()) {
                case ATTRIBUTE_MATCH -> false;
                case PRICE_RANGE -> evaluatePriceRange(condition, context);
                case MARGIN_RANGE -> evaluateMarginRange(condition, context);
                case DATE_RANGE -> false;
                case INVENTORY_LEVEL -> evaluateInventoryLevel(condition, context);
                case COMPETITOR_PRICE -> evaluateCompetitorPrice(condition, context);
                case SALES_VELOCITY -> evaluateSalesVelocity(condition, context);
                case CATEGORY_ATTRIBUTE -> evaluateCategoryAttribute(condition, context);
                case PRODUCT_ATTRIBUTE -> evaluateProductAttribute(condition, context);
                case TIME_BASED -> evaluateTimeBased(condition, context);
                case CATEGORY_MATCH -> false;
                case BRAND_MATCH -> false;
                case CUSTOM -> evaluateCustomCondition(condition, context);
            };
        } catch (Exception e) {
            log.error("Error evaluating condition {}: {}", condition.getId(), e.getMessage());
            return false;
        }
    }

    private boolean evaluateCategoryAttribute(RuleCondition condition, RuleEvaluationContext context) {
        String categoryId = context.getCategoryId();
        if (categoryId == null) {
            return false;
        }

        String attributeValue = productAttributeService.getAttributeValue(
                categoryId,
                condition.getAttribute()
        );

        if (attributeValue == null) {
            return false;
        }

        return switch (condition.getOperator()) {
            case EQUALS -> attributeValue.equals(condition.getValue());
            case CONTAINS -> attributeValue.contains(condition.getValue());
            case IN -> {
                List<String> validValues = List.of(condition.getValue().split(","));
                yield validValues.contains(attributeValue);
            }
            default -> false;
        };
    }

    // ... existing code ...
    private boolean evaluateSalesVelocity(RuleCondition condition, RuleEvaluationContext context) throws JsonProcessingException {
        // Retrieve sales velocity data from the context or an external service
        BigDecimal salesVelocity = (BigDecimal) context.getFromCache("salesVelocity");

        if (salesVelocity == null) {
            // Example: Fetch sales velocity from an external service
            salesVelocity = evaluateSalesVelocity(context.getProductId(), context.getTimePeriod());
            context.addToCache("salesVelocity", salesVelocity);
        }

        // Parse the condition value to get the expected sales velocity range or threshold
        Map<String, BigDecimal> velocityRange = objectMapper.readValue(condition.getValue(),
                new TypeReference<Map<String, BigDecimal>>() {});

        // Evaluate the sales velocity against the condition's operator and value
        return evaluateNumericCondition(salesVelocity, condition.getOperator(), velocityRange);
    }

    private boolean evaluatePriceRange(RuleCondition condition, RuleEvaluationContext context) throws JsonProcessingException {
        BigDecimal price = context.getCurrentPrice();
        Map<String, BigDecimal> range = objectMapper.readValue(condition.getValue(),
                new TypeReference<Map<String, BigDecimal>>() {});

        return switch (condition.getOperator()) {
            case BETWEEN -> price.compareTo(range.get("min")) >= 0 &&
                    price.compareTo(range.get("max")) <= 0;
            case GREATER_THAN -> price.compareTo(range.get("value")) > 0;
            case LESS_THAN -> price.compareTo(range.get("value")) < 0;
            case GREATER_THAN_EQUALS -> price.compareTo(range.get("value")) >= 0;
            case LESS_THAN_EQUALS -> price.compareTo(range.get("value")) <= 0;
            default -> false;
        };
    }

    private boolean evaluateMarginRange(RuleCondition condition, RuleEvaluationContext context) throws JsonProcessingException {
        if (context.getCostPrice() == null || context.getCostPrice().equals(BigDecimal.ZERO)) {
            return false;
        }

        BigDecimal margin = calculateMarginPercentage(
                context.getCurrentPrice(),
                context.getCostPrice()
        );

        Map<String, BigDecimal> range = objectMapper.readValue(condition.getValue(),
                new TypeReference<Map<String, BigDecimal>>() {});

        return evaluateNumericCondition(margin, condition.getOperator(), range);
    }

    private boolean evaluateInventoryLevel(RuleCondition condition, RuleEvaluationContext context) throws JsonProcessingException {
        // Cache inventory check to avoid multiple service calls
        Integer inventoryLevel = (Integer) context.getFromCache("inventoryLevel");

        if (inventoryLevel == null) {
            inventoryLevel = inventoryService.getInventoryLevel(
                    context.getProductId(),
                    context.getSellerId(),
                    context.getSiteId()
            );
            context.addToCache("inventoryLevel", inventoryLevel);
        }

        Map<String, Integer> threshold = objectMapper.readValue(condition.getValue(),
                new TypeReference<Map<String, Integer>>() {});

        return evaluateNumericCondition(
                BigDecimal.valueOf(inventoryLevel),
                condition.getOperator(),
                threshold
        );
    }

    private boolean evaluateCompetitorPrice(RuleCondition condition, RuleEvaluationContext context) {
        BigDecimal competitorPrice = (BigDecimal) context.getFromCache("competitorPrice");

        if (competitorPrice == null) {
            competitorPrice = competitorPriceService.getCompetitorPrice(
                    context.getProductId(),
                    condition.getValue() // competitor identifier
            );
            context.addToCache("competitorPrice", competitorPrice);
        }

        if (competitorPrice == null) {
            return false;
        }

        return switch (condition.getOperator()) {
            case GREATER_THAN -> context.getCurrentPrice().compareTo(competitorPrice) > 0;
            case LESS_THAN -> context.getCurrentPrice().compareTo(competitorPrice) < 0;
            case EQUALS -> context.getCurrentPrice().compareTo(competitorPrice) == 0;
            default -> false;
        };
    }

    private boolean evaluateTimeBased(RuleCondition condition, RuleEvaluationContext context) throws JsonProcessingException {
        Map<String, String> timeConfig = objectMapper.readValue(condition.getValue(),
                new TypeReference<Map<String, String>>() {});

        LocalDateTime now = context.getEvaluationTime();
        LocalTime currentTime = now.toLocalTime();

        LocalTime startTime = LocalTime.parse(timeConfig.get("startTime"));
        LocalTime endTime = LocalTime.parse(timeConfig.get("endTime"));

        return currentTime.isAfter(startTime) && currentTime.isBefore(endTime);
    }

    private boolean evaluateProductAttribute(RuleCondition condition, RuleEvaluationContext context) throws JsonProcessingException {
        String attributeValue = productAttributeService.getAttributeValue(
                context.getProductId(),
                condition.getAttribute()
        );

        if (attributeValue == null) {
            return false;
        }

        return switch (condition.getOperator()) {
            case EQUALS -> attributeValue.equals(condition.getValue());
            case CONTAINS -> attributeValue.contains(condition.getValue());
            case IN -> {
                List<String> validValues = objectMapper.readValue(condition.getValue(),
                        new TypeReference<List<String>>() {});
                yield validValues.contains(attributeValue);
            }
            default -> false;
        };
    }

    private boolean evaluateCustomCondition(RuleCondition condition, RuleEvaluationContext context) {
        // Implement custom condition logic based on your business requirements
        // This could involve calling external services or implementing complex business rules
        return true;
    }

    private BigDecimal calculateMarginPercentage(BigDecimal sellingPrice, BigDecimal costPrice) {
        if (costPrice.equals(BigDecimal.ZERO)) {
            return BigDecimal.ZERO;
        }

        return sellingPrice.subtract(costPrice)
                .divide(costPrice, 4, RoundingMode.HALF_UP)
                .multiply(new BigDecimal("100"));
    }

    private boolean evaluateNumericCondition(
            BigDecimal value,
            Operator operator,
            Map<String, ? extends Number> range) {

        return switch (operator) {
            case BETWEEN -> {
                BigDecimal min = new BigDecimal(range.get("min").toString());
                BigDecimal max = new BigDecimal(range.get("max").toString());
                yield value.compareTo(min) >= 0 && value.compareTo(max) <= 0;
            }
            case GREATER_THAN -> {
                BigDecimal threshold = new BigDecimal(range.get("value").toString());
                yield value.compareTo(threshold) > 0;
            }
            case LESS_THAN -> {
                BigDecimal threshold = new BigDecimal(range.get("value").toString());
                yield value.compareTo(threshold) < 0;
            }
            case EQUALS -> {
                BigDecimal target = new BigDecimal(range.get("value").toString());
                yield value.compareTo(target) == 0;
            }
            default -> false;
        };
    }
}
