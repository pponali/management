package com.scaler.price.core.management.utils;

import com.google.common.collect.Sets;
import com.scaler.price.rule.domain.ChangeDiff;
import com.scaler.price.rule.domain.SellerSiteConfig;
import com.scaler.price.rule.domain.constraint.MarginConstraints;
import com.scaler.price.rule.domain.constraint.PriceConstraints;
import com.scaler.price.rule.domain.constraint.CategoryLimit;
import com.scaler.price.rule.domain.constraint.PriceThreshold;
import com.scaler.price.rule.domain.constraint.TimeConstraints;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Component
public class ConfigChangeDetector {

    public void detectConfigChanges(
            Map<String, ChangeDiff> changes,
            Set<SellerSiteConfig> oldConfigs,
            Set<SellerSiteConfig> newConfigs) {

        // Create maps for easier comparison
        Map<String, SellerSiteConfig> oldConfigMap = createConfigMap(oldConfigs);
        Map<String, SellerSiteConfig> newConfigMap = createConfigMap(newConfigs);

        // Detect added configurations
        Set<String> addedKeys = new HashSet<>(newConfigMap.keySet());
        addedKeys.removeAll(oldConfigMap.keySet());
        if (!addedKeys.isEmpty()) {
            changes.put("added_configs", new ChangeDiff(
                    null,
                    serializeConfigKeys(addedKeys)
            ));
        }

        // Detect removed configurations
        Set<String> removedKeys = new HashSet<>(oldConfigMap.keySet());
        removedKeys.removeAll(newConfigMap.keySet());
        if (!removedKeys.isEmpty()) {
            changes.put("removed_configs", new ChangeDiff(
                    serializeConfigKeys(removedKeys),
                    null
            ));
        }

        // Detect modified configurations
        Set<String> commonKeys = new HashSet<>(newConfigMap.keySet());
        commonKeys.retainAll(oldConfigMap.keySet());

        for (String key : commonKeys) {
            SellerSiteConfig oldConfig = oldConfigMap.get(key);
            SellerSiteConfig newConfig = newConfigMap.get(key);
            detectIndividualConfigChanges(changes, key, oldConfig, newConfig);
        }
    }

    private Map<String, SellerSiteConfig> createConfigMap(Set<SellerSiteConfig> configs) {
        if (configs == null) {
            return new HashMap<>();
        }

        return configs.stream()
                .collect(Collectors.toMap(
                        this::createConfigKey,
                        Function.identity()
                ));
    }

    private String createConfigKey(SellerSiteConfig config) {
        return String.format("%s-%s", config.getSellerId(), config.getSiteId());
    }

    private void detectIndividualConfigChanges(
            Map<String, ChangeDiff> changes,
            String configKey,
            SellerSiteConfig oldConfig,
            SellerSiteConfig newConfig) {

        // Price Changes
        detectPriceChanges(changes, configKey, oldConfig, newConfig);

        // Margin Changes
        detectMarginChanges(changes, configKey, oldConfig, newConfig);

        // Time Constraint Changes
        detectTimeConstraintChanges(changes, configKey, 
            oldConfig.getTimeConstraints(), 
            newConfig.getTimeConstraints());

        // Priority Changes
        if (!Objects.equals(oldConfig.getPriority(), newConfig.getPriority())) {
            changes.put(configKey + "_priority", new ChangeDiff(
                    String.valueOf(oldConfig.getPriority()),
                    String.valueOf(newConfig.getPriority())
            ));
        }

        // Status Changes
        if (!Objects.equals(oldConfig.getIsActive(), newConfig.getIsActive())) {
            changes.put(configKey + "_status", new ChangeDiff(
                    String.valueOf(oldConfig.getIsActive()),
                    String.valueOf(newConfig.getIsActive())
            ));
        }
    }

