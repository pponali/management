package com.scaler.price.core.management.service;

import com.scaler.price.core.management.dto.PriceDTO;
import com.scaler.price.core.management.exceptions.PriceValidationException;

import java.util.List;

public interface PriceService {
    PriceDTO createPrice(PriceDTO priceDTO) throws PriceValidationException;
    PriceDTO updatePrice(Long id, PriceDTO priceDTO) throws PriceValidationException;
    PriceDTO getPrice(Long id);
    List<PriceDTO> getPricesByProduct(Long productId);
    void deletePrice(Long id);

    void validatePrice(PriceDTO price) throws PriceValidationException;
}