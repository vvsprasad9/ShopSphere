package com.shopsphere.service;

import com.shopsphere.dto.request.cart.AddToCartRequest;
import com.shopsphere.dto.request.cart.UpdateCartItemRequest;
import com.shopsphere.dto.response.cart.CartResponse;

public interface CartService {

    CartResponse getCart(Long userId);

    CartResponse addToCart(Long userId, AddToCartRequest request);

    CartResponse updateCartItem(Long userId, Long itemId, UpdateCartItemRequest request);

    CartResponse removeCartItem(Long userId, Long itemId);

    void clearCart(Long userId);
}
