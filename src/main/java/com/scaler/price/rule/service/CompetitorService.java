package com.scaler.price.rule.service;

public class CompetitorService {
    public boolean existsById(Long competitorId) {
        return competitorId != null && competitorId > 0;
    }

    public boolean isActive(Long competitorId) {
        return competitorId != null && competitorId > 0;
    }

    public boolean isValidCompetitor(String competitorId) {
        return competitorId != null && competitorId.length() > 0;
    }
}
