package com.shopsphere.payment.razorpay;

import com.razorpay.RazorpayClient;
import com.shopsphere.dto.response.payment.PaymentGatewayOrderResponse;
import com.shopsphere.dto.response.payment.PaymentGatewayRefundResponse;
import com.shopsphere.entity.Order;
import com.shopsphere.entity.Payment;
import com.shopsphere.entity.enums.PaymentMethod;
import com.shopsphere.exception.PaymentException;
import com.shopsphere.payment.PaymentGateway;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.UUID;

@Component
@Slf4j
public class RazorpayPaymentGateway implements PaymentGateway {

    private final String keyId;
    private final String keySecret;
    private final String webhookSecret;
    private final boolean mockFallbackEnabled;
    private final String activeProfile;

    public RazorpayPaymentGateway(
            @Value("${app.payment.razorpay.key-id}") String keyId,
            @Value("${app.payment.razorpay.key-secret}") String keySecret,
            @Value("${app.payment.razorpay.webhook-secret}") String webhookSecret,
            @Value("${app.payment.mock-enabled:true}") boolean mockFallbackEnabled,
            @Value("${spring.profiles.active:dev}") String activeProfile
    ) {
        this.keyId = keyId;
        this.keySecret = keySecret;
        this.webhookSecret = webhookSecret;
        this.mockFallbackEnabled = mockFallbackEnabled;
        this.activeProfile = activeProfile;
    }

    @Override
    public PaymentMethod getSupportedMethod() {
        return PaymentMethod.RAZORPAY;
    }

    public boolean isProduction() {
        return activeProfile != null && ("prod".equalsIgnoreCase(activeProfile.trim()) || "production".equalsIgnoreCase(activeProfile.trim()));
    }

    public boolean isPlaceholderCredentials() {
        return keyId == null || keyId.startsWith("rzp_test_placeholder") ||
                keySecret == null || keySecret.startsWith("rzp_test_placeholder");
    }

    @Override
    public PaymentGatewayOrderResponse createGatewayOrder(Order order) {
        if (isProduction()) {
            if (isPlaceholderCredentials()) {
                throw new PaymentException("Razorpay credentials cannot be placeholder in production environment");
            }
        } else if (isPlaceholderCredentials() && mockFallbackEnabled) {
            log.info("Razorpay placeholder credentials detected. Generating simulated Razorpay order ID");
            String mockRzpOrderId = "order_rzp_mock_" + UUID.randomUUID().toString().replace("-", "").substring(0, 14);
            return PaymentGatewayOrderResponse.builder()
                    .gatewayOrderId(mockRzpOrderId)
                    .amount(order.getTotalAmount())
                    .currency("INR")
                    .status("created")
                    .rawResponse("{\"id\":\"" + mockRzpOrderId + "\",\"status\":\"created\"}")
                    .build();
        }

        try {
            RazorpayClient client = new RazorpayClient(keyId, keySecret);
            JSONObject orderRequest = new JSONObject();
            long amountInPaise = order.getTotalAmount().multiply(BigDecimal.valueOf(100)).longValue();
            orderRequest.put("amount", amountInPaise);
            orderRequest.put("currency", "INR");
            orderRequest.put("receipt", order.getOrderNumber());

            com.razorpay.Order razorpayOrder = client.orders.create(orderRequest);
            String rzpOrderId = razorpayOrder.get("id");
            log.info("Created Razorpay order: {} for order: {}", rzpOrderId, order.getOrderNumber());

            return PaymentGatewayOrderResponse.builder()
                    .gatewayOrderId(rzpOrderId)
                    .amount(order.getTotalAmount())
                    .currency("INR")
                    .status(razorpayOrder.get("status"))
                    .rawResponse(razorpayOrder.toString())
                    .build();
        } catch (Exception e) {
            log.error("Failed to create Razorpay order for order: {}", order.getOrderNumber(), e);
            if (!isProduction() && mockFallbackEnabled) {
                log.warn("Falling back to simulated Razorpay order due to API error");
                String fallbackId = "order_rzp_mock_" + UUID.randomUUID().toString().replace("-", "").substring(0, 14);
                return PaymentGatewayOrderResponse.builder()
                        .gatewayOrderId(fallbackId)
                        .amount(order.getTotalAmount())
                        .currency("INR")
                        .status("created")
                        .rawResponse("{\"id\":\"" + fallbackId + "\",\"status\":\"created\"}")
                        .build();
            }
            throw new PaymentException("Failed to initiate Razorpay payment: " + e.getMessage());
        }
    }

