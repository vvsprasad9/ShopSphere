package com.shopsphere.dto.response.payment;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentGatewayRefundResponse {

    private String refundId;
    private String paymentId;
    private BigDecimal amount;
    private String status;
    private String rawResponse;
}
