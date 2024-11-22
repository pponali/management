package com.scaler.price.core.management.utils;

import com.scaler.price.rule.domain.RuleType;
import com.scaler.price.rule.events.RuleEventType;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

@Component
public class PriceServiceMetrics {
    private final MeterRegistry meterRegistry;

    public PriceServiceMetrics(MeterRegistry registry) {
        this.meterRegistry = registry;
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
    }

    public void recordActionValidationFailure() {

    }

    public void recordTimeValidationFailure() {
    }

    public void recordTimeValidation() {
    }

    public void recordDiscountValidation() {
    }

    public void recordDiscountValidationFailure() {

    }

    public void recordConditionValidation(int size) {
    }

    public void recordConditionValidationFailure() {

    }

    public void recordEventPublished(RuleEventType eventType, RuleType ruleType, int size, int size1) {
    }

    public void incrementEventPublishFailure(RuleEventType eventType) {
    }

    public void incrementEventPublishSuccess(RuleEventType eventType) {
    }
}