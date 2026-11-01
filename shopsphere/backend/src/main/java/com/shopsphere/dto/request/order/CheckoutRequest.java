package com.shopsphere.dto.request.order;

import com.shopsphere.dto.request.user.AddressRequest;
import com.shopsphere.entity.enums.PaymentMethod;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CheckoutRequest {

    private Long shippingAddressId;
    private AddressRequest newShippingAddress;

    private Long billingAddressId;
    private AddressRequest newBillingAddress;

    private String couponCode;

    @NotNull(message = "Payment method is required")
    @Builder.Default
    private PaymentMethod paymentMethod = PaymentMethod.MOCK_TEST;

    private String notes;
    private String idempotencyKey;
}
