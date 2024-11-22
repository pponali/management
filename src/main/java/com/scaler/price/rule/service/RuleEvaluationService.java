package com.scaler.price.rule.service;

import com.scaler.price.rule.domain.PricingRule;
import com.scaler.price.rule.dto.RuleDTO;
import com.scaler.price.rule.dto.RuleEvaluationContext;
import com.scaler.price.rule.dto.RuleEvaluationRequest;
import com.scaler.price.rule.dto.RuleEvaluationResult;
import com.scaler.price.rules.dto.*;
import com.scaler.price.rule.exceptions.ActionExecutionException;
import com.scaler.price.rule.exceptions.ActionRegistrationException;
import com.scaler.price.rule.exceptions.RuleEvaluationException;
import com.scaler.price.rule.repository.RuleRepository;
import com.scaler.price.core.management.service.PriceValidationService;
import com.scaler.price.core.management.utils.PriceServiceMetrics;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

@Service
@Slf4j
@RequiredArgsConstructor
public class RuleEvaluationService {
    private final RuleRepository ruleRepository;
    private final ConditionEvaluatorService conditionEvaluator;
    private final ActionExecutorService actionExecutor;
    private final PriceValidationService priceValidator;
    private final PriceServiceMetrics metricsService;
    private BundleService bundleService;

    public List<RuleEvaluationResult> evaluateRules(RuleEvaluationRequest request) throws RuleEvaluationException {
        log.info("Starting rule evaluation for request: {}", request);

        // Find applicable rules
        List<PricingRule> applicableRules = findApplicableRules(request);

        if (applicableRules.isEmpty()) {
            log.info("No applicable rules found for request: {}", request);
            return Collections.emptyList();
        }

        // Create evaluation context
        RuleEvaluationContext context = createEvaluationContext(request);

        // Evaluate rules and collect results
        List<RuleEvaluationResult> results = new ArrayList<>();
        BigDecimal currentPrice = request.getBasePrice();

        for (PricingRule rule : applicableRules) {
            try {
                RuleEvaluationResult result = evaluateRule(rule, context, currentPrice);
                if (result != null) {
                    results.add(result);
                    currentPrice = result.getAdjustedPrice();
                    context.setCurrentPrice(currentPrice);
                }
            } catch (Exception e) {
                log.error("Error evaluating rule {}: {}", rule.getId(), e.getMessage(), e);
            }
        }

        return results;
    }

    private List<PricingRule> findApplicableRules(RuleEvaluationRequest request) {
        return ruleRepository.findApplicableRules(
                Set.of(request.getSellerId()).toString(),
                Set.of(request.getSiteId()).toString(),
                Set.of(request.getCategoryId()).toString(),
                Set.of(request.getBrandId()).toString(),
                LocalDateTime.now()
        );
    }

    private RuleEvaluationContext createEvaluationContext(RuleEvaluationRequest request) {
        return RuleEvaluationContext.builder()
                .productId(request.getProductId())
                .sellerId(request.getSellerId())
                .siteId(request.getSiteId())
                .categoryId(request.getCategoryId())
                .brandId(request.getBrandId())
                .basePrice(request.getBasePrice())
                .costPrice(request.getCostPrice())
                .currentPrice(request.getBasePrice())
                .attributes(request.getAttributes())
                .evaluationTime(LocalDateTime.now())
                .build();
    }

    private RuleEvaluationResult evaluateRule(
            PricingRule rule,
            RuleEvaluationContext context,
            BigDecimal currentPrice) throws RuleEvaluationException {

        log.debug("Evaluating rule: {} for context: {}", rule.getId(), context);

        // Check if all conditions are met
        if (!conditionEvaluator.evaluateConditions(rule.getConditions(), context)) {
            log.debug("Rule {} conditions not met", rule.getId());
            return null;
        }

        // Execute actions
        RuleEvaluationResult result = actionExecutor.executeActions(
                rule.getActions(),
                context,
                currentPrice
        );

        if (result != null) {
            // Validate price bounds
            result.setAdjustedPrice(
                    priceValidator.validatePriceBounds(
                            result.getAdjustedPrice(),
                            rule,
                            context
                    )
            );

            result.setRuleId(rule.getId());
            result.setRuleName(rule.getRuleName());
            result.setRuleType(rule.getRuleType());
        }

        return result;
    }

