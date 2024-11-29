package com.scaler.price.core.management.repository;


import com.scaler.price.core.management.domain.BulkUploadTracker;
import com.scaler.price.core.management.domain.UploadStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface BulkUploadTrackerRepository extends JpaRepository<BulkUploadTracker, String> {

    List<BulkUploadTracker> findBySellerId(Long sellerId);

    List<BulkUploadTracker> findBySellerIdAndSiteId(Long sellerId, Long siteId);

    List<BulkUploadTracker> findByStatus(UploadStatus status);

    @Query("SELECT bt FROM BulkUploadTracker bt WHERE bt.status = :status AND bt.uploadedAt < :threshold")
    List<BulkUploadTracker> findStuckUploads(UploadStatus status, LocalDateTime threshold);

    @Query("SELECT bt FROM BulkUploadTracker bt WHERE bt.uploadedAt BETWEEN :startDate AND :endDate")
    List<BulkUploadTracker> findUploadsInDateRange(LocalDateTime startDate, LocalDateTime endDate);
}
