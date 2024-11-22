package com.scaler.price.core.management.service.impl;

import com.scaler.price.core.management.exceptions.PriceValidationException;
import com.scaler.price.core.management.dto.PriceDTO;
import com.scaler.price.core.management.service.PriceValidationService;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;

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
        if (priceDTO.getProductId() == null || priceDTO.getProductId().trim().isEmpty()) {
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
        LocalDateTime now = LocalDateTime.now();

        if (priceDTO.getEffectiveFrom() == null) {
            throw new PriceValidationException("Effective from date is required");
        }

        if (priceDTO.getEffectiveFrom().isBefore(now)) {
            throw new PriceValidationException("Effective from date cannot be in the past");
        }

        if (priceDTO.getEffectiveTo() != null &&
                priceDTO.getEffectiveTo().isBefore(priceDTO.getEffectiveFrom())) {
            throw new PriceValidationException("Effective to date must be after effective from date");
        }
    }
}