    private RuleEvaluationResult evaluateRule(
            PricingRule rule,
            RuleEvaluationContext context) throws RuleEvaluationException, ActionRegistrationException, ActionExecutionException {

        try {
            // Evaluate conditions
            if (!conditionEvaluator.evaluateConditions(rule.getConditions(), context)) {
                log.debug("Rule {} conditions not met", rule.getId());
                return null;
            }

            // Execute actions
            RuleEvaluationResult result = actionExecutor.executeActions(
                    rule.getActions(), context, context.getBasePrice());

            if (result != null) {
                // Validate price bounds
                result.setAdjustedPrice(
                        priceValidator.validatePriceBounds(
                                result.getAdjustedPrice(),
                                rule,
                                context
                        )
                );

                // Calculate metrics
                calculateMetrics(result, context);

                // Set rule information
                enrichResultWithRuleInfo(result, rule);
            }

            return result;

        } catch (Exception e) {
            log.error("Error evaluating rule {}: {}", rule.getId(), e.getMessage(), e);
            metricsService.recordRuleEvaluationError(rule.getId());
            return null;
        }
    }

    private void calculateMetrics(RuleEvaluationResult result, RuleEvaluationContext context) {
        BigDecimal originalPrice = context.getCurrentPrice();
        BigDecimal adjustedPrice = result.getAdjustedPrice();

        result.setOriginalPrice(originalPrice);
        result.setDiscountAmount(originalPrice.subtract(adjustedPrice));

        if (context.getCostPrice() != null && !context.getCostPrice().equals(BigDecimal.ZERO)) {
            BigDecimal margin = adjustedPrice.subtract(context.getCostPrice())
                    .divide(adjustedPrice, 2, RoundingMode.HALF_UP)
                    .multiply(new BigDecimal("100"));
            result.setMarginPercentage(margin);
        }
    }

    private void enrichResultWithRuleInfo(RuleEvaluationResult result, PricingRule rule) {
        result.setRuleId(rule.getId());
        result.setRuleName(rule.getRuleName());
        result.setRuleType(rule.getRuleType());
    }

    public RuleEvaluationResult previewRuleApplication(RuleDTO ruleDTO, RuleEvaluationRequest ruleEvaluationRequest ) throws ActionRegistrationException, ActionExecutionException, RuleEvaluationException {
        RuleEvaluationContext context = createEvaluationContext(ruleEvaluationRequest);
        RuleEvaluationResult result = new RuleEvaluationResult();

        try {
            // Evaluate conditions
            if (!conditionEvaluator.evaluateConditions(ruleDTO.getConditions(), context)) {
                log.debug("Rule conditions not met");
                return null;
            }

            // Execute actions
            result = actionExecutor.executeActions(
                    ruleDTO.getActions(), context, context.getBasePrice());

            if (result != null) {
                // Validate price bounds
                result.setAdjustedPrice(
                        priceValidator.validatePriceBounds(
                                result.getAdjustedPrice(),
                                null,
                                context
                        )
                );

                // Calculate metrics
                calculateMetrics(result, context);
            }

            return result;

        } catch (Exception e) {
            log.error("Error previewing rule application: {}", e.getMessage(), e);
            return null;
        }
    }

    public RuleEvaluationResult evaluatePrice(RuleEvaluationRequest ruleEvaluationRequest) {
        try {
            List<RuleEvaluationResult> results = evaluateRules(ruleEvaluationRequest);
            if (results.isEmpty()) {
                return new RuleEvaluationResult(
                        ruleEvaluationRequest.getBasePrice(),
                        ruleEvaluationRequest.getBasePrice(),
                        BigDecimal.ZERO,
                        BigDecimal.ZERO
                );
            } else {
                return results.get(results.size() - 1);
            }
        } catch (RuleEvaluationException e) {
            log.error("Error evaluating price: {}", e.getMessage(), e);
            return null;
        }

    }

    public RuleEvaluationResult evaluateBundlePrice(Long bundleId) {
        try {
            // Fetch bundle details
            BundleEligibility eligibility = bundleService.checkEligibility(bundleId.toString());

            // Evaluate bundle price
            BigDecimal bundlePrice = bundleService.getBundleDiscount(bundleId.toString());

            // Create RuleEvaluationResult
            RuleEvaluationResult result = new RuleEvaluationResult();
            result.setAdjustedPrice(bundlePrice);
            result.setOriginalPrice(eligibility.getOriginalPrice());
            result.setDiscountAmount(eligibility.getDiscountAmount());
            result.setMarginPercentage(eligibility.getMarginPercentage());

            return result;
        } catch (Exception e) {
            log.error("Error evaluating bundle price: {}", e.getMessage(), e);
            return null;
        }
    }
}
