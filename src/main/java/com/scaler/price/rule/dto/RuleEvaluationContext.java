package com.scaler.price.rule.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Data
@Builder
public class RuleEvaluationContext {
    private Long productId;
    private Long sellerId;
    private Long siteId;
    private Long categoryId;
    private Long brandId;
    private Integer quantity;
    private BigDecimal basePrice;
    private BigDecimal costPrice;
    private BigDecimal currentPrice;
    private Map<String, Object> attributes;
    private LocalDateTime evaluationTime;
    private Map<String, Object> cache;
    private String timePeriod;

    public void addToCache(String key, Object value) {
        if (cache == null) {
            cache = new ConcurrentHashMap<>();
        }
        cache.put(key, value);
    }

    public Object getFromCache(String key) {
        return cache != null ? cache.get(key) : null;
    }

    public String getTimePeriod() {
        return timePeriod;
    }

    public void setTimePeriod(String timePeriod) {
        this.timePeriod = timePeriod;
    }

    public void setParameters(Map<String, Object> parameters) {
        if (this.attributes == null) {
            this.attributes = new ConcurrentHashMap<>();
        }
        this.attributes.putAll(parameters);
    }
}
