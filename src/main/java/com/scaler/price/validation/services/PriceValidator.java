package com.scaler.price.validation.services;

import com.scaler.price.core.management.utils.PriceServiceMetrics;
import com.scaler.price.rule.config.ConfigurationService;
import com.scaler.price.rule.domain.PricingRule;
import com.scaler.price.rule.domain.RuleAction;
import com.scaler.price.rule.exceptions.RuleValidationException;
import com.scaler.price.validation.helper.MarginActionParameters;
import com.scaler.price.validation.helper.PriceActionParameters;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

// PriceValidator.java
@Component
@Slf4j
@RequiredArgsConstructor
public class PriceValidator {
    private final ConfigurationService configService;
    private final PriceServiceMetrics metricsService;

    private static final BigDecimal MIN_PRICE = new BigDecimal("0.01");
    private static final BigDecimal MAX_PRICE_CHANGE_PERCENTAGE = new BigDecimal("50.0");
    private static final BigDecimal MIN_MARGIN = new BigDecimal("0.0");
    private static final BigDecimal MAX_MARGIN = new BigDecimal("100.0");

    public void validatePrices(PricingRule rule) throws RuleValidationException {
        if (rule.getMinimumPrice() != null && rule.getMaximumPrice() != null) {
            validatePriceRange(
                    rule.getMinimumPrice(),
                    rule.getMaximumPrice()
            );
        }
    }

    public void validateMargins(PricingRule rule) throws RuleValidationException {
        if (rule.getMinimumMargin() != null && rule.getMaximumMargin() != null) {
            validateMarginRange(
                    rule.getMinimumMargin(),
                    rule.getMaximumMargin()
            );
        }
    }

    public void validateSetPriceAction(RuleAction action) throws RuleValidationException {
        PriceActionParameters params = parsePriceParameters(action);

        validatePriceValue(params.getPrice());
        validatePriceChange(
                params.getCurrentPrice(),
                params.getPrice()
        );
        validatePriceConstraints(params);
    }

    private PriceActionParameters parsePriceParameters(RuleAction action) {
        BigDecimal price = new BigDecimal(String.valueOf(action.getParameters().get("price")));
        BigDecimal currentPrice = new BigDecimal(String.valueOf(action.getParameters().get("currentPrice")));

        return new PriceActionParameters(
                price,
                currentPrice
        );
    }

    public void validateMarginAction(RuleAction action) throws RuleValidationException {
        MarginActionParameters params = parseMarginParameters(action);

        validateMarginValue(params.getMargin());
        validateMarginConstraints(params);
    }

    private void validateMarginConstraints(MarginActionParameters params) throws RuleValidationException {
        BigDecimal actualMargin = calculateMargin(
                params.getCurrentPrice(),
                params.getCostPrice()
        );

        if (actualMargin.compareTo(params.getMargin()) < 0) {
            throw new RuleValidationException(
                    "Margin cannot be less than " + params.getMargin() + "%"
            );
        }
    }

    private void validateMarginValue(BigDecimal margin) {
    }

    private MarginActionParameters parseMarginParameters(RuleAction action) {
        BigDecimal margin = new BigDecimal(String.valueOf(action.getParameters().get("margin")));
        BigDecimal costPrice = new BigDecimal(String.valueOf(action.getParameters().get("costPrice")));
        BigDecimal currentPrice = new BigDecimal(String.valueOf(action.getParameters().get("currentPrice")));

        return new MarginActionParameters(
                margin,
                costPrice,
                currentPrice
        );

    }

    private void validatePriceRange(BigDecimal minPrice, BigDecimal maxPrice) throws RuleValidationException {
        if (minPrice.compareTo(maxPrice) > 0) {
            throw new RuleValidationException(
                    "Minimum price cannot be greater than maximum price"
            );
        }

        if (minPrice.compareTo(MIN_PRICE) < 0) {
            throw new RuleValidationException("Minimum price cannot be less than " + MIN_PRICE);
        }

        validatePriceValue(minPrice);
        validatePriceValue(maxPrice);
    }

    private void validateMarginRange(BigDecimal minMargin, BigDecimal maxMargin) throws RuleValidationException {
        if (minMargin.compareTo(maxMargin) > 0) {
            throw new RuleValidationException(
                    "Minimum margin cannot be greater than maximum margin"
            );
        }

        if (minMargin.compareTo(MIN_MARGIN) < 0 ||
                maxMargin.compareTo(MAX_MARGIN) > 0) {
            throw new RuleValidationException(
                    "Margins must be between " + MIN_MARGIN + " and " + MAX_MARGIN + " percent"
            );
        }
    }

    private void validatePriceValue(BigDecimal price) throws RuleValidationException {
        if (price == null) {
            throw new RuleValidationException("Price cannot be null");
        }

        if (price.compareTo(MIN_PRICE) < 0) {
            throw new RuleValidationException("Price cannot be less than " + MIN_PRICE);
        }

        if (price.scale() > 2) {
            throw new RuleValidationException("Price cannot have more than 2 decimal places");
        }
    }

    private void validatePriceChange(BigDecimal oldPrice, BigDecimal newPrice) throws RuleValidationException {
        if (oldPrice != null) {
            BigDecimal changePercentage = calculatePriceChangePercentage(oldPrice, newPrice);

            if (changePercentage.abs().compareTo(MAX_PRICE_CHANGE_PERCENTAGE) > 0) {
                throw new RuleValidationException(
                        "Price change exceeds maximum allowed percentage of " +
                                MAX_PRICE_CHANGE_PERCENTAGE + "%"
                );
            }
        }
    }

    private void validatePriceConstraints(PriceActionParameters params) throws RuleValidationException {
        // Validate minimum margin
        if (params.getMinMarginPercentage() != null &&
                params.getCostPrice() != null) {

            BigDecimal actualMargin = calculateMargin(
                    params.getPrice(),
                    params.getCostPrice()
            );

            if (actualMargin.compareTo(params.getMinMarginPercentage()) < 0) {
                throw new RuleValidationException(
                        "Price violates minimum margin requirement of " +
                                params.getMinMarginPercentage() + "%"
                );
            }
        }
    }

    private BigDecimal calculatePriceChangePercentage(
            BigDecimal oldPrice,
            BigDecimal newPrice) {
        return newPrice.subtract(oldPrice)
                .divide(oldPrice, 4, RoundingMode.HALF_UP)
                .multiply(new BigDecimal("100"));
    }

    private BigDecimal calculateMargin(BigDecimal price, BigDecimal costPrice) {
        return price.subtract(costPrice)
                .divide(price, 4, RoundingMode.HALF_UP)
                .multiply(new BigDecimal("100"));
    }
}

