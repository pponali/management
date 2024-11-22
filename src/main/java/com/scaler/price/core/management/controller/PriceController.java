
package com.scaler.price.core.management.controller;

import com.scaler.price.core.management.dto.PriceDTO;
import com.scaler.price.core.management.exceptions.PriceValidationException;
import com.scaler.price.core.management.service.PriceService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;


import java.util.List;

@RestController
@RequestMapping("/api/v1/prices")
@RequiredArgsConstructor
public class PriceController {
    private final PriceService priceService;

    @PostMapping
    public ResponseEntity<PriceDTO> createPrice(@Valid @RequestBody PriceDTO priceDTO) {
        try {
            return ResponseEntity.ok(priceService.createPrice(priceDTO));
        } catch (PriceValidationException e) {
            throw new RuntimeException(e);
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<PriceDTO> updatePrice(
            @PathVariable Long id,
            @Valid @RequestBody PriceDTO priceDTO) {
        try {
            return ResponseEntity.ok(priceService.updatePrice(id, priceDTO));
        } catch (PriceValidationException e) {
            throw new RuntimeException(e);
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<PriceDTO> getPrice(@PathVariable Long id) {
        return ResponseEntity.ok(priceService.getPrice(id));
    }

    @GetMapping("/product/{productId}")
    public ResponseEntity<List<PriceDTO>> getPricesByProduct(@PathVariable String productId) {
        return ResponseEntity.ok(priceService.getPricesByProduct(productId));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletePrice(@PathVariable Long id) {
        priceService.deletePrice(id);
        return ResponseEntity.noContent().build();
    }

}
