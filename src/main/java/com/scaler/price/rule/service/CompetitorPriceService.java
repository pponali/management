package com.scaler.price.rule.service;

import java.math.BigDecimal;
import org.springframework.stereotype.Service;

@Service
public class CompetitorPriceService {

    public BigDecimal getCompetitorPrice(Long competitorId, Long productId) {
        // TODO: Implement actual competitor price retrieval logic
        // This is a placeholder implementation
        return BigDecimal.valueOf(100.00);
    }
}
