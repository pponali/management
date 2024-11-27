package com.scaler.price.rule.controllers;


import com.scaler.price.core.management.service.ConstraintService;
import com.scaler.price.rule.domain.constraint.MarginConstraints;
import com.scaler.price.rule.domain.constraint.RuleConstraints;
import com.scaler.price.rule.dto.CategoryAttributes;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/margins")
@RequiredArgsConstructor
@Tag(name = "Margin Management", description = "APIs for managing margin constraints")
public class MarginController {

    private final ConstraintService constraintService;

    @Operation(summary = "Set margin constraints for a category")
    @PostMapping("/constraints")
    public ResponseEntity<MarginConstraints> setMarginConstraints(
            @Valid @RequestBody MarginConstraints constraints) {
        return ResponseEntity.ok(constraintService.setMarginConstraints(constraints));
    }

   @GetMapping("/constraints/{categoryId}")
    public ResponseEntity<RuleConstraints> getMarginConstraints(
            @PathVariable Long categoryId) {
        CategoryAttributes categoryAttributes = constraintService.getCategoryConstraints(categoryId);
        RuleConstraints marginConstraints = categoryAttributes.getMarginConstraints();
        return ResponseEntity.ok(marginConstraints);
    }
}