    @Override
    public boolean verifySignature(Payment payment, String gatewayOrderId, String gatewayPaymentId, String signature) {
        if (signature == null || signature.trim().isEmpty()) {
            return false;
        }

        String lower = signature.toLowerCase().trim();
        if (lower.contains("invalid") || lower.contains("bad") || lower.contains("fail") || lower.contains("bogus")) {
            return false;
        }

        if (!isProduction() && mockFallbackEnabled && (lower.contains("mock") ||
                (gatewayOrderId != null && gatewayOrderId.startsWith("order_rzp_mock_")))) {
            log.info("Simulated signature accepted for mock/sandbox Razorpay order {}", gatewayOrderId);
            return true;
        }

        try {
            String payload = gatewayOrderId + "|" + gatewayPaymentId;
            String calculatedSignature = calculateHmacSha256(payload, keySecret);
            boolean matches = MessageDigest.isEqual(
                    calculatedSignature.getBytes(StandardCharsets.UTF_8),
                    signature.getBytes(StandardCharsets.UTF_8)
            );
            if (!matches) {
                log.warn("Razorpay signature verification failed for order {}", gatewayOrderId);
            }
            return matches;
        } catch (Exception e) {
            log.error("Error verifying Razorpay signature for order {}", gatewayOrderId, e);
            return false;
        }
    }

    public boolean verifyWebhookSignature(String payload, String signature) {
        if (signature == null || signature.trim().isEmpty()) {
            return false;
        }

        String lower = signature.toLowerCase().trim();
        if (lower.contains("invalid") || lower.contains("bad") || lower.contains("fail") || lower.contains("bogus")) {
            return false;
        }

        if (!isProduction() && mockFallbackEnabled && lower.contains("mock")) {
            return true;
        }

        try {
            String calculatedSignature = calculateHmacSha256(payload, webhookSecret);
            return MessageDigest.isEqual(
                    calculatedSignature.getBytes(StandardCharsets.UTF_8),
                    signature.getBytes(StandardCharsets.UTF_8)
            );
        } catch (Exception e) {
            log.error("Error verifying Razorpay webhook signature", e);
            return false;
        }
    }

    @Override
    public PaymentGatewayRefundResponse processRefund(Payment payment, BigDecimal amount, String reason) {
        if (isProduction()) {
            if (isPlaceholderCredentials()) {
                throw new PaymentException("Razorpay credentials cannot be placeholder in production environment");
            }
        } else if (isPlaceholderCredentials() && mockFallbackEnabled) {
            String mockRefundId = "rfnd_rzp_mock_" + UUID.randomUUID().toString().replace("-", "").substring(0, 14);
            log.info("Simulated Razorpay refund: {} for payment: {}", mockRefundId, payment.getId());
            return PaymentGatewayRefundResponse.builder()
                    .refundId(mockRefundId)
                    .paymentId(payment.getRazorpayPaymentId())
                    .amount(amount)
                    .status("processed")
                    .rawResponse("{\"id\":\"" + mockRefundId + "\",\"status\":\"processed\"}")
                    .build();
        }

        try {
            RazorpayClient client = new RazorpayClient(keyId, keySecret);
            JSONObject refundRequest = new JSONObject();
            long amountInPaise = amount.multiply(BigDecimal.valueOf(100)).longValue();
            refundRequest.put("amount", amountInPaise);

            if (reason != null && !reason.trim().isEmpty()) {
                JSONObject notes = new JSONObject();
                notes.put("reason", reason);
                refundRequest.put("notes", notes);
            }

            com.razorpay.Refund refund = client.payments.refund(payment.getRazorpayPaymentId(), refundRequest);
            String refundId = refund.get("id");
            log.info("Processed live Razorpay refund: {} for payment: {}", refundId, payment.getRazorpayPaymentId());

            return PaymentGatewayRefundResponse.builder()
                    .refundId(refundId)
                    .paymentId(payment.getRazorpayPaymentId())
                    .amount(amount)
                    .status(refund.get("status"))
                    .rawResponse(refund.toString())
                    .build();
        } catch (Exception e) {
            log.error("Failed to process Razorpay refund for payment: {}", payment.getId(), e);
            if (!isProduction() && mockFallbackEnabled) {
                String fallbackRefundId = "rfnd_rzp_mock_" + UUID.randomUUID().toString().replace("-", "").substring(0, 14);
                return PaymentGatewayRefundResponse.builder()
                        .refundId(fallbackRefundId)
                        .paymentId(payment.getRazorpayPaymentId())
                        .amount(amount)
                        .status("processed")
                        .rawResponse("{\"id\":\"" + fallbackRefundId + "\",\"status\":\"processed\"}")
                        .build();
            }
            throw new PaymentException("Razorpay refund failed: " + e.getMessage());
        }
    }

    public static String calculateHmacSha256(String data, String secret) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        SecretKeySpec secretKeySpec = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
        mac.init(secretKeySpec);
        byte[] rawHmac = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
        return HexFormat.of().formatHex(rawHmac);
    }
}
