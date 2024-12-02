package com.scaler.price.rule.repository;

import com.scaler.price.rule.domain.ProductAttribute;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.Set;

@Repository
public interface ProductAttributeRepository extends JpaRepository<ProductAttribute, Long> {

    List<ProductAttribute> findByProductId(@Param("productId") Long productId);

    Optional<ProductAttribute> findByProductIdAndAttributeKey(
            @Param("productId") Long productId,
            @Param("attributeKey") Long attributeKey
    );

    List<ProductAttribute> findByProductIdAndCategory(
            @Param("productId") Long productId,
            @Param("category") Long category
    );

    @Query("""
        SELECT DISTINCT pa.attributeKey 
        FROM ProductAttribute pa 
        WHERE pa.category = :category 
        AND pa.isFilterable = true
        """)
    List<Long> findFilterableAttributesByCategory(@Param("category") Long category);

    @Query("""
        SELECT pa FROM ProductAttribute pa 
        WHERE pa.productId = :productId 
        AND pa.attributeKey IN :attributeKeys
        """)
    List<ProductAttribute> findByProductIdAndAttributeKeys(
            @Param("productId") Long productId,
            @Param("attributeKeys") Set<Long> attributeKeys
    );

    void deleteByProductIdAndAttributeKey(
            @Param("productId") Long productId,
            @Param("attributeKey") Long attributeKey
    );
}
