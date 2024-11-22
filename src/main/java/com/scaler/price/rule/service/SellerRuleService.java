package com.scaler.price.rule.service;

import com.scaler.price.rule.domain.PricingRule;
import com.scaler.price.rule.domain.RuleType;
import com.scaler.price.rule.repository.RuleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

@Service
@Slf4j
@RequiredArgsConstructor
public class SellerRuleService {
    private final RuleRepository ruleRepository;

    public List<PricingRule> getActiveSellerRules(
            String sellerId,
            Set<String> siteIds) {
        return ruleRepository.findActiveRulesForSeller(
                sellerId,
                siteIds,
                LocalDateTime.now()
        );
    }

    public List<PricingRule> getApplicableRules(
            String sellerId,
            String siteId,
            BigDecimal price) {
        return ruleRepository.findApplicableRulesForPrice(
                sellerId,
                siteId,
                price,
                LocalDateTime.now()
        );
    }

    public List<RuleRepository.RuleConflict> checkRuleConflicts(String sellerId) {
        return ruleRepository.findSellerRuleConflicts(sellerId);
    }

    public void deactivateSellerRules(
            String sellerId,
            String modifiedBy) {
        int count = ruleRepository.deactivateSellerRules(
                sellerId,
                LocalDateTime.now(),
                modifiedBy
        );
        log.info("Deactivated {} rules for seller: {}", count, sellerId);
    }

    public List<PricingRule> findRulesWithSpecs(
            String sellerId,
            Set<String> siteIds,
            Set<RuleType> ruleTypes,
            boolean activeOnly) {
        Specification<PricingRule> spec = ruleRepository
                .createSellerSpecification(
                        sellerId,
                        siteIds,
                        ruleTypes,
                        activeOnly
                );
        return ruleRepository.findAll((Sort) spec);
    }
}
