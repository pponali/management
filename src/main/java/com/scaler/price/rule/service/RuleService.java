package com.scaler.price.rule.service;

import com.scaler.price.rule.config.ConfigurationService;
import com.scaler.price.rule.domain.PricingRule;
import com.scaler.price.rule.domain.RuleStatus;
import com.scaler.price.rule.domain.RuleType;
import com.scaler.price.rule.dto.RuleSiteSummary;
import com.scaler.price.rule.exceptions.InvalidStatusTransitionException;
import com.scaler.price.rule.exceptions.RuleNotFoundException;
import com.scaler.price.rule.exceptions.SiteNotFoundException;
import com.scaler.price.rule.repository.RuleRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@Slf4j
@Transactional
public class RuleService {
    private final RuleStatusManager statusManager;
    private RuleRepository ruleRepository;
    private ConfigurationService configService;

    public RuleService(RuleStatusManager statusManager) {
        this.statusManager = statusManager;
    }

    public PricingRule approveRule(Long ruleId, String approverComments) throws InvalidStatusTransitionException {
        PricingRule rule = getRule(ruleId);

        if (rule.getStatus() != RuleStatus.PENDING_APPROVAL) {
            throw new InvalidStatusTransitionException(
                    "Only rules in PENDING_APPROVAL status can be approved"
            );
        }

        statusManager.updateRuleStatus(
                rule,
                RuleStatus.APPROVED,
                approverComments
        );

        return ruleRepository.save(rule);
    }

    public PricingRule activateRule(Long ruleId) throws InvalidStatusTransitionException {
        PricingRule rule = getRule(ruleId);

        if (rule.getStatus() != RuleStatus.APPROVED &&
                rule.getStatus() != RuleStatus.INACTIVE) {
            throw new InvalidStatusTransitionException(
                    "Rule must be in APPROVED or INACTIVE status to be activated"
            );
        }

        statusManager.updateRuleStatus(
                rule,
                RuleStatus.ACTIVE,
                "Rule activated"
        );

        return ruleRepository.save(rule);
    }

    public PricingRule suspendRule(Long ruleId, String reason) throws InvalidStatusTransitionException {
        PricingRule rule = getRule(ruleId);

        statusManager.updateRuleStatus(
                rule,
                RuleStatus.SUSPENDED,
                reason
        );

        return ruleRepository.save(rule);
    }

    @Transactional(readOnly = true)
    @Cacheable(value = "rules", key = "#ruleId", unless = "#result == null")
    public PricingRule getRule(Long ruleId) {
        log.debug("Fetching rule with ID: {}", ruleId);
        return ruleRepository.findById(ruleId)
                .orElseThrow(() -> new RuleNotFoundException(
                        "Rule not found with ID: " + ruleId
                ));
    }

    @Transactional(readOnly = true)
    public PricingRule getRuleWithDetails(Long ruleId) {
        log.debug("Fetching rule with details for ID: {}", ruleId);
        return ruleRepository.findRuleWithDetails(ruleId)
                .orElseThrow(() -> new RuleNotFoundException(
                        "Rule not found with ID: " + ruleId
                ));
    }

    @Transactional(readOnly = true)
    public PricingRule getRuleForSeller(Long ruleId, String sellerId) {
        log.debug("Fetching rule {} for seller {}", ruleId, sellerId);
        return ruleRepository.findBySellerAndId(ruleId, sellerId, LocalDateTime.now())
                .orElseThrow(() -> new RuleNotFoundException(
                        String.format("Rule not found with ID: %d for seller: %s",
                                ruleId, sellerId)
                ));
    }

    @Transactional(readOnly = true)
    public PricingRule getActiveRule(Long ruleId) {
        log.debug("Fetching active rule with ID: {}", ruleId);
        return ruleRepository.findByIdAndStatus(ruleId, RuleStatus.ACTIVE, LocalDateTime.now())
                .orElseThrow(() -> new RuleNotFoundException(
                        "Active rule not found with ID: " + ruleId
                ));
    }

    @Transactional(readOnly = true)
    public PricingRule getRuleForSite(Long ruleId, String siteId) {
        log.debug("Fetching rule {} for site {}", ruleId, siteId);
        return ruleRepository.findBySiteAndId(ruleId, siteId)
                .orElseThrow(() -> new RuleNotFoundException(
                        String.format("Rule not found with ID: %d for site: %s",
                                ruleId, siteId)
                ));
    }

    @Transactional(readOnly = true)
    public List<PricingRule> getActiveRulesForSite(
            String siteId,
            RuleType ruleType,
            Integer minPriority) {

        Specification<PricingRule> spec = Specification.where(
                ruleRepository.siteSpec(siteId)
        ).and(
                ruleRepository.activeSpec()
        );

        if (ruleType != null) {
            spec = spec.and(ruleRepository.typeSpec(ruleType));
        }

        if (minPriority != null) {
            spec = spec.and(ruleRepository.prioritySpec(minPriority));
        }

        return ruleRepository.findAll(spec, Sort.by(Sort.Direction.DESC,"priority"));
    }

    @Transactional(readOnly = true)
    public List<PricingRule> getRuleHierarchy(String siteId) {
        return ruleRepository.findRuleHierarchyBySite(
                siteId,
                configService.getMaxRuleHierarchyDepth()
        );
    }

    @Transactional(readOnly = true)
    public RuleSiteSummary getSiteSummary(String siteId) {
        return ruleRepository.getRuleSummariesBySite()
                .stream()
                .filter(summary -> summary.getSiteId().equals(siteId))
                .findFirst()
                .orElseThrow(() -> new SiteNotFoundException(
                        "Site not found: " + siteId
                ));
    }
}
