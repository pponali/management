package com.scaler.price.core.management.service.impl;

import com.scaler.price.core.management.service.SecurityService;
import org.springframework.stereotype.Service;

@Service
public class SecurityServiceImpl implements SecurityService {
    @Override
    public String getCurrentUserIp() {
        // TODO: Implement actual logic to get user IP from SecurityContext
        return "127.0.0.1";
    }

    @Override
    public String getCurrentUserAgent() {
        // TODO: Implement actual logic to get user agent from request headers
        return "Default User Agent";
    }

    @Override
    public String getCurrentSessionId() {
        // TODO: Implement actual logic to get session ID
        return "default-session-id";
    }

    @Override
    public String getCurrentUser() {
        // TODO: Implement actual logic to get current user from SecurityContext
        return "default-user";
    }

    @Override
    public String getCurrentUserId() {
        // TODO: Implement actual logic to get current user ID from SecurityContext
        return "default-user-id";
    }
}
