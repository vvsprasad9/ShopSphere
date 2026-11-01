package com.shopsphere.service.impl;

import com.shopsphere.dto.request.order.CancelOrderRequest;
import com.shopsphere.dto.request.order.CheckoutPreviewRequest;
import com.shopsphere.dto.request.order.CheckoutRequest;
import com.shopsphere.dto.request.order.UpdateOrderStatusRequest;
import com.shopsphere.dto.request.payment.PaymentRefundRequest;
import com.shopsphere.dto.response.PageResponse;
import com.shopsphere.dto.response.cart.CartItemResponse;
import com.shopsphere.dto.response.order.CheckoutPreviewResponse;
import com.shopsphere.dto.response.order.OrderResponse;
import com.shopsphere.dto.response.order.OrderTrackingResponse;
import com.shopsphere.entity.*;
import com.shopsphere.entity.enums.NotificationType;
import com.shopsphere.entity.enums.OrderStatus;
import com.shopsphere.entity.enums.PaymentMethod;
import com.shopsphere.entity.enums.PaymentStatus;
import com.shopsphere.exception.BadRequestException;
import com.shopsphere.exception.ConflictException;
import com.shopsphere.exception.ResourceNotFoundException;
import com.shopsphere.repository.*;
import com.shopsphere.service.*;
import com.shopsphere.util.OrderNumberGenerator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderServiceImpl implements OrderService {

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final CartRepository cartRepository;
    private final CartService cartService;
    private final AddressRepository addressRepository;
    private final CouponRepository couponRepository;
    private final UserRepository userRepository;
    private final InventoryService inventoryService;
    private final PaymentService paymentService;
    private final IdempotencyService idempotencyService;
    private final OrderNumberGenerator orderNumberGenerator;
    private final NotificationService notificationService;

    @Override
    @Transactional(readOnly = true)
    public CheckoutPreviewResponse previewCheckout(Long userId, CheckoutPreviewRequest request) {
        log.info("Previewing checkout for user id: {}", userId);
        Cart cart = cartRepository.findByUserIdWithItems(userId)
                .orElseThrow(() -> new BadRequestException("Shopping cart is empty"));

        if (cart.getItems() == null || cart.getItems().isEmpty()) {
            throw new BadRequestException("Shopping cart is empty");
        }

        List<CartItemResponse> itemResponses = new ArrayList<>();
        List<String> warnings = new ArrayList<>();
        boolean hasStockIssues = false;
        int totalQuantity = 0;
        BigDecimal subtotal = BigDecimal.ZERO;

        for (CartItem item : cart.getItems()) {
            Product product = item.getProduct();
            ProductVariant variant = item.getVariant();

            BigDecimal currentPrice = calculateCurrentItemPrice(product, variant);
            BigDecimal lineTotal = currentPrice.multiply(BigDecimal.valueOf(item.getQuantity())).setScale(2, RoundingMode.HALF_UP);
            subtotal = subtotal.add(lineTotal);
            totalQuantity += item.getQuantity();

            int purchasable = inventoryService.getPurchasableStock(product, variant);
            if (purchasable < item.getQuantity()) {
                hasStockIssues = true;
                String itemName = variant != null ? (product.getName() + " (" + variant.getSku() + ")") : product.getName();
                warnings.add("Insufficient stock for " + itemName + ". Available: " + purchasable + ", In Cart: " + item.getQuantity());
            }

            CartItemResponse itemResp = CartItemResponse.fromEntity(item);
            itemResp.setUnitPrice(currentPrice);
            itemResp.setTotalPrice(lineTotal);
            itemResponses.add(itemResp);
        }

        // Coupon calculation for preview
        BigDecimal discountAmount = BigDecimal.ZERO;
        boolean couponApplied = false;
        String couponMessage = null;

        if (request != null && request.getCouponCode() != null && !request.getCouponCode().trim().isEmpty()) {
            String code = request.getCouponCode().trim();
            Optional<Coupon> couponOpt = couponRepository.findByCodeIgnoreCase(code);
            if (couponOpt.isEmpty()) {
                couponMessage = "Invalid coupon code";
            } else {
                Coupon coupon = couponOpt.get();
                LocalDateTime now = LocalDateTime.now();
                if (!coupon.isActive()) {
                    couponMessage = "Coupon is inactive";
                } else if (now.isBefore(coupon.getStartDate()) || now.isAfter(coupon.getExpirationDate())) {
                    couponMessage = "Coupon has expired or is not yet active";
                } else if (coupon.getTimesUsed() >= coupon.getUsageLimit()) {
                    couponMessage = "Coupon usage limit has been reached";
                } else if (coupon.getMinOrderValue() != null && subtotal.compareTo(coupon.getMinOrderValue()) < 0) {
                    couponMessage = "Minimum order value is ₹" + coupon.getMinOrderValue();
                } else {
                    long userCount = orderRepository.countByCouponIdAndUserIdAndOrderStatusNot(coupon.getId(), userId, OrderStatus.CANCELLED);
                    if (userCount >= coupon.getPerUserLimit()) {
                        couponMessage = "You have already used this coupon the maximum allowed times";
                    } else {
                        discountAmount = coupon.computeDiscount(subtotal);
                        couponApplied = true;
                        couponMessage = "Coupon applied successfully! You saved ₹" + discountAmount;
                    }
                }
            }
        }

        BigDecimal taxableAmount = subtotal.subtract(discountAmount).max(BigDecimal.ZERO);
        BigDecimal shippingFee = taxableAmount.compareTo(BigDecimal.valueOf(500)) >= 0 ?
                BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP) :
                BigDecimal.valueOf(50).setScale(2, RoundingMode.HALF_UP);
        BigDecimal taxAmount = taxableAmount.multiply(new BigDecimal("0.05")).setScale(2, RoundingMode.HALF_UP);
        BigDecimal totalAmount = taxableAmount.add(shippingFee).add(taxAmount);

        // Address resolution for preview
        AddressSnapshot shippingAddress = null;
        if (request != null) {
            if (request.getShippingAddressId() != null) {
                shippingAddress = addressRepository.findByIdAndUserId(request.getShippingAddressId(), userId)
                        .map(AddressSnapshot::fromAddress)
                        .orElse(null);
            } else if (request.getNewShippingAddress() != null) {
                shippingAddress = AddressSnapshot.fromAddressRequest(request.getNewShippingAddress());
            }
        }
        if (shippingAddress == null) {
            shippingAddress = addressRepository.findByUserIdAndIsDefaultTrue(userId)
                    .map(AddressSnapshot::fromAddress)
                    .orElse(null);
        }

        return CheckoutPreviewResponse.builder()
                .items(itemResponses)
                .totalQuantity(totalQuantity)
                .subtotal(subtotal)
                .discountAmount(discountAmount)
                .shippingFee(shippingFee)
                .taxAmount(taxAmount)
                .totalAmount(totalAmount)
                .couponCode(request != null ? request.getCouponCode() : null)
                .couponApplied(couponApplied)
                .couponMessage(couponMessage)
                .shippingAddress(shippingAddress)
                .billingAddress(shippingAddress)
                .hasStockIssues(hasStockIssues)
                .warnings(warnings)
                .build();
    }

    @Override
    @Transactional
    public OrderResponse checkout(Long userId, CheckoutRequest request) {
        log.info("Processing checkout for user id: {}", userId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        Optional<Cart> cartOpt = cartRepository.findByUserIdWithItems(userId);

        // Check Idempotency Key with canonical request fingerprinting
        if (request.getIdempotencyKey() != null && !request.getIdempotencyKey().trim().isEmpty()) {
            String key = request.getIdempotencyKey().trim();
            Cart cartForHash = cartOpt.orElse(null);
            boolean cartHasItems = cartForHash != null && cartForHash.getItems() != null && !cartForHash.getItems().isEmpty();

            Optional<IdempotencyRecord> recordOpt = idempotencyService.getRecord(key, userId);
            Optional<Order> existing = orderRepository.findByIdempotencyKeyAndUserId(key, userId);

            if (recordOpt.isPresent()) {
                IdempotencyRecord record = recordOpt.get();
                if (cartHasItems) {
                    String requestHash = computeCheckoutFingerprint(request, cartForHash);
                    if (record.getRequestHash() != null && !record.getRequestHash().equals(requestHash)) {
                        log.warn("Idempotency key reuse detected with differing payload for key: {}", key);
                        throw new ConflictException("Idempotency key reuse detected with differing request payload");
                    }
                } else if (existing.isPresent()) {
                    Order prev = existing.get();
                    if (request.getPaymentMethod() != null && prev.getPayment() != null
                            && request.getPaymentMethod() != prev.getPayment().getPaymentMethod()) {
                        throw new ConflictException("Idempotency key reuse detected with differing request payload");
                    }
                }
                if (existing.isPresent()) {
                    log.info("Returning existing order for idempotency key: {}", key);
                    return OrderResponse.fromEntity(existing.get());
                }
            } else if (existing.isPresent()) {
                return OrderResponse.fromEntity(existing.get());
            } else {
                String requestHash = computeCheckoutFingerprint(request, cartForHash);
                idempotencyService.recordPending(key, userId, "/api/v1/orders/checkout", requestHash, 1440);
            }
        }

        Cart cart = cartOpt.orElseThrow(() -> new BadRequestException("Shopping cart is empty"));
        if (cart.getItems() == null || cart.getItems().isEmpty()) {
            throw new BadRequestException("Shopping cart is empty");
        }

        // Resolve addresses
        AddressSnapshot shippingAddress = resolveShippingAddress(userId, request);
        AddressSnapshot billingAddress = resolveBillingAddress(userId, request, shippingAddress);

        // Reserve stock and create order items with deterministic pessimistic lock ordering
        List<OrderItem> orderItems = new ArrayList<>();
        BigDecimal subtotal = BigDecimal.ZERO;

        Order order = Order.builder()
                .orderNumber(orderNumberGenerator.generateUniqueOrderNumber())
                .user(user)
                .shippingAddress(shippingAddress)
                .billingAddress(billingAddress)
                .orderStatus(OrderStatus.PENDING)
                .idempotencyKey(request.getIdempotencyKey())
                .notes(request.getNotes())
                .reservationExpiresAt(LocalDateTime.now().plusMinutes(15))
                .build();

        // Deterministic lock ordering: sort by variant/product composite key
        List<CartItem> sortedCartItems = new ArrayList<>(cart.getItems());
        sortedCartItems.sort(Comparator.comparing(item -> {
            if (item.getVariant() != null && item.getVariant().getId() != null) {
                return "VARIANT:" + item.getVariant().getId();
            } else if (item.getProduct() != null && item.getProduct().getId() != null) {
                return "PRODUCT:" + item.getProduct().getId();
            }
            return "";
        }));

        for (CartItem cartItem : sortedCartItems) {
            Product product = cartItem.getProduct();
            ProductVariant variant = cartItem.getVariant();

            if (!product.isActive()) {
                throw new BadRequestException("Product is no longer available: " + product.getName());
            }

            // Atomic pessimistic inventory reservation
            inventoryService.reserveStock(product, variant, cartItem.getQuantity());

            BigDecimal unitPrice = calculateCurrentItemPrice(product, variant);
            BigDecimal lineTotal = unitPrice.multiply(BigDecimal.valueOf(cartItem.getQuantity())).setScale(2, RoundingMode.HALF_UP);
            subtotal = subtotal.add(lineTotal);

            String primaryImg = null;
            if (product.getImages() != null && !product.getImages().isEmpty()) {
                primaryImg = product.getImages().stream()
                        .filter(ProductImage::isPrimary)
                        .findFirst()
                        .map(ProductImage::getImageUrl)
                        .orElse(product.getImages().iterator().next().getImageUrl());
            }

            OrderItem orderItem = OrderItem.builder()
                    .order(order)
                    .product(product)
                    .variant(variant)
                    .productName(product.getName())
                    .productSku(variant != null ? variant.getSku() : product.getSku())
                    .productImageUrl(primaryImg)
                    .unitPrice(unitPrice)
                    .quantity(cartItem.getQuantity())
                    .totalPrice(lineTotal)
                    .build();

            orderItems.add(orderItem);
        }
        order.setItems(orderItems);

        // Apply coupon if supplied
        BigDecimal discountAmount = BigDecimal.ZERO;
        if (request.getCouponCode() != null && !request.getCouponCode().trim().isEmpty()) {
            String code = request.getCouponCode().trim();
            Coupon coupon = couponRepository.findByCodeIgnoreCase(code)
                    .orElseThrow(() -> new BadRequestException("Invalid coupon code: " + code));

            LocalDateTime now = LocalDateTime.now();
            if (!coupon.isActive()) {
                throw new BadRequestException("Coupon is no longer active");
            }
            if (now.isBefore(coupon.getStartDate()) || now.isAfter(coupon.getExpirationDate())) {
                throw new BadRequestException("Coupon has expired or is not yet active");
            }
            if (coupon.getTimesUsed() >= coupon.getUsageLimit()) {
                throw new BadRequestException("Coupon usage limit has been reached");
            }
            if (coupon.getMinOrderValue() != null && subtotal.compareTo(coupon.getMinOrderValue()) < 0) {
                throw new BadRequestException("Minimum order value for coupon is ₹" + coupon.getMinOrderValue());
            }
            long userCount = orderRepository.countByCouponIdAndUserIdAndOrderStatusNot(coupon.getId(), userId, OrderStatus.CANCELLED);
            if (userCount >= coupon.getPerUserLimit()) {
                throw new BadRequestException("You have already used this coupon the maximum allowed times");
            }

            discountAmount = coupon.computeDiscount(subtotal);
            coupon.setTimesUsed(coupon.getTimesUsed() + 1);
            couponRepository.save(coupon);
            order.setCoupon(coupon);
        }

        BigDecimal taxableAmount = subtotal.subtract(discountAmount).max(BigDecimal.ZERO);
        BigDecimal shippingFee = taxableAmount.compareTo(BigDecimal.valueOf(500)) >= 0 ?
                BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP) :
                BigDecimal.valueOf(50).setScale(2, RoundingMode.HALF_UP);
        BigDecimal taxAmount = taxableAmount.multiply(new BigDecimal("0.05")).setScale(2, RoundingMode.HALF_UP);
        BigDecimal totalAmount = taxableAmount.add(shippingFee).add(taxAmount);

        order.setSubtotal(subtotal);
        order.setDiscountAmount(discountAmount);
        order.setShippingFee(shippingFee);
        order.setTaxAmount(taxAmount);
        order.setTotalAmount(totalAmount);

        // COD handling: instantly confirm order & commit inventory
        if (request.getPaymentMethod() == PaymentMethod.COD) {
            order.setOrderStatus(OrderStatus.CONFIRMED);
            order.setReservationExpiresAt(null);
            for (OrderItem item : order.getItems()) {
                inventoryService.commitReservedStock(item.getProduct(), item.getVariant(), item.getQuantity());
            }
        }

        Order savedOrder = orderRepository.save(order);

        // Initiate payment gateway record
        Payment payment = paymentService.initiatePayment(savedOrder, request.getPaymentMethod());
        savedOrder.setPayment(payment);

        // Clear user's cart
        cartService.clearCart(userId);

        if (request.getPaymentMethod() == PaymentMethod.COD) {
            notificationService.sendNotification(userId, "Order Placed Successfully",
                    "Your order #" + savedOrder.getOrderNumber() + " has been placed and confirmed.",
                    NotificationType.ORDER_PLACED, "/orders/" + savedOrder.getId(), "order_placed_" + savedOrder.getId());
            notificationService.sendNotification(userId, "Order Confirmed",
                    "Your order #" + savedOrder.getOrderNumber() + " is confirmed.",
                    NotificationType.ORDER_CONFIRMED, "/orders/" + savedOrder.getId(), "order_confirmed_" + savedOrder.getId());
        } else {
            notificationService.sendNotification(userId, "Order Placed",
                    "Your order #" + savedOrder.getOrderNumber() + " has been placed. Please complete payment.",
                    NotificationType.ORDER_PLACED, "/orders/" + savedOrder.getId(), "order_placed_" + savedOrder.getId());
        }

        if (request.getIdempotencyKey() != null && !request.getIdempotencyKey().trim().isEmpty()) {
            idempotencyService.completeRecord(request.getIdempotencyKey().trim(), 201, savedOrder.getOrderNumber());
        }

        log.info("Order placed successfully with order number: {}", savedOrder.getOrderNumber());
        return OrderResponse.fromEntity(savedOrder);
    }

    private String computeCheckoutFingerprint(CheckoutRequest request, Cart cart) {
        StringBuilder sb = new StringBuilder();
        if (request != null) {
            sb.append("shipId=").append(request.getShippingAddressId()).append(";");
            if (request.getNewShippingAddress() != null) {
                sb.append("newShip=").append(request.getNewShippingAddress().getAddressLine1())
                        .append("|").append(request.getNewShippingAddress().getPostalCode()).append(";");
            }
            sb.append("billId=").append(request.getBillingAddressId()).append(";");
            sb.append("method=").append(request.getPaymentMethod() != null ? request.getPaymentMethod().name() : "").append(";");
            sb.append("coupon=").append(request.getCouponCode() != null ? request.getCouponCode().trim().toUpperCase() : "").append(";");
        }
        if (cart != null && cart.getItems() != null && !cart.getItems().isEmpty()) {
            List<CartItem> items = new ArrayList<>(cart.getItems());
            items.sort(Comparator.comparing(item -> {
                if (item.getVariant() != null && item.getVariant().getId() != null) {
                    return "V:" + item.getVariant().getId();
                } else if (item.getProduct() != null && item.getProduct().getId() != null) {
                    return "P:" + item.getProduct().getId();
                }
                return "";
            }));
            sb.append("items=[");
            for (CartItem item : items) {
                Long pId = item.getProduct() != null ? item.getProduct().getId() : 0L;
                Long vId = item.getVariant() != null ? item.getVariant().getId() : 0L;
                sb.append(pId).append(":").append(vId).append(":").append(item.getQuantity()).append(",");
            }
            sb.append("]");
        }
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(sb.toString().getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (Exception e) {
            return Integer.toHexString(sb.toString().hashCode());
        }
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<OrderResponse> getCustomerOrders(Long userId, Pageable pageable) {
        Page<Order> page = orderRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable);
        return PageResponse.fromPage(page.map(OrderResponse::fromEntity));
    }

    @Override
    @Transactional(readOnly = true)
    public OrderResponse getCustomerOrderById(Long userId, Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with id: " + orderId));

        if (userId != null && order.getUser() != null && !userId.equals(order.getUser().getId())) {
            throw new ResourceNotFoundException("Order not found with id: " + orderId);
        }

        return OrderResponse.fromEntity(order);
    }

    @Override
    @Transactional(readOnly = true)
    public OrderTrackingResponse trackOrderByNumber(String orderNumber) {
        Order order = orderRepository.findByOrderNumber(orderNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with order number: " + orderNumber));
        return OrderTrackingResponse.fromEntity(order);
    }

    @Override
    @Transactional
    public OrderResponse cancelOrder(Long userId, Long orderId, CancelOrderRequest request) {
        log.info("Cancelling order id: {} by user: {}", orderId, userId);
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with id: " + orderId));

        if (userId != null && order.getUser() != null && !userId.equals(order.getUser().getId())) {
            throw new BadRequestException("You are not authorized to cancel this order");
        }

        OrderStatus currentStatus = order.getOrderStatus();
        if (currentStatus != OrderStatus.PENDING && currentStatus != OrderStatus.CONFIRMED) {
            throw new BadRequestException("Order cannot be cancelled in " + currentStatus + " status");
        }

        if (currentStatus == OrderStatus.PENDING) {
            // Release reserved stock
            for (OrderItem item : order.getItems()) {
                inventoryService.releaseReservedStock(item.getProduct(), item.getVariant(), item.getQuantity());
            }
            if (order.getPayment() != null) {
                order.getPayment().setPaymentStatus(PaymentStatus.FAILED);
            }
        } else if (currentStatus == OrderStatus.CONFIRMED) {
            // Restore committed stock
            for (OrderItem item : order.getItems()) {
                inventoryService.restoreCommittedStock(item.getProduct(), item.getVariant(), item.getQuantity());
            }
            if (order.getPayment() != null && order.getPayment().getPaymentStatus() == PaymentStatus.SUCCESS) {
                paymentService.refundPayment(userId, PaymentRefundRequest.builder()
                        .orderId(orderId)
                        .reason(request != null ? request.getReason() : "Customer cancelled order")
                        .build());
            }
        }

        order.setOrderStatus(OrderStatus.CANCELLED);
        String cancelNote = "Cancelled: " + (request != null && request.getReason() != null ? request.getReason() : "By customer");
        order.setNotes(order.getNotes() != null ? (order.getNotes() + " | " + cancelNote) : cancelNote);
        orderRepository.save(order);

        if (order.getUser() != null) {
            notificationService.sendNotification(order.getUser().getId(), "Order Cancelled",
                    "Your order #" + order.getOrderNumber() + " has been cancelled.",
                    NotificationType.ORDER_CANCELLED, "/orders/" + order.getId(), "order_cancelled_" + order.getId());
        }

        log.info("Order {} cancelled successfully", order.getOrderNumber());
        return OrderResponse.fromEntity(order);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<OrderResponse> getAllOrders(OrderStatus status, Pageable pageable) {
        Page<Order> page;
        if (status != null) {
            page = orderRepository.findByOrderStatus(status, pageable);
        } else {
            page = orderRepository.findAll(pageable);
        }
        return PageResponse.fromPage(page.map(OrderResponse::fromEntity));
    }

    @Override
    @Transactional
    public OrderResponse updateOrderStatus(Long orderId, UpdateOrderStatusRequest request) {
        log.info("Updating order status for id: {} to: {}", orderId, request.getStatus());
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with id: " + orderId));

        OrderStatus current = order.getOrderStatus();
        OrderStatus target = request.getStatus();

        validateOrderStatusTransition(current, target);

        // If transitioning to CANCELLED by admin:
        if (target == OrderStatus.CANCELLED) {
            if (current == OrderStatus.PENDING) {
                for (OrderItem item : order.getItems()) {
                    inventoryService.releaseReservedStock(item.getProduct(), item.getVariant(), item.getQuantity());
                }
            } else {
                for (OrderItem item : order.getItems()) {
                    inventoryService.restoreCommittedStock(item.getProduct(), item.getVariant(), item.getQuantity());
                }
                if (order.getPayment() != null && order.getPayment().getPaymentStatus() == PaymentStatus.SUCCESS) {
                    paymentService.refundPayment(null, PaymentRefundRequest.builder()
                            .orderId(orderId)
                            .reason(request.getNotes() != null ? request.getNotes() : "Administrative cancellation")
                            .build());
                }
            }
        }

        order.setOrderStatus(target);
        if (request.getTrackingNumber() != null && !request.getTrackingNumber().trim().isEmpty()) {
            order.setTrackingNumber(request.getTrackingNumber().trim());
        }
        if (request.getEstimatedDeliveryDate() != null) {
            order.setEstimatedDeliveryDate(request.getEstimatedDeliveryDate());
        }
        if (request.getNotes() != null && !request.getNotes().trim().isEmpty()) {
            order.setNotes(request.getNotes().trim());
        }

        Order saved = orderRepository.save(order);

        if (saved.getUser() != null) {
            Long uId = saved.getUser().getId();
            String num = saved.getOrderNumber();
            Long oId = saved.getId();
            if (target == OrderStatus.CONFIRMED) {
                notificationService.sendNotification(uId, "Order Confirmed",
                        "Your order #" + num + " has been confirmed.",
                        NotificationType.ORDER_CONFIRMED, "/orders/" + oId, "order_confirmed_" + oId);
            } else if (target == OrderStatus.SHIPPED) {
                notificationService.sendNotification(uId, "Order Shipped",
                        "Your order #" + num + " has been shipped." + (saved.getTrackingNumber() != null ? " Tracking: " + saved.getTrackingNumber() : ""),
                        NotificationType.ORDER_SHIPPED, "/orders/" + oId, "order_shipped_" + oId);
            } else if (target == OrderStatus.OUT_FOR_DELIVERY) {
                notificationService.sendNotification(uId, "Out for Delivery",
                        "Your order #" + num + " is out for delivery today.",
                        NotificationType.ORDER_OUT_FOR_DELIVERY, "/orders/" + oId, "order_out_delivery_" + oId);
            } else if (target == OrderStatus.DELIVERED) {
                notificationService.sendNotification(uId, "Order Delivered",
                        "Your order #" + num + " has been delivered. Thank you for shopping with us!",
                        NotificationType.ORDER_DELIVERED, "/orders/" + oId, "order_delivered_" + oId);
            } else if (target == OrderStatus.CANCELLED) {
                notificationService.sendNotification(uId, "Order Cancelled",
                        "Your order #" + num + " has been cancelled.",
                        NotificationType.ORDER_CANCELLED, "/orders/" + oId, "order_cancelled_admin_" + oId);
            }
        }

        log.info("Order {} status updated to {}", saved.getOrderNumber(), target);
        return OrderResponse.fromEntity(saved);
    }

    @Override
    @Transactional
    public void cleanupExpiredReservations() {
        log.info("Running expired reservation cleanup");
        List<Order> expiredOrders = orderRepository.findByOrderStatusAndReservationExpiresAtBefore(
                OrderStatus.PENDING, LocalDateTime.now()
        );

        for (Order order : expiredOrders) {
            log.info("Expiring reservation for order {}", order.getOrderNumber());
            for (OrderItem item : order.getItems()) {
                inventoryService.releaseReservedStock(item.getProduct(), item.getVariant(), item.getQuantity());
            }
            order.setOrderStatus(OrderStatus.CANCELLED);
            order.setNotes("Reservation expired due to non-payment");
            if (order.getPayment() != null) {
                order.getPayment().setPaymentStatus(PaymentStatus.FAILED);
            }
            orderRepository.save(order);

            if (order.getUser() != null) {
                notificationService.sendNotification(order.getUser().getId(), "Order Reservation Expired",
                        "Your order #" + order.getOrderNumber() + " reservation expired due to non-payment.",
                        NotificationType.ORDER_CANCELLED, "/orders/" + order.getId(), "order_expired_" + order.getId());
            }
        }
        log.info("Completed expired reservation cleanup. Cancelled {} orders", expiredOrders.size());
    }

    private void validateOrderStatusTransition(OrderStatus current, OrderStatus target) {
        if (current == target) return;

        if (current == OrderStatus.CANCELLED || current == OrderStatus.RETURNED) {
            throw new BadRequestException("Cannot change status of an order in terminal status: " + current);
        }

        boolean valid = switch (current) {
            case PENDING -> target == OrderStatus.CONFIRMED || target == OrderStatus.CANCELLED;
            case CONFIRMED -> target == OrderStatus.PROCESSING || target == OrderStatus.CANCELLED;
            case PROCESSING -> target == OrderStatus.SHIPPED || target == OrderStatus.CANCELLED;
            case SHIPPED -> target == OrderStatus.OUT_FOR_DELIVERY || target == OrderStatus.DELIVERED || target == OrderStatus.CANCELLED;
            case OUT_FOR_DELIVERY -> target == OrderStatus.DELIVERED || target == OrderStatus.CANCELLED;
            case DELIVERED -> target == OrderStatus.RETURNED;
            default -> false;
        };

        if (!valid) {
            throw new BadRequestException("Invalid status transition from " + current + " to " + target);
        }
    }

    private AddressSnapshot resolveShippingAddress(Long userId, CheckoutRequest request) {
        if (request.getShippingAddressId() != null) {
            Address address = addressRepository.findByIdAndUserId(request.getShippingAddressId(), userId)
                    .orElseThrow(() -> new ResourceNotFoundException("Shipping address not found or not owned by you"));
            return AddressSnapshot.fromAddress(address);
        } else if (request.getNewShippingAddress() != null) {
            return AddressSnapshot.fromAddressRequest(request.getNewShippingAddress());
        } else {
            return addressRepository.findByUserIdAndIsDefaultTrue(userId)
                    .map(AddressSnapshot::fromAddress)
                    .orElseThrow(() -> new BadRequestException("Shipping address is required"));
        }
    }

    private AddressSnapshot resolveBillingAddress(Long userId, CheckoutRequest request, AddressSnapshot defaultShipping) {
        if (request.getBillingAddressId() != null) {
            Address address = addressRepository.findByIdAndUserId(request.getBillingAddressId(), userId)
                    .orElseThrow(() -> new ResourceNotFoundException("Billing address not found or not owned by you"));
            return AddressSnapshot.fromAddress(address);
        } else if (request.getNewBillingAddress() != null) {
            return AddressSnapshot.fromAddressRequest(request.getNewBillingAddress());
        }
        return defaultShipping;
    }

    private BigDecimal calculateCurrentItemPrice(Product product, ProductVariant variant) {
        BigDecimal base = product.getDiscountPrice() != null ? product.getDiscountPrice() : product.getPrice();
        if (variant != null && variant.getPriceAdjustment() != null) {
            base = base.add(variant.getPriceAdjustment());
        }
        return base;
    }
}
