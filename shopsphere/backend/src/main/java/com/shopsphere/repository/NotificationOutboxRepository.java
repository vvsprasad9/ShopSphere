package com.shopsphere.repository;

import com.shopsphere.entity.NotificationOutbox;
import com.shopsphere.entity.enums.OutboxStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NotificationOutboxRepository extends JpaRepository<NotificationOutbox, Long> {

    List<NotificationOutbox> findTop50ByStatusOrderByCreatedAtAsc(OutboxStatus status);

    boolean existsByUserIdAndDedupKey(Long userId, String dedupKey);
}
