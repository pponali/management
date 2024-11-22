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
    private String productId;
    private String sellerId;
    private String siteId;
    private String categoryId;
    private String brandId;
    private Integer quantity;
    private BigDecimal basePrice;
    private BigDecimal costPrice;
    private BigDecimal currentPrice;
    private Map<String, Object> attributes;
    private LocalDateTime evaluationTime;
    private Map<String, Object> cache;

    public void addToCache(String key, Object value) {
        if (cache == null) {
            cache = new ConcurrentHashMap<>();
        }
        cache.put(key, value);
    }

    public Object getFromCache(String key) {
        return cache != null ? cache.get(key) : null;
    }
}
