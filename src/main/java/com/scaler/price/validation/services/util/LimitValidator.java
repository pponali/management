package com.scaler.price.validation.services.util;

import com.scaler.price.rule.config.ConfigurationService;
import com.scaler.price.rule.domain.PricingRule;
import com.scaler.price.rule.domain.SellerLimits;
import com.scaler.price.rule.domain.SiteLimits;
import com.scaler.price.rule.exceptions.RuleValidationException;
import com.scaler.price.rule.repository.RuleRepository;
import com.scaler.price.rule.service.SellerService;
import com.scaler.price.rule.service.SiteService;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class LimitValidator {
    private final ConfigurationService configService;
    private final RuleRepository ruleRepository;
    private final SellerService sellerService;
    private final SiteService siteService;

    public void validate(PricingRule rule) {
        validateAgainstSystemLimits(rule);

        for (String sellerId : rule.getSellerIds()) {
            validateAgainstSellerLimits(rule, sellerId);
        }

        for (String siteId : rule.getSiteIds()) {
            validateAgainstSiteLimits(rule, siteId);
        }
    }

    private void validateAgainstSystemLimits(PricingRule rule) throws RuleValidationException {
        int maxRulesPerSeller = configService.getMaxRulesPerSeller();
        int maxRulesPerSite = configService.getMaxRulesPerSite();

        for (String sellerId : rule.getSellerIds()) {
            long sellerRuleCount = ruleRepository.countBySellerIdsContaining(sellerId);
            if (sellerRuleCount >= maxRulesPerSeller) {
                throw new RuleValidationException(
                        "Seller " + sellerId + " has reached maximum allowed rules"
                );
            }
        }

        for (String siteId : rule.getSiteIds()) {
            long siteRuleCount = ruleRepository.countBySiteIdsContaining(siteId);
            if (siteRuleCount >= maxRulesPerSite) {
                throw new RuleValidationException(
                        "Site " + siteId + " has reached maximum allowed rules"
                );
            }
        }
    }

    private void validateAgainstSellerLimits(PricingRule rule, String sellerId) throws RuleValidationException {
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

    private void validateAgainstSiteLimits(PricingRule rule, String siteId) throws RuleValidationException {
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
}
