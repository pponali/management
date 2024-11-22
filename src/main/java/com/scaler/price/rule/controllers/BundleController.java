package com.scaler.price.rule.controllers;

import com.scaler.price.rule.domain.Bundle;
import com.scaler.price.rule.service.BundleEligibility;
import com.scaler.price.rule.service.BundleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/bundles")
@RequiredArgsConstructor
@Tag(name = "Bundle Management", description = "APIs for managing product bundles and discounts")
public class BundleController {

    private final BundleService bundleService;

    @Operation(summary = "Create a new bundle")
    @PostMapping
    public ResponseEntity<Bundle> createBundle(@Valid @RequestBody Bundle bundle) {
        return ResponseEntity.ok(bundleService.createBundle(bundle));
    }

    @Operation(summary = "Get bundle by ID")
    @GetMapping("/{id}")
    public ResponseEntity<Bundle> getBundle(@PathVariable Long id) {
        return bundleService.getBundleById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @Operation(summary = "Get all bundles with pagination")
    @GetMapping
    public ResponseEntity<Page<Bundle>> getAllBundles(Pageable pageable) {
        return ResponseEntity.ok(bundleService.getAllBundles(pageable));
    }

    @Operation(summary = "Update an existing bundle")
    @PutMapping("/{id}")
    public ResponseEntity<Bundle> updateBundle(
            @PathVariable Long id,
            @Valid @RequestBody Bundle bundle) {
        return ResponseEntity.ok(bundleService.updateBundle(id, bundle));
    }

    @Operation(summary = "Delete a bundle")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteBundle(@PathVariable Long id) {
        bundleService.deleteBundle(id);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Check bundle eligibility")
    @GetMapping("/{bundleId}/eligibility")
    public ResponseEntity<BundleEligibility> checkEligibility(
            @PathVariable String bundleId,
            @RequestParam String productId,
            @RequestParam(required = false) Map<String, Object> attributes) {
        return ResponseEntity.ok(bundleService.checkEligibility(bundleId, productId, attributes));
    }

    @Operation(summary = "Calculate bundle discount")
    @GetMapping("/{bundleId}/discount")
    public ResponseEntity<BigDecimal> getBundleDiscount(
            @PathVariable String bundleId,
            @RequestParam String productId) {
        return ResponseEntity.ok(bundleService.getBundleDiscount(bundleId, productId));
    }

    @Operation(summary = "Get active bundles for a product")
    @GetMapping("/product/{productId}")
    public ResponseEntity<List<Bundle>> getBundlesForProduct(
            @PathVariable String productId) {
        return ResponseEntity.ok(bundleService.getBundlesForProduct(productId));
    }
}