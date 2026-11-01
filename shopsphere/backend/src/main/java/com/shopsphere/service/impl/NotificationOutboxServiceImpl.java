package com.shopsphere.service.impl;

import com.shopsphere.entity.Notification;
import com.shopsphere.entity.NotificationOutbox;
import com.shopsphere.entity.User;
import com.shopsphere.entity.enums.NotificationType;
import com.shopsphere.entity.enums.OutboxStatus;
import com.shopsphere.repository.NotificationOutboxRepository;
import com.shopsphere.repository.NotificationRepository;
import com.shopsphere.repository.UserRepository;
import com.shopsphere.service.NotificationOutboxService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationOutboxServiceImpl implements NotificationOutboxService {

    private final NotificationOutboxRepository outboxRepository;
    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;
    private final ApplicationEventPublisher eventPublisher;

    public record OutboxScheduledEvent(Long outboxId) {}

    @Override
    @Transactional
    public void scheduleNotification(Long userId, String title, String message, NotificationType type, String linkUrl, String dedupKey) {
        log.info("Scheduling outbox notification for user: {}, type: {}, dedupKey: {}", userId, type, dedupKey);

        NotificationOutbox outbox = NotificationOutbox.builder()
                .userId(userId)
                .title(title)
                .message(message)
                .type(type)
                .linkUrl(linkUrl)
                .dedupKey(dedupKey)
                .status(OutboxStatus.PENDING)
                .retryCount(0)
                .maxRetries(3)
                .createdAt(LocalDateTime.now())
                .build();

        NotificationOutbox saved = outboxRepository.save(outbox);
        eventPublisher.publishEvent(new OutboxScheduledEvent(saved.getId()));
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Async
    public void onOutboxScheduled(OutboxScheduledEvent event) {
        try {
            processOutbox();
        } catch (Exception ex) {
            log.warn("Asynchronous outbox dispatch failed: {}", ex.getMessage());
        }
    }

    @Override
    @Scheduled(fixedDelay = 5000)
    public void processOutbox() {
        List<NotificationOutbox> pendingEntries = outboxRepository.findTop50ByStatusOrderByCreatedAtAsc(OutboxStatus.PENDING);
        if (pendingEntries.isEmpty()) {
            return;
        }

        log.debug("Processing {} pending notification outbox entries", pendingEntries.size());
        for (NotificationOutbox entry : pendingEntries) {
            try {
                processSingleEntry(entry.getId());
            } catch (Exception e) {
                log.error("Unexpected error processing outbox entry id {}: {}", entry.getId(), e.getMessage());
            }
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void processSingleEntry(Long entryId) {
        NotificationOutbox entry = outboxRepository.findById(entryId).orElse(null);
        if (entry == null || entry.getStatus() != OutboxStatus.PENDING) {
            return;
        }

        try {
            // Deduplication check
            if (entry.getDedupKey() != null && !entry.getDedupKey().isBlank()) {
                if (notificationRepository.existsByUserIdAndDedupKey(entry.getUserId(), entry.getDedupKey())) {
                    log.info("Notification with dedupKey {} already exists for user {}. Marking outbox entry {} as PROCESSED.",
                            entry.getDedupKey(), entry.getUserId(), entry.getId());
                    entry.setStatus(OutboxStatus.PROCESSED);
                    entry.setProcessedAt(LocalDateTime.now());
                    outboxRepository.save(entry);
                    return;
                }
            }

            User user = userRepository.findById(entry.getUserId()).orElse(null);
            if (user == null) {
                log.warn("User id {} not found for outbox entry {}. Marking as FAILED.", entry.getUserId(), entry.getId());
                entry.setStatus(OutboxStatus.FAILED);
                entry.setErrorMessage("User not found with id: " + entry.getUserId());
                entry.setProcessedAt(LocalDateTime.now());
                outboxRepository.save(entry);
                return;
            }

            Notification notification = Notification.builder()
                    .user(user)
                    .title(entry.getTitle())
                    .message(entry.getMessage())
                    .type(entry.getType())
                    .linkUrl(entry.getLinkUrl())
                    .dedupKey(entry.getDedupKey())
                    .isRead(false)
                    .createdAt(LocalDateTime.now())
                    .build();

            try {
                notificationRepository.save(notification);
            } catch (DataIntegrityViolationException dive) {
                log.info("Duplicate notification caught via constraint for user {} dedupKey {}", entry.getUserId(), entry.getDedupKey());
            }

            entry.setStatus(OutboxStatus.PROCESSED);
            entry.setProcessedAt(LocalDateTime.now());
            entry.setErrorMessage(null);
            outboxRepository.save(entry);
            log.info("Successfully processed notification outbox id: {} for user: {}", entry.getId(), entry.getUserId());

        } catch (Exception ex) {
            log.error("Failed processing outbox entry id {}: {}", entry.getId(), ex.getMessage());
            int newRetryCount = entry.getRetryCount() + 1;
            entry.setRetryCount(newRetryCount);
            entry.setErrorMessage(ex.getMessage());
            if (newRetryCount >= entry.getMaxRetries()) {
                entry.setStatus(OutboxStatus.FAILED);
                log.warn("Outbox entry id {} exceeded max retries. Marked as FAILED.", entry.getId());
            }
            outboxRepository.save(entry);
        }
    }
}
