

package com.scaler.price.validation.services.impl;

import com.scaler.price.core.management.service.ConfigurationService;
import com.scaler.price.core.management.utils.PriceServiceMetrics;
import com.scaler.price.rule.domain.PricingRule;
import com.scaler.price.rule.domain.RuleAction;
import com.scaler.price.rule.dto.action.PriceActionParameters;
import com.scaler.price.rule.exceptions.RuleValidationException;
import lombok.Generated;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Component
public class PriceValidator {
    @Generated
    private static final Logger log = LoggerFactory.getLogger(PriceValidator.class);
    private final ConfigurationService configService;
    private final PriceServiceMetrics metricsService;
    private static final BigDecimal MIN_PRICE = new BigDecimal("0.01");
    private static final BigDecimal MAX_PRICE_CHANGE_PERCENTAGE = new BigDecimal("50.0");
    private static final BigDecimal MIN_MARGIN = new BigDecimal("0.0");
    private static final BigDecimal MAX_MARGIN = new BigDecimal("100.0");

    public void validatePrices(PricingRule rule) throws RuleValidationException {
        if (rule.getMinimumPrice() != null && rule.getMaximumPrice() != null) {
            this.validatePriceRange(rule.getMinimumPrice(), rule.getMaximumPrice());
        }

    }

    public void validateMargins(PricingRule rule) throws RuleValidationException {
        if (rule.getMinimumMargin() != null && rule.getMaximumMargin() != null) {
            this.validateMarginRange(rule.getMinimumMargin(), rule.getMaximumMargin());
        }

    }

    public void validateSetPriceAction(RuleAction var1) {
        throw new Error("Unresolved compilation problem: \n\tThe method parsePriceParameters(RuleAction) is undefined for the type PriceValidator\n");
    }

    public void validateMarginAction(RuleAction var1) {
        throw new Error("Unresolved compilation problems: \n\tMarginActionParameters cannot be resolved to a type\n\tThe method parseMarginParameters(RuleAction) is undefined for the type PriceValidator\n");
    }

    private void validatePriceRange(BigDecimal minPrice, BigDecimal maxPrice) throws RuleValidationException {
        if (minPrice.compareTo(maxPrice) > 0) {
            throw new RuleValidationException("Minimum price cannot be greater than maximum price");
        } else if (minPrice.compareTo(MIN_PRICE) < 0) {
            throw new RuleValidationException("Minimum price cannot be less than " + String.valueOf(MIN_PRICE));
        } else {
            this.validatePriceValue(minPrice);
            this.validatePriceValue(maxPrice);
        }
    }

    private void validateMarginRange(BigDecimal minMargin, BigDecimal maxMargin) throws RuleValidationException {
        if (minMargin.compareTo(maxMargin) > 0) {
            throw new RuleValidationException("Minimum margin cannot be greater than maximum margin");
        } else if (minMargin.compareTo(MIN_MARGIN) < 0 || maxMargin.compareTo(MAX_MARGIN) > 0) {
            String var10002 = String.valueOf(MIN_MARGIN);
            throw new RuleValidationException("Margins must be between " + var10002 + " and " + String.valueOf(MAX_MARGIN) + " percent");
        }
    }

    private void validatePriceValue(BigDecimal price) throws RuleValidationException {
        if (price == null) {
            throw new RuleValidationException("Price cannot be null");
        } else if (price.compareTo(MIN_PRICE) < 0) {
            throw new RuleValidationException("Price cannot be less than " + String.valueOf(MIN_PRICE));
        } else if (price.scale() > 2) {
            throw new RuleValidationException("Price cannot have more than 2 decimal places");
        }
    }

    private void validatePriceChange(BigDecimal oldPrice, BigDecimal newPrice) throws RuleValidationException {
        if (oldPrice != null) {
            BigDecimal changePercentage = this.calculatePriceChangePercentage(oldPrice, newPrice);
            if (changePercentage.abs().compareTo(MAX_PRICE_CHANGE_PERCENTAGE) > 0) {
                throw new RuleValidationException("Price change exceeds maximum allowed percentage of " + String.valueOf(MAX_PRICE_CHANGE_PERCENTAGE) + "%");
            }
        }

    }

    private void validatePriceConstraints(PriceActionParameters var1) {
        throw new Error("Unresolved compilation problems: \n\tThe method getMinMarginPercentage() is undefined for the type PriceActionParameters\n\tThe method getCostPrice() is undefined for the type PriceActionParameters\n\tThe method getCostPrice() is undefined for the type PriceActionParameters\n\tThe method getMinMarginPercentage() is undefined for the type PriceActionParameters\n\tThe method getMinMarginPercentage() is undefined for the type PriceActionParameters\n");
    }

    private BigDecimal calculatePriceChangePercentage(BigDecimal oldPrice, BigDecimal newPrice) {
        return newPrice.subtract(oldPrice).divide(oldPrice, 4, RoundingMode.HALF_UP).multiply(new BigDecimal("100"));
    }

    private BigDecimal calculateMargin(BigDecimal price, BigDecimal costPrice) {
        return price.subtract(costPrice).divide(price, 4, RoundingMode.HALF_UP).multiply(new BigDecimal("100"));
    }

    @Generated
    public PriceValidator(ConfigurationService configService, PriceServiceMetrics metricsService) {
        this.configService = configService;
        this.metricsService = metricsService;
    }
}
