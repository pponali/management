package com.scaler.price.rule.repository;


import com.scaler.price.rule.domain.ProductAttribute;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.Set;

@Repository
public interface ProductAttributeRepository extends JpaRepository<ProductAttribute, Long> {

    List<ProductAttribute> findByProductId(String productId);

    Optional<ProductAttribute> findByProductIdAndAttributeKey(
            String productId,
            String attributeKey
    );

    List<ProductAttribute> findByProductIdAndCategory(
            String productId,
            String category
    );

    @Query("""
        SELECT DISTINCT pa.attributeKey 
        FROM ProductAttribute pa 
        WHERE pa.category = :category 
        AND pa.isFilterable = true
        """)
    List<String> findFilterableAttributesByCategory(String category);

    @Query("""
        SELECT pa FROM ProductAttribute pa 
        WHERE pa.productId = :productId 
        AND pa.attributeKey IN :attributeKeys
        """)
    List<ProductAttribute> findByProductIdAndAttributeKeys(
            String productId,
            Set<String> attributeKeys
    );

    void deleteByProductIdAndAttributeKey(String productId, String attributeKey);
}
