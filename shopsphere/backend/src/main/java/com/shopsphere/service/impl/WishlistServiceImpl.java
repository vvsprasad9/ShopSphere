package com.shopsphere.service.impl;

import com.shopsphere.dto.response.wishlist.WishlistItemResponse;
import com.shopsphere.dto.response.wishlist.WishlistResponse;
import com.shopsphere.entity.Product;
import com.shopsphere.entity.User;
import com.shopsphere.entity.Wishlist;
import com.shopsphere.entity.WishlistItem;
import com.shopsphere.exception.BadRequestException;
import com.shopsphere.exception.ResourceNotFoundException;
import com.shopsphere.repository.ProductRepository;
import com.shopsphere.repository.UserRepository;
import com.shopsphere.repository.WishlistItemRepository;
import com.shopsphere.repository.WishlistRepository;
import com.shopsphere.service.WishlistService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class WishlistServiceImpl implements WishlistService {

    private final WishlistRepository wishlistRepository;
    private final WishlistItemRepository wishlistItemRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional
    public WishlistResponse getWishlist(Long userId) {
        log.info("Retrieving wishlist for user id: {}", userId);
        Wishlist wishlist = getOrCreateWishlistEntity(userId);
        return mapWishlistResponse(wishlist);
    }

    @Override
    @Transactional
    public WishlistResponse addToWishlist(Long userId, Long productId) {
        log.info("Adding product id: {} to wishlist for user id: {}", productId, userId);

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + productId));

        if (!product.isActive()) {
            throw new BadRequestException("Cannot add inactive product to wishlist");
        }

        Wishlist wishlist = getOrCreateWishlistEntity(userId);

        boolean alreadyWishlisted = wishlistItemRepository.existsByWishlistIdAndProductId(wishlist.getId(), productId);
        if (!alreadyWishlisted) {
            WishlistItem item = WishlistItem.builder()
                    .wishlist(wishlist)
                    .product(product)
                    .build();
            wishlist.getItems().add(item);
            wishlistItemRepository.save(item);
            log.info("Product id: {} added to wishlist for user id: {}", productId, userId);
        } else {
            log.info("Product id: {} is already in wishlist for user id: {}", productId, userId);
        }

        return getWishlist(userId);
    }

    @Override
    @Transactional
    public WishlistResponse removeFromWishlist(Long userId, Long productId) {
        log.info("Removing product id: {} from wishlist for user id: {}", productId, userId);

        Wishlist wishlist = getOrCreateWishlistEntity(userId);
        wishlistItemRepository.deleteByWishlistIdAndProductId(wishlist.getId(), productId);
        wishlist.getItems().removeIf(item -> item.getProduct().getId().equals(productId));

        return getWishlist(userId);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isProductWishlisted(Long userId, Long productId) {
        return wishlistRepository.findByUserId(userId)
                .map(wishlist -> wishlistItemRepository.existsByWishlistIdAndProductId(wishlist.getId(), productId))
                .orElse(false);
    }

    private Wishlist getOrCreateWishlistEntity(Long userId) {
        return wishlistRepository.findByUserIdWithItems(userId)
                .orElseGet(() -> {
                    User user = userRepository.findById(userId)
                            .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));
                    Wishlist newWishlist = Wishlist.builder()
                            .user(user)
                            .build();
                    return wishlistRepository.save(newWishlist);
                });
    }

    private WishlistResponse mapWishlistResponse(Wishlist wishlist) {
        List<WishlistItemResponse> items = wishlist.getItems().stream()
                .filter(item -> item.getProduct() != null && item.getProduct().isActive())
                .map(WishlistItemResponse::fromEntity)
                .collect(Collectors.toList());

        return WishlistResponse.builder()
                .id(wishlist.getId())
                .userId(wishlist.getUser().getId())
                .items(items)
                .totalItems(items.size())
                .build();
    }
}
