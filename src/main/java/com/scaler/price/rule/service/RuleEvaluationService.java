package com.scaler.price.rule.service;

import com.scaler.price.rule.domain.PricingRule;
import com.scaler.price.rule.dto.*;
import com.scaler.price.rule.exceptions.ActionExecutionException;
import com.scaler.price.rule.exceptions.ActionRegistrationException;
import com.scaler.price.rule.exceptions.ProductFetchException;
import com.scaler.price.rule.exceptions.RuleEvaluationException;
import com.scaler.price.rule.repository.RuleRepository;
import com.scaler.price.core.management.exceptions.PriceValidationException;
import com.scaler.price.core.management.service.PriceValidationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.*;

@Service
@Slf4j
@RequiredArgsConstructor
public class RuleEvaluationService {
    private final RuleRepository ruleRepository;
    private final ConditionEvaluatorService conditionEvaluator;
    private final ActionExecutorService actionExecutor;
    private final PriceValidationService priceValidator;
    private final BundleService bundleService;

    public List<RuleEvaluationResult> evaluateRules(RuleEvaluationRequest request) throws RuleEvaluationException, ActionExecutionException, ActionRegistrationException, ProductFetchException, PriceValidationException {
        log.info("Starting rule evaluation for request: {}", request);

        List<PricingRule> applicableRules = findApplicableRules(request);

        if (applicableRules.isEmpty()) {
            log.info("No applicable rules found for request: {}", request);
            return Collections.emptyList();
        }

        RuleEvaluationContext context = createEvaluationContext(request);
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
                request.getSellerId(),
                request.getSiteId(),
                request.getCategoryId(),
                request.getBrandId(),
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

    public RuleEvaluationResult evaluateBundlePrice(Long bundleId) throws ProductFetchException {
        try {
            BundleEligibility eligibility = bundleService.checkEligibility(
                    bundleId,
                    null,
                    null
            );
    
            // Add null checks and handle potential empty bundle scenarios
            Long productId = Optional.ofNullable(eligibility.getBundleId())
                .flatMap(id -> bundleService.getBundleById(bundleId))
                .map(bundle -> {
                    Set<Long> productIds = null;
                    try {
                        productIds = bundle.getProductIds();
                    } catch (ProductFetchException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                    return productIds != null && !productIds.isEmpty() 
                        ? productIds.iterator().next() 
                        : null;
                })
                .orElseThrow(() -> new ProductFetchException("No products found in bundle " + bundleId));
    
            BigDecimal bundlePrice = bundleService.getBundleDiscount(bundleId, productId);
    
            RuleEvaluationResult result = new RuleEvaluationResult();
            result.setAdjustedPrice(bundlePrice);
            result.setOriginalPrice(eligibility.getOriginalPrice());
            result.setDiscountAmount(eligibility.getDiscountAmount());
            result.setMarginPercentage(eligibility.getMarginPercentage());
    
            return result;
        } catch (ProductFetchException e) {
            log.error("Product fetch error for bundle {}: {}", bundleId, e.getMessage(), e);
            throw e;  // Re-throw the specific exception
        } catch (Exception e) {
            log.error("Error evaluating bundle price: {}", e.getMessage(), e);
            throw new RuleEvaluationException("Failed to evaluate bundle price", e);
        }
    }

    private RuleEvaluationResult evaluateRule(
            PricingRule rule,
            RuleEvaluationContext context,
            BigDecimal currentPrice) throws RuleEvaluationException, ActionExecutionException, ActionRegistrationException, ProductFetchException, PriceValidationException {

        log.debug("Evaluating rule: {} for context: {}", rule.getId(), context);

        if (!conditionEvaluator.evaluateConditions(rule.getConditions(), context)) {
            log.debug("Rule {} conditions not met", rule.getId());
            return null;
        }

        RuleEvaluationResult result = actionExecutor.executeActions(
                rule.getActions(),
                context,
                currentPrice
        );

        if (result != null) {
            result.setAdjustedPrice(
                    priceValidator.validatePriceBounds(
                            result.getAdjustedPrice(),
                            rule,
                            context
                    )
            );

            calculateMetrics(result, context);
            enrichResultWithRuleInfo(result, rule);
        }

        return result;
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

    public RuleEvaluationResult previewRuleApplication(RuleDTO ruleDTO, RuleEvaluationRequest request) 
            throws ActionRegistrationException, ActionExecutionException, RuleEvaluationException, 
                   ProductFetchException, PriceValidationException {
        RuleEvaluationContext context = createEvaluationContext(request);
        PricingRule rule = convertDTOToPricingRule(ruleDTO);
        return evaluateRule(rule, context, request.getBasePrice());
    }

    private PricingRule convertDTOToPricingRule(RuleDTO dto) {
        PricingRule rule = new PricingRule();
        rule.setId(dto.getId());
        rule.setRuleName(dto.getName());
        rule.setRuleType(dto.getType());
        rule.setConditions(dto.getConditions());
        rule.setActions(dto.getActions());
        return rule;
    }
}