package com.shopsphere.repository;

import com.shopsphere.entity.OrderItem;
import com.shopsphere.entity.enums.OrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

@Repository
public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {

    List<OrderItem> findByOrderId(Long orderId);

    boolean existsByProductId(Long productId);

    @Query("SELECT COUNT(oi) > 0 FROM OrderItem oi " +
            "JOIN oi.order o " +
            "WHERE oi.product.id = :productId " +
            "AND o.user.id = :userId " +
            "AND o.orderStatus IN :statuses")
    boolean hasUserOrderedProductWithStatuses(
            @Param("userId") Long userId,
            @Param("productId") Long productId,
            @Param("statuses") Collection<OrderStatus> statuses
    );

    @Query("SELECT COUNT(oi) > 0 FROM OrderItem oi " +
            "JOIN oi.order o " +
            "LEFT JOIN o.payment p " +
            "WHERE oi.product.id = :productId " +
            "AND o.user.id = :userId " +
            "AND o.orderStatus = com.shopsphere.entity.enums.OrderStatus.DELIVERED " +
            "AND (p IS NULL OR p.paymentStatus NOT IN (com.shopsphere.entity.enums.PaymentStatus.REFUNDED, com.shopsphere.entity.enums.PaymentStatus.REFUND_PENDING))")
    boolean hasUserDeliveredOrderForProduct(
            @Param("userId") Long userId,
            @Param("productId") Long productId
    );

    @Query("SELECT oi.product.id, oi.productName, oi.productSku, " +
            "COALESCE(SUM(oi.quantity), 0), COALESCE(SUM(oi.totalPrice), 0) " +
            "FROM OrderItem oi " +
            "JOIN oi.order o " +
            "WHERE o.orderStatus != com.shopsphere.entity.enums.OrderStatus.CANCELLED " +
            "AND o.createdAt >= :startDate AND o.createdAt < :endDate " +
            "GROUP BY oi.product.id, oi.productName, oi.productSku " +
            "ORDER BY SUM(oi.quantity) DESC")
    List<Object[]> getTopSellingProducts(
            @Param("startDate") java.time.LocalDateTime startDate,
            @Param("endDate") java.time.LocalDateTime endDate,
            org.springframework.data.domain.Pageable pageable);

    @Query("SELECT c.id, c.name, COALESCE(SUM(oi.quantity), 0), COALESCE(SUM(oi.totalPrice), 0) " +
            "FROM OrderItem oi " +
            "JOIN oi.order o " +
            "JOIN oi.product p " +
            "JOIN p.category c " +
            "WHERE o.orderStatus != com.shopsphere.entity.enums.OrderStatus.CANCELLED " +
            "AND o.createdAt >= :startDate AND o.createdAt < :endDate " +
            "GROUP BY c.id, c.name " +
            "ORDER BY SUM(oi.totalPrice) DESC")
    List<Object[]> getCategorySalesBetween(
            @Param("startDate") java.time.LocalDateTime startDate,
            @Param("endDate") java.time.LocalDateTime endDate);
}
