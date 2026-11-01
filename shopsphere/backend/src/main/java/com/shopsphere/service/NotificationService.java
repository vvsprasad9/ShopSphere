package com.shopsphere.service;

import com.shopsphere.dto.response.PageResponse;
import com.shopsphere.dto.response.notification.NotificationResponse;
import com.shopsphere.dto.response.notification.UnreadNotificationCountResponse;
import com.shopsphere.entity.enums.NotificationType;
import org.springframework.data.domain.Pageable;

public interface NotificationService {

    PageResponse<NotificationResponse> getUserNotifications(Long userId, Pageable pageable);

    UnreadNotificationCountResponse getUnreadNotificationCount(Long userId);

    NotificationResponse markAsRead(Long userId, Long notificationId);

    void markAllAsRead(Long userId);

    void deleteNotification(Long userId, Long notificationId);

    void sendNotification(Long userId, String title, String message, NotificationType type, String linkUrl, String dedupKey);
}
