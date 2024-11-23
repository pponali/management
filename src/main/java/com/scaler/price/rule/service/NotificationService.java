package com.scaler.price.rule.service;

import com.scaler.price.rule.events.NotificationEvent;
import com.scaler.price.rule.dto.StatusChangeNotification;
import org.springframework.stereotype.Service;

@Service
public interface NotificationService {
    void sendNotification(NotificationEvent notification);
    
    void notifyStakeholders(String subject, String message, StatusChangeNotification notification);
    
    void notifyOwners(String subject, String message, StatusChangeNotification notification);
    
    void notifyApprovers(String subject, String message, StatusChangeNotification notification);
}