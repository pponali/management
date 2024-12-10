package com.scaler.price.core.management.buybox.model;

public enum SellerRegion {
    LOCAL(1),      // Same region as customer
    NATIONAL(2),   // Same country, different region
    INTERNATIONAL(3); // Different country

    private final int priority;

    SellerRegion(int priority) {
        this.priority = priority;
    }

    public int getPriority() {
        return priority;
    }
}
