package com.scaler.price.rule.repository;


import com.scaler.price.rule.domain.Product;
import com.scaler.price.rule.domain.Product.ProductStatus;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Set;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {

    @Query("""
        SELECT p FROM Product p
        WHERE p.sellerId = :sellerId
        AND p.status = 'ACTIVE'
        """)
    List<Product> findActiveProductsBySeller(Long sellerId);

    @Query("""
        SELECT p FROM Product p
        JOIN p.siteIds s
        WHERE s = :siteId
        AND p.status = 'ACTIVE'
        """)
    List<Product> findActiveProductsBySite(Long siteId);

    @Query("""
        SELECT p FROM Product p
        WHERE p.categoryId = :categoryId
        AND p.status = 'ACTIVE'
        """)
    List<Product> findActiveProductsByCategory(Long categoryId);

    @Query("""
        SELECT DISTINCT p FROM Product p
        JOIN p.siteIds s
        WHERE p.sellerId = :sellerId
        AND s IN :siteIds
        AND p.status = 'ACTIVE'
        """)
    List<Product> findActiveProductsBySellerAndSites(
            String sellerId,
            Set<String> siteIds
    );

    @Query("""
        SELECT p FROM Product p
        WHERE p.productId IN :productIds
        AND p.status = 'ACTIVE'
        """)
    List<Product> findActiveProductsByIds(Set<Long> productIds);

    boolean existsByProductIdAndStatus(Long productId, ProductStatus active);
}
