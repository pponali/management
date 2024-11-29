package com.scaler.price.rule.events;

import com.scaler.price.rule.domain.AttributeEventType;
import com.scaler.price.rule.domain.ProductAttribute;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AttributeEvent {
    private String eventId;
    private AttributeEventType eventType;
    private ProductAttribute attribute;
    private LocalDateTime timestamp;
}
