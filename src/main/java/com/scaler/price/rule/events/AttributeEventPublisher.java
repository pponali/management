package com.scaler.price.rule.events;

import com.scaler.price.rule.domain.AttributeEventType;
import com.scaler.price.rule.domain.ProductAttribute;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
@Getter
@Setter
public class AttributeEventPublisher {
    private final KafkaTemplate<String, AttributeEvent> kafkaTemplate;
    private final ApplicationEventPublisher eventPublisher;
    private static final String TOPIC_NAME = "attribute-events";
    private static final int MAX_RETRIES = 3;
    private static final long RETRY_DELAY_MS = 1000;

    public void publishEvent(AttributeEvent event) {
        if (event == null) {
            log.error("Cannot publish null event");
            return;
        }

        try {
            publishKafkaEventWithRetry(event);
            publishSpringEvent(event);
            log.info("Successfully published event: {}", event.getEventId());
        } catch (Exception e) {
            log.error("Failed to publish event: {}", event.getEventId(), e);
            throw new EventPublishingException("Failed to publish event: " + event.getEventId(), e);
        }
    }

    public void publishEvent(AttributeEventType eventType, ProductAttribute attribute) {
        if (eventType == null || attribute == null) {
            log.error("Cannot publish event with null eventType or attribute");
            return;
        }

        AttributeEvent event = AttributeEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .eventType(eventType)
                .attribute(attribute)
                .timestamp(LocalDateTime.now())
                .build();
        
        publishEvent(event);
    }

    public void publishBulkEvents(List<AttributeEvent> events) {
        if (events == null || events.isEmpty()) {
            log.warn("No events to publish in bulk operation");
            return;
        }

        events.forEach(this::publishEvent);
    }

    private void publishKafkaEventWithRetry(AttributeEvent event) {
        int retryCount = 0;
        Exception lastException = null;

        while (retryCount < MAX_RETRIES) {
            try {
                kafkaTemplate.send(TOPIC_NAME, event.getEventId(), event)
                        .whenComplete((result, ex) -> {
                            if (ex == null) {
                                log.debug("Successfully sent event to Kafka: {}", event.getEventId());
                            } else {
                                log.error("Failed to send event to Kafka: {}", event.getEventId(), ex);
                                throw new EventPublishingException("Kafka publish failed for event: " + event.getEventId(), ex);
                            }
                        });
                return;
            } catch (Exception e) {
                lastException = e;
                retryCount++;
                if (retryCount < MAX_RETRIES) {
                    try {
                        Thread.sleep(RETRY_DELAY_MS * retryCount);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw new EventPublishingException("Retry interrupted for event: " + event.getEventId(), ie);
                    }
                }
            }
        }

        throw new EventPublishingException("Failed to publish event after " + MAX_RETRIES + " retries: " + event.getEventId(), lastException);
    }

    private void publishSpringEvent(AttributeEvent event) {
        try {
            eventPublisher.publishEvent(event);
            log.debug("Successfully published Spring event: {}", event.getEventId());
        } catch (Exception e) {
            log.error("Failed to publish Spring event: {}", event.getEventId(), e);
            throw new EventPublishingException("Spring event publish failed for event: " + event.getEventId(), e);
        }
    }

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
                .attribute(attributes.get(0))
                .build();

        publishEvent(event);
    }
}

class EventPublishingException extends RuntimeException {
    public EventPublishingException(String message, Throwable cause) {
        super(message, cause);
    }
}