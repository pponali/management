package com.scaler.price.audit.domain;

import com.scaler.price.rule.domain.AuditAction;
import com.scaler.price.rule.domain.ChangeDiff;
import com.scaler.price.rule.domain.RuleType;
import com.vladmihalcea.hibernate.type.json.JsonBinaryType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Type;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Entity
@Table(name = "rule_audit_log")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Audit Event Model")
public class AuditEntry {
    @Schema(description = "Audit Event ID", example = "1")
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
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

    @Column(nullable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;

    @PrePersist
    protected void onCreate() {
        Instant now = Instant.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }
}
