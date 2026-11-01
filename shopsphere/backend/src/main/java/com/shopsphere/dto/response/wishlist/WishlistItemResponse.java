package com.shopsphere.dto.response.wishlist;

import com.shopsphere.dto.response.product.ProductSummaryResponse;
import com.shopsphere.entity.WishlistItem;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WishlistItemResponse {

    private Long id;
    private ProductSummaryResponse product;
    private LocalDateTime addedAt;

    public static WishlistItemResponse fromEntity(WishlistItem item) {
        if (item == null) return null;
        return WishlistItemResponse.builder()
                .id(item.getId())
                .product(ProductSummaryResponse.fromEntity(item.getProduct()))
                .addedAt(item.getCreatedAt())
                .build();
    }
}
