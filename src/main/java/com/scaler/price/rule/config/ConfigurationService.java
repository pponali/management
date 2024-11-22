package com.scaler.price.rule.config;

import com.scaler.price.core.management.repository.ConfigurationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
@Slf4j
@RequiredArgsConstructor
public class ConfigurationService {
    private final ConfigurationRepository configRepository;

    public int getMaxRuleNameLength() {
        return configRepository.getIntValue("max.rule.name.length", 100);
    }

    public int getMaxRuleDurationDays() {
        return configRepository.getIntValue("max.rule.duration.days", 365);
    }

    public int getMaxConditionsPerRule() {
        return configRepository.getIntValue("max.conditions.per.rule", 10);
    }

    public int getMaxActionsPerRule() {
        return configRepository.getIntValue("max.actions.per.rule", 5);
    }

    public BigDecimal getMaxDiscountPercentage() {
        return configRepository.getBigDecimalValue(
                "max.discount.percentage",
                new BigDecimal("90")
        );
    }


    public int getBusinessHourStart() {
        return 0;
    }



    public int getBusinessHourEnd() {
        return 0;
    }
}
