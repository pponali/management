package com.scaler.price.rule.domain;

import com.scaler.price.rule.exceptions.ProductFetchException;
import com.scaler.price.rule.repository.ProductRepository;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Data
@Entity
@Slf4j
@NoArgsConstructor
@Table(name = "bundles")
public class Bundle {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

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

    public Collection<String> getProductIds() throws ProductFetchException {
        Set<String> productIds = new HashSet<>();
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