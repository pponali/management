package com.scaler.price.rule.events;

import com.scaler.price.rule.domain.AttributeEventType;
import com.scaler.price.rule.domain.ProductAttribute;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class AttributeEventPublisher {
    private final KafkaTemplate<String, AttributeEvent> kafkaTemplate;

    public void publishAttributeCreated(ProductAttribute attribute) {
        publishEvent(AttributeEventType.CREATED, attribute);
    }

    public void publishAttributeUpdated(ProductAttribute attribute) {
        publishEvent(AttributeEventType.UPDATED, attribute);
    }

    public void publishAttributeDeleted(ProductAttribute attribute) {
        publishEvent(AttributeEventType.DELETED, attribute);
    }

    public void publishAttributesBulkUpdated(
            String productId,
            List<ProductAttribute> attributes) {

        AttributeEvent event = AttributeEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .eventType(AttributeEventType.BULK_UPDATED)
                .productId(productId)
                .payload(attributes)
                .timestamp(LocalDateTime.now())
                .build();

        publishEvent(event);
    }

    private void publishEvent(
            AttributeEventType eventType,
            ProductAttribute attribute) {

        AttributeEvent event = AttributeEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .eventType(eventType)
                .productId(attribute.getProductId())
                .attributeKey(attribute.getAttributeKey())
                .payload(attribute)
                .timestamp(LocalDateTime.now())
                .build();

        publishEvent(event);
    }

    private void publishEvent(AttributeEvent event) {
        try {
            kafkaTemplate.send("product-attribute-events", event)
                    .addCallback(
                            result -> log.debug("Published event: {}", event.getEventId()),
                            ex -> log.error("Error publishing event: {}", ex.getMessage())
                    );
        } catch (Exception e) {
            log.error("Failed to publish event: {}", e.getMessage(), e);
        }
    }
}