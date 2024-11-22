package com.scaler.price.core.management.events.handler;

import com.scaler.price.core.management.dto.PriceEvent;
import com.scaler.price.audit.service.AuditService;
import com.scaler.price.core.management.service.NotificationService;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
public class PriceEventHandler {
    private final AuditService auditService;
    private final NotificationService notificationService;

    public PriceEventHandler(AuditService auditService, NotificationService notificationService) {
        this.auditService = auditService;
        this.notificationService = notificationService;
    }

    @KafkaListener(topics = "price-events")
    public void handlePriceEvent(PriceEvent event) {
        switch (event.getEventType()) {
            case "PRICE_CREATED":
                auditService.logPriceCreation(event);
                break;
            case "PRICE_UPDATED":
                notificationService.notifyPriceUpdate(event);
                break;
        }
    }
}
