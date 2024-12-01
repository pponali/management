package com.scaler.price.core.management.events.handler;

import com.scaler.price.core.management.dto.PriceEvent;
import com.scaler.price.audit.service.AuditService;
import com.scaler.price.core.management.service.NotificationService;

import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
@NoArgsConstructor
@AllArgsConstructor
public class PriceEventHandler {

    private AuditService auditService;
    private NotificationService notificationService;

    @KafkaListener(topics = "price-events")
    public void handlePriceEvent(PriceEvent event) {
        switch (event.getEventType()) {
            case "PRICE_CREATED":
                auditService.logPriceCreation(event);
                break;
            case "PRICE_UPDATED":
                notificationService.notifyPriceUpdate(event);
                break;
            default:
                throw new IllegalArgumentException("Unknown event type: " + event.getEventType());
        }
    }
}
