package com.scaler.price.rule.actions.handler;

import com.scaler.price.rule.domain.RuleAction;
import com.scaler.price.validation.helper.ActionParameters;
import com.scaler.price.rule.dto.RuleEvaluationContext;
import com.scaler.price.rule.dto.RuleEvaluationResult;
import com.scaler.price.rule.exceptions.ActionExecutionException;
import com.scaler.price.rule.exceptions.ActionValidationException;
import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;
import java.util.Map;

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

    private BigDecimal getSeasonalFactor(ActionParameters params, String season, String productId) {
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

    private ActionParameters parseParameters(Map parameters) {
        return new ActionParameters(parameters);
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