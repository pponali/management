package com.scaler.price.rule.domain;

import com.fasterxml.jackson.databind.JsonNode;
import com.scaler.price.core.management.domain.AuditInfo;
import com.vladmihalcea.hibernate.type.json.JsonBinaryType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import org.hibernate.annotations.Type;

import java.time.LocalDateTime;

@Entity
@Table(name = "rule_history")
@Setter
@Getter
@SuperBuilder
@AllArgsConstructor
@NoArgsConstructor
public class RuleHistory extends AuditInfo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long ruleId;

    @Column(nullable = false)
    private Long ruleVersion;

    @Type(JsonBinaryType.class)
    @Column(columnDefinition = "jsonb")
    private JsonNode changeSummary;

    @Column(nullable = false)
    private byte[] ruleSnapshot;

    @Column(nullable = false)
    private String userId;

    @Column(nullable = false)
    private String comment;

    @Column(nullable = false)
    private LocalDateTime timestamp;

    @Column(nullable = false)
    private LocalDateTime cutoffDate;

    @Column
    private String batchId;

    @Column
    private Long productId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ChangeType changeType;

    @PrePersist
    protected void onCreate() {
        super.onCreate();
        if (timestamp == null) {
            timestamp = LocalDateTime.now();
        }
    }
}
