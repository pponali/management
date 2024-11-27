package com.scaler.price.audit.controller;

import com.scaler.price.audit.domain.AuditEntry;
import com.scaler.price.audit.domain.AuditEventType;
import com.scaler.price.audit.exception.AuditSearchException;
import com.scaler.price.audit.service.AuditService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/audit")
@RequiredArgsConstructor
@Tag(name = "Audit Management", description = "APIs for managing audit events and history")
public class AuditController {

    private final AuditService auditService;

    @Operation(
            summary = "Get audit events by criteria",
            description = "Retrieve audit events based on user ID, event type, and time range"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Found the audit events",
                    content = { @Content(schema = @Schema(implementation = Page.class)) }),
            @ApiResponse(responseCode = "400", description = "Invalid parameters supplied",
                    content = @Content),
            @ApiResponse(responseCode = "404", description = "Audit events not found",
                    content = @Content)
    })
    @GetMapping("/events")
    public ResponseEntity<Page<AuditEntry>> getAuditEvents(
            @RequestParam(required = false) String userId,
            @RequestParam(required = false) AuditEventType eventType,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant startTime,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant endTime,
            Pageable pageable) throws AuditSearchException {
        return ResponseEntity.ok(auditService.findByComplexCriteria(
                userId, eventType, startTime, endTime, pageable));
    }

    @Operation(summary = "Get audit events by user",
            description = "Retrieve audit events based on user ID")

    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Found the audit events",
                    content = { @Content(schema = @Schema(implementation = List.class)) }),
            @ApiResponse(responseCode = "400", description = "Invalid user ID supplied",
                    content = @Content),
            @ApiResponse(responseCode = "404", description = "Audit events not found",
                    content = @Content)
    })
    @GetMapping("/events/user/{userId}")
    public ResponseEntity<List<AuditEntry>> getAuditEventsByUser(
            @PathVariable String userId) {
        return ResponseEntity.ok(auditService.findByUserId(userId));
    }

    @Operation(summary = "Get audit events by type",
            description = "Retrieve audit events based on event type")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Found the audit events",
                    content = { @Content(schema = @Schema(implementation = List.class)) }),
            @ApiResponse(responseCode = "400", description = "Invalid event type supplied",
                    content = @Content),
            @ApiResponse(responseCode = "404", description = "Audit events not found",
                    content = @Content)
    })
    @GetMapping("/events/type/{eventType}")
    public ResponseEntity<List<AuditEntry>> getAuditEventsByType(
            @PathVariable AuditEventType eventType) {
        return ResponseEntity.ok(auditService.findByEventType(eventType));
    }

    @Operation(summary = "Get audit event statistics",
            description = "Retrieve statistics of audit events within a time range")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Found the audit event statistics",
                    content = { @Content(schema = @Schema(implementation = Map.class)) }),
            @ApiResponse(responseCode = "400", description = "Invalid time range supplied",
                    content = @Content),
            @ApiResponse(responseCode = "404", description = "Audit event statistics not found",
                    content = @Content)
    })
    @GetMapping("/statistics")
    public ResponseEntity<Map<String, Object>> getAuditStatistics(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant startTime,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant endTime) {
        return ResponseEntity.ok(auditService.getEventStatistics(startTime, endTime));
    }
}