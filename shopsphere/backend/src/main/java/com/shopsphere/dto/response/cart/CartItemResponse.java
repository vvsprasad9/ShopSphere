package com.shopsphere.dto.response.cart;

import com.shopsphere.entity.CartItem;
import com.shopsphere.entity.ProductImage;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CartItemResponse {

    private Long id;
    private Long productId;
    private String productName;
    private String productSlug;
    private String productSku;
    private String productImageUrl;
    private Long variantId;
    private String variantName;
    private Integer quantity;
    private BigDecimal unitPrice;
    private BigDecimal totalPrice;
    private int stockAvailable;
    private boolean inStock;
    @Builder.Default
    private boolean active = true;
    @Builder.Default
    private boolean priceChanged = false;
    private BigDecimal previousPrice;

    public static CartItemResponse fromEntity(CartItem item) {
        if (item == null) return null;
        String imageUrl = null;
        if (item.getProduct().getImages() != null && !item.getProduct().getImages().isEmpty()) {
            imageUrl = item.getProduct().getImages().stream()
                    .filter(ProductImage::isPrimary)
                    .map(ProductImage::getImageUrl)
                    .findFirst()
                    .orElse(item.getProduct().getImages().iterator().next().getImageUrl());
        }

        // Independent variant vs product stock
        int stock;
        if (item.getVariant() != null && item.getVariant().getInventory() != null) {
            stock = item.getVariant().getInventory().getPurchasableQuantity();
        } else if (item.getProduct().getInventory() != null) {
            stock = item.getProduct().getInventory().getPurchasableQuantity();
        } else {
            stock = 0;
        }

        boolean productActive = item.getProduct().isActive() &&
                (item.getVariant() == null || item.getVariant().isActive());

        return CartItemResponse.builder()
                .id(item.getId())
                .productId(item.getProduct().getId())
                .productName(item.getProduct().getName())
                .productSlug(item.getProduct().getSlug())
                .productSku(item.getVariant() != null ? item.getVariant().getSku() : item.getProduct().getSku())
                .productImageUrl(imageUrl)
                .variantId(item.getVariant() != null ? item.getVariant().getId() : null)
                .variantName(item.getVariant() != null ? item.getVariant().getVariantName() : null)
                .quantity(item.getQuantity())
                .unitPrice(item.getUnitPriceSnapshot())
                .totalPrice(item.calculateTotalPrice())
                .stockAvailable(stock)
                .inStock(stock >= item.getQuantity() && productActive)
                .active(productActive)
                .priceChanged(false)
                .build();
    }
}
