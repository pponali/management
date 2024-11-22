package com.scaler.price.rule.domain;


import com.scaler.price.rule.events.RuleEventType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "failed_events")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FailedEventEntity {
    @Id
    private String eventId;

    @Enumerated(EnumType.STRING)
    private RuleEventType eventType;

    private Long ruleId;

    @Column(columnDefinition = "text")
    private String payload;

    @Column(columnDefinition = "text")
    private String errorMessage;

    @Column(columnDefinition = "text")
    private String stackTrace;

    private LocalDateTime failureTimestamp;

    private Integer retryCount;

    private String status;
}
