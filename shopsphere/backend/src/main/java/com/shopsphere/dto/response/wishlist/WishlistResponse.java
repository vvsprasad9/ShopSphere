package com.shopsphere.dto.response.wishlist;

import com.shopsphere.entity.Wishlist;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WishlistResponse {

    private Long id;
    private Long userId;
    @Builder.Default
    private List<WishlistItemResponse> items = new ArrayList<>();
    private Integer totalItems;

    public static WishlistResponse fromEntity(Wishlist wishlist) {
        if (wishlist == null) return null;
        List<WishlistItemResponse> itemResponses = wishlist.getItems() != null ?
                wishlist.getItems().stream().map(WishlistItemResponse::fromEntity).collect(Collectors.toList()) :
                new ArrayList<>();

        return WishlistResponse.builder()
                .id(wishlist.getId())
                .userId(wishlist.getUser().getId())
                .items(itemResponses)
                .totalItems(itemResponses.size())
                .build();
    }
}
