package com.scaler.price.rule.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Embeddable
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChangeDiff {
    @Column(name = "old_value")
    private String oldValue;

    @Column(name = "new_value")
    private String newValue;

    public static ChangeDiff of(Object oldValue, Object newValue) {
        try {
            return ChangeDiff.builder()
                    .oldValue(oldValue != null ? oldValue.toString() : null)
                    .newValue(newValue != null ? newValue.toString() : null)
                    .build();
        } catch (Exception e) {
            throw new IllegalArgumentException("Failed to create ChangeDiff", e);
        }
    }

    public static ChangeDiff empty() {
        return new ChangeDiff(null, null);
    }

    public boolean hasChanged() {
        if (oldValue == null && newValue == null) {
            return false;
        }
        if (oldValue == null || newValue == null) {
            return true;
        }
        return !oldValue.equals(newValue);
    }
}
