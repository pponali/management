package com.scaler.price.core.management.repository;

import com.scaler.price.rule.dto.CategoryAttributes;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface CategoryAttributesRepository extends JpaRepository<CategoryAttributes, Long> {
    Optional<CategoryAttributes> findByCategoryId(Long categoryId);
    List<CategoryAttributes> findByCategoryIdIn(List<Long> categoryIds);
    List<CategoryAttributes> findByMinMarginPercentageBetween(Double minMargin, Double maxMargin);
    List<CategoryAttributes> findByMaxMarginPercentageBetween(Double minMargin, Double maxMargin);
    List<CategoryAttributes> findByTargetMarginPercentageBetween(Double minTarget, Double maxTarget);
    List<CategoryAttributes> findByIsActiveTrue();
    Optional<CategoryAttributes> findByCategoryIdAndIsActiveTrue(Long categoryId);
    List<CategoryAttributes> findByLastModifiedDateAfter(Instant modifiedDate);
    List<CategoryAttributes> findByModifiedUser(String modifiedUser);
}
