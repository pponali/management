// com.scaler.price.core.management.service.impl.PriceServiceImpl.java
package com.scaler.price.core.management.service.impl;

import com.scaler.price.core.management.domain.Price;
import com.scaler.price.core.management.dto.PriceDTO;
import com.scaler.price.core.management.exceptions.PriceNotFoundException;
import com.scaler.price.core.management.exceptions.PriceValidationException;
import com.scaler.price.core.management.mappers.PriceMapper;
import com.scaler.price.core.management.repository.PriceRepository;
import com.scaler.price.core.management.service.PriceService;
import com.scaler.price.core.management.service.PriceValidationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PriceServiceImpl implements PriceService {
    private final PriceRepository priceRepository;
    private final PriceValidationService validationService;
    private final PriceMapper priceMapper;

    @Override
    @Transactional
    public PriceDTO createPrice(PriceDTO priceDTO) throws PriceValidationException {
        validationService.validatePrice(priceDTO);
        Price price = priceMapper.toEntity(priceDTO);
        Price savedPrice = priceRepository.save(price);
        return priceMapper.toDTO(savedPrice);
    }

    @Override
    @Transactional
    public PriceDTO updatePrice(Long id, PriceDTO priceDTO) throws PriceValidationException {
        Price existingPrice = priceRepository.findById(id)
                .orElseThrow(() -> new PriceNotFoundException("Price not found with id: " + id));

        validationService.validatePrice(priceDTO);
        Price updatedPrice = priceMapper.updateEntity(existingPrice, priceDTO);
        Price savedPrice = priceRepository.save(updatedPrice);
        return priceMapper.toDTO(savedPrice);
    }

    @Override
    public PriceDTO getPrice(Long id) {
        Price price = priceRepository.findById(id)
                .orElseThrow(() -> new PriceNotFoundException("Price not found with id: " + id));
        return priceMapper.toDTO(price);
    }

    @Override
    public List<PriceDTO> getPricesByProduct(Long productId) {
        List<Price> prices = priceRepository.findByProductId(productId);
        return prices.stream()
                .map(priceMapper::toDTO)
                .toList();
    }

    @Override
    public void deletePrice(Long id) {
        if (!priceRepository.existsById(id)) {
            throw new PriceNotFoundException("Price not found with id: " + id);
        }
        priceRepository.deleteById(id);
    }

    @Override
    public void validatePrice(PriceDTO price) throws PriceValidationException {
        validationService.validatePrice(price);
    }
}