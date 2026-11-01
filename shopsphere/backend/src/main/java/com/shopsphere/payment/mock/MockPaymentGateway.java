package com.shopsphere.payment.mock;

import com.shopsphere.dto.response.payment.PaymentGatewayOrderResponse;
import com.shopsphere.dto.response.payment.PaymentGatewayRefundResponse;
import com.shopsphere.entity.Order;
import com.shopsphere.entity.Payment;
import com.shopsphere.entity.enums.PaymentMethod;
import com.shopsphere.exception.PaymentException;
import com.shopsphere.payment.PaymentGateway;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.UUID;

@Component
@Slf4j
public class MockPaymentGateway implements PaymentGateway {

    private final boolean mockEnabled;
    private final String activeProfile;

    public MockPaymentGateway(
            @Value("${app.payment.mock-enabled:true}") boolean mockEnabled,
            @Value("${spring.profiles.active:dev}") String activeProfile
    ) {
        this.mockEnabled = mockEnabled;
        this.activeProfile = activeProfile;
        if (isProduction() && mockEnabled) {
            log.error("CRITICAL SECURITY RISK: Mock payment gateway is enabled in production environment!");
        }
    }

    private boolean isProduction() {
        return "prod".equalsIgnoreCase(activeProfile) || "production".equalsIgnoreCase(activeProfile);
    }

    @Override
    public PaymentMethod getSupportedMethod() {
        return PaymentMethod.MOCK_TEST;
    }

    @Override
    public PaymentGatewayOrderResponse createGatewayOrder(Order order) {
        validateMockAllowed();
        String mockOrderId = "order_mock_" + UUID.randomUUID().toString().replace("-", "").substring(0, 16);
        String mockTxnId = "txn_mock_" + UUID.randomUUID().toString().replace("-", "").substring(0, 16);

        log.info("MockPaymentGateway created mock order: {} for order: {}", mockOrderId, order.getOrderNumber());

        return PaymentGatewayOrderResponse.builder()
                .gatewayOrderId(mockOrderId)
                .transactionId(mockTxnId)
                .amount(order.getTotalAmount())
                .currency("INR")
                .status("created")
                .rawResponse("{\"id\":\"" + mockOrderId + "\",\"status\":\"created\"}")
                .build();
    }

    @Override
    public boolean verifySignature(Payment payment, String gatewayOrderId, String gatewayPaymentId, String signature) {
        validateMockAllowed();
        if (signature == null || signature.trim().isEmpty()) {
            return false;
        }
        String lower = signature.toLowerCase().trim();
        if (lower.contains("invalid") || lower.contains("bad") || lower.contains("fail") || lower.contains("bogus")) {
            return false;
        }
        return lower.contains("mock") || lower.equals("valid") || signature.equals("sig_" + gatewayOrderId);
    }

    @Override
    public PaymentGatewayRefundResponse processRefund(Payment payment, BigDecimal amount, String reason) {
        validateMockAllowed();
        String refundId = "rfnd_mock_" + UUID.randomUUID().toString().replace("-", "").substring(0, 16);
        log.info("MockPaymentGateway processed refund: {} for payment: {}, amount: {}",
                refundId, payment.getId(), amount);

        return PaymentGatewayRefundResponse.builder()
                .refundId(refundId)
                .paymentId(payment.getRazorpayPaymentId() != null ? payment.getRazorpayPaymentId() : payment.getTransactionId())
                .amount(amount)
                .status("processed")
                .rawResponse("{\"id\":\"" + refundId + "\",\"status\":\"processed\"}")
                .build();
    }

    private void validateMockAllowed() {
        if (isProduction() || !mockEnabled) {
            throw new PaymentException("Mock payment gateway is disabled in this environment");
        }
    }
}
