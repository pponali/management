package com.scaler.price.rule.controllers;

import com.scaler.price.validation.services.TimeValidator;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

@RestController
@RequestMapping("/api/v1/validations/time")
@RequiredArgsConstructor
@Tag(name = "Time Validation", description = "APIs for time-based validation operations")
public class TimeValidationController {

    private final TimeValidator timeValidator;

    @Operation(summary = "Validate time range")
    @GetMapping("/validate")
    public ResponseEntity<Boolean> validateTimeRange(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant endDate,
            @RequestParam(required = false) String timeZone) {
        ZoneId zone = timeZone != null ? ZoneId.of(timeZone) : ZoneId.systemDefault();
        LocalDateTime now = LocalDateTime.now(zone);
        return ResponseEntity.ok(timeValidator.isValidTime(startDate, endDate, now));
    }
}