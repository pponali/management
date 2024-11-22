// CategoryAttributeValue.java
package com.scaler.price.rule.dto;

import lombok.Data;
import lombok.Builder;

@Data
@Builder
public class CategoryAttributeValue {
    private String categoryId;
    private String attribute;
    private String value;
}