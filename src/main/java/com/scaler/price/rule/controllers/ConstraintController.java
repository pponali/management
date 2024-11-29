package com.scaler.price.rule.controllers;

import com.scaler.price.core.management.service.ConstraintService;
import com.scaler.price.rule.domain.PricingRule;
import com.scaler.price.rule.domain.constraint.MarginConstraints;
import com.scaler.price.rule.domain.constraint.PriceConstraints;
import com.scaler.price.rule.domain.constraint.TimeConstraints;
import com.scaler.price.rule.dto.CategoryAttributes;
import com.scaler.price.validation.helper.ActionParameters;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/constraints")
public class ConstraintController {

    private final ConstraintService constraintService;

    @Autowired
    public ConstraintController(ConstraintService constraintService) {
        this.constraintService = constraintService;
    }

    @PostMapping("/margin")
    public ResponseEntity<MarginConstraints> setMarginConstraints(@RequestBody MarginConstraints constraints) {
        MarginConstraints savedConstraints = constraintService.setMarginConstraints(constraints);
        return ResponseEntity.ok(savedConstraints);
    }

    @PostMapping("/price")
    public ResponseEntity<PriceConstraints> setPriceConstraints(@RequestBody PriceConstraints constraints) {
        PriceConstraints savedConstraints = constraintService.setPriceConstraints(constraints);
        return ResponseEntity.ok(savedConstraints);
    }

    @PostMapping("/time")
    public ResponseEntity<TimeConstraints> setTimeConstraints(@RequestBody TimeConstraints constraints) {
        TimeConstraints savedConstraints = constraintService.setTimeConstraints(constraints);
        return ResponseEntity.ok(savedConstraints);
    }

    @GetMapping("/category/{categoryId}")
    public ResponseEntity<CategoryAttributes> getCategoryConstraints(@PathVariable Long categoryId) {
        CategoryAttributes attributes = constraintService.getCategoryConstraints(categoryId);
        return ResponseEntity.ok(attributes);
    }

        @PostMapping("/validate/rule")
    public ResponseEntity<List<String>> validateRuleConstraints(@RequestBody PricingRule rule) {
        List<String> violations = constraintService.validateRuleConstraints(rule);
        return ResponseEntity.ok(violations);
    }

    @PostMapping("/validate/price")
    public ResponseEntity<List<String>> validatePriceConstraints(
            @RequestParam BigDecimal price,
            @RequestBody ActionParameters parameters) {
        List<String> violations = constraintService.validatePriceConstraints(price, parameters);
        return ResponseEntity.ok(violations);
    }

    @PostMapping("/validate/margin")
    public ResponseEntity<List<String>> validateMarginConstraints(
            @RequestParam BigDecimal margin,
            @RequestBody ActionParameters parameters) {
        List<String> violations = constraintService.validateMarginConstraints(margin, parameters);
        return ResponseEntity.ok(violations);
    }

    @PostMapping("/validate/discount-stacking")
    public ResponseEntity<List<String>> validateDiscountStacking(
            @RequestBody List<BigDecimal> existingDiscounts,
            @RequestParam BigDecimal newDiscount,
            @RequestBody ActionParameters parameters) {
        List<String> violations = constraintService.validateDiscountStacking(
            existingDiscounts, newDiscount, parameters);
        return ResponseEntity.ok(violations);
    }

    @PostMapping("/validate/time")
    public ResponseEntity<Boolean> isTimeConstraintSatisfied(@RequestBody PricingRule rule) {
        return ResponseEntity.ok(constraintService.isTimeConstraintSatisfied(rule));
    }

    @PostMapping("/validate/inventory")
    public ResponseEntity<List<String>> validateInventoryConstraints(
            @RequestParam String productId,
            @RequestParam Integer quantity,
            @RequestBody ActionParameters parameters) {
        List<String> violations = constraintService.validateInventoryConstraints(
            productId, quantity, parameters);
        return ResponseEntity.ok(violations);
    }

    @PostMapping("/validate/category")
    public ResponseEntity<List<String>> validateCategoryConstraints(
            @RequestParam String categoryId,
            @RequestBody PricingRule rule) {
        List<String> violations = constraintService.validateCategoryConstraints(categoryId, rule);
        return ResponseEntity.ok(violations);
    }

    @PostMapping("/validate/customer-segment")
    public ResponseEntity<List<String>> validateCustomerSegmentConstraints(
            @RequestParam String customerId,
            @RequestBody PricingRule rule) {
        List<String> violations = constraintService.validateCustomerSegmentConstraints(
            customerId, rule);
        return ResponseEntity.ok(violations);
    }

    @PostMapping("/validate/channel")
    public ResponseEntity<List<String>> validateChannelConstraints(
            @RequestParam String channelId,
            @RequestBody PricingRule rule) {
        List<String> violations = constraintService.validateChannelConstraints(channelId, rule);
        return ResponseEntity.ok(violations);
    }

    @PostMapping("/validate/geographical")
    public ResponseEntity<List<String>> validateGeographicalConstraints(
            @RequestBody Map<String, String> location,
            @RequestBody PricingRule rule) {
        List<String> violations = constraintService.validateGeographicalConstraints(location, rule);
        return ResponseEntity.ok(violations);
    }

    @PostMapping("/validate/competitor-price")
    public ResponseEntity<List<String>> validateCompetitorPriceConstraints(
            @RequestParam Long productId,
            @RequestParam BigDecimal price,
            @RequestBody ActionParameters parameters) {
        List<String> violations = constraintService.validateCompetitorPriceConstraints(
            productId, price, parameters);
        return ResponseEntity.ok(violations);
    }

    
}
