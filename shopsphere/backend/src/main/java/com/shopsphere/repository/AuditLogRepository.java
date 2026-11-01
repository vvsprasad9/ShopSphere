package com.shopsphere.repository;

import com.shopsphere.entity.AuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {

    Page<AuditLog> findByUserId(Long userId, Pageable pageable);

    Page<AuditLog> findByAction(String action, Pageable pageable);

    Page<AuditLog> findByCreatedAtGreaterThanEqualAndCreatedAtLessThanOrderByCreatedAtDesc(
            java.time.LocalDateTime startDate, java.time.LocalDateTime endDate, Pageable pageable);

    Page<AuditLog> findByUserIdAndCreatedAtGreaterThanEqualAndCreatedAtLessThanOrderByCreatedAtDesc(
            Long userId, java.time.LocalDateTime startDate, java.time.LocalDateTime endDate, Pageable pageable);

    Page<AuditLog> findByActionAndCreatedAtGreaterThanEqualAndCreatedAtLessThanOrderByCreatedAtDesc(
            String action, java.time.LocalDateTime startDate, java.time.LocalDateTime endDate, Pageable pageable);

    Page<AuditLog> findByUserIdAndActionAndCreatedAtGreaterThanEqualAndCreatedAtLessThanOrderByCreatedAtDesc(
            Long userId, String action, java.time.LocalDateTime startDate, java.time.LocalDateTime endDate, Pageable pageable);
}
