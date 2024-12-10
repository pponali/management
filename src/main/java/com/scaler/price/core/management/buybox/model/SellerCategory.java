package com.scaler.price.core.management.buybox.model;

public enum SellerCategory {
    PLATINUM(1),
    GOLD(2),
    SILVER(3),
    BRONZE(4);

    private final int priority;

    SellerCategory(int priority) {
        this.priority = priority;
    }

    public int getPriority() {
        return priority;
    }
}