    private void detectPriceChanges(
            Map<String, ChangeDiff> changes,
            String configKey,
            SellerSiteConfig oldConfig,
            SellerSiteConfig newConfig) {

        PriceConstraints oldPriceConstraints = oldConfig.getPriceConstraints();
        PriceConstraints newPriceConstraints = newConfig.getPriceConstraints();

        if (!Objects.equals(oldConfig.getMinimumPrice(), newConfig.getMinimumPrice())) {
            changes.put(configKey + "_min_price", new ChangeDiff(
                    formatPrice(oldConfig.getMinimumPrice()),
                    formatPrice(newConfig.getMinimumPrice())
            ));
        }

        if (!Objects.equals(oldConfig.getMaximumPrice(), newConfig.getMaximumPrice())) {
            changes.put(configKey + "_max_price", new ChangeDiff(
                    formatPrice(oldConfig.getMaximumPrice()),
                    formatPrice(newConfig.getMaximumPrice())
            ));
        }

        // Detect changes in price constraints
        if (oldPriceConstraints != null || newPriceConstraints != null) {
            detectPriceConstraintChanges(
                    changes,
                    configKey,
                    oldPriceConstraints,
                    newPriceConstraints
            );
        }
    }

    private void detectMarginChanges(
            Map<String, ChangeDiff> changes,
            String configKey,
            SellerSiteConfig oldConfig,
            SellerSiteConfig newConfig) {

        MarginConstraints oldMarginConstraints = oldConfig.getMarginConstraints();
        MarginConstraints newMarginConstraints = newConfig.getMarginConstraints();

        if (!Objects.equals(oldConfig.getMinimumMargin(), newConfig.getMinimumMargin())) {
            changes.put(configKey + "_min_margin", new ChangeDiff(
                    formatPercentage(oldConfig.getMinimumMargin()),
                    formatPercentage(newConfig.getMinimumMargin())
            ));
        }

        if (!Objects.equals(oldConfig.getMaximumMargin(), newConfig.getMaximumMargin())) {
            changes.put(configKey + "_max_margin", new ChangeDiff(
                    formatPercentage(oldConfig.getMaximumMargin()),
                    formatPercentage(newConfig.getMaximumMargin())
            ));
        }

        // Detect changes in margin constraints
        if (oldMarginConstraints != null || newMarginConstraints != null) {
            detectMarginConstraintChanges(
                    changes,
                    configKey,
                    oldMarginConstraints,
                    newMarginConstraints
            );
        }
    }

    private void detectTimeConstraintChanges(
            Map<String, ChangeDiff> changes,
            String configKey,
            TimeConstraints oldTimeConstraints,
            TimeConstraints newTimeConstraints) {

        if (oldTimeConstraints != null || newTimeConstraints != null) {
            detectTimeWindowChanges(
                    changes,
                    configKey,
                    oldTimeConstraints,
                    newTimeConstraints
            );
            detectBlackoutPeriodChange(
                    changes,
                    configKey,
                    oldTimeConstraints,
                    newTimeConstraints
            );
        }
    }

    private String serializeConfigKeys(Set<String> keys) {
        return String.join(",", keys);
    }

    private String formatPrice(BigDecimal price) {
        return price != null ? price.toString() : null;
    }

    private String formatPercentage(BigDecimal percentage) {
        return percentage != null ? percentage.toString() + "%" : null;
    }

