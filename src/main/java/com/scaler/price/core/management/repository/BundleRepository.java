package com.scaler.price.core.management.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.scaler.price.rule.domain.Bundle;

@Repository
public interface BundleRepository extends JpaRepository<Bundle, Long> {

    // Find bundles by name
    List<Bundle> findByName(String name);

    // Find bundles containing a specific product
    List<Bundle> findByProductsContaining(Long productId);

    // Find bundles with a name containing a specific keyword
    List<Bundle> findByNameContaining(String keyword);

    // Find bundles with a specific number of products
    @Query("SELECT b FROM Bundle b WHERE SIZE(b.products) = :size")
    List<Bundle> findByProductsSize(@Param("size") int size);
}
