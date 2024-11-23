package com.scaler.price.rule.events;

import com.scaler.price.rule.domain.Product;
import com.scaler.price.rule.domain.ProductEventType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class ProductEventPublisher {
    private final KafkaTemplate<String, ProductEvent> kafkaTemplate;

    public void publishProductCreated(Product product) {
        publish(ProductEventType.CREATED, product);
    }

    public void publishProductUpdated(Product product) {
        publish(ProductEventType.UPDATED, product);
    }

    public void publishProductStatusChanged(Product product) {
        publish(ProductEventType.STATUS_CHANGED, product);
    }

    public void publishProductSitesUpdated(Product product) {
        publish(ProductEventType.SITES_UPDATED, product);
    }

    public void publishProductPricesUpdated(Product product) {
        publish(ProductEventType.PRICES_UPDATED, product);
    }

    private void publish(ProductEventType eventType, Product product) {
        ProductEvent event = ProductEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .eventType(eventType)
                .productId(product.getId())
                .payload(product)
                .timestamp(LocalDateTime.now())
                .build();

        try {
            kafkaTemplate.send("product-events", event)
                    .whenComplete((result, ex) -> {
                        if (ex == null) {
                            log.debug("Published event: {}", event.getEventId());
                        } else {
                            log.error("Error publishing event: {}", ex.getMessage());
                        }
                    });
        } catch (Exception e) {
            log.error("Failed to publish event: {}", e.getMessage(), e);
        }
    }
}