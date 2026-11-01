package com.shopsphere.service;

import com.shopsphere.dto.request.order.CancelOrderRequest;
import com.shopsphere.dto.request.order.CheckoutPreviewRequest;
import com.shopsphere.dto.request.order.CheckoutRequest;
import com.shopsphere.dto.request.order.UpdateOrderStatusRequest;
import com.shopsphere.dto.response.PageResponse;
import com.shopsphere.dto.response.order.CheckoutPreviewResponse;
import com.shopsphere.dto.response.order.OrderTrackingResponse;
import com.shopsphere.dto.response.order.OrderResponse;
import com.shopsphere.entity.enums.OrderStatus;
import org.springframework.data.domain.Pageable;

public interface OrderService {

    CheckoutPreviewResponse previewCheckout(Long userId, CheckoutPreviewRequest request);

    OrderResponse checkout(Long userId, CheckoutRequest request);

    PageResponse<OrderResponse> getCustomerOrders(Long userId, Pageable pageable);

    OrderResponse getCustomerOrderById(Long userId, Long orderId);

    OrderTrackingResponse trackOrderByNumber(String orderNumber);

    OrderResponse cancelOrder(Long userId, Long orderId, CancelOrderRequest request);

    PageResponse<OrderResponse> getAllOrders(OrderStatus status, Pageable pageable);

    OrderResponse updateOrderStatus(Long orderId, UpdateOrderStatusRequest request);

    void cleanupExpiredReservations();
}
