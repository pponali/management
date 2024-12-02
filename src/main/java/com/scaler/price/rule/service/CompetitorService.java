package com.scaler.price.rule.service;

import org.springframework.stereotype.Service;

@Service
public class CompetitorService {
    public boolean existsById(Long competitorId) {
        return competitorId != null && competitorId > 0;
    }

    public boolean isActive(Long competitorId) {
        return competitorId != null && competitorId > 0;
    }

    public boolean isValidCompetitor(String competitorId) {
        return competitorId != null && !competitorId.trim().isEmpty();
    }
}
