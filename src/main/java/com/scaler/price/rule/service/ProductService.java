package com.scaler.price.rule.service;

import com.scaler.price.rule.domain.Product;
import com.scaler.price.rule.domain.Product.ProductStatus;
import com.scaler.price.rule.dto.ProductDTO;
import com.scaler.price.rule.events.ProductEventPublisher;
import com.scaler.price.rule.exceptions.ProductNotFoundException;
import com.scaler.price.rule.exceptions.ProductValidationException;
import com.scaler.price.rule.exceptions.RuleValidationException;
import com.scaler.price.rule.mapper.ProductMapper;
import com.scaler.price.rule.repository.ProductRepository;
import com.scaler.price.validation.services.ProductValidationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProductService {
    private final ProductRepository productRepository;
    private final ProductMapper productMapper;
    private final ProductValidationService productValidator;
    private final SiteService siteService;
    private final ProductEventPublisher eventPublisher;

    @Transactional
    public ProductDTO createProduct(ProductDTO productDTO) throws RuleValidationException, ProductValidationException {
        log.info("Creating new product: {}", productDTO.getId());

        Product product = productMapper.toEntity(productDTO);
        productValidator.validateProduct(product);

        product.setCreatedAt(LocalDateTime.now());
        product.setStatus(product.getStatus());

        Product savedProduct = productRepository.save(product);
        eventPublisher.publishProductCreated(savedProduct);

        return productMapper.toDTO(savedProduct);
    }

    @Transactional
    public ProductDTO updateProduct(Long productId, ProductDTO productDTO) throws RuleValidationException, ProductValidationException {
        log.info("Updating product: {}", productId);

        Product existingProduct = getProductEntity(productId);
        productValidator.validateProductUpdate(productDTO, existingProduct);

        Product updatedProduct = productMapper.updateEntity(existingProduct, productDTO);
        updatedProduct.setUpdatedAt(LocalDateTime.now());

        Product savedProduct = productRepository.save(updatedProduct);
        eventPublisher.publishProductUpdated(savedProduct);

        return productMapper.toDTO(savedProduct);
    }

    @Transactional(readOnly = true)
    @Cacheable(value = "productCache", key = "#productId")
    public ProductDTO getProduct(Long productId) {
        log.debug("Fetching product: {}", productId);
        return productMapper.toDTO(getProductEntity(productId));
    }

    @Transactional(readOnly = true)
    public List<ProductDTO> getProductsBySeller(Long sellerId) {
        log.debug("Fetching products for seller: {}", sellerId);
        return productRepository.findActiveProductsBySeller(sellerId, ProductStatus.ACTIVE)
                .stream()
                .map(productMapper::toDTO)
                .toList();
    }


    @Transactional(readOnly = true)
    public List<ProductDTO> getProductsBySite(Long siteId) {
        log.debug("Fetching products for site: {}", siteId);
        return productRepository.findActiveProductsBySite(siteId, ProductStatus.ACTIVE)
                .stream()
                .map(productMapper::toDTO)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ProductDTO> getProductsByCategory(Long categoryId) {
        log.debug("Fetching products for category: {}", categoryId);
        return productRepository.findActiveProductsByCategory(categoryId, ProductStatus.ACTIVE)
                .stream()
                .map(productMapper::toDTO)
                .toList();
    }

    @Transactional
    public ProductDTO updateProductStatus(
            Long productId,
            ProductStatus status) {
        log.info("Updating status for product: {} to {}", productId, status);

        Product product = getProductEntity(productId);
        product.setStatus(status);
        product.setUpdatedAt(LocalDateTime.now());

        Product savedProduct = productRepository.save(product);
        eventPublisher.publishProductStatusChanged(savedProduct);

        return productMapper.toDTO(savedProduct);
    }

    @Transactional
    public ProductDTO updateProductSites(
            Long productId,
            Set<Long> siteIds) {
        log.info("Updating sites for product: {}", productId);

        Product product = getProductEntity(productId);

        // Validate sites
        siteIds.forEach(siteId -> {
            if (!siteService.isValidSite(siteId)) {
                try {
                    throw new ProductValidationException("Invalid site: " + siteId);
                } catch (ProductValidationException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
        });

        product.setSiteIds(new HashSet<>(siteIds));
        product.setUpdatedAt(LocalDateTime.now());

        Product savedProduct = productRepository.save(product);
        eventPublisher.publishProductSitesUpdated(savedProduct);

        return productMapper.toDTO(savedProduct);
    }

    @Transactional
    public ProductDTO updateProductPrices(
            Long productId,
            Map<String, Object> priceUpdate) throws ProductValidationException {
        log.info("Updating prices for product: {}", productId);

        Product product = getProductEntity(productId);

        validatePriceUpdate(priceUpdate);

        if (priceUpdate.containsKey("mrp")) {
            product.setMrp(new BigDecimal(priceUpdate.get("mrp").toString()));
        }

        if (priceUpdate.containsKey("costPrice")) {
            product.setCostPrice(new BigDecimal(priceUpdate.get("costPrice").toString()));
        }

        product.setUpdatedAt(LocalDateTime.now());

        Product savedProduct = productRepository.save(product);
        eventPublisher.publishProductPricesUpdated(savedProduct);

        return productMapper.toDTO(savedProduct);
    }

    @Transactional(readOnly = true)
    public boolean isProductActive(Long productId) {
        return productRepository.existsByIdAndStatus(
                productId,
                ProductStatus.ACTIVE
        );
    }

    @Transactional(readOnly = true)
    public Set<Long> validateProducts(Set<Long> productIds) {
        List<Product> activeProducts = productRepository.findActiveProductsByIds(productIds, ProductStatus.ACTIVE);
        return activeProducts.stream()
                .map(Product::getId)
                .collect(Collectors.toSet());
    }

    @Transactional(readOnly = true)
    public boolean existsById(Long productId) {
        return productRepository.existsById(productId);
    }

    @Transactional(readOnly = true)
    public int getAvailableQuantity(Long productId) {
        Product product = productRepository.findById(productId)
            .orElseThrow(() -> new ProductNotFoundException("Product not found with ID: " + productId));
        
        // Assuming you want to return the quantity from the Product entity
        // Modify this logic based on your specific requirements for tracking available quantity
        return product.getInventory() != null ? product.getInventory() : 0;
    }

    @Transactional(readOnly = true)
    public Set<Long> getProductCategories(Long productId) {
        Optional<Product> productOptional = productRepository.findById(productId);
        
        if (productOptional.isEmpty()) {
            throw new ProductNotFoundException("Product not found with ID: " + productId);
        }
        
        Product product = productOptional.get();
        return product.getCategoryId() != null ? Set.of(product.getCategoryId()) : new HashSet<>();
    }

    private Product getProductEntity(Long productId) {
        return productRepository.findById(productId)
                .orElseThrow(() -> new ProductNotFoundException(
                        "Product not found: " + productId
                ));
    }

    private void validatePriceUpdate(Map<String, Object> priceUpdate) throws ProductValidationException {
        if (priceUpdate.containsKey("mrp")) {
            BigDecimal mrp = new BigDecimal(priceUpdate.get("mrp").toString());
            if (mrp.compareTo(BigDecimal.ZERO) <= 0) {
                throw new ProductValidationException("MRP must be greater than zero");
            }
        }

        if (priceUpdate.containsKey("costPrice")) {
            BigDecimal costPrice = new BigDecimal(priceUpdate.get("costPrice").toString());
            if (costPrice.compareTo(BigDecimal.ZERO) <= 0) {
                throw new ProductValidationException("Cost price must be greater than zero");
            }

            if (priceUpdate.containsKey("mrp")) {
                BigDecimal mrp = new BigDecimal(priceUpdate.get("mrp").toString());
                if (costPrice.compareTo(mrp) > 0) {
                    throw new ProductValidationException(
                            "Cost price cannot be greater than MRP"
                    );
                }
            }
        }
    }
}
