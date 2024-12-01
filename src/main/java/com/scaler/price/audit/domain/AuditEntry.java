package com.scaler.price.audit.domain;

import com.scaler.price.core.management.domain.AuditInfo;
import com.scaler.price.rule.domain.AuditAction;
import com.scaler.price.rule.domain.ChangeDiff;
import com.scaler.price.rule.domain.RuleType;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.scaler.price.rule.mapper.ChangeDiffMapConverter;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;
import java.util.Map;

@Entity
@Table(name = "audit_log")
@Setter
@Getter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Audit Event Model")
public class AuditEntry extends AuditInfo {
    
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

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AuditEventType type;

    @Column(columnDefinition = "jsonb")
    private String data;

    @Column(name = "event_time")
    private LocalDateTime eventTime;

    @Column(columnDefinition = "jsonb")
    @Convert(converter = ChangeDiffMapConverter.class)
    private Map<String, ChangeDiff> changes;

    @Column(name = "user_id")
    private String userId;

    private String source;
    private String comment;

    private String snapshot;

    @Column(columnDefinition = "text")
    public String getSnapshot() {
        return snapshot;
    }

    public void setSnapshot(String snapshot) {
        this.snapshot = snapshot;
    }

    @PrePersist
    @PreUpdate
    protected void onSave() {
        if (changes != null) {
            try {
                ObjectMapper mapper = new ObjectMapper();
                data = mapper.writeValueAsString(changes);
            } catch (JsonProcessingException e) {
                throw new IllegalStateException("Failed to serialize changes to JSON", e);
            }
        }
    }

    @PostLoad
    protected void onLoad() {
        if (data != null) {
            try {
                ObjectMapper mapper = new ObjectMapper();
                changes = mapper.readValue(data, 
                    new com.fasterxml.jackson.core.type.TypeReference<Map<String, ChangeDiff>>() {});
            } catch (JsonProcessingException e) {
                throw new IllegalStateException("Failed to deserialize changes from JSON", e);
            }
        }
    }
}
