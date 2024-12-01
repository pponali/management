package com.scaler.price.core.management.utils;

import com.scaler.price.core.management.dto.RuleMetrics;
import com.scaler.price.rule.domain.ConditionType;
import com.scaler.price.rule.domain.RuleType;
import com.scaler.price.rule.events.RuleEventType;
import com.scaler.price.rule.repository.RuleRepository;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Component
public class PriceServiceMetrics {
    private final MeterRegistry meterRegistry;
    private final RuleRepository ruleRepository;

    public PriceServiceMetrics(MeterRegistry meterRegistry, RuleRepository ruleRepository) {
        this.meterRegistry = meterRegistry;
        this.ruleRepository = ruleRepository;
    }

    public RuleMetrics calculateRuleMetrics(Long siteId) {
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

    private void recordMetrics(RuleMetrics metrics, Long siteId) {
        recordGaugeValue(
                "rules.total",
                metrics.getTotalRules(),
                "siteId", siteId
        );
        recordGaugeValue(
                "rules.active",
                metrics.getActiveRules(),
                "siteId", siteId
        );
        recordGaugeValue(
                "rules.active.percentage",
                metrics.getActivePercentage(),
                "siteId", siteId
        );
    }

    public void recordPriceOperation(String operation) {
        meterRegistry.counter("price.operations",
                "type", operation).increment();
    }

    public void recordProcessingTime(long milliseconds) {
        meterRegistry.timer("price.processing.time")
                .record(milliseconds, TimeUnit.MILLISECONDS);
    }

    public void recordRuleEvaluation(
            String productId,
            int totalRules,
            int appliedRules,
            long executionTime) {

        // Record rule execution metrics
        meterRegistry.counter("rules.evaluation.total").increment();
        meterRegistry.counter("rules.evaluated",
                Tags.of("productId", productId)).increment(totalRules);
        meterRegistry.counter("rules.applied",
                Tags.of("productId", productId)).increment(appliedRules);

        // Record timing
        meterRegistry.timer("rules.evaluation.time",
                Tags.of("productId", productId)).record(executionTime, TimeUnit.MILLISECONDS);

        // Record success rate
        if (totalRules > 0) {
            double applicationRate = (double) appliedRules / totalRules;
            meterRegistry.gauge("rules.application.rate",
                    Tags.of("productId", productId), applicationRate);
        }
    }

    public void recordRuleEvaluationError(Long ruleId) {
        meterRegistry.counter("rules.evaluation.errors",
                Tags.of("ruleId", String.valueOf(ruleId))).increment();
    }

    public void recordEvaluationError(String productId) {
        meterRegistry.counter("rules.evaluation.errors",
                Tags.of("productId", productId)).increment();
    }

    public void recordActionValidation(int size) {
        meterRegistry.counter("rules.validation.actions.total").increment(size);
        meterRegistry.counter("rules.validation.actions.success").increment();
    }

    public void recordActionValidationFailure() {
        meterRegistry.counter("rules.validation.actions.failure").increment();
    }

    public void recordTimeValidationFailure() {
        meterRegistry.counter("rules.validation.time.failure").increment();
    }

    public void recordTimeValidation() {
        meterRegistry.counter("rules.validation.time.success").increment();
    }

    public void recordDiscountValidation() {
        meterRegistry.counter("rules.validation.discount.success").increment();
    }

    public void recordDiscountValidationFailure() {
        meterRegistry.counter("rules.validation.discount.failure").increment();
    }

    public void recordConditionValidation(int size) {
        meterRegistry.counter("rules.validation.conditions.total").increment(size);
        meterRegistry.counter("rules.validation.conditions.success").increment();
    }

    public void recordConditionValidationFailure() {
        meterRegistry.counter("rules.validation.conditions.failure").increment();
    }

    public void recordEventPublished(RuleEventType eventType, RuleType ruleType, int sellerCount, int siteCount) {
        Tags tags = Tags.of(
            "eventType", eventType.toString(),
            "ruleType", ruleType.toString(),
            "sellerCount", String.valueOf(sellerCount),
            "siteCount", String.valueOf(siteCount)
        );
        meterRegistry.counter("rules.events.published", tags).increment();
    }

    public void incrementEventPublishFailure(RuleEventType eventType) {
        meterRegistry.counter("rules.events.publish.failure",
            Tags.of("eventType", eventType.toString())).increment();
    }

    public void incrementEventPublishSuccess(RuleEventType eventType) {
        meterRegistry.counter("rules.events.publish.success",
            Tags.of("eventType", eventType.toString())).increment();
    }

    public void recordGaugeValue(String metricName, Long value, String metricType, Long siteId) {
        meterRegistry.gauge(metricName,
            Tags.of("type", metricType)
                .and("siteId", siteId.toString()),
            value);
    }

    public void recordGaugeValue(String metricName, Double value, String metricType, Long siteId) {
        meterRegistry.gauge(metricName,
            Tags.of("type", metricType)
                .and("siteId", siteId.toString()),
            value);
    }
}