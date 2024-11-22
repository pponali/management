package com.scaler.price.core.management.dto;

import jakarta.persistence.Entity;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Entity
public class PriceEvent {
    private String eventId;
    private String eventType;
    private LocalDateTime timestamp;
    private String priceId;
    private PriceDTO payload;
    private int version;
}
