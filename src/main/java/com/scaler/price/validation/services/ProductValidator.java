package com.scaler.price.validation.services;

import com.scaler.price.rule.domain.Product;
import com.scaler.price.rule.dto.ProductDTO;
import com.scaler.price.rule.exceptions.ProductValidationException;
import com.scaler.price.rule.exceptions.RuleValidationException;
import com.scaler.price.rule.mapper.ProductMapper;
import com.scaler.price.rule.service.CategoryService;
import com.scaler.price.rule.service.ProductService;
import com.scaler.price.rule.service.SellerService;
import com.scaler.price.rule.service.SiteService;
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
public class ProductValidator {
    private final CategoryService categoryService;
    private final SellerService sellerService;
    private final SiteService siteService;
    private final ProductService productService;
    private final ProductMapper productMapper;

    private static final BigDecimal MIN_PRICE = new BigDecimal("0.01");
    private static final int MAX_PRODUCT_NAME_LENGTH = 255;
    private static final String PRODUCT_ID_PATTERN = "^[A-Za-z0-9_-]{2,50}$";

    public void validateProduct(Product product) throws ProductValidationException, RuleValidationException {
        validateBasicFields(product);
        validatePrices(product);
        validateSiteIds(product.getSiteIds());
        validateSeller(product.getSellerId());
        validateCategory(product.getCategoryId());
        validateAttributes(product.getCustomAttributes());
    }

    private void validateBasicFields(Product product) throws ProductValidationException {
        if (product == null) {
            throw new ProductValidationException("Product cannot be null");
        }

        if (product.getId() == null) {
            throw new ProductValidationException("Product ID is required");
        }

        if (StringUtils.isBlank(product.getDisplayName())) {
            throw new ProductValidationException("Product name is required");
        }

        if (product.getDisplayName().length() > MAX_PRODUCT_NAME_LENGTH) {
            throw new ProductValidationException(
                    "Product name cannot exceed " + MAX_PRODUCT_NAME_LENGTH + " characters"
            );
        }
    }

    private void validatePrices(Product product) throws ProductValidationException {
        if (product.getMrp() == null) {
            throw new ProductValidationException("MRP is required");
        }

        if (product.getMrp().compareTo(MIN_PRICE) < 0) {
            throw new ProductValidationException("MRP must be greater than " + MIN_PRICE);
        }

        if (product.getCostPrice() != null && product.getCostPrice().compareTo(MIN_PRICE) < 0) {
            throw new ProductValidationException("Cost price must be greater than " + MIN_PRICE);
        }

        if (product.getCostPrice() != null && product.getCostPrice().compareTo(product.getMrp()) > 0) {
            throw new ProductValidationException("Cost price cannot be greater than MRP");
        }

        if (StringUtils.isBlank(product.getCurrency())) {
            throw new ProductValidationException("Currency is required");
        }
    }

    private void validateSiteIds(Set<Long> siteIds) throws ProductValidationException {
        if (siteIds == null || siteIds.isEmpty()) {
            throw new ProductValidationException("At least one site ID is required");
        }

        for (Long siteId : siteIds) {
            if (!siteService.isValidSite(siteId)) {
                throw new ProductValidationException("Invalid site ID: " + siteId);
            }
        }
    }

    private void validateSeller(Long sellerId) throws ProductValidationException {
        if (sellerId == null) {
            throw new ProductValidationException("Seller ID is required");
        }

        if (!sellerService.isValidSeller(sellerId)) {
            throw new ProductValidationException("Invalid seller ID: " + sellerId);
        }
    }

    private void validateAttributes(Map<String, String> attributes) throws ProductValidationException {
        if (attributes != null && !attributes.isEmpty()) {
            for (Map.Entry<String, String> entry : attributes.entrySet()) {
                if (StringUtils.isBlank(entry.getKey())) {
                    throw new ProductValidationException("Attribute key cannot be blank");
                }
                if (StringUtils.isBlank(entry.getValue())) {
                    throw new ProductValidationException("Attribute value cannot be blank for key: " + entry.getKey());
                }
            }
        }
    }

