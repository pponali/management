package com.scaler.price.core.management.controller;

import com.scaler.price.core.management.dto.PriceDTO;
import com.scaler.price.core.management.exceptions.PriceNotFoundException;
import com.scaler.price.core.management.exceptions.PriceValidationException;
import com.scaler.price.core.management.service.PriceService;
import com.scaler.price.core.management.service.PriceValidationService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/prices")
@Validated
public class PriceController {
    
    private final PriceService priceService;
    private final PriceValidationService validationService;

    @Autowired
    public PriceController(PriceService priceService, PriceValidationService validationService) {
        this.priceService = priceService;
        this.validationService = validationService;
    }

    @PostMapping
    public ResponseEntity<PriceDTO> createPrice(@Valid @RequestBody PriceDTO priceDTO) throws PriceValidationException {
        validationService.validatePrice(priceDTO);
        return ResponseEntity.ok(priceService.createPrice(priceDTO));
    }

    @PutMapping("/{id}")
    public ResponseEntity<PriceDTO> updatePrice(
            @PathVariable Long id,
            @Valid @RequestBody PriceDTO priceDTO) throws PriceValidationException {
        validationService.validatePriceUpdate(id, priceDTO);
        return ResponseEntity.ok(priceService.updatePrice(id, priceDTO));
    }

    @GetMapping("/{id}")
    public ResponseEntity<PriceDTO> getPrice(@PathVariable Long id) {
        return ResponseEntity.ok(priceService.getPrice(id));
    }

    @GetMapping("/product/{productId}")
    public ResponseEntity<List<PriceDTO>> getPricesByProduct(@PathVariable Long productId) {
        return ResponseEntity.ok(priceService.getPricesByProduct(productId));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletePrice(@PathVariable Long id) {
        priceService.deletePrice(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/validate/{id}")
    public ResponseEntity<List<String>> validatePrice(@PathVariable Long id) throws PriceValidationException {
        PriceDTO price = priceService.getPrice(id);
        try {
            validationService.validatePrice(price);
            return ResponseEntity.ok(List.of("Price validation successful"));
        } catch (Exception e) {
            return ResponseEntity.ok(List.of(e.getMessage()));
        }
    }

    @PostMapping("/validate")
    public ResponseEntity<List<String>> validateNewPrice(@Valid @RequestBody PriceDTO priceDTO) throws PriceValidationException {
        try {
            validationService.validatePrice(priceDTO);
            return ResponseEntity.ok(List.of("Price validation successful"));
        } catch (Exception e) {
            return ResponseEntity.ok(List.of(e.getMessage()));
        }
    }

    @GetMapping("/site/{siteId}/seller/{sellerId}/product/{productId}")
    public ResponseEntity<PriceDTO> getPriceBySiteAndSeller(
            @PathVariable Long productId,
            @PathVariable Long sellerId,
            @PathVariable Long siteId) {

        try {
            PriceDTO price = priceService.getActivePrice(productId, sellerId, siteId);
            return ResponseEntity.ok(price);
        } catch (PriceNotFoundException ex) {
            throw ex; // Will be handled by GlobalExceptionHandler
        }
    }

    @GetMapping("/site/{siteId}/product/{productId}")
    public ResponseEntity<PriceDTO> getPriceBySite(
            @PathVariable Long productId,
            @PathVariable Long siteId) {

        try {
            PriceDTO price = priceService.getWinningSellerPrice(productId, siteId);
            return ResponseEntity.ok(price);
        } catch (PriceNotFoundException ex) {
            throw ex; // Will be handled by GlobalExceptionHandler
        }
    }
}