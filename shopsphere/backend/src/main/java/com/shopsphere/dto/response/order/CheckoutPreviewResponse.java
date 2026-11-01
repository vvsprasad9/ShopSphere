package com.shopsphere.dto.response.order;

import com.shopsphere.dto.response.cart.CartItemResponse;
import com.shopsphere.entity.AddressSnapshot;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CheckoutPreviewResponse {

    @Builder.Default
    private List<CartItemResponse> items = new ArrayList<>();

    private int totalQuantity;
    private BigDecimal subtotal;
    private BigDecimal discountAmount;
    private BigDecimal shippingFee;
    private BigDecimal taxAmount;
    private BigDecimal totalAmount;

    private String couponCode;
    private boolean couponApplied;
    private String couponMessage;

    private AddressSnapshot shippingAddress;
    private AddressSnapshot billingAddress;

    private boolean hasStockIssues;
    @Builder.Default
    private List<String> warnings = new ArrayList<>();
}
