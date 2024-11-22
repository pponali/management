package com.scaler.price.rule.mapper;

import com.scaler.price.core.management.dto.PriceDTO;
import com.scaler.price.rule.domain.Product;
import com.scaler.price.rule.dto.ProductDTO;
import org.springframework.stereotype.Component;

import java.util.HashSet;

@Component
public class ProductMapper {

    /**
     * Convert Product entity to ProductDTO
     */
    public ProductDTO toDTO(Product product) {
        if (product == null) {
            return null;
        }

        return ProductDTO.builder()
                .productId(product.getProductId())
                .productName(product.getProductName())
                .categoryId(product.getCategoryId())
                .brandId(product.getBrandId())
                .sellerId(product.getSellerId())
                .siteIds(new HashSet<>(product.getSiteIds()))
                .mrp(product.getMrp())
                .costPrice(product.getCostPrice())
                .currency(product.getCurrency())
                .status(product.getStatus())
                .build();
    }

    /**
     * Convert ProductDTO to Product entity
     */
    public Product toEntity(ProductDTO dto) {
        if (dto == null) {
            return null;
        }

        return Product.builder()
                .productId(dto.getProductId())
                .productName(dto.getProductName())
                .categoryId(dto.getCategoryId())
                .brandId(dto.getBrandId())
                .sellerId(dto.getSellerId())
                .siteIds(new HashSet<>(dto.getSiteIds()))
                .mrp(dto.getMrp())
                .costPrice(dto.getCostPrice())
                .currency(dto.getCurrency())
                .status(dto.getStatus())
                .build();
    }

    /**
     * Update existing Product entity with ProductDTO data
     */
    public void updateProductFromDTO(ProductDTO dto, Product product) {
        if (dto == null || product == null) {
            return;
        }

        if (dto.getProductName() != null) product.setProductName(dto.getProductName());
        if (dto.getCategoryId() != null) product.setCategoryId(dto.getCategoryId());
        if (dto.getBrandId() != null) product.setBrandId(dto.getBrandId());
        if (dto.getSellerId() != null) product.setSellerId(dto.getSellerId());
        if (dto.getSiteIds() != null) product.setSiteIds(new HashSet<>(dto.getSiteIds()));
        if (dto.getMrp() != null) product.setMrp(dto.getMrp());
        if (dto.getCostPrice() != null) product.setCostPrice(dto.getCostPrice());
        if (dto.getCurrency() != null) product.setCurrency(dto.getCurrency());
        if (dto.getStatus() != null) product.setStatus(dto.getStatus());
    }

    /**
     * Convert PriceDTO to Product entity
     */
    public Product priceToProduct(PriceDTO priceDTO) {
        if (priceDTO == null) {
            return null;
        }

        return Product.builder()
                .productId(priceDTO.getProductId())
                .sellerId(priceDTO.getSellerId())
                .mrp(priceDTO.getMrp())
                .currency(priceDTO.getCurrency())
                .build();
    }

    /**
     * Convert Product to PriceDTO
     */
    public PriceDTO productToPrice(Product product) {
        if (product == null) {
            return null;
        }

        return PriceDTO.builder()
                .productId(product.getProductId())
                .sellerId(product.getSellerId())
                .mrp(product.getMrp())
                .currency(product.getCurrency())
                .build();
    }

    /**
     * Merge two products, taking non-null values from the source
     */
    public Product mergeProducts(Product source, Product target) {
        if (source == null) {
            return target;
        }
        if (target == null) {
            return source;
        }

        // Only update non-null values
        if (source.getProductName() != null) target.setProductName(source.getProductName());
        if (source.getCategoryId() != null) target.setCategoryId(source.getCategoryId());
        if (source.getBrandId() != null) target.setBrandId(source.getBrandId());
        if (source.getSellerId() != null) target.setSellerId(source.getSellerId());
        if (source.getSiteIds() != null && !source.getSiteIds().isEmpty()) {
            target.setSiteIds(new HashSet<>(source.getSiteIds()));
        }
        if (source.getMrp() != null) target.setMrp(source.getMrp());
        if (source.getCostPrice() != null) target.setCostPrice(source.getCostPrice());
        if (source.getCurrency() != null) target.setCurrency(source.getCurrency());
        if (source.getStatus() != null) target.setStatus(source.getStatus());

        return target;
    }

    /**
     * Create a deep copy of a Product entity
     */
    public Product cloneProduct(Product source) {
        if (source == null) {
            return null;
        }

        return Product.builder()
                .productId(source.getProductId())
                .productName(source.getProductName())
                .categoryId(source.getCategoryId())
                .brandId(source.getBrandId())
                .sellerId(source.getSellerId())
                .siteIds(new HashSet<>(source.getSiteIds()))
                .mrp(source.getMrp())
                .costPrice(source.getCostPrice())
                .currency(source.getCurrency())
                .status(source.getStatus())
                .build();
    }
}
