package com.scaler.price.rule.domain.constraint;

import com.scaler.price.core.management.domain.AuditInfo;
import com.scaler.price.rule.domain.PriceAdjustment;
import com.vladmihalcea.hibernate.type.json.JsonBinaryType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Type;

import java.math.BigDecimal;
import java.time.*;
import java.util.*;

@Embeddable
@Data
@Builder
@NoArgsConstructor
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

    @Column
    private LocalTime startTime;

    @Column
    private LocalTime endTime;

    @Column
    private String timezoneId;

    @Column
    private String timezone;

    @Column
    private String minDuration;

    @Column
    private String maxDuration;

    @Column
    private String categoryId;

    @Column
    private Date lastModifiedDate;

    @ElementCollection
    @CollectionTable(name = "blackout_periods")
    @Builder.Default
    private List<BlackoutPeriod> blackoutPeriods = new ArrayList<>();

    @ElementCollection
    @CollectionTable(name = "special_time_windows")
    @Builder.Default
    private Map<String, SpecialTimeWindow> specialWindows = new HashMap<>();

    @ElementCollection
    @CollectionTable(name = "time_slots")
    @Builder.Default
    private Map<String, TimeSlot> timeSlots = new HashMap<>();

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

    public boolean isTimeWithinConstraints(LocalDateTime dateTime) {
        try {
            if (allowedDays != null && !allowedDays.contains(dateTime.getDayOfWeek())) {
                return false;
            }

            LocalTime time = dateTime.toLocalTime();
            if (startTime != null && endTime != null) {
                if (startTime.isBefore(endTime)) {
                    return !time.isBefore(startTime) && !time.isAfter(endTime);
                } else {
                    // Handle overnight time ranges
                    return !time.isBefore(startTime) || !time.isAfter(endTime);
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
}