    private void detectPriceConstraintChanges(
            Map<String, ChangeDiff> changes,
            String configKey,
            PriceConstraints oldConstraints,
            PriceConstraints newConstraints) {

        if (oldConstraints == null && newConstraints == null) {
            return;
        }

        if (oldConstraints == null || newConstraints == null) {
            changes.put(configKey + "_price_constraints", new ChangeDiff(
                    oldConstraints != null ? "Present" : "Null",
                    newConstraints != null ? "Present" : "Null"
            ));
            return;
        }

        // Check max price change percentage
        if (!Objects.equals(oldConstraints.getMaxPriceChangePercentage(),
                newConstraints.getMaxPriceChangePercentage())) {
            changes.put(configKey + "_max_price_change", new ChangeDiff(
                    formatPercentage(oldConstraints.getMaxPriceChangePercentage()),
                    formatPercentage(newConstraints.getMaxPriceChangePercentage())
            ));
        }

        // Check rounding strategy
        if (!Objects.equals(oldConstraints.getRoundingStrategy(),
                newConstraints.getRoundingStrategy())) {
            changes.put(configKey + "_rounding_strategy", new ChangeDiff(
                    oldConstraints.getRoundingStrategy().toString(),
                    newConstraints.getRoundingStrategy().toString()
            ));
        }

        // Check price increase settings
        if (!Objects.equals(oldConstraints.getAllowPriceIncrease(),
                newConstraints.getAllowPriceIncrease())) {
            changes.put(configKey + "_allow_price_increase", new ChangeDiff(
                    String.valueOf(oldConstraints.getAllowPriceIncrease()),
                    String.valueOf(newConstraints.getAllowPriceIncrease())
            ));
        }

        // Compare price thresholds
        detectPriceThresholdChanges(changes, configKey,
                oldConstraints.getPriceThresholds(),
                newConstraints.getPriceThresholds()
        );

        // Compare category specific limits
        detectCategoryLimitChanges(changes, configKey,
                        oldConstraints.getCategorySpecificLimits(),
                        newConstraints.getCategorySpecificLimits()
                );
            }
        

        
            private void detectPriceThresholdChanges(
            Map<String, ChangeDiff> changes,
            String configKey,
            List<PriceThreshold> oldThresholds,
            List<PriceThreshold> newThresholds) {

        if (oldThresholds == null && newThresholds == null) {
            return;
        }

        List<PriceThreshold> oldList =
                oldThresholds != null ? oldThresholds : Collections.emptyList();
        List<PriceThreshold> newList =
                newThresholds != null ? newThresholds : Collections.emptyList();

        if (oldList.size() != newList.size()) {
            changes.put(configKey + "_price_thresholds_count", new ChangeDiff(
                    String.valueOf(oldList.size()),
                    String.valueOf(newList.size())
            ));
        }

        // Compare individual thresholds
        int maxSize = Math.max(oldList.size(), newList.size());
        for (int i = 0; i < maxSize; i++) {
            String thresholdKey = configKey + "_threshold_" + i;
            PriceThreshold oldThreshold =
                    i < oldList.size() ? oldList.get(i) : null;
            PriceThreshold newThreshold =
                    i < newList.size() ? newList.get(i) : null;

            detectThresholdChanges(changes, thresholdKey, oldThreshold, newThreshold);
        }
    }

    private void detectCategoryLimitChanges(
        Map<String, ChangeDiff> changes,
        String configKey,
        Map<String, CategoryLimit> oldLimits,
        Map<String, CategoryLimit> newLimits
    ) {
        // Handle null cases
        if (oldLimits == null && newLimits == null) {
            return;
        }

        // Get all unique categories
        Set<String> allCategories = new HashSet<>();
        if (oldLimits != null) allCategories.addAll(oldLimits.keySet());
        if (newLimits != null) allCategories.addAll(newLimits.keySet());

        // Iterate through categories and detect changes
        for (String category : allCategories) {
            CategoryLimit oldLimit =
                oldLimits != null ? oldLimits.get(category) : null;
            CategoryLimit newLimit =
                newLimits != null ? newLimits.get(category) : null;

            // Compare specific attributes of CategoryLimit
            if (oldLimit == null && newLimit == null) continue;

            // Example: Compare max price limit
            if (!Objects.equals(
                oldLimit != null ? oldLimit.getMaxPrice() : null, 
                newLimit != null ? newLimit.getMaxPrice() : null
            )) {
                changes.put(configKey + "_" + category + "_max_price_limit", 
                    new ChangeDiff(
                        oldLimit != null ? oldLimit.getMaxPrice().toString() : "null",
                        newLimit != null ? newLimit.getMaxPrice().toString() : "null"
                    )
                );
            }

            // Add more comparisons as needed for other attributes
        }
    }
    

