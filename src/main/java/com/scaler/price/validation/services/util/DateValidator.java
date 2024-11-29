package com.scaler.price.validation.services.util;

import com.scaler.price.core.management.utils.PriceServiceMetrics;
import com.scaler.price.core.management.service.ConfigurationService;

import com.scaler.price.rule.domain.PricingRule;
import com.scaler.price.rule.exceptions.RuleValidationException;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class DateValidator {
    private final ConfigurationService configService;
    private final PriceServiceMetrics metrics;

    public void validate(PricingRule rule) throws RuleValidationException {
        long startTime = System.currentTimeMillis();
        try {
            if (rule.getEffectiveFrom() == null) {
                throw new RuleValidationException("Effective from date is required");
            }

            LocalDateTime now = LocalDateTime.now();
            if (rule.getEffectiveFrom().isBefore(now.minusMinutes(5))) {
                throw new RuleValidationException("Effective from date cannot be in the past");
            }

            if (rule.getEffectiveTo() != null) {
                if (rule.getEffectiveTo().isBefore(rule.getEffectiveFrom())) {
                    throw new RuleValidationException("Effective to date must be after effective from date");
                }

                long durationInDays = java.time.temporal.ChronoUnit.DAYS.between(
                        rule.getEffectiveFrom(),
                        rule.getEffectiveTo()
                );

                if (durationInDays > configService.getMaxRuleDurationDays()) {
                    throw new RuleValidationException(
                            "Rule duration exceeds maximum allowed days: " +
                                    configService.getMaxRuleDurationDays()
                    );
                }
            }
            metrics.recordPriceOperation("date_validation");
        } catch (RuleValidationException e) {
            metrics.recordEvaluationError(rule.getId().toString());
            throw e;
        } finally {
            metrics.recordProcessingTime(System.currentTimeMillis() - startTime);
        }
    }
}