package com.scaler.price.rule.controllers;

import com.scaler.price.rule.domain.PricingRule;
import com.scaler.price.rule.domain.RuleHistory;
import com.scaler.price.rule.domain.RuleStatus;
import com.scaler.price.rule.dto.RuleDTO;
import com.scaler.price.rule.dto.RuleSiteSummary;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/pricing-rules")
public class PricingRuleController {

    @PostMapping
    public ResponseEntity<PricingRule> createRule(@RequestBody RuleDTO ruleDTO) {
        //add business logic here
        
        return null;
        
    }

    @GetMapping("/{ruleId}")
    public ResponseEntity<PricingRule> getRule(@PathVariable Long ruleId) {
        return null;
    }

    @PutMapping("/{ruleId}")
    public ResponseEntity<PricingRule> updateRule(@PathVariable Long ruleId, @RequestBody RuleDTO ruleDTO) {
        return null;
    }

    @DeleteMapping("/{ruleId}")
    public ResponseEntity<Void> deleteRule(@PathVariable Long ruleId) {
        return null;
    }

    @PostMapping("/{ruleId}/activate")
    public ResponseEntity<RuleStatus> activateRule(@PathVariable Long ruleId) {
        return null;
    }

    @PostMapping("/{ruleId}/deactivate")
    public ResponseEntity<RuleStatus> deactivateRule(@PathVariable Long ruleId) {
        return null;
    }

    @GetMapping("/{ruleId}/history")
    public ResponseEntity<List<RuleHistory>> getRuleHistory(@PathVariable Long ruleId) {
        return null;
    }

    @GetMapping("/site/{siteId}")
    public ResponseEntity<RuleSiteSummary> getSiteRules(@PathVariable Long siteId) {
        return null;
    }
}
