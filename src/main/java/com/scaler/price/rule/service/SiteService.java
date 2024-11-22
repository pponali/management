package com.scaler.price.rule.service;

import com.scaler.price.rule.domain.SiteLimits;
import com.scaler.price.rule.repository.SiteRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class SiteService {
    private final SiteRepository siteRepository;

    public boolean isValidSite(String siteId) {
        return siteRepository.existsById(siteId);
    }

    public boolean isSiteActive(String siteId) {
        return siteRepository.findActiveStatusById(siteId)
                .orElse(false);
    }

    public SiteLimits getSiteLimits(String siteId) {
        return siteRepository.findLimitsById(siteId)
                .orElse(SiteLimits.getDefaultLimits());
    }
}