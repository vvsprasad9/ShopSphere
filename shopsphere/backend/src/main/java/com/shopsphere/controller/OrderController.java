package com.shopsphere.controller;

import com.shopsphere.dto.request.order.CancelOrderRequest;
import com.shopsphere.dto.request.order.CheckoutPreviewRequest;
import com.shopsphere.dto.request.order.CheckoutRequest;
import com.shopsphere.dto.request.order.UpdateOrderStatusRequest;
import com.shopsphere.dto.response.ApiResponse;
import com.shopsphere.dto.response.PageResponse;
import com.shopsphere.dto.response.order.CheckoutPreviewResponse;
import com.shopsphere.dto.response.order.OrderResponse;
import com.shopsphere.entity.enums.OrderStatus;
import com.shopsphere.security.UserPrincipal;
import com.shopsphere.service.OrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/orders")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Order Processing", description = "APIs for checkout, orders, tracking, and management")
public class OrderController {

    private final OrderService orderService;

    @PostMapping("/checkout/preview")
    @Operation(summary = "Preview checkout totals, taxes, shipping and discounts before ordering")
    public ResponseEntity<ApiResponse<CheckoutPreviewResponse>> previewCheckout(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @RequestBody(required = false) CheckoutPreviewRequest request
    ) {
        CheckoutPreviewResponse preview = orderService.previewCheckout(userPrincipal.getId(), request);
        return ResponseEntity.ok(ApiResponse.success("Checkout preview calculated successfully", preview));
    }

    @PostMapping("/checkout")
    @Operation(summary = "Place order from active shopping cart")
    public ResponseEntity<ApiResponse<OrderResponse>> checkout(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKeyHeader,
            @Valid @RequestBody CheckoutRequest request
    ) {
        if (request.getIdempotencyKey() == null && idempotencyKeyHeader != null) {
            request.setIdempotencyKey(idempotencyKeyHeader.trim());
        }
        OrderResponse orderResponse = orderService.checkout(userPrincipal.getId(), request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Order placed successfully", orderResponse));
    }

    @GetMapping
    @Operation(summary = "Get authenticated customer's order history")
    public ResponseEntity<ApiResponse<PageResponse<OrderResponse>>> getCustomerOrders(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String direction
    ) {
        Sort sort = "asc".equalsIgnoreCase(direction) ? Sort.by(sortBy).ascending() : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(page, size, sort);
        PageResponse<OrderResponse> orders = orderService.getCustomerOrders(userPrincipal.getId(), pageable);
        return ResponseEntity.ok(ApiResponse.success("Orders retrieved successfully", orders));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get customer order details by order ID")
    public ResponseEntity<ApiResponse<OrderResponse>> getOrderById(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @PathVariable Long id
    ) {
        OrderResponse order = orderService.getCustomerOrderById(userPrincipal.getId(), id);
        return ResponseEntity.ok(ApiResponse.success("Order retrieved successfully", order));
    }

    @PostMapping("/{id}/cancel")
    @Operation(summary = "Cancel an eligible customer order")
    public ResponseEntity<ApiResponse<OrderResponse>> cancelOrder(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @PathVariable Long id,
            @RequestBody(required = false) CancelOrderRequest request
    ) {
        OrderResponse cancelledOrder = orderService.cancelOrder(userPrincipal.getId(), id, request);
        return ResponseEntity.ok(ApiResponse.success("Order cancelled successfully", cancelledOrder));
    }

    @GetMapping("/track/{orderNumber}")
    @Operation(summary = "Public tracking of order status by order number")
    public ResponseEntity<ApiResponse<com.shopsphere.dto.response.order.OrderTrackingResponse>> trackOrder(
            @PathVariable String orderNumber
    ) {
        com.shopsphere.dto.response.order.OrderTrackingResponse trackingInfo = orderService.trackOrderByNumber(orderNumber);
        return ResponseEntity.ok(ApiResponse.success("Order tracking info retrieved", trackingInfo));
    }

    @GetMapping("/admin")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @Operation(summary = "Admin/Manager list all orders with optional status filter")
    public ResponseEntity<ApiResponse<PageResponse<OrderResponse>>> getAllOrders(
            @RequestParam(required = false) OrderStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String direction
    ) {
        Sort sort = "asc".equalsIgnoreCase(direction) ? Sort.by(sortBy).ascending() : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(page, size, sort);
        PageResponse<OrderResponse> orders = orderService.getAllOrders(status, pageable);
        return ResponseEntity.ok(ApiResponse.success("All orders retrieved successfully", orders));
    }

    @PatchMapping("/admin/{id}/status")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @Operation(summary = "Admin/Manager update order status and tracking info")
    public ResponseEntity<ApiResponse<OrderResponse>> updateOrderStatus(
            @PathVariable Long id,
            @Valid @RequestBody UpdateOrderStatusRequest request
    ) {
        OrderResponse updated = orderService.updateOrderStatus(id, request);
        return ResponseEntity.ok(ApiResponse.success("Order status updated successfully", updated));
    }
}
