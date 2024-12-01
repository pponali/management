package com.scaler.price.validation.services.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.scaler.price.core.management.service.ConfigurationService;
import com.scaler.price.rule.domain.*;
import com.scaler.price.rule.exceptions.RuleValidationException;
import com.scaler.price.rule.repository.RuleRepository;
import com.scaler.price.rule.service.SellerService;
import com.scaler.price.rule.service.SiteService;
import lombok.RequiredArgsConstructor;

import java.math.BigDecimal;
import java.math.RoundingMode;

@RequiredArgsConstructor
public class LimitValidator {
    private final ConfigurationService configService;
    private final RuleRepository ruleRepository;
    private final SellerService sellerService;
    private final SiteService siteService;
    private final ObjectMapper objectMapper;

    public void validate(PricingRule rule) throws RuleValidationException, JsonProcessingException, IllegalArgumentException {
        validateAgainstSystemLimits(rule);

        for (Long sellerId : rule.getSellerIds()) {
            validateAgainstSellerLimits(rule, sellerId);
        }

        for (Long siteId : rule.getSiteIds()) {
            validateAgainstSiteLimits(rule, siteId);
        }
    }

    private void validateAgainstSystemLimits(PricingRule rule) throws RuleValidationException {
        int maxRulesPerSeller = configService.getMaxRulesPerSeller();
        int maxRulesPerSite = configService.getMaxRulesPerSite();

        for (Long sellerId : rule.getSellerIds()) {
            long sellerRuleCount = ruleRepository.countBySellerIdsContaining(sellerId);
            if (sellerRuleCount >= maxRulesPerSeller) {
                throw new RuleValidationException(
                        "Seller " + sellerId + " has reached maximum allowed rules"
                );
            }
        }

        for (Long siteId : rule.getSiteIds()) {
            long siteRuleCount = ruleRepository.countBySiteIdsContaining(siteId);
            if (siteRuleCount >= maxRulesPerSite) {
                throw new RuleValidationException(
                        "Site " + siteId + " has reached maximum allowed rules"
                );
            }
        }
    }

    private void validateAgainstSellerLimits(PricingRule rule, Long sellerId) throws RuleValidationException, JsonProcessingException, IllegalArgumentException {
        SellerLimits limits = sellerService.getSellerLimits(sellerId);

        if (limits.getMaxRules() > 0) {
            long currentRuleCount = ruleRepository.countBySellerIdsContaining(sellerId);
            if (currentRuleCount >= limits.getMaxRules()) {
                throw new RuleValidationException(
                        "Seller has reached maximum allowed rules: " + limits.getMaxRules()
                );
            }
        }

        if (limits.getMaxDiscount() != null) {
            validateDiscountAction(rule, limits.getMaxDiscount());
        }
    }

    private void validateAgainstSiteLimits(PricingRule rule, Long siteId) throws RuleValidationException, JsonProcessingException, IllegalArgumentException {
        SiteLimits limits = siteService.getSiteLimits(siteId);

        if (limits.getMaxRules() > 0) {
            long currentRuleCount = ruleRepository.countBySiteIdsContaining(siteId);
            if (currentRuleCount >= limits.getMaxRules()) {
                throw new RuleValidationException(
                        "Site has reached maximum allowed rules: " + limits.getMaxRules()
                );
            }
        }

        if (limits.getMaxDiscount() != null) {
            validateDiscountAgainstLimit(rule, limits.getMaxDiscount());
        }
    }

    private void validateDiscountAgainstLimit(PricingRule rule, BigDecimal maxDiscount) throws RuleValidationException, JsonProcessingException, IllegalArgumentException {
        for (RuleAction action : rule.getActions()) {
            if (action.getActionType() == ActionType.APPLY_DISCOUNT) {
                DiscountAction discountAction = objectMapper.treeToValue(action.getParameters(), DiscountAction.class);
                BigDecimal discountValue = discountAction.getDiscountValue();
                
                if (discountAction.getDiscountType() == DiscountType.PERCENTAGE) {
                    if (discountValue.compareTo(maxDiscount) > 0) {
                        throw new RuleValidationException("Discount percentage " + discountValue + 
                            " exceeds maximum allowed discount of " + maxDiscount + "%");
                    }
                } else if (discountAction.getDiscountType() == DiscountType.FIXED_AMOUNT) {
                    // For fixed amounts, convert to percentage based on current price
                    if (discountAction.getCurrentPrice() != null && discountAction.getCurrentPrice().compareTo(BigDecimal.ZERO) > 0) {
                        BigDecimal percentageDiscount = discountValue
                            .divide(discountAction.getCurrentPrice(), 2, RoundingMode.HALF_UP)
                            .multiply(BigDecimal.valueOf(100));
                        if (percentageDiscount.compareTo(maxDiscount) > 0) {
                            throw new RuleValidationException("Fixed discount amount " + discountValue +
                                " exceeds maximum allowed discount of " + maxDiscount + "%");
                        }
                    }
                }
            }
        }
    }

    private void validateDiscountAction(PricingRule rule, BigDecimal maxDiscount) throws RuleValidationException, JsonProcessingException, IllegalArgumentException {
        // Reuse the same validation logic since both methods serve the same purpose
        validateDiscountAgainstLimit(rule, maxDiscount);
    }
}
