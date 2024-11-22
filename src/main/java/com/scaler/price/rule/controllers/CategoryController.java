package com.scaler.price.rule.controllers;

import com.scaler.price.core.management.service.ConstraintService;
import com.scaler.price.rule.dto.CategoryAttributes;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/categories")
@RequiredArgsConstructor
@Tag(name = "Category Management", description = "APIs for managing category attributes and constraints")
public class CategoryController {

    private final ConstraintService constraintService;

    @Operation(summary = "Get category constraints and attributes")
    @GetMapping("/{categoryId}/constraints")
    public ResponseEntity<CategoryAttributes> getCategoryConstraints(
            @PathVariable Long categoryId) {
        return ResponseEntity.ok(constraintService.getCategoryConstraints(categoryId));
    }
}