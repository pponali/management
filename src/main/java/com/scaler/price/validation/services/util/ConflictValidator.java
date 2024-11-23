package com.scaler.price.validation.services.util;

import com.scaler.price.rule.domain.PricingRule;
import com.scaler.price.rule.exceptions.RuleValidationException;
import com.scaler.price.rule.repository.RuleRepository;
import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.stream.Collectors;

@RequiredArgsConstructor
public class ConflictValidator {
    private final RuleRepository ruleRepository;

    public void validate(PricingRule rule) throws RuleValidationException{
        List<PricingRule> conflictingRules = findConflictingRules(rule);

        if (!conflictingRules.isEmpty()) {
            throw new RuleValidationException(
                    "Rule conflicts with existing rules: " +
                            conflictingRules.stream()
                                    .map(PricingRule::getRuleName)
                                    .collect(Collectors.joining(", "))
            );
        }
    }

    private List<PricingRule> findConflictingRules(PricingRule rule) {
        return ruleRepository.findConflictingRules(
                rule.getSellerIds(),
                rule.getSiteIds(),
                rule.getEffectiveFrom(),
                rule.getEffectiveTo()
        );
    }
}