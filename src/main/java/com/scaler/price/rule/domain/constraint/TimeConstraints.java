package com.scaler.price.rule.domain.constraint;

import com.scaler.price.rule.domain.PriceAdjustment;
import com.vladmihalcea.hibernate.type.json.JsonBinaryType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.Type;

import java.math.BigDecimal;
import java.time.*;
import java.util.*;

@Embeddable
@SuperBuilder
@NoArgsConstructor
@DiscriminatorValue("time_constraints")
@AllArgsConstructor
public class TimeConstraints extends RuleConstraints{

    public enum BlackoutType {
        HOLIDAY,
        MAINTENANCE,
        BUSINESS_CLOSURE,
        SPECIAL_EVENT,
        EMERGENCY
    }

    public enum WindowType {
        PEAK_HOURS,
        OFF_PEAK_HOURS,
        SPECIAL_EVENT,
        PROMOTIONAL
    }

    public enum SlotStatus {
        ACTIVE,
        INACTIVE,
        FULL,
        MAINTENANCE
    }

    @ElementCollection
    @CollectionTable(name = "time_constraints_allowed_days")
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private Set<DayOfWeek> allowedDays = new HashSet<>();

    @ElementCollection
    @CollectionTable(name = "special_time_windows")
    @Builder.Default
    private Map<String, SpecialTimeWindow> specialWindows = new HashMap<>();

    @Column
    private LocalTime mainStartTime;

    @Column
    private LocalTime mainEndTime;

    @Column
    private String timezoneId;

    @Column
    private String timezone;

    @Column
    private String minDuration;

    @Column
    private String maxDuration;

    @Column
    private Long categoryId;

    @Column
    private Date lastModifiedDate;

    @ElementCollection
    @CollectionTable(name = "blackout_periods")
    @Builder.Default
    private List<BlackoutPeriod> blackoutPeriods = new ArrayList<>();

    @Type(JsonBinaryType.class)
    @Column(columnDefinition = "jsonb")
    private Map<String, TimeSlot> timeSlots = new HashMap<>();

    @Type(JsonBinaryType.class)
    @Column(columnDefinition = "jsonb")
    private Map<String, List<TimeWindow>> weeklySchedule = new HashMap<>();

    @Type(JsonBinaryType.class)
    @Column(columnDefinition = "jsonb")
    private Map<String, List<TimeWindow>> dateSpecificSchedule = new HashMap<>();

    @Type(JsonBinaryType.class)
    @Column(columnDefinition = "jsonb")
    private Map<String, SpecialTimeWindow> specialSchedules = new HashMap<>();

    @Type(JsonBinaryType.class)
    @Column(columnDefinition = "jsonb")
    private Map<String, BlackoutPeriod> blackoutPeriodMap = new HashMap<>();

    @Embeddable
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BlackoutPeriod {
        private Instant startDate;
        private Instant endDate;
        private String reason;
        
        @Enumerated(EnumType.STRING)
        private BlackoutType type;
        
        private String alternateRuleId;
        
        @Embedded
        private PriceAdjustment priceAdjustment;
        
        @ElementCollection
        private Set<String> applicableCategories;

        // New field to track days of the week
        @ElementCollection
        @Enumerated(EnumType.STRING)
        private Set<DayOfWeek> applicableDays;

        @Type(JsonBinaryType.class)
        @Column(columnDefinition = "jsonb")
        private Map<String, String> blackoutProperties = new HashMap<>();

        public Set<DayOfWeek> getDaysOfWeek() {
            return this.applicableDays != null ? this.applicableDays : Collections.emptySet();
        }

    }

    @Embeddable
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SpecialTimeWindow {
        private String windowName;
        private LocalTime startTime;
        private LocalTime endTime;
        
        @ElementCollection
        private Set<DayOfWeek> applicableDays;
        
        private BigDecimal adjustmentFactor;
        private String specialRuleId;
        
        @Enumerated(EnumType.STRING)
        private WindowType windowType;
        
        @Type(JsonBinaryType.class)
        @Column(columnDefinition = "jsonb")
        private Map<String, Object> customSettings;

        @Type(JsonBinaryType.class)
        @Column(columnDefinition = "jsonb")
        private Map<String, String> overrideRules = new HashMap<>();
    }

    @Embeddable
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TimeSlot {
        private String slotName;
        private LocalTime startTime;
        private LocalTime endTime;
        private Integer maxCapacity;
        private BigDecimal priceMultiplier;
        
        @ElementCollection
        private Set<DayOfWeek> applicableDays;
        
        @Enumerated(EnumType.STRING)
        private SlotStatus status;
    }

    @Embeddable
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TimeWindow {
        private LocalTime startTime;
        private LocalTime endTime;
        private String description;
        
        @Type(JsonBinaryType.class)
        @Column(columnDefinition = "jsonb")
        private Map<String, String> additionalProperties = new HashMap<>();
    }

