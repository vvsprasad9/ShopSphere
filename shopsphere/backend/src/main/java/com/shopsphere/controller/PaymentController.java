package com.shopsphere.controller;

import com.shopsphere.dto.request.payment.MockPaymentRequest;
import com.shopsphere.dto.request.payment.PaymentRefundRequest;
import com.shopsphere.dto.request.payment.PaymentVerificationRequest;
import com.shopsphere.dto.response.ApiResponse;
import com.shopsphere.dto.response.payment.PaymentResponse;
import com.shopsphere.security.UserPrincipal;
import com.shopsphere.service.PaymentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/payments")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Payment Gateway", description = "APIs for payment processing, verification, and webhooks")
public class PaymentController {

    private final PaymentService paymentService;

    @PostMapping("/verify")
    @Operation(summary = "Verify Razorpay or sandbox payment signature and confirm order")
    public ResponseEntity<ApiResponse<PaymentResponse>> verifyPayment(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @Valid @RequestBody PaymentVerificationRequest request
    ) {
        PaymentResponse response = paymentService.verifyPayment(userPrincipal.getId(), request);
        return ResponseEntity.ok(ApiResponse.success("Payment verified and order confirmed", response));
    }

    @PostMapping("/mock-process")
    @Operation(summary = "Process simulated payment for development sandbox and automated tests")
    public ResponseEntity<ApiResponse<PaymentResponse>> processMockPayment(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @Valid @RequestBody MockPaymentRequest request
    ) {
        PaymentResponse response = paymentService.processMockPayment(userPrincipal.getId(), request);
        return ResponseEntity.ok(ApiResponse.success("Mock payment processed successfully", response));
    }

    @GetMapping("/order/{orderId}")
    @Operation(summary = "Get payment details by order ID")
    public ResponseEntity<ApiResponse<PaymentResponse>> getPaymentByOrderId(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @PathVariable Long orderId
    ) {
        PaymentResponse response = paymentService.getPaymentByOrderId(userPrincipal.getId(), orderId);
        return ResponseEntity.ok(ApiResponse.success("Payment details retrieved", response));
    }

    @PostMapping("/refund")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @Operation(summary = "Admin/Manager issue refund for an order")
    public ResponseEntity<ApiResponse<PaymentResponse>> refundPayment(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @Valid @RequestBody PaymentRefundRequest request
    ) {
        PaymentResponse response = paymentService.refundPayment(userPrincipal.getId(), request);
        return ResponseEntity.ok(ApiResponse.success("Refund processed successfully", response));
    }

    @PostMapping("/admin/orders/{orderId}/collect-cod")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @Operation(summary = "Admin/Manager mark Cash-On-Delivery payment as collected")
    public ResponseEntity<ApiResponse<PaymentResponse>> collectCodPayment(
            @PathVariable Long orderId
    ) {
        PaymentResponse response = paymentService.markCodPaymentCollected(orderId);
        return ResponseEntity.ok(ApiResponse.success("COD payment marked as collected", response));
    }

    @PostMapping("/webhook")
    @Operation(summary = "Public webhook receiver for Razorpay asynchronous payment events")
    public ResponseEntity<String> handleRazorpayWebhook(
            @RequestHeader(value = "X-Razorpay-Signature", required = false) String signature,
            @RequestBody String payload
    ) {
        log.info("Received Razorpay webhook callback");
        paymentService.processWebhook(payload, signature);
        return ResponseEntity.ok("Webhook processed successfully");
    }
}
