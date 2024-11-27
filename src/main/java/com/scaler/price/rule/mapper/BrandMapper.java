package com.scaler.price.rule.mapper;

import com.scaler.price.rule.domain.Brand;
import com.scaler.price.rule.domain.constraint.CategoryConstraints;
import com.scaler.price.rule.dto.BrandDTO;
import com.scaler.price.rule.repository.CategoryRepository;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
@Slf4j
public class BrandMapper {

    private final CategoryRepository categoryRepository;

    public BrandDTO toDTO(Brand brand) {
        if (brand == null) {
            return null;
        }

        return BrandDTO.builder()
                .id(brand.getId())
                .name(brand.getName())
                .description(brand.getDescription())
                .logoUrl(brand.getLogoUrl())
                .active(brand.isActive())
                .contactEmail(brand.getContactEmail())
                .contactPhone(brand.getContactPhone())
                .websiteUrl(brand.getWebsiteUrl())
                .categoryIds(mapToStringIds(brand.getCategories().stream()
                        .map(CategoryConstraints::getCategoryId)
                        .collect(Collectors.toSet())))
                .createdAt(brand.getCreatedAt())
                .updatedAt(brand.getUpdatedAt())
                .build();
    }

    public Brand toEntity(BrandDTO dto) {
        if (dto == null) {
            return null;
        }

        Brand brand = Brand.builder()
                .id(dto.getId())
                .name(dto.getName())
                .description(dto.getDescription())
                .logoUrl(dto.getLogoUrl())
                .active(dto.isActive())
                .contactEmail(dto.getContactEmail())
                .contactPhone(dto.getContactPhone())
                .websiteUrl(dto.getWebsiteUrl())
                .build();

        if (dto.getCategoryIds() != null && !dto.getCategoryIds().isEmpty()) {
            brand.setCategories(mapToCategories(dto.getCategoryIds()));
        } else {
            brand.setCategories(new HashSet<>());
        }

        return brand;
    }

    private Set<CategoryConstraints> mapToCategories(Set<String> categoryIds) {
            if (categoryIds == null || categoryIds.isEmpty()) {
                return Collections.emptySet();
            }
        
            return categoryIds.stream()
            .map(this::convertToCategoryConstraint)
            .collect(Collectors.toSet());
    }
    


    // Add this overloaded method to handle Set<String>
    private Set<String> mapToStringIds(Set<String> ids) {
        if (ids == null || ids.isEmpty()) {
            return Collections.emptySet();
        }
        return ids;
    }

    private CategoryConstraints convertToCategoryConstraint(Object input) {
        if (input instanceof String) {
            String categoryId = (String) input;
            return categoryRepository.findById(categoryId)
                    .orElseThrow(() -> new EntityNotFoundException("Category not found with ID: " + categoryId));
        } else if (input instanceof CategoryConstraints) {
            return (CategoryConstraints) input;
        } else {
            throw new IllegalArgumentException("Input must be either a category ID or a CategoryConstraints object");
        }
    }

    public void updateEntityFromDTO(BrandDTO dto, Brand brand) {
        if (brand == null || dto == null) {
            throw new IllegalArgumentException("Both brand and dto must not be null");
        }

        brand.setName(dto.getName());
        brand.setDescription(dto.getDescription());
        brand.setLogoUrl(dto.getLogoUrl());
        brand.setActive(dto.isActive());
        brand.setContactEmail(dto.getContactEmail());
        brand.setContactPhone(dto.getContactPhone());
        brand.setWebsiteUrl(dto.getWebsiteUrl());

        if (dto.getCategoryIds() != null) {
            Set<CategoryConstraints> categories = dto.getCategoryIds().stream()
                    .map((String categoryId) -> convertToCategoryConstraint(categoryId))
                    .collect(Collectors.toSet());
            brand.setCategories(categories);
        } else {
            brand.setCategories(new HashSet<>());
        }
    }
}