    public void validateProductUpdate(Object proposedProduct, Product existingProduct) throws ProductValidationException, RuleValidationException {
        Product updatedProduct = (proposedProduct instanceof ProductDTO) 
            ? productMapper.toEntity((ProductDTO) proposedProduct) 
            : (Product) proposedProduct;
        
        validateProduct(updatedProduct);
    
        // Additional update-specific validations
        if (!existingProduct.getId().equals(updatedProduct.getId())) {
            throw new ProductValidationException("Product ID cannot be changed");
        }
    
        // Validate version for optimistic locking
        if (updatedProduct.getVersion() == null ||
                !updatedProduct.getVersion().equals(existingProduct.getVersion())) {
            throw new ProductValidationException("Invalid product version");
        }
    }

    public void validateProduct(Long productId) throws RuleValidationException {
        validateProductExists(productId);
        validateProductStatus(productId);
        validateProductPricing(productId);
        validateProductCategories(productId);
    }

    private void validateProductExists(Long productId) throws RuleValidationException {
        if (!productService.existsById(productId)) {
            throw new RuleValidationException("Product not found: " + productId);
        }
    }

    private void validateProductStatus(Long productId) throws RuleValidationException {
        ProductDTO product = productService.getProduct(productId);
        if (!product.isActive()) {
            throw new RuleValidationException("Product is not active: " + productId);
        }
    }

    private void validateProductPricing(Long productId) throws RuleValidationException {
        ProductDTO product = productService.getProduct(productId);

        // Validate base price
        if (product.getMrp() == null || product.getMrp().compareTo(BigDecimal.ZERO) <= 0) {
            throw new RuleValidationException("Invalid MRP for product: " + productId);
        }

        // Validate cost price
        if (product.getCostPrice() == null || product.getCostPrice().compareTo(BigDecimal.ZERO) < 0) {
            throw new RuleValidationException("Invalid cost price for product: " + productId);
        }

        // Validate that base price is greater than cost price
        if (product.getMrp().compareTo(product.getCostPrice()) < 0) {
            throw new RuleValidationException("MRP cannot be less than cost price for product: " + productId);
        }

        // Validate margin
        validateProductMargin(product);
    }

    private void validateProductMargin(ProductDTO productDTO) throws RuleValidationException {
        // Convert DTO to domain object
        Product product = productMapper.toEntity(productDTO);
        validateProductMargin(product);
    }

    private void validateProductMargin(Product product) throws RuleValidationException {
        BigDecimal margin = calculateMargin(product.getMrp(), product.getCostPrice());

        if (margin.compareTo(BigDecimal.ZERO) < 0) {
            throw new RuleValidationException(
                    "Negative margin not allowed for product: " + product.getId()
            );
        }

        if (margin.compareTo(new BigDecimal("100")) > 0) {
            log.warn("High margin ({}) detected for product: {}", margin, product.getId());
        }
    }

    private BigDecimal calculateMargin(BigDecimal sellingPrice, BigDecimal costPrice) {
        if (costPrice.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }
        return sellingPrice.subtract(costPrice)
                .multiply(new BigDecimal("100"))
                .divide(costPrice, 2, BigDecimal.ROUND_HALF_UP);
    }

    private void validateProductCategories(Long productId) throws RuleValidationException {
        Set<Long> categories = productService.getProductCategories(productId);

        if (categories == null || categories.isEmpty()) {
            throw new RuleValidationException("Product must belong to at least one category: " + productId);
        }

        // Validate each category exists and is active
        for (Long categoryId : categories) {
            validateCategory(categoryId);
        }
    }

    private void validateCategory(Long categoryId) throws RuleValidationException {
        if (!categoryService.isValidCategory(categoryId)) {
            throw new RuleValidationException("Invalid category: " + categoryId);
        }
    }

    public void validateProductCompatibility(Long productId, Set<String> requiredAttributes)
            throws RuleValidationException {
        ProductDTO product = productService.getProduct(productId);

        for (String attribute : requiredAttributes) {
            if (!product.hasAttribute(attribute)) {
                throw new RuleValidationException(
                        String.format("Product %s missing required attribute: %s",
                                productId, attribute)
                );
            }
        }
    }

    public void validateProductAvailability(Long productId, int requiredQuantity)
            throws RuleValidationException {
        int availableQuantity = productService.getAvailableQuantity(productId);

        if (availableQuantity < requiredQuantity) {
            throw new RuleValidationException(
                    String.format("Insufficient quantity for product %s. Required: %d, Available: %d",
                            productId, requiredQuantity, availableQuantity)
            );
        }
    }

    public void validateForCreate(ProductDTO productDTO) throws ProductValidationException, RuleValidationException {
        Product product = productMapper.toEntity(productDTO);
        validateProduct(product);
    }
}