    private void detectCategoryMarginChange(
        Map<String, ChangeDiff> changes, 
        String configKey, 
        MarginConstraints.CategoryMargin oldMargin, 
        MarginConstraints.CategoryMargin newMargin
    ) {
        if (oldMargin == null && newMargin == null) {
            return;
        }

        if (oldMargin == null || newMargin == null) {
            changes.put(configKey + "_category_margin", new ChangeDiff(
                oldMargin != null ? "Present" : "Null",
                newMargin != null ? "Present" : "Null"
            ));
            return;
        }

        // Compare min margin
        if (!Objects.equals(oldMargin.getMinMargin(), newMargin.getMinMargin())) {
            changes.put(configKey + "_min_margin", new ChangeDiff(
                formatPercentage(oldMargin.getMinMargin()),
                formatPercentage(newMargin.getMinMargin())
            ));
        }

        // Compare max margin
        if (!Objects.equals(oldMargin.getMaxMargin(), newMargin.getMaxMargin())) {
            changes.put(configKey + "_max_margin", new ChangeDiff(
                formatPercentage(oldMargin.getMaxMargin()),
                formatPercentage(newMargin.getMaxMargin())
            ));
        }

        // Compare target margin
        if (!Objects.equals(oldMargin.getTargetMargin(), newMargin.getTargetMargin())) {
            changes.put(configKey + "_target_margin", new ChangeDiff(
                formatPercentage(oldMargin.getTargetMargin()),
                formatPercentage(newMargin.getTargetMargin())
            ));
        }

        // Compare enforce strict flag
        if (!Objects.equals(oldMargin.getEnforceStrict(), newMargin.getEnforceStrict())) {
            changes.put(configKey + "_enforce_strict", new ChangeDiff(
                oldMargin.getEnforceStrict() != null ? oldMargin.getEnforceStrict().toString() : "null",
                newMargin.getEnforceStrict() != null ? newMargin.getEnforceStrict().toString() : "null"
            ));
        }

        // Compare excluded products
        if (!Objects.equals(oldMargin.getExcludedProducts(), newMargin.getExcludedProducts())) {
            changes.put(configKey + "_excluded_products", new ChangeDiff(
                oldMargin.getExcludedProducts() != null ? oldMargin.getExcludedProducts().toString() : "null",
                newMargin.getExcludedProducts() != null ? newMargin.getExcludedProducts().toString() : "null"
            ));
        }

        // Compare additional rules
        if (!Objects.equals(oldMargin.getAdditionalRules(), newMargin.getAdditionalRules())) {
            changes.put(configKey + "_additional_rules", new ChangeDiff(
                oldMargin.getAdditionalRules() != null ? oldMargin.getAdditionalRules().toString() : "null",
                newMargin.getAdditionalRules() != null ? newMargin.getAdditionalRules().toString() : "null"
            ));
        }
    }

    private void detectTimeWindowChanges(
            Map<String, ChangeDiff> changes,
            String configKey,
            TimeConstraints oldConstraints,
            TimeConstraints newConstraints) {

        if (oldConstraints == null && newConstraints == null) {
            return;
        }

        if (oldConstraints == null || newConstraints == null) {
            changes.put(configKey + "_time_constraints", new ChangeDiff(
                    oldConstraints != null ? "Present" : "Null",
                    newConstraints != null ? "Present" : "Null"
            ));
            return;
        }

        // Check allowed days
        if (!Objects.equals(oldConstraints.getAllowedDays(),
                newConstraints.getAllowedDays())) {
            changes.put(configKey + "_allowed_days", new ChangeDiff(
                    formatDays(oldConstraints.getAllowedDays()),
                    formatDays(newConstraints.getAllowedDays())
            ));
        }

        // Check time window
        if (!Objects.equals(oldConstraints.getMainStartTime(),
                newConstraints.getMainStartTime()) ||
                !Objects.equals(oldConstraints.getMainEndTime(),
                        newConstraints.getMainEndTime())) {
            changes.put(configKey + "_time_window", new ChangeDiff(
                formatTimeWindow(
                    oldConstraints.getMainStartTime().atDate(LocalDate.now()).toInstant(ZoneOffset.UTC),
                    oldConstraints.getMainEndTime().atDate(LocalDate.now()).toInstant(ZoneOffset.UTC)
                ),
                formatTimeWindow(
                    newConstraints.getMainStartTime().atDate(LocalDate.now()).toInstant(ZoneOffset.UTC),
                    newConstraints.getMainEndTime().atDate(LocalDate.now()).toInstant(ZoneOffset.UTC)
                )
            ));
        }

        // Compare special windows
        detectSpecialWindowChanges(changes, configKey,
                oldConstraints.getSpecialWindows(),
                newConstraints.getSpecialWindows()
        );
    }

