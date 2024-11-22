package com.scaler.price.rule.mapper;


import com.scaler.price.rule.domain.Brand;
import com.scaler.price.rule.domain.constraint.CategoryConstraints;
import com.scaler.price.rule.dto.BrandDTO;
import com.scaler.price.rule.repository.CategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class BrandMapper {

    private final CategoryRepository categoryRepository;

    public BrandDTO toDTO(Brand brand) {
        BrandDTO dto = new BrandDTO();
        dto.setId(brand.getId());
        dto.setName(brand.getName());
        dto.setDescription(brand.getDescription());
        dto.setLogoUrl(brand.getLogoUrl());
        dto.setActive(brand.isActive());
        dto.setContactEmail(brand.getContactEmail());
        dto.setContactPhone(brand.getContactPhone());
        dto.setWebsiteUrl(brand.getWebsiteUrl());
        dto.setCategoryIds(brand.getCategories().stream()
                .map(CategoryConstraints::getId)
                .collect(Collectors.toSet()));
        return dto;
    }

    public Brand toEntity(BrandDTO dto) {
        return Brand.builder()
                .name(dto.getName())
                .description(dto.getDescription())
                .logoUrl(dto.getLogoUrl())
                .active(dto.isActive())
                .contactEmail(dto.getContactEmail())
                .contactPhone(dto.getContactPhone())
                .websiteUrl(dto.getWebsiteUrl())
                .categories(dto.getCategoryIds() != null ?
                        categoryRepository.findAllById(Collections.unmodifiableSet(dto.getCategoryIds()))
                                .stream()
                                .collect(Collectors.toSet()) :
                        null)
                .build();
    }

    public void updateEntityFromDTO(BrandDTO dto, Brand brand) {
        brand.setName(dto.getName());
        brand.setDescription(dto.getDescription());
        brand.setLogoUrl(dto.getLogoUrl());
        brand.setActive(dto.isActive());
        brand.setContactEmail(dto.getContactEmail());
        brand.setContactPhone(dto.getContactPhone());
        brand.setWebsiteUrl(dto.getWebsiteUrl());
        if (dto.getCategoryIds() != null) {
            brand.setCategories(categoryRepository.findAllById(dto.getCategoryIds())
                    .stream()
                    .collect(Collectors.toSet()));
        }
    }
}