    @Embeddable
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SpecialTimeWindowExt extends TimeWindow {
        private LocalDate startDate;
        private LocalDate endDate;
        private String reason;
        private Priority priority;
        
        @Type(JsonBinaryType.class)
        @Column(columnDefinition = "jsonb")
        private Map<String, String> overrideRules = new HashMap<>();
    }

    @Embeddable
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BlackoutPeriodExt {
        private Instant startTime;
        private Instant endTime;
        private BlackoutType type;
        private String reason;
        
        @Type(JsonBinaryType.class)
        @Column(columnDefinition = "jsonb")
        private Map<String, String> blackoutProperties = new HashMap<>();
    }

    public enum Priority {
        LOW,
        MEDIUM,
        HIGH,
        CRITICAL
    }

    public Set<DayOfWeek> getAllowedDays() {
        return this.allowedDays != null ? this.allowedDays : Collections.emptySet();
    }

    public List<BlackoutPeriod> getBlackoutPeriods() {
        return this.blackoutPeriods != null ? this.blackoutPeriods : Collections.emptyList();
    }

    public void setBlackoutPeriods(List<BlackoutPeriod> blackoutPeriods) {
        this.blackoutPeriods = blackoutPeriods != null ? blackoutPeriods : new ArrayList<>();
    }

    public boolean isTimeWithinConstraints(LocalDateTime dateTime) {
        try {
            if (allowedDays != null && !allowedDays.contains(dateTime.getDayOfWeek())) {
                return false;
            }

            LocalTime time = dateTime.toLocalTime();
            if (mainStartTime != null && mainEndTime != null) {
                if (mainStartTime.isBefore(mainEndTime)) {
                    return !time.isBefore(mainStartTime) && !time.isAfter(mainEndTime);
                } else {
                    // Handle overnight time ranges
                    return !time.isBefore(mainStartTime) || !time.isAfter(mainEndTime);
                }
            }

            return true;
        } catch (Exception e) {
            throw new IllegalStateException("Error checking time constraints", e);
        }
    }

    public boolean isWithinBlackoutPeriod(Instant instant) {
        try {
            if (blackoutPeriods == null || blackoutPeriods.isEmpty()) {
                return false;
            }

            return blackoutPeriods.stream()
                    .anyMatch(period -> 
                            (!period.startDate.isAfter(instant)) &&
                            (period.endDate == null || !period.endDate.isBefore(instant)));
        } catch (Exception e) {
            throw new IllegalStateException("Error checking blackout period", e);
        }
    }

    public Optional<SpecialTimeWindow> getApplicableSpecialWindow(LocalDateTime dateTime) {
        try {
            return specialWindows.values().stream()
                    .filter(window -> isWindowApplicable(window, dateTime))
                    .findFirst();
        } catch (Exception e) {
            throw new IllegalStateException("Error finding applicable special window", e);
        }
    }

    private boolean isWindowApplicable(SpecialTimeWindow window, LocalDateTime dateTime) {
        return window.getApplicableDays().contains(dateTime.getDayOfWeek()) &&
                !dateTime.toLocalTime().isBefore(window.getStartTime()) &&
                !dateTime.toLocalTime().isAfter(window.getEndTime());
    }

    public Optional<TimeSlot> getAvailableTimeSlot(LocalDateTime dateTime) {
        try {
            return timeSlots.values().stream()
                    .filter(slot -> isSlotAvailable(slot, dateTime))
                    .findFirst();
        } catch (Exception e) {
            throw new IllegalStateException("Error finding available time slot", e);
        }
    }

    private boolean isSlotAvailable(TimeSlot slot, LocalDateTime dateTime) {
        return slot.getStatus() == SlotStatus.ACTIVE &&
                slot.getApplicableDays().contains(dateTime.getDayOfWeek()) &&
                !dateTime.toLocalTime().isBefore(slot.getStartTime()) &&
                !dateTime.toLocalTime().isAfter(slot.getEndTime());
    }

    public LocalTime getMainStartTime() {
        return this.mainStartTime;
    }

    public LocalTime getMainEndTime() {
        return this.mainEndTime;
    }

    public void setMainStartTime(LocalTime startTime) {
        this.mainStartTime = startTime;
    }

    public void setMainEndTime(LocalTime endTime) {
        this.mainEndTime = endTime;
    }

    public Map<String, SpecialTimeWindow> getSpecialWindows() {
        return specialWindows;
    }

    public String getMinDuration() {
        return this.minDuration;
    }

    public void setMinDuration(String minDuration) {
        this.minDuration = minDuration;
    }

    public String getMaxDuration() {
        return this.maxDuration;
    }

    public void setMaxDuration(String maxDuration) {
        this.maxDuration = maxDuration;
    }

    public Long getCategoryId() {
        return this.categoryId;
    }
}
