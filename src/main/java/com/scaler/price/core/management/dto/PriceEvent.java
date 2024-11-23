package com.scaler.price.core.management.dto;

import com.scaler.price.rule.domain.RuleType;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Entity
public class PriceEvent {
    @Id
    private String eventId;
    private String eventType;
    private LocalDateTime timestamp;
    private String priceId;
    private String ruleId;
    private String ruleName;
    private RuleType ruleType;
    private PriceDTO payload;
    private int version;
}
