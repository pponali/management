package com.scaler.price.rule.repository;

import com.scaler.price.rule.domain.SellerLimits;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SellerRepository extends JpaRepository<SellerLimits, Long> {

    Optional<SellerLimits> findLimitsById(Long sellerId);

    boolean findActiveStatusById(Long sellerId);




}
