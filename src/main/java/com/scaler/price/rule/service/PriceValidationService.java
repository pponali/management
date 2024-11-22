package com.scaler.price.rule.service;

import com.scaler.price.rule.domain.PricingRule;
import com.scaler.price.rule.dto.RuleEvaluationContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Service
@Slf4j
@RequiredArgsConstructor
public class PriceValidationService {

    public BigDecimal validatePriceBounds(
            BigDecimal price,
            PricingRule rule,
            RuleEvaluationContext context) {

        // Check minimum price
        if (rule.getMinimumPrice() != null &&
                price.compareTo(rule.getMinimumPrice()) < 0) {
            log.debug("Adjusting price to minimum bound: {}", rule.getMinimumPrice());
            return rule.getMinimumPrice();
        }

        // Check maximum price
        if (rule.getMaximumPrice() != null &&
                price.compareTo(rule.getMaximumPrice()) > 0) {
            log.debug("Adjusting price to maximum bound: {}", rule.getMaximumPrice());
            return rule.getMaximumPrice();
        }

        // Check minimum margin if cost price is available
        if (context.getCostPrice() != null &&
                rule.getMinimumMargin() != null) {
            BigDecimal currentMargin = calculateMargin(price, context.getCostPrice());
            if (currentMargin.compareTo(rule.getMinimumMargin()) < 0) {
                BigDecimal adjustedPrice = calculatePriceForMargin(
                        context.getCostPrice(),
                        rule.getMinimumMargin()
                );
                log.debug("Adjusting price to maintain minimum margin: {}", adjustedPrice);
                return adjustedPrice;
            }
        }

        return price;
    }

    private BigDecimal calculateMargin(BigDecimal price, BigDecimal costPrice) {
        return price.subtract(costPrice)
                .divide(price, 4, RoundingMode.HALF_UP)
                .multiply(new BigDecimal("100"));
    }

    private BigDecimal calculatePriceForMargin(
            BigDecimal costPrice,
            BigDecimal targetMargin) {

        BigDecimal marginMultiplier = BigDecimal.ONE.subtract(
                targetMargin.divide(new BigDecimal("100"), 4, RoundingMode.HALF_UP)
        );

        return costPrice.divide(marginMultiplier, 2, RoundingMode.HALF_UP);
    }

    public void validateRuleUpdate(PricingRule existingRule, PricingRule updatedRule) {
    }

    public void validateRule(PricingRule rule) {

    }
}