package com.scaler.price.rule.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import com.scaler.price.rule.domain.SellerLimits;
import com.scaler.price.rule.repository.SellerRepository;

@Service
@Slf4j
@RequiredArgsConstructor
public class SellerService {
    private final SellerRepository sellerRepository;

    public boolean isValidSeller(Long sellerId) {
        return sellerRepository.existsById(sellerId);
    }

    public boolean isSellerActive(Long sellerId) {
        return sellerRepository.findActiveStatusById(sellerId);
    }

    public SellerLimits getSellerLimits(Long sellerId) {
        return sellerRepository.findLimitsById(sellerId)
                .orElse(SellerLimits.getDefaultLimits());
    }

    public boolean existsById(Long sellerId) {
        return sellerRepository.existsById(sellerId);
    }
}
