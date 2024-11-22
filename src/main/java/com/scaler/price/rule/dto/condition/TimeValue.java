package com.scaler.price.rule.dto.condition;

import com.scaler.price.rule.dto.TimeRestrictions;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.DayOfWeek;
import java.util.List;
import java.util.Set;
import java.util.TimeZone;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TimeValue {
    private String startTime;
    private String endTime;
    private Set<DayOfWeek> daysOfWeek;
    private TimeZone timeZone;
    private List<TimeRestrictions.BlackoutDate> blackoutDates;
    private TimeRestrictionType restrictionType;

    public enum TimeRestrictionType {
        BUSINESS_HOURS,
        PEAK_HOURS,
        OFF_PEAK_HOURS,
        CUSTOM
    }
}
