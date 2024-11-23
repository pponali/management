package com.scaler.price.rule.events;

import com.scaler.price.rule.domain.AttributeEventType;
import com.scaler.price.rule.domain.ProductAttribute;
import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AttributeEvent {
    private String eventId;
    private AttributeEventType eventType;
    private ProductAttribute attribute;
    private LocalDateTime timestamp;
}