    private void detectBlackoutPeriodChange(
        Map<String, ChangeDiff> changes,
        String configKey,
        TimeConstraints oldTimeConstraints,
        TimeConstraints newTimeConstraints
) {
    // If both periods are null, no change
    if (oldTimeConstraints == null && newTimeConstraints == null) {
        return;
    }

    // If one period is null and the other is not, mark as a complete change
    if (oldTimeConstraints == null || newTimeConstraints == null) {
        changes.put(configKey, new ChangeDiff(
                oldTimeConstraints != null ? oldTimeConstraints.toString() : "null",
                newTimeConstraints != null ? newTimeConstraints.toString() : "null"
        ));
        return;
    }

    // Compare individual attributes of the blackout period
    if (!Objects.equals(oldTimeConstraints.getBlackoutPeriods(), newTimeConstraints.getBlackoutPeriods())) {
        changes.put(configKey + "_blackout_periods", new ChangeDiff(
                oldTimeConstraints.getBlackoutPeriods().toString(),
                newTimeConstraints.getBlackoutPeriods().toString()
        ));
    }
}

    private Map<String, TimeConstraints.BlackoutPeriod> mapBlackoutPeriods(
            List<TimeConstraints.BlackoutPeriod> periods) {
        return periods.stream()
                .collect(Collectors.toMap(
                        period -> period.getStartTime().toString(),
                        Function.identity(),
                        (p1, p2) -> p1
                ));
    }

    private String formatDays(Set<DayOfWeek> days) {
        if (days == null || days.isEmpty()) {
            return null;
        }
        return days.stream()
                .map(DayOfWeek::name)
                .sorted()
                .collect(Collectors.joining(","));
    }

    private String formatTimeWindow(Instant start, Instant end) {
        if (start == null || end == null) {
            return null;
        }
        return start + "-" + end;
    }

    private void detectThresholdChanges(
            Map<String, ChangeDiff> changes,
            String thresholdKey,
            PriceThreshold oldThreshold,
            PriceThreshold newThreshold) {

        if (oldThreshold == null && newThreshold == null) {
            return;
        }

        if (oldThreshold == null || newThreshold == null) {
            changes.put(thresholdKey, new ChangeDiff(
                    oldThreshold != null ? "Present" : "Null",
                    newThreshold != null ? "Present" : "Null"
            ));
            return;
        }

        if (!Objects.equals(oldThreshold.getFromPrice(), newThreshold.getFromPrice()) ||
                !Objects.equals(oldThreshold.getToPrice(), newThreshold.getToPrice())) {
            changes.put(thresholdKey + "_range", new ChangeDiff(
                    formatPriceRange(oldThreshold.getFromPrice(), oldThreshold.getToPrice()),
                    formatPriceRange(newThreshold.getFromPrice(), newThreshold.getToPrice())
            ));
        }

        if (!Objects.equals(oldThreshold.getMaxChangePercentage(),
                newThreshold.getMaxChangePercentage())) {
            changes.put(thresholdKey + "_max_change", new ChangeDiff(
                    formatPercentage(oldThreshold.getMaxChangePercentage()),
                    formatPercentage(newThreshold.getMaxChangePercentage())
            ));
        }
    }

    private String formatPriceRange(BigDecimal from, BigDecimal to) {
        return from + "-" + to;
    }

