package com.scaler.price.rule.dto;

import com.scaler.price.rule.domain.HolidayAction;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TimeRestrictions {
    private LocalTime startTime;
    private LocalTime endTime;
    private Set<DayOfWeek> daysOfWeek;
    private List<BlackoutDate> blackoutDates;
    private TimeZone timeZone;
    private HolidayCalendar holidayCalendar;

    @Data
    @Builder
    public static class BlackoutDate {
        private LocalDate date;
        private String reason;
        private BlackoutType type;
        private BlackoutAction action;
    }

    @Data
    @Builder
    public static class HolidayCalendar {
        private String calendarId;
        private Set<String> locations;
        private HolidayAction defaultAction;
        private Map<String, HolidayAction> specificActions;
    }

    public enum BlackoutType {
        HOLIDAY,
        MAINTENANCE,
        BUSINESS_CLOSURE,
        SPECIAL_EVENT
    }

    public enum BlackoutAction {
        DISABLE_PRICING,
        USE_DEFAULT_PRICE,
        APPLY_SPECIAL_PRICE,
        CUSTOM_ACTION
    }
}