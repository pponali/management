package com.scaler.price.validation.services;

import com.scaler.price.rule.domain.Product;
import com.scaler.price.rule.dto.ProductDTO;
import com.scaler.price.rule.exceptions.ProductValidationException;
import com.scaler.price.rule.exceptions.RuleValidationException;
import java.util.Map;
import java.util.Set;

public interface ProductValidationService {
    void validateProduct(Product product) throws ProductValidationException, RuleValidationException;
    void validateBasicFields(Product product) throws ProductValidationException;
    void validatePrices(Product product) throws ProductValidationException;
    void validateSiteIds(Set<Long> siteIds) throws ProductValidationException;
    void validateSeller(Long sellerId) throws ProductValidationException;
    void validateCategory(Long categoryId) throws ProductValidationException;
    void validateAttributes(Map<String, Object> attributes) throws ProductValidationException;
    void validateProductUpdate(ProductDTO productDTO, Product existingProduct) throws ProductValidationException, RuleValidationException;
}