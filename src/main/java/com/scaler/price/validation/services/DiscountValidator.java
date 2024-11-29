package com.scaler.price.validation.services;

import com.scaler.price.core.management.utils.PriceServiceMetrics;
import com.scaler.price.core.management.service.ConfigurationService;

import com.scaler.price.rule.domain.DiscountAction;
import com.scaler.price.rule.exceptions.RuleValidationException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Component
@Slf4j
@RequiredArgsConstructor
public class DiscountValidator {
    private final TimeValidator timeValidator;
    private final ConfigurationService configService;
    private final PriceServiceMetrics metricsService;

    private static final BigDecimal MAX_DISCOUNT_PERCENTAGE = new BigDecimal("70.0");
    private static final BigDecimal MIN_MARGIN_AFTER_DISCOUNT = new BigDecimal("5.0");
    public static final int MAX_STACK_COUNT = 3;

    public void validateDiscountAction(DiscountAction action) throws RuleValidationException {
        try {
            validateBasicDiscountRules(action);
            if (action.isStackable()) {
                validateStackableDiscount(action);
            } else {
                timeValidator.validateNonStackableDiscount(action);
            }
            validateDiscountTiming(action);
            validateDiscountBounds(action);
            validateMarginAfterDiscount(action);

            metricsService.recordDiscountValidation();
        } catch (Exception e) {
            metricsService.recordDiscountValidationFailure();
            throw e;
        }
    }

    private void validateBasicDiscountRules(DiscountAction action) throws RuleValidationException {
        if (action.getDiscountType() == null) {
            throw new RuleValidationException("Discount type is required");
        }

        if (action.getDiscountValue() == null) {
            throw new RuleValidationException("Discount value is required");
        }

        timeValidator.validateDiscountValue(action.getDiscountValue(), action.getDiscountType());
    }

    private void validateStackableDiscount(DiscountAction action) throws RuleValidationException {
        if (action.getMaxStackCount() == 0 || action.getMaxStackCount() < 1) {
            throw new RuleValidationException(
                    "Stackable discount must specify valid max stack count"
            );
        }

        if (action.getMaxStackCount() > MAX_STACK_COUNT) {
            throw new RuleValidationException(
                    "Max stack count cannot exceed " + MAX_STACK_COUNT
            );
        }

        if (action.getStackInterval() == null) {
            throw new RuleValidationException("Stack interval is required");
        }

        timeValidator.validateStackInterval(action.getStackInterval());
    }

    private void validateDiscountTiming(DiscountAction action) throws RuleValidationException {
        if (action.getStartDate() != null && action.getEndDate() != null) {
            if (action.getStartDate().isAfter(action.getEndDate())) {
                throw new RuleValidationException(
                        "Discount start date must be before end date"
                );
            }
        }

        if (action.getTimeRestrictions() != null) {
            timeValidator.validateTimeRestrictions(action.getTimeRestrictions());
        }
    }

    private void validateDiscountBounds(DiscountAction action) throws RuleValidationException {
        BigDecimal maxDiscount = calculateMaxDiscount(action);
        if (maxDiscount.compareTo(MAX_DISCOUNT_PERCENTAGE) > 0) {
            throw new RuleValidationException(
                    "Maximum discount cannot exceed " + MAX_DISCOUNT_PERCENTAGE + "%"
            );
        }
    }

    private void validateMarginAfterDiscount(DiscountAction action) throws RuleValidationException {
        if (action.getCostPrice() != null) {
            BigDecimal marginAfterDiscount = calculateMarginAfterDiscount(
                    action.getCurrentPrice(),
                    action.getCostPrice(),
                    action.getDiscountValue(),
                    action.getDiscountType().name()
            );

            if (marginAfterDiscount.compareTo(MIN_MARGIN_AFTER_DISCOUNT) < 0) {
                throw new RuleValidationException(
                        "Margin after discount cannot be less than " +
                                MIN_MARGIN_AFTER_DISCOUNT + "%"
                );
            }
        }
    }

    private BigDecimal calculateMaxDiscount(DiscountAction action) {
        // If no discount value is provided, return 0
        if (action.getDiscountValue() == null) {
            return BigDecimal.ZERO;
        }

        // Calculate max discount based on discount type
        switch (action.getDiscountType()) {
            case PERCENTAGE:
                return action.getDiscountValue();
            case FIXED_AMOUNT:
                // Calculate percentage discount for fixed amount
                if (action.getCurrentPrice() != null && action.getCurrentPrice().compareTo(BigDecimal.ZERO) > 0) {
                    return action.getDiscountValue()
                            .divide(action.getCurrentPrice(), 2, RoundingMode.HALF_UP)
                            .multiply(BigDecimal.valueOf(100));
                }
                return BigDecimal.ZERO;
            default:
                return BigDecimal.ZERO;
        }
    }

    private BigDecimal calculateMarginAfterDiscount(
            BigDecimal currentPrice,
            BigDecimal costPrice,
            BigDecimal discountValue,
            String discountType
    ) {
        if (currentPrice == null || costPrice == null || discountValue == null) {
            return BigDecimal.ZERO;
        }

        BigDecimal discountedPrice;
        switch (discountType) {
            case "PERCENTAGE":
                discountedPrice = currentPrice.multiply(BigDecimal.ONE.subtract(discountValue.divide(BigDecimal.valueOf(100))));
                break;
            case "FLAT":
                discountedPrice = currentPrice.subtract(discountValue);
                break;
            default:
                throw new IllegalArgumentException("Invalid discount type: " + discountType);
        }

        // Calculate margin percentage
        BigDecimal marginAmount = discountedPrice.subtract(costPrice);
        BigDecimal marginPercentage = marginAmount.divide(discountedPrice, 2, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100));

        return marginPercentage;
    }
}
