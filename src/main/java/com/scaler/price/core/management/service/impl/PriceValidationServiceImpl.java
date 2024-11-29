package com.scaler.price.core.management.service.impl;

import com.scaler.price.core.management.exceptions.PriceValidationException;
import com.scaler.price.core.management.dto.PriceDTO;
import com.scaler.price.core.management.service.PriceValidationService;
import com.scaler.price.rule.domain.PricingRule;
import com.scaler.price.rule.dto.RuleEvaluationContext;

import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
public class PriceValidationServiceImpl implements PriceValidationService {

    @Override
    public void validatePrice(PriceDTO priceDTO) throws PriceValidationException {
        validateBasicFields(priceDTO);
        validatePriceValues(priceDTO);
        validateDates(priceDTO);
    }

    @Override
    public void validatePriceUpdate(Long id, PriceDTO priceDTO) throws PriceValidationException {
        validatePrice(priceDTO);
        // Add additional validation for updates
        if (id == null) {
            throw new PriceValidationException("Price ID cannot be null for update");
        }
    }

    private void validateBasicFields(PriceDTO priceDTO) throws PriceValidationException {
        if (priceDTO.getProductId() == null) {
            throw new PriceValidationException("Product ID is required");
        }

        if (priceDTO.getCurrency() == null || priceDTO.getCurrency().trim().isEmpty()) {
            throw new PriceValidationException("Currency is required");
        }
    }

    private void validatePriceValues(PriceDTO priceDTO) throws PriceValidationException {
        if (priceDTO.getBasePrice() == null || priceDTO.getBasePrice().compareTo(BigDecimal.ZERO) <= 0) {
            throw new PriceValidationException("Base price must be greater than zero");
        }

        if (priceDTO.getSellingPrice() == null || priceDTO.getSellingPrice().compareTo(BigDecimal.ZERO) <= 0) {
            throw new PriceValidationException("Selling price must be greater than zero");
        }

        if (priceDTO.getSellingPrice().compareTo(priceDTO.getBasePrice()) > 0) {
            throw new PriceValidationException("Selling price cannot be greater than base price");
        }
    }

    private void validateDates(PriceDTO priceDTO) throws PriceValidationException {
        if (priceDTO.getEffectiveFrom() == null) {
            throw new PriceValidationException("Start date is required");
        }
    
        if (priceDTO.getEffectiveTo() != null && priceDTO.getEffectiveTo().isBefore(priceDTO.getEffectiveFrom())) {
            throw new PriceValidationException("End date must be after start date");
        }
    }

    @Override
    public BigDecimal validatePriceBounds(BigDecimal adjustedPrice, PricingRule rule, RuleEvaluationContext context) throws PriceValidationException {
        // Implement price bounds validation logic here
        if (adjustedPrice == null) {
            throw new PriceValidationException("Adjusted price cannot be null");
        }

        // Example validation: Check if the price is within the rule's bounds
        if (rule.getMinimumPrice() != null && adjustedPrice.compareTo(rule.getMinimumPrice()) < 0) {
            throw new PriceValidationException("Price is below the minimum allowed price");
        }

        if (rule.getMaximumPrice() != null && adjustedPrice.compareTo(rule.getMaximumPrice()) > 0) {
            throw new PriceValidationException("Price is above the maximum allowed price");
        }

        return adjustedPrice;
    }
}