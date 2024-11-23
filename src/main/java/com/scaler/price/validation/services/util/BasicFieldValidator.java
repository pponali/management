package com.scaler.price.validation.services.util;

import com.scaler.price.rule.domain.PricingRule;
import com.scaler.price.rule.exceptions.RuleValidationException;
import io.micrometer.common.util.StringUtils;
import org.springframework.stereotype.Component;

@Component
public class BasicFieldValidator {

    public void validate(PricingRule rule) throws RuleValidationException {
        // Validate rule name
        if (StringUtils.isBlank(rule.getRuleName())) {
            throw new RuleValidationException("Rule name cannot be empty", null);
        }

        // Validate rule type
        if (rule.getRuleType() == null) {
            throw new RuleValidationException("Rule type cannot be null", null);
        }

        // Add more basic field validations as needed
        // For example, check if other essential fields are present or valid
    }
}
