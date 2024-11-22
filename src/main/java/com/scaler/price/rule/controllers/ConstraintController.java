package com.scaler.price.rule.controllers;

import com.scaler.price.core.management.service.ConstraintService;
import com.scaler.price.rule.domain.constraint.MarginConstraints;
import com.scaler.price.rule.domain.constraint.PriceConstraints;
import com.scaler.price.rule.domain.constraint.TimeConstraints;
import com.scaler.price.rule.dto.CategoryAttributes;
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
}
