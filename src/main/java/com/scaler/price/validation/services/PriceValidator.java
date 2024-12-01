package com.scaler.price.validation.services;

import com.scaler.price.core.management.service.ConfigurationService;
import com.scaler.price.core.management.utils.PriceServiceMetrics;
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

    private void validatePriceRange(BigDecimal minPrice, BigDecimal maxPrice) throws RuleValidationException {
        if (minPrice.compareTo(MIN_PRICE) < 0) {
            throw new RuleValidationException("Minimum price cannot be less than " + MIN_PRICE);
        }

        if (minPrice.compareTo(maxPrice) >= 0) {
            throw new RuleValidationException("Minimum price must be less than maximum price");
        }
    }

    private void validateMarginRange(BigDecimal minMargin, BigDecimal maxMargin) throws RuleValidationException {
        if (minMargin.compareTo(MIN_MARGIN) < 0 || maxMargin.compareTo(MAX_MARGIN) > 0) {
            throw new RuleValidationException(
                    String.format("Margin must be between %s%% and %s%%", MIN_MARGIN, MAX_MARGIN)
            );
        }

        if (minMargin.compareTo(maxMargin) >= 0) {
            throw new RuleValidationException("Minimum margin must be less than maximum margin");
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

    private void validatePriceValue(BigDecimal price) throws RuleValidationException {
        if (price.compareTo(MIN_PRICE) < 0) {
            throw new RuleValidationException("Price cannot be less than " + MIN_PRICE);
        }
    }

    private void validatePriceChange(BigDecimal currentPrice, BigDecimal newPrice) throws RuleValidationException {
        BigDecimal priceChangePercentage = calculatePriceChangePercentage(currentPrice, newPrice);

        if (priceChangePercentage.abs().compareTo(MAX_PRICE_CHANGE_PERCENTAGE) > 0) {
            throw new RuleValidationException(
                    "Price change percentage cannot exceed " + MAX_PRICE_CHANGE_PERCENTAGE + "%"
            );
        }
    }

    private void validatePriceConstraints(PriceActionParameters params) throws RuleValidationException {
        BigDecimal minPrice = configService.getBigDecimalValue("price.min", "default", MIN_PRICE);
        BigDecimal maxPriceChange = configService.getBigDecimalValue(
                "price.max.change.percentage",
                "default",
                MAX_PRICE_CHANGE_PERCENTAGE
        );

        if (params.getPrice().compareTo(minPrice) < 0) {
            throw new RuleValidationException("Price cannot be less than " + minPrice);
        }

        BigDecimal priceChangePercentage = calculatePriceChangePercentage(
                params.getCurrentPrice(),
                params.getPrice()
        );

        if (priceChangePercentage.abs().compareTo(maxPriceChange) > 0) {
            throw new RuleValidationException(
                    "Price change percentage cannot exceed " + maxPriceChange + "%"
            );
        }
    }

    private PriceActionParameters parsePriceParameters(RuleAction action) {
        BigDecimal price = new BigDecimal(String.valueOf(action.getParameters().get("price")));
        BigDecimal currentPrice = new BigDecimal(String.valueOf(action.getParameters().get("currentPrice")));

        return new PriceActionParameters(price, currentPrice);
    }

    public void validateMarginAction(RuleAction action) throws RuleValidationException {
        MarginActionParameters params = parseMarginParameters(action);

        validateMarginValue(params.getMargin());
        validateMarginConstraints(params);
    }

    private void validateMarginValue(BigDecimal margin) throws RuleValidationException {
        if (margin.compareTo(MIN_MARGIN) < 0 || margin.compareTo(MAX_MARGIN) > 0) {
            throw new RuleValidationException(
                    String.format("Margin must be between %s%% and %s%%", MIN_MARGIN, MAX_MARGIN)
            );
        }
    }

    private void validateMarginConstraints(MarginActionParameters params) throws RuleValidationException {
        BigDecimal minMargin = configService.getBigDecimalValue("margin.min", "default", MIN_MARGIN);
        BigDecimal maxMargin = configService.getBigDecimalValue("margin.max", "default", MAX_MARGIN);

        BigDecimal actualMargin = calculateMargin(params.getCurrentPrice(), params.getCostPrice());

        if (actualMargin.compareTo(minMargin) < 0) {
            throw new RuleValidationException("Margin cannot be less than " + minMargin + "%");
        }

        if (actualMargin.compareTo(maxMargin) > 0) {
            throw new RuleValidationException("Margin cannot exceed " + maxMargin + "%");
        }
    }

    private MarginActionParameters parseMarginParameters(RuleAction action) {
        BigDecimal margin = new BigDecimal(String.valueOf(action.getParameters().get("margin")));
        BigDecimal costPrice = new BigDecimal(String.valueOf(action.getParameters().get("costPrice")));
        BigDecimal currentPrice = new BigDecimal(String.valueOf(action.getParameters().get("currentPrice")));

        return new MarginActionParameters(margin, costPrice, currentPrice);
    }

    private BigDecimal calculatePriceChangePercentage(BigDecimal oldPrice, BigDecimal newPrice) {
        return newPrice.subtract(oldPrice)
                .divide(oldPrice, 4, RoundingMode.HALF_UP)
                .multiply(new BigDecimal("100"));
    }

    private BigDecimal calculateMargin(BigDecimal sellingPrice, BigDecimal costPrice) {
        return sellingPrice.subtract(costPrice)
                .divide(sellingPrice, 4, RoundingMode.HALF_UP)
                .multiply(new BigDecimal("100"));
    }
}
