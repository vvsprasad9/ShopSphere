package com.shopsphere.service;

import com.shopsphere.dto.request.payment.MockPaymentRequest;
import com.shopsphere.dto.request.payment.PaymentRefundRequest;
import com.shopsphere.dto.request.payment.PaymentVerificationRequest;
import com.shopsphere.dto.response.payment.PaymentResponse;
import com.shopsphere.entity.Order;
import com.shopsphere.entity.Payment;
import com.shopsphere.entity.enums.PaymentMethod;

import java.math.BigDecimal;

public interface PaymentService {

    Payment initiatePayment(Order order, PaymentMethod paymentMethod);

    PaymentResponse verifyPayment(Long userId, PaymentVerificationRequest request);

    PaymentResponse processMockPayment(Long userId, MockPaymentRequest request);

    PaymentResponse getPaymentByOrderId(Long userId, Long orderId);

    PaymentResponse refundPayment(Long userId, PaymentRefundRequest request);

    PaymentResponse markCodPaymentCollected(Long orderId);

    void processWebhook(String payload, String signature);
}
