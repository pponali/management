package com.scaler.price.rule.domain;

import lombok.Data;

@Data
public class ChangeType {
    private String typeName;

    public ChangeType(String typeName) {
        this.typeName = typeName;
    }

    @Override
    public String toString() {
        return "ChangeType{" +
                "typeName='" + typeName + '\'' +
                '}';
    }
}
