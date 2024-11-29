package com.scaler.price.validation.services;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.scaler.price.core.management.utils.PriceServiceMetrics;
import com.scaler.price.core.management.service.ConfigurationService;

import com.scaler.price.rule.domain.ConditionType;
import com.scaler.price.rule.domain.Operator;
import com.scaler.price.rule.domain.RuleCondition;
import com.scaler.price.rule.dto.condition.CompetitorPriceValue;
import com.scaler.price.rule.dto.condition.PriceRangeValue;
import com.scaler.price.rule.exceptions.RuleValidationException;
import com.scaler.price.rule.service.CompetitorService;
import io.micrometer.common.util.StringUtils;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Set;

@Component
@Slf4j
@RequiredArgsConstructor
public class ConditionValidator {
    private final TimeValidator timeValidator;
    private final CompetitorService competitorService;
    private final ConfigurationService configService;
    private final ObjectMapper objectMapper;
    private final PriceServiceMetrics metricsService;

    @SneakyThrows
    public void validateConditions(Set<RuleCondition> conditions) throws RuleValidationException {
        long startTime = System.currentTimeMillis();
        int totalConditions = conditions != null ? conditions.size() : 0;
        int validatedConditions = 0;
        
        try {
            validateBasicConditionRules(conditions);
            for (RuleCondition condition : conditions) {
                validateCondition(condition);
                validatedConditions++;
            }
            metricsService.recordRuleEvaluation(
                "conditions",
                totalConditions,
                validatedConditions,
                System.currentTimeMillis() - startTime
            );
        } catch (RuleValidationException e) {
            // Get the rule ID from the first condition if available
            Long ruleId = conditions != null && !conditions.isEmpty() ? 
                conditions.iterator().next().getRule().getId() : null;
            if (ruleId != null) {
                metricsService.recordRuleEvaluationError(ruleId);
            }
            throw e;
        } finally {
            metricsService.recordProcessingTime(System.currentTimeMillis() - startTime);
        }
    }

    private void validateBasicConditionRules(Set<RuleCondition> conditions) throws RuleValidationException {
        if (conditions == null || conditions.isEmpty()) {
            throw new RuleValidationException("At least one condition is required");
        }

        if (conditions.size() > configService.getMaxConditionsPerRule()) {
            throw new RuleValidationException(
                    "Number of conditions exceeds maximum allowed: " +
                            configService.getMaxConditionsPerRule()
            );
        }
    }

    private void validateCondition(RuleCondition condition) throws RuleValidationException {
        validateConditionBasics(condition);
        validateConditionValue(condition);
        validateOperatorCompatibility(condition);
        validateAttributeCompatibility(condition);
    }

    private void validateAttributeCompatibility(RuleCondition condition) {
    }

    private void validateConditionBasics(RuleCondition condition) throws RuleValidationException {
        if (condition.getType() == null) {
            throw new RuleValidationException("Condition type is required");
        }

        if (condition.getAttribute() != null) {
            throw new RuleValidationException("Condition attribute is required");
        }

        if (condition.getOperator() == null) {
            throw new RuleValidationException("Condition operator is required");
        }
    }

    private void validateConditionValue(RuleCondition condition) throws RuleValidationException {
        long startTime = System.currentTimeMillis();
        try {
            switch (condition.getType()) {
                case PRICE_RANGE -> validatePriceRangeCondition(condition);
                case MARGIN_RANGE -> validateSimpleRangeCondition(condition, "margin", BigDecimal.ZERO, new BigDecimal("100"));
                case INVENTORY_LEVEL -> validateNumericCondition(condition, "inventory", 0L);
                case TIME_BASED -> timeValidator.validateTimeCondition(condition);
                case COMPETITOR_PRICE -> validateCompetitorPriceCondition(condition);
                case SALES_VELOCITY -> validateNumericRangeCondition(condition, "velocity", 0.0);
                case CATEGORY_ATTRIBUTE -> validateStringCondition(condition);
                case CUSTOM -> validateCustomCondition(condition);
                default -> throw new RuleValidationException(
                        "Unsupported condition type: " + condition.getType()
                );
            }
            metricsService.recordPriceOperation("validate_" + condition.getType().name().toLowerCase());
        } catch (Exception e) {
            metricsService.recordRuleEvaluationError(condition.getId());
            throw e;
        } finally {
            metricsService.recordProcessingTime(System.currentTimeMillis() - startTime);
        }
    }

