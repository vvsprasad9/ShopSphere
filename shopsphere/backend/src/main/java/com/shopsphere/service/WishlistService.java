package com.shopsphere.service;

import com.shopsphere.dto.response.wishlist.WishlistResponse;

public interface WishlistService {

    WishlistResponse getWishlist(Long userId);

    WishlistResponse addToWishlist(Long userId, Long productId);

    WishlistResponse removeFromWishlist(Long userId, Long productId);

    boolean isProductWishlisted(Long userId, Long productId);
}
