package com.scaler.price.core.management.utils;

import com.scaler.price.core.management.dto.RuleMetrics;
import com.scaler.price.rule.domain.ConditionType;
import com.scaler.price.rule.domain.RuleType;
import com.scaler.price.rule.repository.RuleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class RuleMetricsService {
    private final RuleRepository ruleRepository;
    private final PriceServiceMetrics metricsService;

    public RuleMetrics calculateRuleMetrics(String siteId) {
        RuleMetrics metrics = new RuleMetrics();

        // Basic counts
        metrics.setTotalRules(ruleRepository.countBySiteIdsContaining(siteId));
        metrics.setActiveRules(ruleRepository.countActiveRulesBySite(
                siteId,
                LocalDateTime.now()
        ));

        // Count by rule type
        metrics.setPriceRules(ruleRepository.countActiveRulesBySellerAndSite(
                siteId,
                siteId,
                RuleType.PRICE
        ));
        metrics.setDiscountRules(ruleRepository.countActiveRulesBySellerAndSite(
                siteId,
                siteId,
                RuleType.DISCOUNT
        ));

        // Count by condition type
        metrics.setPriceConditions(ruleRepository.countRulesByConditionType(
                siteId,
                ConditionType.PRICE_RANGE
        ));
        metrics.setTimeConditions(ruleRepository.countRulesByConditionType(
                siteId,
                ConditionType.TIME_BASED
        ));

        // Get detailed statistics
        List<RuleRepository.RuleStatistics> statistics = ruleRepository.getRuleStatistics(siteId);
        if (!statistics.isEmpty()) {
            RuleRepository.RuleStatistics stats = statistics.get(0);
            metrics.setExpiredRules(stats.getExpiredRules());
            metrics.setActivePercentage(stats.getActivePercentage());
        }

        // Record metrics
        recordMetrics(metrics, siteId);

        return metrics;
    }

    private void recordMetrics(RuleMetrics metrics, String siteId) {
        metricsService.recordGaugeValue(
                "rules.total",
                metrics.getTotalRules(),
                "siteId", siteId
        );
        metricsService.recordGaugeValue(
                "rules.active",
                metrics.getActiveRules(),
                "siteId", siteId
        );
        metricsService.recordGaugeValue(
                "rules.active.percentage",
                metrics.getActivePercentage(),
                "siteId", siteId
        );
    }
}
