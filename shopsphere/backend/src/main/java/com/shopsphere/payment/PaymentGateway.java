package com.shopsphere.payment;

import com.shopsphere.dto.response.payment.PaymentGatewayOrderResponse;
import com.shopsphere.dto.response.payment.PaymentGatewayRefundResponse;
import com.shopsphere.entity.Order;
import com.shopsphere.entity.Payment;
import com.shopsphere.entity.enums.PaymentMethod;

import java.math.BigDecimal;

public interface PaymentGateway {

    PaymentMethod getSupportedMethod();

    PaymentGatewayOrderResponse createGatewayOrder(Order order);

    boolean verifySignature(Payment payment, String gatewayOrderId, String gatewayPaymentId, String signature);

    PaymentGatewayRefundResponse processRefund(Payment payment, BigDecimal amount, String reason);
}
