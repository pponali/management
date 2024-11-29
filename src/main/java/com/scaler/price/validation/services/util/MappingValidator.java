package com.scaler.price.validation.services.util;

import com.scaler.price.rule.domain.PricingRule;
import com.scaler.price.rule.exceptions.RuleValidationException;
import com.scaler.price.rule.service.BrandService;
import com.scaler.price.rule.service.CategoryService;
import com.scaler.price.rule.service.SellerService;
import com.scaler.price.rule.service.SiteService;
import lombok.RequiredArgsConstructor;

import java.util.Set;

@RequiredArgsConstructor
public class MappingValidator {
    private final SellerService sellerService;
    private final SiteService siteService;
    private final CategoryService categoryService;
    private final BrandService brandService;

    public void validate(PricingRule rule) throws RuleValidationException {
        validateSellerMappings(rule.getSellerIds());
        validateSiteMappings(rule.getSiteIds());
        validateCategoryMappings(rule.getCategoryIds());
        validateBrandMappings(rule.getBrandIds());
    }

    private void validateSellerMappings(Set<Long> sellerIds) throws RuleValidationException {
        if (sellerIds == null || sellerIds.isEmpty()) {
            throw new RuleValidationException("At least one seller mapping is required");
        }

        for (Long sellerId : sellerIds) {
            if (!sellerService.isValidSeller(sellerId)) {
                throw new RuleValidationException("Invalid seller ID: " + sellerId);
            }

            if (!sellerService.isSellerActive(sellerId)) {
                throw new RuleValidationException("Seller is not active: " + sellerId);
            }
        }
    }

    private void validateSiteMappings(Set<Long> siteIds) throws RuleValidationException {
        if (siteIds == null || siteIds.isEmpty()) {
            throw new RuleValidationException("At least one site mapping is required");
        }

        for (Long siteId : siteIds) {
            if (!siteService.isValidSite(siteId)) {
                throw new RuleValidationException("Invalid site ID: " + siteId);
            }

            if (!siteService.isSiteActive(siteId)) {
                throw new RuleValidationException("Site is not active: " + siteId);
            }
        }
    }

    private void validateCategoryMappings(Set<Long> categoryIds)throws RuleValidationException {
        if (categoryIds != null && !categoryIds.isEmpty()) {
            for (Long categoryId : categoryIds) {
                if (!categoryService.isValidCategory(categoryId)) {
                    throw new RuleValidationException("Invalid category ID: " + categoryId);
                }
            }
        }
    }

    private void validateBrandMappings(Set<String> brandIds) throws RuleValidationException {
        if (brandIds != null && !brandIds.isEmpty()) {
            for (String brandId : brandIds) {
                if (!brandService.isValidBrand(brandId)) {
                    throw new RuleValidationException("Invalid brand ID: " + brandId);
                }
            }
        }
    }
}