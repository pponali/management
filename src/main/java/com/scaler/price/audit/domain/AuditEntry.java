package com.scaler.price.audit.domain;

import com.scaler.price.core.management.domain.AuditInfo;
import com.scaler.price.rule.domain.AuditAction;
import com.scaler.price.rule.domain.ChangeDiff;
import com.scaler.price.rule.domain.RuleType;
import com.vladmihalcea.hibernate.type.json.JsonBinaryType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import org.hibernate.annotations.Type;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Entity
@Table(name = "rule_audit_log")
@Setter
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Audit Event Model")
public class AuditEntry extends AuditInfo{

    
    @Schema(description = "Rule ID", example = "1")
    @Column(nullable = false)
    private Long ruleId;
    
    @Column(nullable = false)
    private String ruleName;

    @Enumerated(EnumType.STRING)
    private RuleType ruleType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AuditAction action;

    @Type(JsonBinaryType.class)
    @Column(columnDefinition = "jsonb")
    private Map<String, ChangeDiff> changes;

    @Type(JsonBinaryType.class)
    @Column(columnDefinition = "jsonb")
    private String snapshot;

    @Column(nullable = false)
    private String performedBy;

    @Column(nullable = false)
    private LocalDateTime performedAt;

    @ElementCollection
    private List<String> sellerIds;

    @ElementCollection
    private List<String> siteIds;

    private Long version;

    private String ipAddress;

    private String userAgent;

    private String sessionId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AuditEventType eventType;

    @Column(nullable = false)
    private Instant timestamp;

    @Column(nullable = false)
    private String userId;

    @Column(columnDefinition = "TEXT")
    private String eventData;

    @Column(nullable = false)
    private String comment;

    @Column(nullable = false)
    private String source;

    private String correlationId;



}
