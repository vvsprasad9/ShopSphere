package com.shopsphere.service.impl;

import com.shopsphere.dto.response.PageResponse;
import com.shopsphere.dto.response.notification.NotificationResponse;
import com.shopsphere.dto.response.notification.UnreadNotificationCountResponse;
import com.shopsphere.entity.Notification;
import com.shopsphere.entity.enums.NotificationType;
import com.shopsphere.exception.ResourceNotFoundException;
import com.shopsphere.repository.NotificationRepository;
import com.shopsphere.service.NotificationOutboxService;
import com.shopsphere.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationServiceImpl implements NotificationService {

    private final NotificationRepository notificationRepository;
    private final NotificationOutboxService notificationOutboxService;

    @Override
    @Transactional(readOnly = true)
    public PageResponse<NotificationResponse> getUserNotifications(Long userId, Pageable pageable) {
        Page<Notification> page = notificationRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable);
        return PageResponse.fromPage(page.map(NotificationResponse::fromEntity));
    }

    @Override
    @Transactional(readOnly = true)
    public UnreadNotificationCountResponse getUnreadNotificationCount(Long userId) {
        long count = notificationRepository.countByUserIdAndIsReadFalse(userId);
        return UnreadNotificationCountResponse.builder().unreadCount(count).build();
    }

    @Override
    @Transactional
    public NotificationResponse markAsRead(Long userId, Long notificationId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new ResourceNotFoundException("Notification not found with id: " + notificationId));

        if (notification.getUser() == null || !notification.getUser().getId().equals(userId)) {
            throw new ResourceNotFoundException("Notification not found with id: " + notificationId);
        }

        notification.setRead(true);
        Notification saved = notificationRepository.save(notification);
        return NotificationResponse.fromEntity(saved);
    }

    @Override
    @Transactional
    public void markAllAsRead(Long userId) {
        notificationRepository.markAllAsReadForUser(userId);
    }

    @Override
    @Transactional
    public void deleteNotification(Long userId, Long notificationId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new ResourceNotFoundException("Notification not found with id: " + notificationId));

        if (notification.getUser() == null || !notification.getUser().getId().equals(userId)) {
            throw new ResourceNotFoundException("Notification not found with id: " + notificationId);
        }

        notificationRepository.delete(notification);
    }

    @Override
    @Transactional
    public void sendNotification(Long userId, String title, String message, NotificationType type, String linkUrl, String dedupKey) {
        notificationOutboxService.scheduleNotification(userId, title, message, type, linkUrl, dedupKey);
    }
}
