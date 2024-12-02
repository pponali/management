package com.scaler.price.rule.domain.constraint;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.scaler.price.rule.domain.PriceAdjustment;
import com.scaler.price.rule.domain.constraint.TimeConstraints.BlackoutPeriod;
import com.scaler.price.rule.domain.constraint.TimeConstraints.SpecialTimeWindow;
import com.scaler.price.rule.domain.constraint.TimeConstraints.TimeSlot;
import com.scaler.price.rule.domain.constraint.TimeConstraints.TimeWindow;
import com.vladmihalcea.hibernate.type.json.JsonBinaryType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.Type;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.*;
import java.util.*;

@Entity
@DiscriminatorValue("time_constraints")
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@Setter
@Getter
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

    // JSON storage fields
    @Column(columnDefinition = "jsonb")
    private String timeSlotsJson;

    @Column(columnDefinition = "jsonb")
    private String weeklyScheduleJson;

    @Column(columnDefinition = "jsonb")
    private String dateSpecificScheduleJson;

    @Column(columnDefinition = "jsonb")
    private String specialSchedulesJson;

    @Column(columnDefinition = "jsonb")
    private String blackoutPeriodMapJson;

    // Transient fields for in-memory use
    @Transient
    private Map<String, TimeSlot> timeSlots = new HashMap<>();

    @Transient
    private Map<String, List<TimeWindow>> weeklySchedule = new HashMap<>();

    @Transient
    private Map<String, List<TimeWindow>> dateSpecificSchedule = new HashMap<>();

    @Transient
    private Map<String, SpecialTimeWindow> specialSchedules = new HashMap<>();

    @Transient
    private Map<String, BlackoutPeriod> blackoutPeriodMap = new HashMap<>();

    // JSON storage fields
    @Column(columnDefinition = "jsonb")
    private String specialWindowsJson;

    @Transient
    private Map<String, SpecialTimeWindow> specialWindows = new HashMap<>();

    @ElementCollection
    @CollectionTable(name = "time_constraints_allowed_days")
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private Set<DayOfWeek> allowedDays = new HashSet<>();

    @PostLoad
    public void loadJsonFields() {
        ObjectMapper mapper = new ObjectMapper();
        try {
            if (timeSlotsJson != null) {
                timeSlots = mapper.readValue(timeSlotsJson, 
                    new TypeReference<Map<String, TimeSlot>>() {});
            }
            if (weeklyScheduleJson != null) {
                weeklySchedule = mapper.readValue(weeklyScheduleJson, 
                    new TypeReference<Map<String, List<TimeWindow>>>() {});
            }
            if (dateSpecificScheduleJson != null) {
                dateSpecificSchedule = mapper.readValue(dateSpecificScheduleJson, 
                    new TypeReference<Map<String, List<TimeWindow>>>() {});
            }
            if (specialSchedulesJson != null) {
                specialSchedules = mapper.readValue(specialSchedulesJson, 
                    new TypeReference<Map<String, SpecialTimeWindow>>() {});
            }
            if (blackoutPeriodMapJson != null) {
                blackoutPeriodMap = mapper.readValue(blackoutPeriodMapJson, 
                    new TypeReference<Map<String, BlackoutPeriod>>() {});
            }
            if (specialWindowsJson != null) {
                specialWindows = mapper.readValue(specialWindowsJson, 
                    new TypeReference<Map<String, SpecialTimeWindow>>() {});
            }
        } catch (Exception e) {
            // Initialize with empty maps if JSON parsing fails
            timeSlots = new HashMap<>();
            weeklySchedule = new HashMap<>();
            dateSpecificSchedule = new HashMap<>();
            specialSchedules = new HashMap<>();
            blackoutPeriodMap = new HashMap<>();
            specialWindows = new HashMap<>();
        }
    }

    @PrePersist
    @PreUpdate
    public void saveJsonFields() {
        ObjectMapper mapper = new ObjectMapper();
        try {
            timeSlotsJson = mapper.writeValueAsString(timeSlots);
            weeklyScheduleJson = mapper.writeValueAsString(weeklySchedule);
            dateSpecificScheduleJson = mapper.writeValueAsString(dateSpecificSchedule);
            specialSchedulesJson = mapper.writeValueAsString(specialSchedules);
            blackoutPeriodMapJson = mapper.writeValueAsString(blackoutPeriodMap);
            specialWindowsJson = mapper.writeValueAsString(specialWindows);
        } catch (Exception e) {
            timeSlotsJson = "{}";
            weeklyScheduleJson = "{}";
            dateSpecificScheduleJson = "{}";
            specialSchedulesJson = "{}";
            blackoutPeriodMapJson = "{}";
            specialWindowsJson = "{}";
        }
    }

    // Add getter/setter for specialWindows
    public Map<String, SpecialTimeWindow> getSpecialWindows() {
        return specialWindows;
    }

    public void setSpecialWindows(Map<String, SpecialTimeWindow> specialWindows) {
        this.specialWindows = specialWindows != null ? specialWindows : new HashMap<>();
    }

    // Getters and setters for the maps (the transient fields)
    public Map<String, TimeSlot> getTimeSlots() {
        return timeSlots;
    }

    public void setTimeSlots(Map<String, TimeSlot> timeSlots) {
        this.timeSlots = timeSlots != null ? timeSlots : new HashMap<>();
    }

    public Map<String, List<TimeWindow>> getWeeklySchedule() {
        return weeklySchedule;
    }

    public void setWeeklySchedule(Map<String, List<TimeWindow>> weeklySchedule) {
        this.weeklySchedule = weeklySchedule != null ? weeklySchedule : new HashMap<>();
    }

    public Map<String, List<TimeWindow>> getDateSpecificSchedule() {
        return dateSpecificSchedule;
    }

    public void setDateSpecificSchedule(Map<String, List<TimeWindow>> dateSpecificSchedule) {
        this.dateSpecificSchedule = dateSpecificSchedule != null ? dateSpecificSchedule : new HashMap<>();
    }

    public Map<String, SpecialTimeWindow> getSpecialSchedules() {
        return specialSchedules;
    }

    public void setSpecialSchedules(Map<String, SpecialTimeWindow> specialSchedules) {
        this.specialSchedules = specialSchedules != null ? specialSchedules : new HashMap<>();
    }

    public Map<String, BlackoutPeriod> getBlackoutPeriodMap() {
        return blackoutPeriodMap;
    }

    public void setBlackoutPeriodMap(Map<String, BlackoutPeriod> blackoutPeriodMap) {
        this.blackoutPeriodMap = blackoutPeriodMap != null ? blackoutPeriodMap : new HashMap<>();
    }

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

    @Embeddable
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class BlackoutPeriod {
        private BlackoutType type;
        private Instant startTime;
        private Instant endTime;
        private String reason;
        private String description;
        
        @Column(columnDefinition = "jsonb")
        private String affectedServicesJson = "[]";
        
        @Transient
        private List<String> affectedServices = new ArrayList<>();
        
        @PostLoad
        public void loadJson() {
            ObjectMapper mapper = new ObjectMapper();
            try {
                if (affectedServicesJson != null) {
                    affectedServices = mapper.readValue(affectedServicesJson, 
                        new TypeReference<List<String>>() {});
                }
            } catch (Exception e) {
                affectedServices = new ArrayList<>();
            }
        }
        
        @PrePersist
        @PreUpdate
        public void saveJson() {
            ObjectMapper mapper = new ObjectMapper();
            try {
                affectedServicesJson = mapper.writeValueAsString(affectedServices);
            } catch (Exception e) {
                affectedServicesJson = "[]";
            }
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
    @Getter
    @Setter
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
                            (!period.startTime.isAfter(instant)) &&
                            (period.endTime == null || !period.endTime.isBefore(instant)));
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
