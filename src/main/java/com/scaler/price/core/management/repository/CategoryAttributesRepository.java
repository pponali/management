package com.scaler.price.core.management.repository;

import com.scaler.price.rule.domain.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface CategoryAttributesRepository extends JpaRepository<Category, Long> {
    Optional<Category> findById(Long categoryId);
    List<Category> findByIdIn(List<Long> categoryIds);
    List<Category> findByIsActiveTrue();
    Optional<Category> findByIdAndIsActiveTrue(Long categoryId);
    List<Category> findByUpdatedAtAfter(Instant modifiedDate);
    List<Category> findByUpdatedBy(String modifiedUser);

    @Query("SELECT c FROM Category c WHERE c.attributes LIKE %:attribute%")
    List<Category> findByAttribute(@Param("attribute") String attribute);
}
