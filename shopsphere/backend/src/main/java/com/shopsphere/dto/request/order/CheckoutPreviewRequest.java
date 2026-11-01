package com.shopsphere.dto.request.order;

import com.shopsphere.dto.request.user.AddressRequest;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CheckoutPreviewRequest {

    private Long shippingAddressId;
    private AddressRequest newShippingAddress;
    private String couponCode;
}
