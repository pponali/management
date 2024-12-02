package com.scaler.price.validation.services.impl;

import com.scaler.price.rule.domain.Product;
import com.scaler.price.rule.dto.ProductDTO;
import com.scaler.price.rule.exceptions.ProductValidationException;
import com.scaler.price.rule.exceptions.RuleValidationException;
import com.scaler.price.rule.mapper.ProductMapper;
import com.scaler.price.rule.service.CategoryService;
import com.scaler.price.rule.service.SellerService;
import com.scaler.price.rule.service.SiteService;
import com.scaler.price.validation.services.ProductValidationService;
import io.micrometer.common.util.StringUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Set;

@Component
@Slf4j
@RequiredArgsConstructor
public class ProductValidatorImpl implements ProductValidationService {
    private final CategoryService categoryService;
    private final SellerService sellerService;
    private final SiteService siteService;
    private final ProductMapper productMapper;

    private static final BigDecimal MIN_PRICE = new BigDecimal("0.01");
    private static final int MAX_PRODUCT_NAME_LENGTH = 255;
    private static final String PRODUCT_ID_PATTERN = "^[A-Za-z0-9_-]{2,50}$";

    @Override
    public void validateProduct(Product product) throws ProductValidationException, RuleValidationException {
        validateBasicFields(product);
        validatePrices(product);
        validateSiteIds(product.getSiteIds());
        validateSeller(product.getSellerId());
        validateCategory(product.getCategoryId());
        validateAttributes(product.getCustomAttributes());
    }

    @Override
    public void validateBasicFields(Product product) throws ProductValidationException {
        if (product == null) {
            throw new ProductValidationException("Product cannot be null");
        }
        if (StringUtils.isBlank(product.getName())) {
            throw new ProductValidationException("Product name is required");
        }
        if (product.getName().length() > MAX_PRODUCT_NAME_LENGTH) {
            throw new ProductValidationException("Product name exceeds maximum length");
        }
    }

    @Override
    public void validatePrices(Product product) throws ProductValidationException {
        if (product.getBasePrice() == null || ((BigDecimal)product.getBasePrice()).compareTo(MIN_PRICE) < 0) {
            throw new ProductValidationException("Invalid base price");
        }
    }

    @Override
    public void validateSiteIds(Set<Long> siteIds) throws ProductValidationException {
        if (siteIds == null || siteIds.isEmpty()) {
            throw new ProductValidationException("At least one site ID is required");
        }
        for (Long siteId : siteIds) {
            if (!siteService.existsById(siteId)) {
                throw new ProductValidationException("Invalid site ID: " + siteId);
            }
        }
    }

    @Override
    public void validateSeller(Long sellerId) throws ProductValidationException {
        if (sellerId == null) {
            throw new ProductValidationException("Seller ID is required");
        }
        if (!sellerService.existsById(sellerId)) {
            throw new ProductValidationException("Invalid seller ID: " + sellerId);
        }
    }

    @Override
    public void validateCategory(Long categoryId) throws ProductValidationException {
        if (categoryId == null) {
            throw new ProductValidationException("Category ID is required");
        }
        if (!categoryService.existsById(categoryId)) {
            throw new ProductValidationException("Invalid category ID: " + categoryId);
        }
    }

    @Override
    public void validateAttributes(Map<String, Object> attributes) throws ProductValidationException {
        if (attributes == null) {
            return;
        }
        // Add any specific attribute validation logic here
    }

    @Override
    public void validateProductUpdate(ProductDTO productDTO, Product existingProduct) throws ProductValidationException, RuleValidationException {
        // Validate the updated product details
        if (productDTO == null) {
            throw new ProductValidationException("Product update details cannot be null");
        }
        
        if (existingProduct == null) {
            throw new ProductValidationException("Existing product cannot be null");
        }
        
        // Convert ProductDTO to Product for validation
        Product updatedProduct = productMapper.toEntity(productDTO);
        
        // Reuse existing validation methods
        validateProduct(updatedProduct);
        
        // Add any specific update validation logic here
        // For example, check if critical fields are being modified
        if (!existingProduct.getSellerId().equals(updatedProduct.getSellerId())) {
            throw new ProductValidationException("Seller ID cannot be modified");
        }
    }
}
