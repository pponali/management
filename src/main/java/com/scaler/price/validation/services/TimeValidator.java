package com.scaler.price.validation.services;

import com.scaler.price.core.management.utils.PriceServiceMetrics;
import com.scaler.price.rule.config.ConfigurationService;
import com.scaler.price.rule.domain.DiscountAction;
import com.scaler.price.rule.domain.DiscountType;
import com.scaler.price.rule.domain.RuleCondition;
import com.scaler.price.rule.dto.TimeRestrictions;
import com.scaler.price.rule.exceptions.RuleValidationException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.*;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;

@Component
@Slf4j
@RequiredArgsConstructor
public class TimeValidator {
    private final ConfigurationService configService;
    private final PriceServiceMetrics metricsService;

    private static final Pattern TIME_PATTERN =
            Pattern.compile("^([01]?[0-9]|2[0-3]):[0-5][0-9]$");

    public void validateTimeCondition(RuleCondition condition) throws RuleValidationException {
        try {
            validateTimeString(condition.getValue());
            validateBusinessHours(LocalTime.parse(condition.getValue()));
            metricsService.recordTimeValidation();
        } catch (Exception e) {
            metricsService.recordTimeValidationFailure();
            throw new RuleValidationException("Invalid time condition: " + e.getMessage());
        }
    }

    public void validateTimeRestrictions(TimeRestrictions restrictions) throws RuleValidationException {
        if (restrictions == null) {
            return;
        }
        
        validateTimeRange(restrictions.getStartTime(), restrictions.getEndTime());
        validateDaysOfWeek(restrictions.getDaysOfWeek());
        validateBlackoutDates(restrictions.getBlackoutDates());
    }

    private void validateTimeRange(LocalTime startTime, LocalTime endTime) throws RuleValidationException {
        if (startTime != null && endTime != null && !startTime.isBefore(endTime)) {
            throw new RuleValidationException("Start time must be before end time");
        }
    }

    private void validateTimeString(String timeStr) throws RuleValidationException {
        if (timeStr == null || !TIME_PATTERN.matcher(timeStr).matches()) {
            throw new RuleValidationException("Invalid time format. Use HH:mm format");
        }
    }

    private void validateBusinessHours(LocalTime time) throws RuleValidationException {
        LocalTime businessStart = LocalTime.of(
                configService.getBusinessHourStart(), 0
        );
        LocalTime businessEnd = LocalTime.of(
                configService.getBusinessHourEnd(), 0
        );

        if (time.isBefore(businessStart) || time.isAfter(businessEnd)) {
            throw new RuleValidationException(
                    "Time must be within business hours (" +
                            businessStart + "-" + businessEnd + ")"
            );
        }
    }

    private void validateDaysOfWeek(Set<DayOfWeek> days) throws RuleValidationException {
        if (days != null && days.isEmpty()) {
            throw new RuleValidationException(
                    "Days of week cannot be empty if specified"
            );
        }
    }

    private void validateBlackoutDates(List<TimeRestrictions.BlackoutDate> dates) throws RuleValidationException {
        if (dates == null || dates.isEmpty()) {
            return;
        }

        Set<LocalDate> uniqueDates = new HashSet<>();
        for (TimeRestrictions.BlackoutDate date : dates) {
            if (!uniqueDates.add(date.getDate())) {
                throw new RuleValidationException(
                        "Duplicate blackout date: " + date.getDate()
                );
            }

            if (date.getDate().isBefore(LocalDate.now())) {
                throw new RuleValidationException(
                        "Blackout date cannot be in the past: " + date.getDate()
                );
            }
        }
    }

    public void validateDiscountAction(DiscountAction action) throws RuleValidationException {
        if (action == null) {
            throw new RuleValidationException("Discount action cannot be null");
        }

        validateDiscountValue(action.getDiscountValue(), action.getDiscountType());
        
        if (!action.isStackable()) {
            validateNonStackableDiscount(action);
        } else {
            validateStackInterval(action.getStackInterval());
            validateMaxStackCount(action);
        }
    }

    public void validateNonStackableDiscount(DiscountAction action) throws RuleValidationException {
        if (action.getDiscountValue().compareTo(BigDecimal.ZERO) <= 0) {
            throw new RuleValidationException("Non-stackable discount value must be greater than zero");
        }
    }

    public void validateDiscountValue(BigDecimal discountValue, DiscountType discountType) throws RuleValidationException {
        if (discountValue == null || discountValue.compareTo(BigDecimal.ZERO) <= 0) {
            throw new RuleValidationException("Discount value must be greater than zero");
        }
        if ("PERCENTAGE".equals(discountType) && discountValue.compareTo(new BigDecimal("100")) > 0) {
            throw new RuleValidationException("Percentage discount cannot exceed 100%");
        }
    }

    public void validateStackInterval(String stackInterval) throws RuleValidationException {
        if (stackInterval == null || stackInterval.trim().isEmpty()) {
            throw new RuleValidationException("Stack interval cannot be null or empty");
        }
    }

    private void validateMaxStackCount(DiscountAction action) throws RuleValidationException {
        if (action.getMaxStackCount() <= 0) {
            throw new RuleValidationException("Maximum stack count must be greater than zero");
        }
        
        BigDecimal maxDiscount = calculateMaxDiscount(action);
        if ("PERCENTAGE".equals(action.getDiscountType()) && maxDiscount.compareTo(new BigDecimal("100")) > 0) {
            throw new RuleValidationException("Maximum stacked percentage discount cannot exceed 100%");
        }
    }

    private BigDecimal calculateMaxDiscount(DiscountAction action) {
        return action.getDiscountValue().multiply(new BigDecimal(action.getMaxStackCount()));
    }

    private BigDecimal calculateMarginAfterDiscount(BigDecimal currentPrice, BigDecimal costPrice, BigDecimal discountValue, String discountType) {
        BigDecimal discountAmount = "PERCENTAGE".equals(discountType) 
            ? currentPrice.multiply(discountValue).divide(new BigDecimal("100"), 2, BigDecimal.ROUND_HALF_UP) 
            : discountValue;
        return currentPrice.subtract(discountAmount).subtract(costPrice);
    }

    public boolean isValidTime(Instant startDate, Instant endDate, LocalDateTime now) {
        return now.isAfter(startDate.atZone(ZoneId.systemDefault()).toLocalDateTime()) &&
                now.isBefore(endDate.atZone(ZoneId.systemDefault()).toLocalDateTime());
    }

    public Optional<Object> findByCategoryId(Long categoryId) {
        return null;
    }
}
