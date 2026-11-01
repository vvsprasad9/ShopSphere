package com.shopsphere.dto.response.cart;

import com.shopsphere.entity.Cart;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CartResponse {

    private Long id;
    private Long userId;
    @Builder.Default
    private List<CartItemResponse> items = new ArrayList<>();
    private Integer totalItems;
    private BigDecimal subtotal;
    private BigDecimal shippingFee;
    private BigDecimal discountAmount;
    private BigDecimal totalAmount;

    public static CartResponse fromEntity(Cart cart, BigDecimal shippingFee, BigDecimal discountAmount) {
        if (cart == null) return null;
        List<CartItemResponse> itemResponses = cart.getItems() != null ?
                cart.getItems().stream().map(CartItemResponse::fromEntity).collect(Collectors.toList()) :
                new ArrayList<>();

        int totalCount = itemResponses.stream().mapToInt(CartItemResponse::getQuantity).sum();
        BigDecimal sub = cart.calculateSubtotal();
        BigDecimal ship = shippingFee != null ? shippingFee : (sub.compareTo(new BigDecimal("1000")) >= 0 ? BigDecimal.ZERO : new BigDecimal("50.00"));
        BigDecimal disc = discountAmount != null ? discountAmount : BigDecimal.ZERO;
        BigDecimal total = sub.add(ship).subtract(disc).max(BigDecimal.ZERO);

        return CartResponse.builder()
                .id(cart.getId())
                .userId(cart.getUser().getId())
                .items(itemResponses)
                .totalItems(totalCount)
                .subtotal(sub)
                .shippingFee(ship)
                .discountAmount(disc)
                .totalAmount(total)
                .build();
    }
}
