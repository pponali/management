package com.scaler.price.rule.service;

import com.scaler.price.audit.exception.AuditSearchException;
import com.scaler.price.rule.domain.PricingRule;
import com.scaler.price.rule.domain.RuleStatus;
import com.scaler.price.rule.dto.RuleEvaluationRequest;
import com.scaler.price.rule.dto.RuleEvaluationResult;
import com.scaler.price.rule.dto.RuleSiteSummary;
import com.scaler.price.rule.exceptions.ActionRegistrationException;
import com.scaler.price.rule.exceptions.ProductFetchException;

import java.math.BigDecimal;
import java.util.List;

public interface PriceRuleService {
    List<RuleEvaluationResult> evaluateRules(RuleEvaluationRequest request) throws ActionRegistrationException, ProductFetchException;
    PricingRule createRule(PricingRule rule) throws AuditSearchException;
    PricingRule updateRule(Long id, PricingRule rule);
    void deleteRule(Long id);
    List<PricingRule> getRulesBySeller(Long sellerId);

    PricingRule getRuleById(Long ruleId);

    RuleSiteSummary getSiteRulesSummary(Long siteId, RuleStatus status);

    RuleStatus activateRule(Long ruleId);
    BigDecimal getCurrentPrice(Long productId);
}