    private void detectMarginConstraintChanges(
            Map<String, ChangeDiff> changes,
            String configKey,
            MarginConstraints oldMarginConstraints,
            MarginConstraints newMarginConstraints) {
        
        // Check if both old and new margin constraints are not null
        if (oldMarginConstraints != null && newMarginConstraints != null) {
            // Detect changes in category margins
            if (oldMarginConstraints.getCategoryMargins() != null || newMarginConstraints.getCategoryMargins() != null) {
                detectCategoryMarginChanges(
                        changes,
                        configKey,
                        oldMarginConstraints.getCategoryMargins(),
                        newMarginConstraints.getCategoryMargins()
                );
            }
        }
    }

    private void detectCategoryMarginChanges(
            Map<String, ChangeDiff> changes,
            String configKey,
            Map<String, MarginConstraints.CategoryMargin> oldMargins,
            Map<String, MarginConstraints.CategoryMargin> newMargins
    ) {
        // Iterate through old margins and compare with new margins
        for (Map.Entry<String, MarginConstraints.CategoryMargin> entry : oldMargins.entrySet()) {
            String categoryKey = entry.getKey();
            MarginConstraints.CategoryMargin oldMargin = entry.getValue();
            MarginConstraints.CategoryMargin newMargin = newMargins.get(categoryKey);
            
            // If margin for this category exists in new margins, detect changes
            if (newMargin != null) {
                detectCategoryMarginChange(changes, configKey + ".category." + categoryKey, oldMargin, newMargin);
            }
        }
        
        // Check for any new categories added in the new margins
        for (Map.Entry<String, MarginConstraints.CategoryMargin> entry : newMargins.entrySet()) {
            String categoryKey = entry.getKey();
            if (!oldMargins.containsKey(categoryKey)) {
                detectCategoryMarginChange(changes, configKey + ".category." + categoryKey, null, entry.getValue());
            }
        }
    }

    private void detectSpecialWindowChanges(
            Map<String, ChangeDiff> changes,
            String configKey,
            Map<String, TimeConstraints.SpecialTimeWindow> oldSpecialWindows,
            Map<String, TimeConstraints.SpecialTimeWindow> newSpecialWindows
    ) {
        // Handle null cases
        if (oldSpecialWindows == null && newSpecialWindows == null) {
            return;
        }

        // If one of the maps is null, treat it as an empty map
        Map<String, TimeConstraints.SpecialTimeWindow> oldWindows = 
            oldSpecialWindows != null ? oldSpecialWindows : Collections.emptyMap();
        Map<String, TimeConstraints.SpecialTimeWindow> newWindows = 
            newSpecialWindows != null ? newSpecialWindows : Collections.emptyMap();

        // Check for added or removed special windows
        Set<String> oldKeys = oldWindows.keySet();
        Set<String> newKeys = newWindows.keySet();

        // Detect removed special windows
        Sets.difference(oldKeys, newKeys).forEach(removedKey -> {
            changes.put(configKey + "_special_window_removed_" + removedKey, 
                new ChangeDiff(oldWindows.get(removedKey).toString(), null));
        });

        // Detect added special windows
        Sets.difference(newKeys, oldKeys).forEach(addedKey -> {
            changes.put(configKey + "_special_window_added_" + addedKey, 
                new ChangeDiff(null, newWindows.get(addedKey).toString()));
        });

        // Compare existing special windows
        Sets.intersection(oldKeys, newKeys).forEach(commonKey -> {
            TimeConstraints.SpecialTimeWindow oldWindow = oldWindows.get(commonKey);
            TimeConstraints.SpecialTimeWindow newWindow = newWindows.get(commonKey);

            if (!Objects.equals(oldWindow, newWindow)) {
                changes.put(configKey + "_special_window_" + commonKey, 
                    new ChangeDiff(oldWindow.toString(), newWindow.toString()));
            }
        });
    }

