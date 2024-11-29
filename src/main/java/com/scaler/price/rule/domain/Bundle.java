package com.scaler.price.rule.domain;

import com.scaler.price.core.management.domain.AuditInfo;
import com.scaler.price.rule.exceptions.ProductFetchException;
import com.scaler.price.rule.repository.ProductRepository;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Setter
@Getter
@Entity
@Slf4j
@NoArgsConstructor
@Table(name = "bundles")
public class Bundle extends AuditInfo{

    @Column(nullable = false)
    private String name;

    @ElementCollection
    @CollectionTable(name = "bundle_products",
            joinColumns = @JoinColumn(name = "bundle_id"))
    private List<String> products;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DiscountType discountType;

    @Column(precision = 19, scale = 4)
    private BigDecimal minimumPurchaseAmount;

    @Column
    private Instant validUntil;

    @Column(precision = 19, scale = 4)
    private BigDecimal maxDiscount;

    @Column(precision = 10, scale = 2)
    private BigDecimal marginPercentage;

    @Column(precision = 19, scale = 4)
    private BigDecimal discountAmount;

    @Column(precision = 10, scale = 2)
    private BigDecimal discountPercentage;

    @Transient
    private ProductRepository productRepository;

    public Bundle(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    public Bundle(ProductRepository productRepository, String name, List<String> products) {
        this.productRepository = productRepository;
        this.name = name;
        this.products = products;
    }

    public Set<Long> getProductIds() throws ProductFetchException {
        Set<Long> productIds = new HashSet<>();
        try {
            List<Product> products = productRepository.findAll();
            for (Product product : products) {
                productIds.add(product.getId());
            }
        } catch (DataAccessException e) {
            log.error("Error fetching product IDs from the database", e);
            throw new ProductFetchException("Unable to retrieve product IDs", e);
        }
        return productIds;
    }


}