package com.shopsphere.service.impl;

import com.shopsphere.dto.request.payment.MockPaymentRequest;
import com.shopsphere.dto.request.payment.PaymentRefundRequest;
import com.shopsphere.dto.request.payment.PaymentVerificationRequest;
import com.shopsphere.dto.response.payment.PaymentGatewayOrderResponse;
import com.shopsphere.dto.response.payment.PaymentGatewayRefundResponse;
import com.shopsphere.dto.response.payment.PaymentResponse;
import com.shopsphere.entity.Order;
import com.shopsphere.entity.OrderItem;
import com.shopsphere.entity.Payment;
import com.shopsphere.entity.enums.NotificationType;
import com.shopsphere.entity.enums.OrderStatus;
import com.shopsphere.entity.enums.PaymentMethod;
import com.shopsphere.entity.enums.PaymentStatus;
import com.shopsphere.exception.BadRequestException;
import com.shopsphere.exception.PaymentException;
import com.shopsphere.exception.ResourceNotFoundException;
import com.shopsphere.payment.mock.MockPaymentGateway;
import com.shopsphere.payment.razorpay.RazorpayPaymentGateway;
import com.shopsphere.repository.OrderRepository;
import com.shopsphere.repository.PaymentRepository;
import com.shopsphere.service.IdempotencyService;
import com.shopsphere.service.InventoryService;
import com.shopsphere.service.NotificationService;
import com.shopsphere.service.PaymentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentServiceImpl implements PaymentService {

    private final PaymentRepository paymentRepository;
    private final OrderRepository orderRepository;
    private final InventoryService inventoryService;
    private final MockPaymentGateway mockPaymentGateway;
    private final RazorpayPaymentGateway razorpayPaymentGateway;
    private final IdempotencyService idempotencyService;
    private final NotificationService notificationService;

    @org.springframework.beans.factory.annotation.Value("${spring.profiles.active:dev}")
    private String activeProfile;

    @org.springframework.beans.factory.annotation.Value("${app.payment.mock-enabled:true}")
    private boolean mockEnabled;

    private boolean isProduction() {
        return activeProfile != null && ("prod".equalsIgnoreCase(activeProfile.trim()) || "production".equalsIgnoreCase(activeProfile.trim()));
    }

    @Override
    @Transactional
    public Payment initiatePayment(Order order, PaymentMethod paymentMethod) {
        log.info("Initiating payment for order {} with method {}", order.getOrderNumber(), paymentMethod);

        if (paymentMethod == PaymentMethod.MOCK_TEST && (isProduction() || !mockEnabled)) {
            throw new BadRequestException("Mock payment gateway cannot be enabled or used in production");
        }

        Payment payment = Payment.builder()
                .order(order)
                .paymentMethod(paymentMethod)
                .amount(order.getTotalAmount())
                .currency("INR")
                .paymentStatus(PaymentStatus.CREATED)
                .build();

        if (paymentMethod == PaymentMethod.RAZORPAY) {
            PaymentGatewayOrderResponse gatewayOrder = razorpayPaymentGateway.createGatewayOrder(order);
            payment.setRazorpayOrderId(gatewayOrder.getGatewayOrderId());
            payment.setPaymentGatewayResponse(gatewayOrder.getRawResponse());
        } else if (paymentMethod == PaymentMethod.MOCK_TEST) {
            PaymentGatewayOrderResponse mockOrder = mockPaymentGateway.createGatewayOrder(order);
            payment.setRazorpayOrderId(mockOrder.getGatewayOrderId());
            payment.setTransactionId(mockOrder.getTransactionId());
            payment.setPaymentGatewayResponse(mockOrder.getRawResponse());
        } else if (paymentMethod == PaymentMethod.COD) {
            payment.setTransactionId("COD-" + order.getOrderNumber());
            payment.setPaymentStatus(PaymentStatus.PENDING);
        } else if (paymentMethod == PaymentMethod.CARD) {
            payment.setTransactionId("CARD-" + UUID.randomUUID().toString().replace("-", "").substring(0, 12));
            payment.setPaymentStatus(PaymentStatus.CREATED);
        }

        return paymentRepository.save(payment);
    }

    @Override
    @Transactional(noRollbackFor = BadRequestException.class)
    public PaymentResponse verifyPayment(Long userId, PaymentVerificationRequest request) {
        log.info("Verifying payment for razorpay order id: {}", request.getRazorpayOrderId());

        Payment payment = paymentRepository.findByRazorpayOrderId(request.getRazorpayOrderId())
                .orElseThrow(() -> new ResourceNotFoundException("Payment not found for order id: " + request.getRazorpayOrderId()));

        Order order = payment.getOrder();
        if (userId != null && order.getUser() != null && !userId.equals(order.getUser().getId())) {
            log.warn("Unauthorized payment verification attempt by user {} for order owned by {}",
                    userId, order.getUser().getId());
            throw new BadRequestException("You are not authorized to verify this payment");
        }

        // Idempotency: return existing successful payment
        if (payment.getPaymentStatus() == PaymentStatus.SUCCESS) {
            log.info("Payment {} already marked as SUCCESS (idempotent call)", payment.getId());
            return PaymentResponse.fromEntity(payment);
        }

        // Verify signature with appropriate gateway
        boolean validSignature;
        if (payment.getPaymentMethod() == PaymentMethod.MOCK_TEST) {
            validSignature = mockPaymentGateway.verifySignature(
                    payment,
                    request.getRazorpayOrderId(),
                    request.getRazorpayPaymentId(),
                    request.getRazorpaySignature()
            );
        } else {
            validSignature = razorpayPaymentGateway.verifySignature(
                    payment,
                    request.getRazorpayOrderId(),
                    request.getRazorpayPaymentId(),
                    request.getRazorpaySignature()
            );
        }

        if (!validSignature) {
            log.warn("Invalid payment signature for razorpay order id: {}", request.getRazorpayOrderId());
            payment.setPaymentStatus(PaymentStatus.FAILED);
            paymentRepository.save(payment);
            if (order != null && order.getUser() != null) {
                notificationService.sendNotification(order.getUser().getId(), "Payment Failed",
                        "Payment verification failed for order #" + order.getOrderNumber() + ".",
                        NotificationType.PAYMENT_FAILED, "/orders/" + order.getId(), "pay_failed_sig_" + payment.getId());
            }
            throw new PaymentException("Invalid payment signature");
        }

        payment.setRazorpayPaymentId(request.getRazorpayPaymentId());
        payment.setRazorpaySignature(request.getRazorpaySignature());
        payment.setTransactionId(request.getRazorpayPaymentId());

        boolean isCancelled = order.getOrderStatus() == OrderStatus.CANCELLED;
        boolean isExpired = order.getReservationExpiresAt() != null && LocalDateTime.now().isAfter(order.getReservationExpiresAt());

        if (isCancelled || isExpired) {
            log.warn("Payment verified for {} order {}. Initiating automatic refund.",
                    isCancelled ? "cancelled" : "expired", order.getOrderNumber());
            payment.setPaymentStatus(PaymentStatus.REFUND_PENDING);
            paymentRepository.save(payment);

            if (!isCancelled) {
                order.setOrderStatus(OrderStatus.CANCELLED);
                orderRepository.save(order);
                for (OrderItem item : order.getItems()) {
                    inventoryService.releaseReservedStock(item.getProduct(), item.getVariant(), item.getQuantity());
                }
            }

            try {
                if (payment.getPaymentMethod() == PaymentMethod.MOCK_TEST) {
                    mockPaymentGateway.processRefund(payment, payment.getAmount(), "Order was cancelled/expired prior to payment confirmation");
                } else {
                    razorpayPaymentGateway.processRefund(payment, payment.getAmount(), "Order was cancelled/expired prior to payment confirmation");
                }
                payment.setPaymentStatus(PaymentStatus.REFUNDED);
                paymentRepository.save(payment);
                if (order != null && order.getUser() != null) {
                    notificationService.sendNotification(order.getUser().getId(), "Refund Initiated",
                            "Payment for expired/cancelled order #" + order.getOrderNumber() + " has been refunded.",
                            NotificationType.REFUND_PROCESSED, "/orders/" + order.getId(), "pay_refund_expired_" + payment.getId());
                }
                log.info("Automatic refund completed for {} order {}", isCancelled ? "cancelled" : "expired", order.getOrderNumber());
            } catch (Exception ex) {
                log.error("Automatic refund failed for order {}: {}", order.getOrderNumber(), ex.getMessage());
                payment.setPaymentStatus(PaymentStatus.REFUND_FAILED);
                paymentRepository.save(payment);
            }

            throw new BadRequestException("Order was cancelled or expired before payment confirmation. A refund has been initiated.");
        }

        // Successful verification: commit inventory and update statuses
        payment.setPaymentStatus(PaymentStatus.SUCCESS);
        paymentRepository.save(payment);

        order.setOrderStatus(OrderStatus.CONFIRMED);
        order.setReservationExpiresAt(null);
        orderRepository.save(order);

        // Commit stock atomically
        for (OrderItem item : order.getItems()) {
            inventoryService.commitReservedStock(item.getProduct(), item.getVariant(), item.getQuantity());
        }

        if (order.getUser() != null) {
            notificationService.sendNotification(order.getUser().getId(), "Payment Successful",
                    "Payment for order #" + order.getOrderNumber() + " was successful.",
                    NotificationType.PAYMENT_SUCCESS, "/orders/" + order.getId(), "pay_success_" + payment.getId());
            notificationService.sendNotification(order.getUser().getId(), "Order Confirmed",
                    "Your order #" + order.getOrderNumber() + " is confirmed.",
                    NotificationType.ORDER_CONFIRMED, "/orders/" + order.getId(), "order_confirmed_" + order.getId());
        }

        log.info("Payment {} verified successfully. Order {} confirmed.", payment.getId(), order.getOrderNumber());
        return PaymentResponse.fromEntity(payment);
    }

    @Override
    @Transactional
    public PaymentResponse processMockPayment(Long userId, MockPaymentRequest request) {
        log.info("Processing mock payment simulation for order id: {}, target status: {}",
                request.getOrderId(), request.getStatus());

        if (isProduction() || !mockEnabled) {
            throw new BadRequestException("Mock payment simulation cannot be enabled or executed in production");
        }

        Order order = orderRepository.findById(request.getOrderId())
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with id: " + request.getOrderId()));

        if (userId != null && order.getUser() != null && !userId.equals(order.getUser().getId())) {
            throw new BadRequestException("You are not authorized to process payment for this order");
        }

        Payment payment = order.getPayment();
        if (payment == null) {
            throw new BadRequestException("No payment record found for order id: " + request.getOrderId());
        }

        if (payment.getPaymentStatus() == PaymentStatus.SUCCESS) {
            return PaymentResponse.fromEntity(payment);
        }

        if (request.getStatus() == PaymentStatus.SUCCESS) {
            payment.setPaymentStatus(PaymentStatus.SUCCESS);
            String mockTxn = "txn_mock_" + UUID.randomUUID().toString().replace("-", "").substring(0, 14);
            payment.setTransactionId(mockTxn);
            payment.setRazorpayPaymentId("pay_mock_" + mockTxn);
            paymentRepository.save(payment);

            order.setOrderStatus(OrderStatus.CONFIRMED);
            order.setReservationExpiresAt(null);
            orderRepository.save(order);

            for (OrderItem item : order.getItems()) {
                inventoryService.commitReservedStock(item.getProduct(), item.getVariant(), item.getQuantity());
            }

            if (order.getUser() != null) {
                notificationService.sendNotification(order.getUser().getId(), "Payment Successful",
                        "Payment for order #" + order.getOrderNumber() + " was successful.",
                        NotificationType.PAYMENT_SUCCESS, "/orders/" + order.getId(), "pay_mock_success_" + payment.getId());
                notificationService.sendNotification(order.getUser().getId(), "Order Confirmed",
                        "Your order #" + order.getOrderNumber() + " is confirmed.",
                        NotificationType.ORDER_CONFIRMED, "/orders/" + order.getId(), "order_confirmed_" + order.getId());
            }

            log.info("Mock payment succeeded for order {}", order.getOrderNumber());
        } else if (request.getStatus() == PaymentStatus.FAILED) {
            payment.setPaymentStatus(PaymentStatus.FAILED);
            paymentRepository.save(payment);

            if (order.getUser() != null) {
                notificationService.sendNotification(order.getUser().getId(), "Payment Failed",
                        "Mock payment for order #" + order.getOrderNumber() + " failed.",
                        NotificationType.PAYMENT_FAILED, "/orders/" + order.getId(), "pay_mock_failed_" + payment.getId());
            }

            log.info("Mock payment marked as FAILED for order {}", order.getOrderNumber());
        } else {
            payment.setPaymentStatus(request.getStatus());
            paymentRepository.save(payment);
        }

        return PaymentResponse.fromEntity(payment);
    }

    @Override
    @Transactional(readOnly = true)
    public PaymentResponse getPaymentByOrderId(Long userId, Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with id: " + orderId));

        if (userId != null && order.getUser() != null && !userId.equals(order.getUser().getId())) {
            throw new BadRequestException("You are not authorized to view this payment");
        }

        Payment payment = order.getPayment();
        if (payment == null) {
            throw new ResourceNotFoundException("Payment not found for order id: " + orderId);
        }

        return PaymentResponse.fromEntity(payment);
    }

    @Override
    @Transactional
    public PaymentResponse refundPayment(Long userId, PaymentRefundRequest request) {
        log.info("Processing refund for order id: {}", request.getOrderId());

        Order order = orderRepository.findById(request.getOrderId())
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with id: " + request.getOrderId()));

        Payment payment = order.getPayment();
        if (payment == null) {
            throw new BadRequestException("No payment found for order: " + order.getOrderNumber());
        }

        if (payment.getPaymentStatus() != PaymentStatus.SUCCESS) {
            throw new BadRequestException("Cannot refund payment that is not in SUCCESS status");
        }

        BigDecimal refundAmount = request.getAmount() != null ? request.getAmount() : payment.getAmount();
        if (refundAmount.compareTo(payment.getAmount()) > 0) {
            throw new BadRequestException("Refund amount cannot exceed paid amount");
        }

        payment.setPaymentStatus(PaymentStatus.REFUND_PENDING);
        paymentRepository.save(payment);

        try {
            PaymentGatewayRefundResponse refundResponse;
            if (payment.getPaymentMethod() == PaymentMethod.MOCK_TEST) {
                refundResponse = mockPaymentGateway.processRefund(payment, refundAmount, request.getReason());
            } else {
                refundResponse = razorpayPaymentGateway.processRefund(payment, refundAmount, request.getReason());
            }

            payment.setPaymentStatus(PaymentStatus.REFUNDED);
            paymentRepository.save(payment);

            if (order.getUser() != null) {
                notificationService.sendNotification(order.getUser().getId(), "Refund Processed",
                        "Refund of ₹" + refundAmount + " for order #" + order.getOrderNumber() + " has been processed.",
                        NotificationType.REFUND_PROCESSED, "/orders/" + order.getId(), "pay_refund_" + payment.getId());
            }

            log.info("Refund {} completed for order {}", refundResponse.getRefundId(), order.getOrderNumber());
            return PaymentResponse.fromEntity(payment);
        } catch (Exception ex) {
            log.error("Refund failed for order {}: {}", order.getOrderNumber(), ex.getMessage());
            payment.setPaymentStatus(PaymentStatus.REFUND_FAILED);
            paymentRepository.save(payment);
            throw new PaymentException("Refund failed: " + ex.getMessage(), ex);
        }
    }

    @Override
    @Transactional
    public PaymentResponse markCodPaymentCollected(Long orderId) {
        log.info("Marking COD payment collected for order id: {}", orderId);
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with id: " + orderId));

        Payment payment = order.getPayment();
        if (payment == null) {
            throw new ResourceNotFoundException("Payment record not found for order id: " + orderId);
        }

        if (payment.getPaymentMethod() != PaymentMethod.COD) {
            throw new BadRequestException("Order " + order.getOrderNumber() + " is not a Cash-On-Delivery order");
        }

        if (payment.getPaymentStatus() == PaymentStatus.SUCCESS) {
            log.info("COD payment for order {} already marked as SUCCESS (idempotent)", order.getOrderNumber());
            return PaymentResponse.fromEntity(payment);
        }

        if (order.getOrderStatus() == OrderStatus.CANCELLED) {
            throw new BadRequestException("Cannot collect COD payment for cancelled order: " + order.getOrderNumber());
        }

        payment.setPaymentStatus(PaymentStatus.SUCCESS);
        paymentRepository.save(payment);

        if (order.getUser() != null) {
            notificationService.sendNotification(order.getUser().getId(), "Payment Collected",
                    "Cash payment of ₹" + order.getTotalAmount() + " collected for order #" + order.getOrderNumber() + ".",
                    NotificationType.PAYMENT_SUCCESS, "/orders/" + order.getId(), "pay_cod_success_" + payment.getId());
        }

        log.info("COD payment collected successfully for order {}", order.getOrderNumber());
        return PaymentResponse.fromEntity(payment);
    }

    @Override
    @Transactional
    public void processWebhook(String payload, String signature) {
        log.info("Processing payment webhook");

        if (!razorpayPaymentGateway.verifyWebhookSignature(payload, signature)) {
            log.warn("Invalid webhook signature received");
            throw new PaymentException("Invalid webhook signature");
        }

        try {
            JSONObject root = new JSONObject(payload);
            String event = root.optString("event");
            String eventId = root.optString("account_id") + "_" + root.optString("created_at") + "_" + event;

            // Check webhook idempotency
            if (idempotencyService.getRecord("webhook_" + eventId, null).isPresent()) {
                log.info("Webhook {} already processed (idempotent)", eventId);
                return;
            }

            if ("payment.captured".equals(event) || "order.paid".equals(event)) {
                JSONObject paymentEntity = root.getJSONObject("payload").getJSONObject("payment").getJSONObject("entity");
                String rzpOrderId = paymentEntity.optString("order_id");
                String rzpPaymentId = paymentEntity.optString("id");

                paymentRepository.findByRazorpayOrderId(rzpOrderId).ifPresent(payment -> {
                    if (payment.getPaymentStatus() != PaymentStatus.SUCCESS && payment.getPaymentStatus() != PaymentStatus.REFUNDED) {
                        payment.setRazorpayPaymentId(rzpPaymentId);
                        payment.setTransactionId(rzpPaymentId);

                        Order order = payment.getOrder();
                        boolean isCancelled = order.getOrderStatus() == OrderStatus.CANCELLED;
                        boolean isExpired = order.getReservationExpiresAt() != null && LocalDateTime.now().isAfter(order.getReservationExpiresAt());

                        if (isCancelled || isExpired) {
                            log.warn("Webhook captured payment for {} order {}. Initiating refund.",
                                    isCancelled ? "cancelled" : "expired", order.getOrderNumber());
                            payment.setPaymentStatus(PaymentStatus.REFUND_PENDING);
                            paymentRepository.save(payment);

                            if (!isCancelled) {
                                order.setOrderStatus(OrderStatus.CANCELLED);
                                orderRepository.save(order);
                                for (OrderItem item : order.getItems()) {
                                    inventoryService.releaseReservedStock(item.getProduct(), item.getVariant(), item.getQuantity());
                                }
                            }

                            try {
                                if (payment.getPaymentMethod() == PaymentMethod.MOCK_TEST) {
                                    mockPaymentGateway.processRefund(payment, payment.getAmount(), "Order was cancelled/expired prior to payment confirmation");
                                } else {
                                    razorpayPaymentGateway.processRefund(payment, payment.getAmount(), "Order was cancelled/expired prior to payment confirmation");
                                }
                                payment.setPaymentStatus(PaymentStatus.REFUNDED);
                                paymentRepository.save(payment);
                            } catch (Exception ex) {
                                log.error("Automatic webhook refund failed for order {}: {}", order.getOrderNumber(), ex.getMessage());
                                payment.setPaymentStatus(PaymentStatus.REFUND_FAILED);
                                paymentRepository.save(payment);
                            }
                        } else {
                            payment.setPaymentStatus(PaymentStatus.SUCCESS);
                            paymentRepository.save(payment);

                            order.setOrderStatus(OrderStatus.CONFIRMED);
                            order.setReservationExpiresAt(null);
                            orderRepository.save(order);

                            for (OrderItem item : order.getItems()) {
                                inventoryService.commitReservedStock(item.getProduct(), item.getVariant(), item.getQuantity());
                            }

                            if (order.getUser() != null) {
                                notificationService.sendNotification(order.getUser().getId(), "Payment Successful",
                                        "Payment for order #" + order.getOrderNumber() + " captured successfully.",
                                        NotificationType.PAYMENT_SUCCESS, "/orders/" + order.getId(), "pay_webhook_success_" + payment.getId());
                                notificationService.sendNotification(order.getUser().getId(), "Order Confirmed",
                                        "Your order #" + order.getOrderNumber() + " is confirmed.",
                                        NotificationType.ORDER_CONFIRMED, "/orders/" + order.getId(), "order_confirmed_" + order.getId());
                            }

                            log.info("Webhook captured payment for order: {}", order.getOrderNumber());
                        }
                    }
                });
            } else if ("payment.failed".equals(event)) {
                JSONObject paymentEntity = root.getJSONObject("payload").getJSONObject("payment").getJSONObject("entity");
                String rzpOrderId = paymentEntity.optString("order_id");

                paymentRepository.findByRazorpayOrderId(rzpOrderId).ifPresent(payment -> {
                    payment.setPaymentStatus(PaymentStatus.FAILED);
                    paymentRepository.save(payment);
                    if (payment.getOrder() != null && payment.getOrder().getUser() != null) {
                        notificationService.sendNotification(payment.getOrder().getUser().getId(), "Payment Failed",
                                "Payment failed for order #" + payment.getOrder().getOrderNumber() + ".",
                                NotificationType.PAYMENT_FAILED, "/orders/" + payment.getOrder().getId(), "pay_webhook_failed_" + payment.getId());
                    }
                    log.info("Webhook payment failed for order: {}", payment.getOrder().getOrderNumber());
                });
            }

            // Save idempotency record
            idempotencyService.recordPending("webhook_" + eventId, null, "/api/v1/payments/webhook", event, 1440);
            idempotencyService.completeRecord("webhook_" + eventId, 200, "{\"status\":\"OK\"}");
        } catch (Exception e) {
            log.error("Error processing webhook payload", e);
            throw new PaymentException("Webhook processing error: " + e.getMessage());
        }
    }
}
