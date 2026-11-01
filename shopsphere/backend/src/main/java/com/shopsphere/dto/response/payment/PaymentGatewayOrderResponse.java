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
public class PaymentGatewayOrderResponse {

    private String gatewayOrderId;
    private String transactionId;
    private BigDecimal amount;
    private String currency;
    private String status;
    private String rawResponse;
}
