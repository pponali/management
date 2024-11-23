package com.scaler.price.core.management.service;

public interface SecurityService {
    String getCurrentUserIp();

    String getCurrentUserAgent();

    String getCurrentSessionId();

    String getCurrentUser();
    
    String getCurrentUserId();
}
