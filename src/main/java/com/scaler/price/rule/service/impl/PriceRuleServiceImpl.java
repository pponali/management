package com.scaler.price.rule.service.impl;

import com.scaler.price.core.management.domain.AuditInfo;
import com.scaler.price.audit.service.AuditService;
import com.scaler.price.rule.actions.CustomActionRegistry;
import com.scaler.price.rule.domain.PricingRule;
import com.scaler.price.rule.domain.RuleStatus;
import com.scaler.price.rule.dto.RuleEvaluationRequest;
import com.scaler.price.rule.dto.RuleEvaluationResult;
import com.scaler.price.rule.dto.RuleSiteSummary;
import com.scaler.price.rule.events.RuleEventPublisher;
import com.scaler.price.rule.exceptions.RuleNotFoundException;
import com.scaler.price.rule.repository.RuleRepository;
import com.scaler.price.rule.service.*;
import com.scaler.price.rules.service.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class PriceRuleServiceImpl implements PriceRuleService {

    private final RuleRepository ruleRepository;
    private final RuleEngineService ruleEngine;
    private final PriceValidationService validationService;
    private final AuditService auditService;
    private final RuleEvaluationService evaluationService;
    private final CustomActionRegistry actionRegistry;
    private final ProductAttributeService productAttributeService;
    private final RuleEventPublisher eventPublisher;

    @Override
    public List<RuleEvaluationResult> evaluateRules(RuleEvaluationRequest request) {
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
            } catch (Exception e) {
                log.error("Error evaluating rule {}: {}", rule.getId(), e.getMessage());
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




    public PricingRule createRule(PricingRule rule) {
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
    public List<PricingRule> getRulesBySeller(String sellerId) {
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

    }

    public PricingRule activateRule(Long id) {
        PricingRule rule = getRule(id);
        rule.setIsActive(true);
        rule.getAuditInfo().setModifiedAt(LocalDateTime.now());

        PricingRule savedRule = ruleRepository.save(rule);
        eventPublisher.publishRuleActivated(savedRule);

        return savedRule;
    }

    public PricingRule deactivateRule(Long id) {
        PricingRule rule = getRule(id);
        rule.setIsActive(false);
        rule.getAuditInfo().setModifiedAt(LocalDateTime.now());

        PricingRule savedRule = ruleRepository.save(rule);
        eventPublisher.publishRuleDeactivated(savedRule);

        return savedRule;
    }

    private void initializeRule(PricingRule rule) {
        rule.setIsActive(true);
        rule.setVersion(1L);

        AuditInfo auditInfo = new AuditInfo();
        auditInfo.setCreatedBy(SecurityContextHolder.getContext().getAuthentication().getName());
        auditInfo.setCreatedAt(LocalDateTime.now());
        rule.setAuditInfo(auditInfo);

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
        existingRule.getAuditInfo().setModifiedBy(
                SecurityContextHolder.getContext().getAuthentication().getName());
        existingRule.getAuditInfo().setModifiedAt(LocalDateTime.now());
    }


}
