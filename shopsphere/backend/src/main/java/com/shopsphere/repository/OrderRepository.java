package com.shopsphere.repository;

import com.shopsphere.entity.Order;
import com.shopsphere.entity.enums.OrderStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.Optional;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long>, JpaSpecificationExecutor<Order> {

    Optional<Order> findByOrderNumber(String orderNumber);

    Page<Order> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);

    @Query("SELECT o FROM Order o " +
            "LEFT JOIN FETCH o.items i " +
            "LEFT JOIN FETCH o.payment " +
            "LEFT JOIN FETCH o.coupon " +
            "WHERE o.orderNumber = :orderNumber")
    Optional<Order> findByOrderNumberWithDetails(@Param("orderNumber") String orderNumber);

    @Query("SELECT o FROM Order o " +
            "LEFT JOIN FETCH o.items i " +
            "LEFT JOIN FETCH o.payment " +
            "LEFT JOIN FETCH o.coupon " +
            "WHERE o.id = :id")
    Optional<Order> findByIdWithDetails(@Param("id") Long id);

    Optional<Order> findByIdAndUserId(Long id, Long userId);

    Optional<Order> findByIdempotencyKeyAndUserId(String idempotencyKey, Long userId);

    Page<Order> findByOrderStatus(OrderStatus orderStatus, Pageable pageable);

    java.util.List<Order> findByOrderStatusAndReservationExpiresAtBefore(OrderStatus orderStatus, java.time.LocalDateTime dateTime);

    long countByCouponIdAndUserIdAndOrderStatusNot(Long couponId, Long userId, OrderStatus orderStatus);

    long countByOrderStatus(OrderStatus orderStatus);

    @Query("SELECT SUM(o.totalAmount) FROM Order o WHERE o.orderStatus NOT IN ('CANCELLED', 'RETURNED')")
    BigDecimal calculateTotalRevenue();

    @Query("SELECT new com.shopsphere.dto.projection.OrderFinancialSummary(" +
            "COALESCE(SUM(o.subtotal), 0), " +
            "COALESCE(SUM(o.discountAmount), 0), " +
            "COALESCE(SUM(o.shippingFee), 0), " +
            "COALESCE(SUM(o.taxAmount), 0), " +
            "COALESCE(SUM(o.totalAmount), 0), " +
            "COUNT(o)) " +
            "FROM Order o " +
            "WHERE o.orderStatus != com.shopsphere.entity.enums.OrderStatus.CANCELLED " +
            "AND o.createdAt >= :startDate AND o.createdAt < :endDate")
    com.shopsphere.dto.projection.OrderFinancialSummary getFinancialSummary(
            @Param("startDate") java.time.LocalDateTime startDate,
            @Param("endDate") java.time.LocalDateTime endDate);

    @Query("SELECT o.orderStatus, COUNT(o) FROM Order o " +
            "WHERE o.createdAt >= :startDate AND o.createdAt < :endDate " +
            "GROUP BY o.orderStatus")
    java.util.List<Object[]> countOrdersByStatusBetween(
            @Param("startDate") java.time.LocalDateTime startDate,
            @Param("endDate") java.time.LocalDateTime endDate);

    @Query("SELECT o.coupon.id, o.coupon.code, o.coupon.discountType, o.coupon.discountValue, " +
            "COUNT(o), COALESCE(SUM(o.discountAmount), 0), COALESCE(SUM(o.totalAmount), 0), " +
            "o.coupon.active, o.coupon.expirationDate " +
            "FROM Order o " +
            "WHERE o.coupon IS NOT NULL " +
            "AND o.orderStatus != com.shopsphere.entity.enums.OrderStatus.CANCELLED " +
            "AND o.createdAt >= :startDate AND o.createdAt < :endDate " +
            "GROUP BY o.coupon.id, o.coupon.code, o.coupon.discountType, o.coupon.discountValue, o.coupon.active, o.coupon.expirationDate")
    java.util.List<Object[]> getCouponPerformanceBetween(
            @Param("startDate") java.time.LocalDateTime startDate,
            @Param("endDate") java.time.LocalDateTime endDate);

    @Query("SELECT o.user.id, COUNT(o) FROM Order o " +
            "WHERE o.orderStatus != com.shopsphere.entity.enums.OrderStatus.CANCELLED " +
            "GROUP BY o.user.id")
    java.util.List<Object[]> getCustomerTotalOrderCounts();

    @Query("SELECT DISTINCT o.user.id FROM Order o " +
            "WHERE o.orderStatus != com.shopsphere.entity.enums.OrderStatus.CANCELLED " +
            "AND o.createdAt >= :startDate AND o.createdAt < :endDate")
    java.util.List<Long> getDistinctUserIdsWithOrdersBetween(
            @Param("startDate") java.time.LocalDateTime startDate,
            @Param("endDate") java.time.LocalDateTime endDate);

    Page<Order> findByCreatedAtGreaterThanEqualAndCreatedAtLessThanOrderByCreatedAtDesc(
            java.time.LocalDateTime startDate,
            java.time.LocalDateTime endDate,
            Pageable pageable);
}
