package com.scaler.price.rule.config;

import com.scaler.price.core.management.repository.ConfigurationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class ConfigurationService {
    
    private final ConfigurationRepository configurationRepository;
    
    private static final String DEFAULT_SITE_ID = "default";

    public Optional<String> getConfigurationValue(String key) {
        return configurationRepository.findByKeyAndSiteIdAndIsActive(key, DEFAULT_SITE_ID, true)
                .map(Configuration::getValue);
    }
    
    public String getConfigurationValueOrDefault(String key, String defaultValue) {
        return getConfigurationValue(key).orElse(defaultValue);
    }
    
    public BigDecimal getDecimalConfigurationValue(String key, BigDecimal defaultValue) {
        return getConfigurationValue(key)
                .map(value -> {
                    try {
                        return new BigDecimal(value);
                    } catch (NumberFormatException e) {
                        log.warn("Invalid decimal value for key: {}. Using default: {}", key, defaultValue);
                        return defaultValue;
                    }
                })
                .orElse(defaultValue);
    }
    
    public Integer getIntegerConfigurationValue(String key, Integer defaultValue) {
        return getConfigurationValue(key)
                .map(value -> {
                    try {
                        return Integer.parseInt(value);
                    } catch (NumberFormatException e) {
                        log.warn("Invalid integer value for key: {}. Using default: {}", key, defaultValue);
                        return defaultValue;
                    }
                })
                .orElse(defaultValue);
    }
    
    public Boolean getBooleanConfigurationValue(String key, Boolean defaultValue) {
        return getConfigurationValue(key)
                .map(Boolean::parseBoolean)
                .orElse(defaultValue);
    }

    // Margin Configuration
    public BigDecimal getDefaultMargin() {
        return getDecimalConfigurationValue("default.margin", new BigDecimal("0.15"));
    }

    public BigDecimal getMinimumMargin() {
        return getDecimalConfigurationValue("minimum.margin", new BigDecimal("0.05"));
    }

    public BigDecimal getMaximumMargin() {
        return getDecimalConfigurationValue("maximum.margin", new BigDecimal("0.50"));
    }

    // Price Configuration
    public BigDecimal getMinimumPrice() {
        return getDecimalConfigurationValue("minimum.price", new BigDecimal("0.01"));
    }

    public BigDecimal getMaximumPrice() {
        return getDecimalConfigurationValue("maximum.price", new BigDecimal("999999.99"));
    }

    // Category Configuration
    public int getMaxCategoryNameLength() {
        return getIntegerConfigurationValue("max.category.name.length", 255);
    }

    public int getMaxCategoryDepth() {
        return getIntegerConfigurationValue("max.category.depth", 5);
    }

    public int getMaxSiteMappingsPerCategory() {
        return getIntegerConfigurationValue("max.site.mappings.per.category", 100);
    }

    public int getMaxCustomAttributesPerCategory() {
        return getIntegerConfigurationValue("max.custom.attributes.per.category", 50);
    }

    public int getMaxAttributeValueLength() {
        return getIntegerConfigurationValue("max.attribute.value.length", 1000);
    }

    public int getMaxValidationRulesPerCategory() {
        return getIntegerConfigurationValue("max.validation.rules.per.category", 20);
    }

    public int getMaxTagsPerCategory() {
        return getIntegerConfigurationValue("max.tags.per.category", 10);
    }

    // Rule Configuration
    public int getMaxRulesPerCategory() {
        return getIntegerConfigurationValue("max.rules.per.category", 50);
    }

    public int getMaxActionsPerRule() {
        return getIntegerConfigurationValue("max.actions.per.rule", 10);
    }

    public int getMaxConditionsPerRule() {
        return getIntegerConfigurationValue("max.conditions.per.rule", 5);
    }

    public int getMaxRuleHierarchyDepth() {
        return getIntegerConfigurationValue("max.rule.hierarchy.depth", 5);
    }

    public int getBusinessHourStart() {
        return 0;
    }

    public int getBusinessHourEnd() {
        return 0;
    }
}
