package com.scaler.price.rule.events;


import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.Map;

@Data
@Builder
public class NotificationEvent {
    private String type;
    private String severity;
    private String source;
    private String description;
    private Map<String, Object> details;
    private LocalDateTime timestamp;
}