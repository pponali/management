package com.scaler.price.core.management.domain;

public enum Channel {
    WEB(1), MOBILE(2), POS(3);
    private final int value;
    Channel(int value) {
        this.value = value;
    }
    public int getValue() {
        return value;
    }
}
