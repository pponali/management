package com.scaler.price.core.management.utils;

import com.scaler.price.rule.domain.ChangeDiff;
import com.scaler.price.rule.domain.PricingRule;
import com.scaler.price.rule.domain.RuleStatus;
import com.scaler.price.rule.domain.RuleType;
import com.scaler.price.rule.domain.SellerSiteConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConfigChangeDetectorTest {

    private ConfigChangeDetector configChangeDetector;

    @BeforeEach
    void setUp() {
        configChangeDetector = new ConfigChangeDetector();
    }

    @Test
    void test_detectConfigChanges() {
        // Arrange
        Map<String, ChangeDiff> changes = new HashMap<>();
        Set<SellerSiteConfig> oldConfigs = new HashSet<>();
        Set<SellerSiteConfig> newConfigs = new HashSet<>();
        
        // Create pricing rule
        PricingRule rule = new PricingRule();
        rule.setId(1L);
        rule.setRuleName("Test Rule");
        rule.setDescription("Test Description");
        rule.setRuleType(RuleType.PRICE);
        rule.setStatus(RuleStatus.ACTIVE);
        rule.setSellerSiteConfigs(new HashSet<>());
        
        // Create test configs
        SellerSiteConfig oldConfig = new SellerSiteConfig();
        oldConfig.setId(1L);
        oldConfig.setSellerId(1L);
        oldConfig.setSiteId(1L);
        oldConfig.setRule(rule);
        Map<String, Object> oldMetadata = new HashMap<>();
        oldMetadata.put("isActive", true);
        oldConfig.setMetadata(oldMetadata);
        
        SellerSiteConfig newConfig = new SellerSiteConfig();
        newConfig.setId(1L);
        newConfig.setSellerId(1L);
        newConfig.setSiteId(1L);
        newConfig.setRule(rule);
        Map<String, Object> newMetadata = new HashMap<>();
        newMetadata.put("isActive", false);
        newConfig.setMetadata(newMetadata);
        
        oldConfigs.add(oldConfig);
        newConfigs.add(newConfig);

        // Act
        configChangeDetector.detectConfigChanges(changes, oldConfigs, newConfigs);

        // Assert
        String key = "1:1"; // sellerId:siteId
        assertTrue(changes.containsKey(key), "Should detect changes for seller 1, site 1");
        ChangeDiff diff = changes.get(key);
        assertTrue(diff.getOldValue().contains("true"), "Old config should be active");
        assertTrue(diff.getNewValue().contains("false"), "New config should be inactive");
    }
}