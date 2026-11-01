package com.shopsphere.service;

import com.shopsphere.entity.enums.NotificationType;

public interface NotificationOutboxService {

    void scheduleNotification(Long userId, String title, String message, NotificationType type, String linkUrl, String dedupKey);

    void processOutbox();
}
