package com.scaler.price.core.management.utils;

import com.scaler.price.rule.domain.ChangeDiff;
import com.scaler.price.rule.domain.SellerSiteConfig;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Map;
import java.util.Set;

class ConfigChangeDetectorTest {

    @BeforeEach
    void setUp() {
    }

    @AfterEach
    void tearDown() {
    }

    @org.junit.jupiter.api.Test
    void detect() {
    }

    @org.junit.jupiter.api.Test
    public void test_detectConfigChanges(
            Map<String, ChangeDiff> changes,
            Set<SellerSiteConfig> oldConfigs,
            Set<SellerSiteConfig> newConfigs) {
        //Arrange
        ConfigChangeDetector configChangeDetector = new ConfigChangeDetector();


        //Act

        //Assert



    }

    @Slf4j
    @Component
    public class ConfigChangeDetector {
        private Map<String, ChangeDiff> changes;
        private boolean hasChanges;
        private com.scaler.price.core.management.utils.ConfigChangeDetector.ChangeType changeType;
        private Instant changeTime;
        private String changeTimeFormatted;
        private TimeWindow changeTimeWindow; // Need to create this class
        private Instant changeTimeWindowStart;
        private Instant changeTimeWindowEnd;
        private String changeTimeWindowStartFormatted;
        private String changeTimeWindowEndFormatted;
        private Long changeTimeWindowStartEpoch;
        private Long changeTimeWindowEndEpoch;
        private String changeTimeWindowStartEpochFormatted;
        private String changeTimeWindowEndEpochFormatted;
    }

    @Data
    @AllArgsConstructor
    public class TimeWindow {
        private Instant start;
        private Instant end;

        public String toString() {
            return start + "-" + end;
        }
    }
}