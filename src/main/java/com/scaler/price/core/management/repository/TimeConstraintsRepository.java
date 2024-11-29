package com.scaler.price.core.management.repository;

import com.scaler.price.rule.domain.constraint.TimeConstraints;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface TimeConstraintsRepository extends JpaRepository<TimeConstraints, Long>  {
    Optional<TimeConstraints> findByCategoryId(Long categoryId);
}
