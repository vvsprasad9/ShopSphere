package com.shopsphere.service;

import com.shopsphere.dto.response.PageResponse;
import com.shopsphere.dto.response.notification.NotificationResponse;
import com.shopsphere.dto.response.notification.UnreadNotificationCountResponse;
import com.shopsphere.entity.Notification;
import com.shopsphere.entity.NotificationOutbox;
import com.shopsphere.entity.Role;
import com.shopsphere.entity.User;
import com.shopsphere.entity.enums.ERole;
import com.shopsphere.entity.enums.NotificationType;
import com.shopsphere.entity.enums.OutboxStatus;
import com.shopsphere.entity.enums.UserStatus;
import com.shopsphere.exception.ResourceNotFoundException;
import com.shopsphere.repository.NotificationOutboxRepository;
import com.shopsphere.repository.NotificationRepository;
import com.shopsphere.repository.RoleRepository;
import com.shopsphere.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class NotificationServiceTest {

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private NotificationOutboxService notificationOutboxService;

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private NotificationOutboxRepository outboxRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private User user1;
    private User user2;

    @BeforeEach
    void setUp() {
        Role customerRole = roleRepository.findByName(ERole.ROLE_CUSTOMER)
                .orElseGet(() -> roleRepository.save(new Role(ERole.ROLE_CUSTOMER)));

        user1 = userRepository.save(User.builder()
                .email("user1.notif@shopsphere.com")
                .password(passwordEncoder.encode("Password123!"))
                .firstName("UserOne")
                .lastName("Test")
                .status(UserStatus.ACTIVE)
                .roles(new HashSet<>(List.of(customerRole)))
                .build());

        user2 = userRepository.save(User.builder()
                .email("user2.notif@shopsphere.com")
                .password(passwordEncoder.encode("Password123!"))
                .firstName("UserTwo")
                .lastName("Test")
                .status(UserStatus.ACTIVE)
                .roles(new HashSet<>(List.of(customerRole)))
                .build());
    }

    @Test
    @DisplayName("Get Notifications - Paginated list ordered by createdAt descending")
    void getUserNotifications_Success() {
        notificationRepository.save(Notification.builder()
                .user(user1)
                .title("First Notification")
                .message("Message 1")
                .type(NotificationType.ORDER_PLACED)
                .isRead(false)
                .createdAt(LocalDateTime.now().minusMinutes(10))
                .build());

        notificationRepository.save(Notification.builder()
                .user(user1)
                .title("Second Notification")
                .message("Message 2")
                .type(NotificationType.ORDER_CONFIRMED)
                .isRead(false)
                .createdAt(LocalDateTime.now().minusMinutes(5))
                .build());

        PageResponse<NotificationResponse> result = notificationService.getUserNotifications(user1.getId(), PageRequest.of(0, 10));

        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getContent().get(0).getTitle()).isEqualTo("Second Notification");
        assertThat(result.getContent().get(1).getTitle()).isEqualTo("First Notification");
    }

    @Test
    @DisplayName("Unread Notification Count - Accurately counts only unread notifications")
    void getUnreadNotificationCount_Success() {
        notificationRepository.save(Notification.builder()
                .user(user1)
                .title("Unread 1")
                .message("Msg")
                .type(NotificationType.ORDER_PLACED)
                .isRead(false)
                .build());

        notificationRepository.save(Notification.builder()
                .user(user1)
                .title("Unread 2")
                .message("Msg")
                .type(NotificationType.ORDER_SHIPPED)
                .isRead(false)
                .build());

        notificationRepository.save(Notification.builder()
                .user(user1)
                .title("Read 1")
                .message("Msg")
                .type(NotificationType.ORDER_DELIVERED)
                .isRead(true)
                .build());

        UnreadNotificationCountResponse response = notificationService.getUnreadNotificationCount(user1.getId());
        assertThat(response.getUnreadCount()).isEqualTo(2);
    }

    @Test
    @DisplayName("Mark As Read - Successfully updates status and persists")
    void markAsRead_Success() {
        Notification notif = notificationRepository.save(Notification.builder()
                .user(user1)
                .title("Important")
                .message("Msg")
                .type(NotificationType.PAYMENT_SUCCESS)
                .isRead(false)
                .build());

        NotificationResponse response = notificationService.markAsRead(user1.getId(), notif.getId());

        assertThat(response.isRead()).isTrue();
        Notification reloaded = notificationRepository.findById(notif.getId()).orElseThrow();
        assertThat(reloaded.isRead()).isTrue();
    }

    @Test
    @DisplayName("Mark As Read - Throws ResourceNotFoundException when non-owner attempts operation")
    void markAsRead_NonOwner_ThrowsException() {
        Notification notif = notificationRepository.save(Notification.builder()
                .user(user1)
                .title("User1 Notif")
                .message("Msg")
                .type(NotificationType.ORDER_PLACED)
                .isRead(false)
                .build());

        assertThatThrownBy(() -> notificationService.markAsRead(user2.getId(), notif.getId()))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Notification not found");
    }

    @Test
    @DisplayName("Mark All As Read - Marks all user notifications as read in bulk")
    void markAllAsRead_Success() {
        notificationRepository.save(Notification.builder()
                .user(user1).title("N1").message("M1").type(NotificationType.ORDER_PLACED).isRead(false).build());
        notificationRepository.save(Notification.builder()
                .user(user1).title("N2").message("M2").type(NotificationType.ORDER_CONFIRMED).isRead(false).build());

        notificationService.markAllAsRead(user1.getId());

        assertThat(notificationRepository.countByUserIdAndIsReadFalse(user1.getId())).isEqualTo(0);
    }

    @Test
    @DisplayName("Delete Notification - Deletes successfully when requested by owner")
    void deleteNotification_Success() {
        Notification notif = notificationRepository.save(Notification.builder()
                .user(user1)
                .title("Delete Me")
                .message("Msg")
                .type(NotificationType.SYSTEM)
                .isRead(false)
                .build());

        notificationService.deleteNotification(user1.getId(), notif.getId());

        assertThat(notificationRepository.findById(notif.getId())).isEmpty();
    }

    @Test
    @DisplayName("Transactional Outbox - Scheduled processing delivers notifications and deduplicates")
    void outboxProcessing_DeliversAndDeduplicates() {
        String dedupKey = "order_placed_dedup_test_1001";

        // Schedule outbox entry 1
        notificationOutboxService.scheduleNotification(
                user1.getId(),
                "Order Placed",
                "Your order #1001 has been placed.",
                NotificationType.ORDER_PLACED,
                "/orders/1001",
                dedupKey
        );

        // Process outbox
        notificationOutboxService.processOutbox();

        // Verify notification delivered
        List<Notification> notifs = notificationRepository.findAll().stream()
                .filter(n -> user1.getId().equals(n.getUser().getId()) && dedupKey.equals(n.getDedupKey()))
                .toList();
        assertThat(notifs).hasSize(1);
        assertThat(notifs.get(0).getTitle()).isEqualTo("Order Placed");

        // Schedule duplicate outbox entry with same dedupKey
        notificationOutboxService.scheduleNotification(
                user1.getId(),
                "Duplicate Order Placed",
                "Your order #1001 has been placed again.",
                NotificationType.ORDER_PLACED,
                "/orders/1001",
                dedupKey
        );

        // Process outbox again
        notificationOutboxService.processOutbox();

        // Notification count with this dedupKey should still be exactly 1
        List<Notification> notifsAfter = notificationRepository.findAll().stream()
                .filter(n -> user1.getId().equals(n.getUser().getId()) && dedupKey.equals(n.getDedupKey()))
                .toList();
        assertThat(notifsAfter).hasSize(1);

        // Verify outbox entries are marked PROCESSED
        List<NotificationOutbox> outboxList = outboxRepository.findAll().stream()
                .filter(o -> user1.getId().equals(o.getUserId()) && dedupKey.equals(o.getDedupKey()))
                .toList();
        assertThat(outboxList).isNotEmpty();
        assertThat(outboxList).allMatch(o -> o.getStatus() == OutboxStatus.PROCESSED);
    }
}
