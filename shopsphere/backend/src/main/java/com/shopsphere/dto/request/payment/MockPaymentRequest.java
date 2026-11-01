package com.shopsphere.dto.request.payment;

import com.shopsphere.entity.enums.PaymentStatus;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MockPaymentRequest {

    @NotNull(message = "Order ID is required")
    private Long orderId;

    @Builder.Default
    private PaymentStatus status = PaymentStatus.SUCCESS;

    private String failureReason;
}
