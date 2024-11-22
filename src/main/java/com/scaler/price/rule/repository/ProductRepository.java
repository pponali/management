package com.scaler.price.rule.repository;


import com.scaler.price.rule.domain.Product;
import com.scaler.price.rule.domain.ProductStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Set;

@Repository
public interface ProductRepository extends JpaRepository<Product, String> {

    @Query("""
        SELECT p FROM Product p
        WHERE p.sellerId = :sellerId
        AND p.status = 'ACTIVE'
        """)
    List<Product> findActiveProductsBySeller(String sellerId);

    @Query("""
        SELECT p FROM Product p
        JOIN p.siteIds s
        WHERE s = :siteId
        AND p.status = 'ACTIVE'
        """)
    List<Product> findActiveProductsBySite(String siteId);

    @Query("""
        SELECT p FROM Product p
        WHERE p.categoryId = :categoryId
        AND p.status = 'ACTIVE'
        """)
    List<Product> findActiveProductsByCategory(String categoryId);

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
    List<Product> findActiveProductsByIds(Set<String> productIds);

    boolean existsByProductIdAndStatus(String productId, ProductStatus status);
}