    private void validateCustomCondition(RuleCondition condition) {
    }

    private void validatePriceRangeCondition(RuleCondition condition) throws RuleValidationException {
        PriceRangeValue value = parseConditionValue(condition, PriceRangeValue.class);

        if (value.getMinPrice() != null && value.getMaxPrice() != null) {
            if (value.getMinPrice().compareTo(value.getMaxPrice()) > 0) {
                throw new RuleValidationException("Min price cannot be greater than max price");
            }
        }

        if (value.getMinPrice() != null && value.getMinPrice().compareTo(BigDecimal.ZERO) < 0) {
            throw new RuleValidationException("Min price cannot be negative");
        }
    }

    private void validateCompetitorPriceCondition(RuleCondition condition) throws RuleValidationException {
        CompetitorPriceValue value = parseConditionValue(condition, CompetitorPriceValue.class);

        if (value.getCompetitorId() == null) {
            throw new RuleValidationException("Competitor ID is required");
        }

        if (!competitorService.isValidCompetitor(value.getCompetitorId())) {
            throw new RuleValidationException(
                    "Invalid competitor ID: " + value.getCompetitorId()
            );
        }

        validatePriceThresholds(value);
    }

    private void validatePriceThresholds(CompetitorPriceValue value) {
    }

    private void validateOperatorCompatibility(RuleCondition condition) throws RuleValidationException {
        Set<Operator> validOperators = getValidOperatorsForType(condition.getType());
        if (!validOperators.contains(condition.getOperator())) {
            throw new RuleValidationException(
                    "Operator " + condition.getOperator() +
                            " is not valid for condition type " + condition.getType()
            );
        }
    }

    private Set<Operator> getValidOperatorsForType(ConditionType type) {
        return null;
    }

    private void validateSimpleRangeCondition(RuleCondition condition, String type,
                                              BigDecimal min, BigDecimal max) throws RuleValidationException {
        try {
            BigDecimal value = new BigDecimal(condition.getValue());
            if (value.compareTo(min) < 0 || value.compareTo(max) > 0) {
                throw new RuleValidationException(
                        String.format("%s must be between %s and %s", type, min, max)
                );
            }
        } catch (NumberFormatException e) {
            throw new RuleValidationException("Invalid " + type + " value format");
        }
    }

    private void validateNumericCondition(RuleCondition condition, String type,
                                          Number minValue) throws RuleValidationException {
        try {
            Number value = Double.parseDouble(condition.getValue());
            if (value.doubleValue() < minValue.doubleValue()) {
                throw new RuleValidationException(
                        type + " cannot be less than " + minValue
                );
            }
        } catch (NumberFormatException e) {
            throw new RuleValidationException("Invalid " + type + " value format");
        }
    }

    private void validateNumericRangeCondition(RuleCondition condition, String type,
                                               Double minValue) throws RuleValidationException {
        try {
            String[] range = condition.getValue().split(",");
            if (range.length != 2) {
                throw new RuleValidationException("Invalid range format");
            }
            Double min = Double.parseDouble(range[0]);
            Double max = Double.parseDouble(range[1]);

            if (min < minValue || max < minValue) {
                throw new RuleValidationException(
                        type + " values cannot be less than " + minValue
                );
            }
            if (min > max) {
                throw new RuleValidationException(
                        "Minimum " + type + " cannot be greater than maximum"
                );
            }
        } catch (NumberFormatException e) {
            throw new RuleValidationException("Invalid " + type + " value format");
        }
    }

    private void validateStringCondition(RuleCondition condition) throws RuleValidationException {
        if (condition.getValue() == null || condition.getValue().trim().isEmpty()) {
            throw new RuleValidationException("Value cannot be empty");
        }
    }

    private <T> T parseConditionValue(RuleCondition condition, Class<T> valueType) throws RuleValidationException {
        try {
            return objectMapper.readValue(condition.getValue(), valueType);
        } catch (JsonProcessingException e) {
            throw new RuleValidationException(
                    "Invalid condition value format: " + e.getMessage()
            );
        }
    }
}
