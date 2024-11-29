package com.scaler.price.rule.service.impl;

import com.scaler.price.audit.exception.AuditSearchException;
import com.scaler.price.audit.service.AuditService;
import com.scaler.price.rule.domain.PricingRule;
import com.scaler.price.rule.domain.RuleStatus;
import com.scaler.price.rule.dto.RuleEvaluationRequest;
import com.scaler.price.rule.dto.RuleEvaluationResult;
import com.scaler.price.rule.dto.RuleSiteSummary;
import com.scaler.price.rule.events.RuleEventPublisher;
import com.scaler.price.rule.exceptions.ActionExecutionException;
import com.scaler.price.rule.exceptions.ActionRegistrationException;
import com.scaler.price.rule.exceptions.ProductFetchException;
import com.scaler.price.rule.exceptions.RuleNotFoundException;
import com.scaler.price.rule.repository.RuleRepository;
import com.scaler.price.rule.service.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class PriceRuleServiceImpl implements PriceRuleService {

    private final RuleRepository ruleRepository;
    private final RuleEngineService ruleEngine;
    private final PriceValidationService validationService;
    private final AuditService auditService;
    private final RuleEventPublisher eventPublisher;

    @Override
    public List<RuleEvaluationResult> evaluateRules(RuleEvaluationRequest request) throws ActionRegistrationException, ProductFetchException {
        log.info("Evaluating pricing rules for product: {}", request.getProductId());

        // Get applicable rules
        List<PricingRule> applicableRules = findApplicableRules(request);

        // Sort rules by priority
        applicableRules.sort((r1, r2) -> r2.getPriority().compareTo(r1.getPriority()));

        List<RuleEvaluationResult> results = new ArrayList<>();
        BigDecimal currentPrice = request.getBasePrice();

        // Evaluate each rule
        for (PricingRule rule : applicableRules) {
            try {
                RuleEvaluationResult result = ruleEngine.evaluate(rule, request, currentPrice);
                if (result != null) {
                    results.add(result);
                    currentPrice = result.getAdjustedPrice();
                }
            } catch (ActionExecutionException e) {
                log.error("Action execution failed for rule {}: {}", rule.getId(), e.getMessage(), e);
                // Optionally, you can choose to rethrow or handle differently
                throw new RuntimeException("Failed to evaluate rule: " + rule.getId(), e);
            } catch (Exception e) {
                log.error("Unexpected error evaluating rule {}: {}", rule.getId(), e.getMessage(), e);
            }
        }

        return results;
    }



    @Override
    public void deleteRule(Long id) {

    }


    private List<PricingRule> findApplicableRules(RuleEvaluationRequest request) {
        LocalDateTime now = LocalDateTime.now();

        return ruleRepository.findApplicableRules(
                request.getSellerId(),
                request.getSiteId(),
                request.getCategoryId(),
                request.getBrandId(),
                now
        );
    }




    public PricingRule createRule(PricingRule rule) throws AuditSearchException {
        log.info("Creating new pricing rule: {}", rule.getRuleName());
        validationService.validateRule(rule);

        initializeRule(rule);
        PricingRule savedRule = ruleRepository.save(rule);

        auditService.auditRuleCreation(savedRule);
        eventPublisher.publishRuleCreated(savedRule);

        return savedRule;
    }

    public PricingRule updateRule(Long id, PricingRule updatedRule) {
        log.info("Updating pricing rule: {}", id);
        PricingRule existingRule = ruleRepository.findById(id)
                .orElseThrow(() -> new RuleNotFoundException("Rule not found: " + id));

        validationService.validateRuleUpdate(existingRule, updatedRule);

        updateRuleFields(existingRule, updatedRule);
        PricingRule savedRule = ruleRepository.save(existingRule);

        auditService.auditRuleUpdate(savedRule);
        eventPublisher.publishRuleUpdated(savedRule);

        return savedRule;
    }

    @Transactional(readOnly = true)
    public PricingRule getRule(Long id) {
        return ruleRepository.findById(id)
                .orElseThrow(() -> new RuleNotFoundException("Rule not found: " + id));
    }

    @Transactional(readOnly = true)
    public List<PricingRule> getRulesBySeller(Long sellerId) {
        return ruleRepository.findBySellerIdsContaining(sellerId);
    }

    @Override
    public PricingRule getRuleById(Long ruleId) {
        PricingRule rule = ruleRepository.findById(ruleId).orElse(null);
        if (rule != null) {
            rule.getConditions().forEach(condition -> {
                condition.setRule(null);
            });
            rule.getActions().forEach(action -> {
                action.setRule(null);
            });
            return rule;
        }
        return null;
    }

    @Override
    public RuleSiteSummary getSiteRulesSummary(Long siteId, RuleStatus status) {
        
        // Find all rules for the site
        List<PricingRule> siteRules = ruleRepository.findRulesBySite(siteId);
        
        // Filter rules by status if provided
        if (status != null) {
            siteRules = siteRules.stream()
                .filter(rule -> rule.getStatus() == status)
                .collect(Collectors.toList());
        }
        
        // Calculate summary metrics
        long totalRules = siteRules.size();
        long activeRules = siteRules.stream()
            .filter(PricingRule::getIsActive)
            .count();
        
        LocalDateTime earliestRule = siteRules.stream()
            .map(PricingRule::getEffectiveFrom)
            .min(LocalDateTime::compareTo)
            .orElse(null);
        
        LocalDateTime latestRule = siteRules.stream()
            .map(PricingRule::getEffectiveTo)
            .max(LocalDateTime::compareTo)
            .orElse(null);
        
        return new RuleSiteSummary(
            siteId, 
            totalRules, 
            activeRules, 
            earliestRule, 
            latestRule
        );
    }

    public RuleStatus activateRule(Long id) {
        PricingRule rule = getRule(id);
        rule.setIsActive(true);
        rule.setModifiedAt(LocalDateTime.now());
    
        PricingRule savedRule = ruleRepository.save(rule);
        eventPublisher.publishRuleActivated(savedRule);
    
        return RuleStatus.ACTIVE; // Or create a method to convert PricingRule to RuleStatus
    }
    public PricingRule deactivateRule(Long id) {
        PricingRule rule = getRule(id);
        rule.setIsActive(false);
        rule.setModifiedAt(LocalDateTime.now());

        PricingRule savedRule = ruleRepository.save(rule);
        eventPublisher.publishRuleDeactivated(savedRule);

        return savedRule;
    }

    private void initializeRule(PricingRule rule) {
        rule.setIsActive(true);
        rule.setVersion(1L);


        if (rule.getEffectiveFrom() == null) {
            rule.setEffectiveFrom(LocalDateTime.now());
        }
    }

    private void updateRuleFields(PricingRule existingRule, PricingRule updatedRule) {
        existingRule.setRuleName(updatedRule.getRuleName());
        existingRule.setDescription(updatedRule.getDescription());
        existingRule.setRuleType(updatedRule.getRuleType());
        existingRule.setSellerIds(updatedRule.getSellerIds());
        existingRule.setSiteIds(updatedRule.getSiteIds());
        existingRule.setCategoryIds(updatedRule.getCategoryIds());
        existingRule.setBrandIds(updatedRule.getBrandIds());
        existingRule.setPriority(updatedRule.getPriority());
        existingRule.setMinimumPrice(updatedRule.getMinimumPrice());
        existingRule.setMaximumPrice(updatedRule.getMaximumPrice());
        existingRule.setMinimumMargin(updatedRule.getMinimumMargin());
        existingRule.setMaximumMargin(updatedRule.getMaximumMargin());
        existingRule.setEffectiveFrom(updatedRule.getEffectiveFrom());
        existingRule.setEffectiveTo(updatedRule.getEffectiveTo());

        // Update conditions
        existingRule.getConditions().clear();
        updatedRule.getConditions().forEach(condition -> {
            condition.setRule(existingRule);
            existingRule.getConditions().add(condition);
        });

        // Update actions
        existingRule.getActions().clear();
        updatedRule.getActions().forEach(action -> {
            action.setRule(existingRule);
            existingRule.getActions().add(action);
        });

        // Update audit info
        existingRule.setLastModifiedBy(
                SecurityContextHolder.getContext().getAuthentication().getName());
        existingRule.setModifiedAt(LocalDateTime.now());
    }



    @Override
    public BigDecimal getCurrentPrice(Long productId) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getCurrentPrice'");
    }


}
