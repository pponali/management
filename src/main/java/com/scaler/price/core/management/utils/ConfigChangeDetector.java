package com.scaler.price.core.management.utils;

import com.scaler.price.rule.domain.ChangeDiff;
import com.scaler.price.rule.domain.SellerSiteConfig;
import com.scaler.price.rule.domain.constraint.MarginConstraints;
import com.scaler.price.rule.domain.constraint.PriceConstraints;
import com.scaler.price.rule.domain.constraint.TimeConstraints;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
@Slf4j
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
        detectTimeConstraintChanges(changes, configKey, oldConfig, newConfig);

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
            SellerSiteConfig oldConfig,
            SellerSiteConfig newConfig) {

        TimeConstraints oldTimeConstraints = oldConfig.getTimeConstraints();
        TimeConstraints newTimeConstraints = newConfig.getTimeConstraints();

        if (oldTimeConstraints != null || newTimeConstraints != null) {
            detectTimeWindowChanges(
                    changes,
                    configKey,
                    oldTimeConstraints,
                    newTimeConstraints
            );
            detectBlackoutPeriodChanges(
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
            List<PriceConstraints.PriceThreshold> oldThresholds,
            List<PriceConstraints.PriceThreshold> newThresholds) {

        if (oldThresholds == null && newThresholds == null) {
            return;
        }

        List<PriceConstraints.PriceThreshold> oldList =
                oldThresholds != null ? oldThresholds : Collections.emptyList();
        List<PriceConstraints.PriceThreshold> newList =
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
            PriceConstraints.PriceThreshold oldThreshold =
                    i < oldList.size() ? oldList.get(i) : null;
            PriceConstraints.PriceThreshold newThreshold =
                    i < newList.size() ? newList.get(i) : null;

            detectThresholdChanges(changes, thresholdKey, oldThreshold, newThreshold);
        }
    }

    private void detectCategoryMarginChanges(
            Map<String, ChangeDiff> changes,
            String configKey,
            Map<String, MarginConstraints.CategoryMargin> oldMargins,
            Map<String, MarginConstraints.CategoryMargin> newMargins) {

        Set<String> allCategories = new HashSet<>();
        if (oldMargins != null) allCategories.addAll(oldMargins.keySet());
        if (newMargins != null) allCategories.addAll(newMargins.keySet());

        for (String category : allCategories) {
            MarginConstraints.CategoryMargin oldMargin =
                    oldMargins != null ? oldMargins.get(category) : null;
            MarginConstraints.CategoryMargin newMargin =
                    newMargins != null ? newMargins.get(category) : null;

            detectCategoryMarginChange(changes,
                    configKey + "_category_" + category,
                    oldMargin,
                    newMargin
            );
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
        if (!Objects.equals(oldConstraints.getStartTime(),
                newConstraints.getStartTime()) ||
                !Objects.equals(oldConstraints.getEndTime(),
                        newConstraints.getEndTime())) {
            changes.put(configKey + "_time_window", new ChangeDiff(
                    formatTimeWindow(oldConstraints.getStartTime(),
                            oldConstraints.getEndTime()),
                    formatTimeWindow(newConstraints.getStartTime(),
                            newConstraints.getEndTime())
            ));
        }

        // Compare special windows
        detectSpecialWindowChanges(changes, configKey,
                oldConstraints.getSpecialWindows(),
                newConstraints.getSpecialWindows()
        );
    }

    private void detectBlackoutPeriodChanges(
            Map<String, ChangeDiff> changes,
            String configKey,
            TimeConstraints oldConstraints,
            TimeConstraints newConstraints) {

        if (oldConstraints == null || newConstraints == null) {
            return;
        }

        List<TimeConstraints.BlackoutPeriod> oldPeriods =
                oldConstraints.getBlackoutPeriods();
        List<TimeConstraints.BlackoutPeriod> newPeriods =
                newConstraints.getBlackoutPeriods();

        if (oldPeriods == null && newPeriods == null) {
            return;
        }

        List<TimeConstraints.BlackoutPeriod> oldList =
                oldPeriods != null ? oldPeriods : Collections.emptyList();
        List<TimeConstraints.BlackoutPeriod> newList =
                newPeriods != null ? newPeriods : Collections.emptyList();

        if (oldList.size() != newList.size()) {
            changes.put(configKey + "_blackout_periods_count", new ChangeDiff(
                    String.valueOf(oldList.size()),
                    String.valueOf(newList.size())
            ));
        }

        // Compare individual blackout periods
        Map<String, TimeConstraints.BlackoutPeriod> oldMap = mapBlackoutPeriods(oldList);
        Map<String, TimeConstraints.BlackoutPeriod> newMap = mapBlackoutPeriods(newList);

        Set<String> allPeriodKeys = new HashSet<>();
        allPeriodKeys.addAll(oldMap.keySet());
        allPeriodKeys.addAll(newMap.keySet());

        for (String periodKey : allPeriodKeys) {
            detectBlackoutPeriodChange(changes,
                    configKey + "_blackout_" + periodKey,
                    oldMap.get(periodKey),
                    newMap.get(periodKey)
            );
        }
    }

    private Map<String, TimeConstraints.BlackoutPeriod> mapBlackoutPeriods(
            List<TimeConstraints.BlackoutPeriod> periods) {
        return periods.stream()
                .collect(Collectors.toMap(
                        period -> period.getStartDate().toString(),
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

    private String formatTimeWindow(LocalTime start, LocalTime end) {
        if (start == null || end == null) {
            return null;
        }
        return start + "-" + end;
    }

    private void detectThresholdChanges(
            Map<String, ChangeDiff> changes,
            String thresholdKey,
            PriceConstraints.PriceThreshold oldThreshold,
            PriceConstraints.PriceThreshold newThreshold) {

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
}