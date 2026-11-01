package com.shopsphere.dto.response.order;

import com.shopsphere.dto.response.payment.PaymentResponse;
import com.shopsphere.entity.AddressSnapshot;
import com.shopsphere.entity.Order;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderResponse {

    private Long id;
    private String orderNumber;
    private Long userId;
    private String customerEmail;
    private AddressSnapshot shippingAddress;
    private AddressSnapshot billingAddress;
    private String orderStatus;
    private BigDecimal subtotal;
    private BigDecimal discountAmount;
    private BigDecimal shippingFee;
    private BigDecimal taxAmount;
    private BigDecimal totalAmount;
    private String couponCode;
    private String trackingNumber;
    private LocalDateTime estimatedDeliveryDate;
    private String notes;
    private String idempotencyKey;
    private LocalDateTime reservationExpiresAt;
    @Builder.Default
    private List<OrderItemResponse> items = new ArrayList<>();
    private PaymentResponse payment;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static OrderResponse fromEntity(Order order) {
        if (order == null) return null;
        return OrderResponse.builder()
                .id(order.getId())
                .orderNumber(order.getOrderNumber())
                .userId(order.getUser() != null ? order.getUser().getId() : null)
                .customerEmail(order.getUser() != null ? order.getUser().getEmail() : null)
                .shippingAddress(order.getShippingAddress())
                .billingAddress(order.getBillingAddress())
                .orderStatus(order.getOrderStatus().name())
                .subtotal(order.getSubtotal())
                .discountAmount(order.getDiscountAmount())
                .shippingFee(order.getShippingFee())
                .taxAmount(order.getTaxAmount())
                .totalAmount(order.getTotalAmount())
                .couponCode(order.getCoupon() != null ? order.getCoupon().getCode() : null)
                .trackingNumber(order.getTrackingNumber())
                .estimatedDeliveryDate(order.getEstimatedDeliveryDate())
                .notes(order.getNotes())
                .idempotencyKey(order.getIdempotencyKey())
                .reservationExpiresAt(order.getReservationExpiresAt())
                .items(order.getItems() != null ?
                        order.getItems().stream().map(OrderItemResponse::fromEntity).collect(Collectors.toList()) :
                        new ArrayList<>())
                .payment(PaymentResponse.fromEntity(order.getPayment()))
                .createdAt(order.getCreatedAt())
                .updatedAt(order.getUpdatedAt())
                .build();
    }
}
