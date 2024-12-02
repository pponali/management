package com.scaler.price.rule.repository;

import com.scaler.price.rule.domain.Product;
import com.scaler.price.rule.domain.Product.ProductStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Set;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {
    @Query("""
        SELECT p FROM Product p
        WHERE p.id IN :productIds
        AND p.status = :status
        """)
    List<Product> findActiveProductsByIds(
            @Param("productIds") Set<Long> productIds,
            @Param("status") ProductStatus status
    );

    boolean existsByIdAndStatus(Long id, ProductStatus status);

    @Query("""
        SELECT p FROM Product p
        WHERE p.sellerId = :sellerId
        AND p.status = :status
        """)
    List<Product> findActiveProductsBySeller(
            @Param("sellerId") Long sellerId,
            @Param("status") ProductStatus status
    );

    @Query("""
        SELECT p FROM Product p
        JOIN p.siteIds s
        WHERE s = :siteId
        AND p.status = :status
        """)
    List<Product> findActiveProductsBySite(
            @Param("siteId") Long siteId,
            @Param("status") ProductStatus status
    );

    @Query("""
        SELECT p FROM Product p
        WHERE p.categoryId = :categoryId
        AND p.status = :status
        """)
    List<Product> findActiveProductsByCategory(
            @Param("categoryId") Long categoryId,
            @Param("status") ProductStatus status
    );

    @Query("""
        SELECT DISTINCT p FROM Product p
        JOIN p.siteIds s
        WHERE p.sellerId = :sellerId
        AND s IN :siteIds
        AND p.status = :status
        """)
    List<Product> findActiveProductsBySellerAndSites(
            @Param("sellerId") Long sellerId,
            @Param("siteIds") Set<Long> siteIds,
            @Param("status") ProductStatus status
    );
}
