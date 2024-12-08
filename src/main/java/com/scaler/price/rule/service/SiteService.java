package com.scaler.price.rule.service;

import com.scaler.price.rule.domain.Site;
import com.scaler.price.rule.domain.SiteLimits;
import com.scaler.price.rule.repository.SiteRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class SiteService {
    private final SiteRepository siteRepository;

    public boolean isValidSite(Long siteId) {
        return siteRepository.existsById(siteId);
    }


    public SiteLimits getSiteLimits(Long siteId) {
        return siteRepository.findLimitsById(siteId)
                .orElse(SiteLimits.getDefaultLimits());
    }

    public boolean existsById(Long siteId) {
       return siteRepository.existsById(siteId);
    }

    @Cacheable(value = "siteStatus", key = "#siteId")
    public boolean isSiteActive(Long siteId) {
        return siteRepository.findById(siteId)
                .map(Site::getIsActive)
                .orElse(false);
    }
}