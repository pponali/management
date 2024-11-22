package com.scaler.price.rule.service;

import com.scaler.price.rule.events.NotificationEvent;
import org.springframework.stereotype.Service;

@Service
public interface NotificationService {
    void sendNotification(NotificationEvent notification);
}