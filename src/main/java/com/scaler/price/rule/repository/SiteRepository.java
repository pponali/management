package com.scaler.price.rule.repository;

import com.scaler.price.rule.domain.Site;
import com.scaler.price.rule.domain.SiteLimits;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SiteRepository extends JpaRepository<Site, Long> {

    

    // Method to check if a site exists by its ID
    public boolean existsById(Long siteId);

    // Method to find the active status of a site by its ID
    public Optional<Boolean> findActiveStatusById(Long siteId) ;

    // Method to find the limits of a site by its ID
    public Optional<SiteLimits> findLimitsById(Long siteId);
}