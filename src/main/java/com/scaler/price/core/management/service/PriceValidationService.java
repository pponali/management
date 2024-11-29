package com.scaler.price.core.management.service;

import com.scaler.price.rule.domain.PricingRule;
import com.scaler.price.rule.dto.RuleEvaluationContext;
import com.scaler.price.core.management.dto.PriceDTO;
import com.scaler.price.core.management.exceptions.PriceValidationException;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
public interface PriceValidationService {
    void validatePrice(PriceDTO priceDTO) throws PriceValidationException;
    void validatePriceUpdate(Long id, PriceDTO priceDTO) throws PriceValidationException;

    BigDecimal validatePriceBounds(BigDecimal adjustedPrice, PricingRule rule, RuleEvaluationContext context) throws PriceValidationException;
}