package com.shopsphere.service.impl;

import com.shopsphere.dto.request.cart.AddToCartRequest;
import com.shopsphere.dto.request.cart.UpdateCartItemRequest;
import com.shopsphere.dto.response.cart.CartItemResponse;
import com.shopsphere.dto.response.cart.CartResponse;
import com.shopsphere.entity.*;
import com.shopsphere.exception.BadRequestException;
import com.shopsphere.exception.ResourceNotFoundException;
import com.shopsphere.repository.*;
import com.shopsphere.service.CartService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class CartServiceImpl implements CartService {

    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final ProductRepository productRepository;
    private final ProductVariantRepository productVariantRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional
    public CartResponse getCart(Long userId) {
        log.info("Retrieving cart for user id: {}", userId);
        Cart cart = getOrCreateCartEntity(userId);

        List<CartItemResponse> itemResponses = new ArrayList<>();
        boolean anyPriceUpdated = false;

        for (CartItem item : cart.getItems()) {
            Product product = item.getProduct();
            ProductVariant variant = item.getVariant();

            BigDecimal currentPrice = calculateCurrentItemPrice(product, variant);
            BigDecimal previousSnapshot = item.getUnitPriceSnapshot();

            boolean priceChanged = previousSnapshot != null && previousSnapshot.compareTo(currentPrice) != 0;
            if (priceChanged) {
                log.info("Price changed for product id: {} from {} to {}", product.getId(), previousSnapshot, currentPrice);
                item.setUnitPriceSnapshot(currentPrice);
                anyPriceUpdated = true;
            }

            CartItemResponse response = CartItemResponse.fromEntity(item);
            if (priceChanged) {
                response.setPriceChanged(true);
                response.setPreviousPrice(previousSnapshot);
            }
            itemResponses.add(response);
        }

        if (anyPriceUpdated) {
            cartRepository.save(cart);
        }

        return buildCartResponse(cart, itemResponses);
    }

    @Override
    @Transactional
    public CartResponse addToCart(Long userId, AddToCartRequest request) {
        log.info("Adding product id: {} (variant: {}) to cart for user id: {}",
                request.getProductId(), request.getVariantId(), userId);

        Product product = productRepository.findById(request.getProductId())
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + request.getProductId()));

        if (!product.isActive()) {
            throw new BadRequestException("Product is currently unavailable");
        }

        ProductVariant variant = null;
        if (request.getVariantId() != null) {
            variant = productVariantRepository.findByIdAndProductId(request.getVariantId(), request.getProductId())
                    .orElseThrow(() -> new ResourceNotFoundException("Variant not found with id: " + request.getVariantId()));

            if (!variant.isActive()) {
                throw new BadRequestException("Product variant is currently unavailable");
            }
        }

        int availableStock = getAvailableStock(product, variant);
        BigDecimal currentPrice = calculateCurrentItemPrice(product, variant);

        Cart cart = getOrCreateCartEntity(userId);

        Optional<CartItem> existingItemOpt = (variant != null)
                ? cartItemRepository.findByCartIdAndProductIdAndVariantId(cart.getId(), product.getId(), variant.getId())
                : cartItemRepository.findByCartIdAndProductIdAndVariantIsNull(cart.getId(), product.getId());

        if (existingItemOpt.isPresent()) {
            CartItem existingItem = existingItemOpt.get();
            int newQuantity = existingItem.getQuantity() + request.getQuantity();

            if (newQuantity > availableStock) {
                throw new BadRequestException("Requested total quantity (" + newQuantity +
                        ") exceeds available stock (" + availableStock + ")");
            }

            existingItem.setQuantity(newQuantity);
            existingItem.setUnitPriceSnapshot(currentPrice);
            cartItemRepository.save(existingItem);
            log.info("Updated existing cart item id: {} to quantity: {}", existingItem.getId(), newQuantity);
        } else {
            if (request.getQuantity() > availableStock) {
                throw new BadRequestException("Requested quantity (" + request.getQuantity() +
                        ") exceeds available stock (" + availableStock + ")");
            }

            CartItem newItem = CartItem.builder()
                    .cart(cart)
                    .product(product)
                    .variant(variant)
                    .quantity(request.getQuantity())
                    .unitPriceSnapshot(currentPrice)
                    .build();

            cart.getItems().add(newItem);
            cartItemRepository.save(newItem);
            log.info("Added new item to cart for user id: {}", userId);
        }

        return getCart(userId);
    }

    @Override
    @Transactional
    public CartResponse updateCartItem(Long userId, Long itemId, UpdateCartItemRequest request) {
        log.info("Updating cart item id: {} for user id: {}", itemId, userId);

        CartItem item = cartItemRepository.findByIdAndCartUserId(itemId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Cart item not found with id: " + itemId));

        Product product = item.getProduct();
        ProductVariant variant = item.getVariant();

        if (!product.isActive() || (variant != null && !variant.isActive())) {
            throw new BadRequestException("Product or variant is currently unavailable");
        }

        int availableStock = getAvailableStock(product, variant);
        if (request.getQuantity() > availableStock) {
            throw new BadRequestException("Requested quantity (" + request.getQuantity() +
                    ") exceeds available stock (" + availableStock + ")");
        }

        BigDecimal currentPrice = calculateCurrentItemPrice(product, variant);
        item.setQuantity(request.getQuantity());
        item.setUnitPriceSnapshot(currentPrice);
        cartItemRepository.save(item);

        return getCart(userId);
    }

    @Override
    @Transactional
    public CartResponse removeCartItem(Long userId, Long itemId) {
        log.info("Removing cart item id: {} for user id: {}", itemId, userId);

        CartItem item = cartItemRepository.findByIdAndCartUserId(itemId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Cart item not found with id: " + itemId));

        Cart cart = item.getCart();
        cart.getItems().remove(item);
        cartItemRepository.delete(item);

        return getCart(userId);
    }

    @Override
    @Transactional
    public void clearCart(Long userId) {
        log.info("Clearing cart for user id: {}", userId);
        Cart cart = getOrCreateCartEntity(userId);
        cart.getItems().clear();
        cartItemRepository.deleteByCartId(cart.getId());
    }

    private Cart getOrCreateCartEntity(Long userId) {
        return cartRepository.findByUserIdWithItems(userId)
                .orElseGet(() -> {
                    User user = userRepository.findById(userId)
                            .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));
                    Cart newCart = Cart.builder()
                            .user(user)
                            .build();
                    return cartRepository.save(newCart);
                });
    }

    private BigDecimal calculateCurrentItemPrice(Product product, ProductVariant variant) {
        BigDecimal base = (product.getDiscountPrice() != null && product.getDiscountPrice().compareTo(BigDecimal.ZERO) > 0)
                ? product.getDiscountPrice()
                : product.getPrice();

        if (variant != null && variant.getPriceAdjustment() != null) {
            base = base.add(variant.getPriceAdjustment());
        }

        return base.max(BigDecimal.ZERO);
    }

    private int getAvailableStock(Product product, ProductVariant variant) {
        if (variant != null) {
            return variant.getInventory() != null ? variant.getInventory().getPurchasableQuantity() : 0;
        }
        return product.getInventory() != null ? product.getInventory().getPurchasableQuantity() : 0;
    }

    private CartResponse buildCartResponse(Cart cart, List<CartItemResponse> itemResponses) {
        BigDecimal subtotal = itemResponses.stream()
                .filter(CartItemResponse::isActive)
                .map(CartItemResponse::getTotalPrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        int totalCount = itemResponses.stream()
                .mapToInt(CartItemResponse::getQuantity)
                .sum();

        // Free shipping over 1000, else 50
        BigDecimal shippingFee = (subtotal.compareTo(new BigDecimal("1000")) >= 0 || subtotal.compareTo(BigDecimal.ZERO) == 0)
                ? BigDecimal.ZERO
                : new BigDecimal("50.00");

        BigDecimal totalAmount = subtotal.add(shippingFee);

        return CartResponse.builder()
                .id(cart.getId())
                .userId(cart.getUser().getId())
                .items(itemResponses)
                .totalItems(totalCount)
                .subtotal(subtotal)
                .shippingFee(shippingFee)
                .discountAmount(BigDecimal.ZERO)
                .totalAmount(totalAmount)
                .build();
    }
}
