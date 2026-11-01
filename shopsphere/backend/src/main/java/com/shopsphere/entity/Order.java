package com.shopsphere.entity;

import com.shopsphere.entity.enums.OrderStatus;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "orders", indexes = {
        @Index(name = "idx_order_number", columnList = "order_number"),
        @Index(name = "idx_order_user", columnList = "user_id"),
        @Index(name = "idx_order_status", columnList = "order_status"),
        @Index(name = "idx_order_idempotency", columnList = "idempotency_key"),
        @Index(name = "idx_order_created_at", columnList = "created_at")
}, uniqueConstraints = {
        @UniqueConstraint(columnNames = "order_number")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Order extends BaseEntity {

    @Column(name = "order_number", nullable = false, unique = true, length = 64)
    private String orderNumber;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "recipientName", column = @Column(name = "shipping_recipient_name", nullable = false, length = 100)),
            @AttributeOverride(name = "phoneNumber", column = @Column(name = "shipping_phone_number", nullable = false, length = 25)),
            @AttributeOverride(name = "addressLine1", column = @Column(name = "shipping_address_line1", nullable = false, length = 255)),
            @AttributeOverride(name = "addressLine2", column = @Column(name = "shipping_address_line2", length = 255)),
            @AttributeOverride(name = "city", column = @Column(name = "shipping_city", nullable = false, length = 100)),
            @AttributeOverride(name = "state", column = @Column(name = "shipping_state", nullable = false, length = 100)),
            @AttributeOverride(name = "postalCode", column = @Column(name = "shipping_postal_code", nullable = false, length = 20)),
            @AttributeOverride(name = "country", column = @Column(name = "shipping_country", nullable = false, length = 100))
    })
    private AddressSnapshot shippingAddress;

    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "recipientName", column = @Column(name = "billing_recipient_name", length = 100)),
            @AttributeOverride(name = "phoneNumber", column = @Column(name = "billing_phone_number", length = 25)),
            @AttributeOverride(name = "addressLine1", column = @Column(name = "billing_address_line1", length = 255)),
            @AttributeOverride(name = "addressLine2", column = @Column(name = "billing_address_line2", length = 255)),
            @AttributeOverride(name = "city", column = @Column(name = "billing_city", length = 100)),
            @AttributeOverride(name = "state", column = @Column(name = "billing_state", length = 100)),
            @AttributeOverride(name = "postalCode", column = @Column(name = "billing_postal_code", length = 20)),
            @AttributeOverride(name = "country", column = @Column(name = "billing_country", length = 100))
    })
    private AddressSnapshot billingAddress;

    @Enumerated(EnumType.STRING)
    @Column(name = "order_status", nullable = false, length = 30)
    @Builder.Default
    private OrderStatus orderStatus = OrderStatus.PENDING;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal subtotal;

    @Column(name = "discount_amount", nullable = false, precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal discountAmount = BigDecimal.ZERO;

    @Column(name = "shipping_fee", nullable = false, precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal shippingFee = BigDecimal.ZERO;

    @Column(name = "tax_amount", nullable = false, precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal taxAmount = BigDecimal.ZERO;

    @Column(name = "total_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal totalAmount;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "coupon_id")
    private Coupon coupon;

    @Column(name = "tracking_number", length = 100)
    private String trackingNumber;

    @Column(name = "estimated_delivery_date")
    private LocalDateTime estimatedDeliveryDate;

    @Column(length = 500)
    private String notes;

    @Column(name = "idempotency_key", length = 128)
    private String idempotencyKey;

    @Column(name = "reservation_expires_at")
    private LocalDateTime reservationExpiresAt;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private List<OrderItem> items = new ArrayList<>();

    @OneToOne(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private Payment payment;
}
