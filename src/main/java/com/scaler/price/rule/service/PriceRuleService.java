package com.scaler.price.rule.service;

import com.scaler.price.rule.domain.PricingRule;
import com.scaler.price.rule.domain.RuleStatus;
import com.scaler.price.rule.dto.RuleEvaluationRequest;
import com.scaler.price.rule.dto.RuleEvaluationResult;
import com.scaler.price.rule.dto.RuleSiteSummary;

import java.math.BigDecimal;
import java.util.List;

public interface PriceRuleService {
    List<RuleEvaluationResult> evaluateRules(RuleEvaluationRequest request);
    PricingRule createRule(PricingRule rule);
    PricingRule updateRule(Long id, PricingRule rule);
    void deleteRule(Long id);
    List<PricingRule> getRulesBySeller(String sellerId);

    PricingRule getRuleById(Long ruleId);

    RuleSiteSummary getSiteRulesSummary(Long siteId, RuleStatus status);

    RuleStatus activateRule(Long ruleId);
    BigDecimal getCurrentPrice(String productId);
}