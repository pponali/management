package com.scaler.price.core.management.service;

import com.scaler.price.core.management.dto.PriceEvent;
import com.scaler.price.rule.dto.StatusChangeNotification;
import com.scaler.price.rule.events.NotificationEvent;
import org.springframework.stereotype.Service;


@Service
public class NotificationService  implements com.scaler.price.rule.service.NotificationService {
    public void notifyPriceUpdate(PriceEvent event) {
    }

    @Override
    public void sendNotification(NotificationEvent notification) {

    }

    @Override
    public void notifyStakeholders(String subject, String message, StatusChangeNotification notification) {

    }

    @Override
    public void notifyOwners(String subject, String message, StatusChangeNotification notification) {

    }

    public void notifyApprovers(String ruleApprovalRequired, String format, StatusChangeNotification notification) {
        // Implementation for notifying approvers


    }
}
