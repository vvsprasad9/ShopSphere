package com.shopsphere.repository;

import com.shopsphere.entity.Payment;
import com.shopsphere.entity.enums.PaymentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {

    Optional<Payment> findByOrderId(Long orderId);

    Optional<Payment> findByTransactionId(String transactionId);

    Optional<Payment> findByRazorpayOrderId(String razorpayOrderId);

    Optional<Payment> findByRazorpayPaymentId(String razorpayPaymentId);

    long countByPaymentStatus(PaymentStatus status);

    @org.springframework.data.jpa.repository.Query("SELECT p.paymentMethod, p.paymentStatus, COUNT(p), COALESCE(SUM(p.amount), 0) " +
            "FROM Payment p " +
            "WHERE p.createdAt >= :startDate AND p.createdAt < :endDate " +
            "GROUP BY p.paymentMethod, p.paymentStatus")
    java.util.List<Object[]> getPaymentBreakdownBetween(
            @org.springframework.data.repository.query.Param("startDate") java.time.LocalDateTime startDate,
            @org.springframework.data.repository.query.Param("endDate") java.time.LocalDateTime endDate);

    org.springframework.data.domain.Page<Payment> findByCreatedAtGreaterThanEqualAndCreatedAtLessThanOrderByCreatedAtDesc(
            java.time.LocalDateTime startDate,
            java.time.LocalDateTime endDate,
            org.springframework.data.domain.Pageable pageable);
}
