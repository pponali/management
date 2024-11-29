package com.scaler.price.rule.actions.handler;

import com.fasterxml.jackson.databind.JsonNode;
import com.scaler.price.rule.domain.RuleAction;
import com.scaler.price.validation.helper.ActionParameters;
import com.scaler.price.rule.dto.RuleEvaluationContext;
import com.scaler.price.rule.dto.RuleEvaluationResult;
import com.scaler.price.rule.exceptions.ActionExecutionException;
import com.scaler.price.rule.exceptions.ActionValidationException;
import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;
import java.util.*;

@Slf4j
public class SeasonalPricingHandler implements CustomActionHandler {

    @Override
    public RuleEvaluationResult execute(
            RuleAction action,
            RuleEvaluationContext context,
            RuleEvaluationResult currentResult) throws ActionExecutionException {

        try {
            ActionParameters params = parseParameters(action.getParameters());
            validate(params);

            String season = determineCurrentSeason();
            BigDecimal seasonalFactor = getSeasonalFactor(
                    params,
                    season,
                    context.getProductId()
            );

            BigDecimal adjustedPrice = applySeasonalAdjustment(
                    currentResult.getAdjustedPrice(),
                    seasonalFactor
            );

            currentResult.setAdjustedPrice(adjustedPrice);
            currentResult.getMetadata().put("appliedSeason", season);
            currentResult.getMetadata().put("seasonalFactor", seasonalFactor);

            return currentResult;

        } catch (Exception e) {
            log.error("Error executing seasonal pricing: {}", e.getMessage());
            throw new ActionExecutionException("Seasonal pricing execution failed", e);
        }
    }

    private BigDecimal applySeasonalAdjustment(BigDecimal adjustedPrice, BigDecimal seasonalFactor) {
        return adjustedPrice.multiply(seasonalFactor);
    }

    private BigDecimal getSeasonalFactor(ActionParameters params, String season, Long productId) {
        Map<String, BigDecimal> seasonalFactors = params.getSeasonalFactors();
        BigDecimal defaultFactor = new BigDecimal("1.0");

        if (seasonalFactors.containsKey(season)) {
            return seasonalFactors.get(season);
        } else {
            log.warn("No seasonal factor found for season: {}", season);
            return defaultFactor;
        }
    }

    private String determineCurrentSeason() {
        // Determine current season based on current date
        return "winter";
    }

    private ActionParameters parseParameters(JsonNode parameters) {
        // Convert JsonNode to Map<String, Object>
        Map<String, Object> parametersMap = new HashMap<>();
        Iterator<Map.Entry<String, JsonNode>> fields = parameters.fields();
        while (fields.hasNext()) {
            Map.Entry<String, JsonNode> entry = fields.next();
            parametersMap.put(entry.getKey(), convertJsonNodeToObject(entry.getValue()));
        }
        return new ActionParameters(parametersMap);
    }

    // Helper method to convert JsonNode to appropriate Java object
    private Object convertJsonNodeToObject(JsonNode node) {
        if (node.isNull()) {
            return null;
        } else if (node.isTextual()) {
            return node.asText();
        } else if (node.isNumber()) {
            if (node.isInt()) {
                return node.asInt();
            } else if (node.isLong()) {
                return node.asLong();
            } else {
                return new BigDecimal(node.asText());
            }
        } else if (node.isBoolean()) {
            return node.asBoolean();
        } else if (node.isArray()) {
            List<Object> list = new ArrayList<>();
            for (JsonNode element : node) {
                list.add(convertJsonNodeToObject(element));
            }
            return list;
        } else if (node.isObject()) {
            Map<String, Object> map = new HashMap<>();
            Iterator<Map.Entry<String, JsonNode>> fields = node.fields();
            while (fields.hasNext()) {
                Map.Entry<String, JsonNode> entry = fields.next();
                map.put(entry.getKey(), convertJsonNodeToObject(entry.getValue()));
            }
            return map;
        }
        return node.toString(); // Fallback
    }

    @Override
    public void validate(ActionParameters parameters) throws ActionValidationException {
        // Validate seasonal factors
        Map<String, BigDecimal> seasonalFactors = parameters.getSeasonalFactors();
        if (seasonalFactors == null || seasonalFactors.isEmpty()) {
            throw new ActionValidationException("Seasonal factors are required");
        }

        // Validate factor ranges
        seasonalFactors.forEach((season, factor) -> {
            if (factor.compareTo(BigDecimal.ZERO) <= 0 ||
                    factor.compareTo(new BigDecimal("5")) > 0) {
                try {
                    throw new ActionValidationException(
                            "Seasonal factor must be between 0 and 5"
                    );
                } catch (ActionValidationException e) {
                    throw new RuntimeException(e);
                }
            }
        });
    }
}