    public Map<String, ChangeType> mapBlackoutPeriods(
            Map<String, TimeConstraints.BlackoutPeriod> oldBlackouts,
            Map<String, TimeConstraints.BlackoutPeriod> newBlackouts) {
        
        Map<String, ChangeType> changes = new HashMap<>();
        
        // Handle null maps
        final Map<String, TimeConstraints.BlackoutPeriod> oldBlackoutsMap = oldBlackouts != null ? oldBlackouts : new HashMap<>();
        final Map<String, TimeConstraints.BlackoutPeriod> newBlackoutsMap = newBlackouts != null ? newBlackouts : new HashMap<>();
        
        // Find additions and modifications
        newBlackoutsMap.forEach((id, blackout) -> {
            if (!oldBlackoutsMap.containsKey(id)) {
                changes.put(id, ChangeType.ADDED);
            } else if (hasBlackoutChanged(oldBlackoutsMap.get(id), blackout)) {
                changes.put(id, ChangeType.MODIFIED);
            }
        });
        
        // Find removals
        oldBlackoutsMap.keySet().forEach(id -> {
            if (!newBlackoutsMap.containsKey(id)) {
                changes.put(id, ChangeType.REMOVED);
            }
        });
        
        return changes;
    }
    
    public Map<String, ChangeType> mapSpecialWindows(
            Map<String, TimeConstraints.SpecialTimeWindow> oldWindows,
            Map<String, TimeConstraints.SpecialTimeWindow> newWindows) {
        
        Map<String, ChangeType> changes = new HashMap<>();
        
        // Handle null maps
        final Map<String, TimeConstraints.SpecialTimeWindow> safeOldWindows = oldWindows != null ? oldWindows : new HashMap<>();
        final Map<String, TimeConstraints.SpecialTimeWindow> safeNewWindows = newWindows != null ? newWindows : new HashMap<>();
        
        // Find additions
        safeNewWindows.forEach((id, window) -> {
            if (!safeOldWindows.containsKey(id)) {
                changes.put(id, ChangeType.ADDED);
            }
        });
        
        // Find removals 
        safeOldWindows.forEach((id, window) -> {
            if (!safeNewWindows.containsKey(id)) {
                changes.put(id, ChangeType.REMOVED);
            }
        });
        
        // Find modifications
        safeOldWindows.forEach((id, oldWindow) -> {
            if (safeNewWindows.containsKey(id)) {
                TimeConstraints.SpecialTimeWindow newWindow = safeNewWindows.get(id);
                if (hasWindowChanged(oldWindow, newWindow)) {
                    changes.put(id, ChangeType.MODIFIED);
                }
            }
        });
        
        return changes;
    }
    
    private boolean hasBlackoutChanged(TimeConstraints.BlackoutPeriod old, TimeConstraints.BlackoutPeriod current) {
        if (old == null || current == null) return true;
        
        return !Objects.equals(old.getStartTime(), current.getStartTime()) ||
               !Objects.equals(old.getEndTime(), current.getEndTime()) ||
               !Objects.equals(old.getReason(), current.getReason()) ||
               !Objects.equals(old.getType(), current.getType()) ||
               !Objects.equals(old.getDescription(), current.getDescription()) ||
               !Objects.equals(old.getAffectedServices(), current.getAffectedServices());
    }
    
    private boolean hasWindowChanged(TimeConstraints.SpecialTimeWindow old, TimeConstraints.SpecialTimeWindow current) {
        if (old == null || current == null) return true;
        
        return !Objects.equals(old.getStartTime(), current.getStartTime()) ||
               !Objects.equals(old.getEndTime(), current.getEndTime()) ||
               !Objects.equals(old.getWindowName(), current.getWindowName()) ||
               !Objects.equals(old.getAdjustmentFactor(), current.getAdjustmentFactor()) ||
               !Objects.equals(old.getSpecialRuleId(), current.getSpecialRuleId()) ||
               !Objects.equals(old.getWindowType(), current.getWindowType());
    }

    public enum ChangeType {
        ADDED,
        REMOVED,
        MODIFIED
    }
}