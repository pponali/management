package com.scaler.price.core.management.repository;

import com.scaler.price.core.management.domain.FailedPrice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FailedPricesRepository extends JpaRepository<FailedPrice, Long> {
    List<FailedPrice> findByUploadId(String uploadId);
}
