package com.shopsphere.controller;

import com.shopsphere.dto.response.ApiResponse;
import com.shopsphere.dto.response.wishlist.WishlistResponse;
import com.shopsphere.security.UserPrincipal;
import com.shopsphere.service.WishlistService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/wishlist")
@RequiredArgsConstructor
@Tag(name = "Wishlist", description = "Endpoints for managing authenticated customer wishlist")
@SecurityRequirement(name = "BearerAuth")
public class WishlistController {

    private final WishlistService wishlistService;

    @GetMapping
    @Operation(summary = "Get authenticated customer's wishlist")
    public ResponseEntity<ApiResponse<WishlistResponse>> getWishlist(
            @AuthenticationPrincipal UserPrincipal userPrincipal
    ) {
        WishlistResponse response = wishlistService.getWishlist(userPrincipal.getId());
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PostMapping("/products/{productId}")
    @Operation(summary = "Add a product to customer's wishlist")
    public ResponseEntity<ApiResponse<WishlistResponse>> addToWishlist(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @PathVariable Long productId
    ) {
        WishlistResponse response = wishlistService.addToWishlist(userPrincipal.getId(), productId);
        return ResponseEntity.ok(ApiResponse.success("Product added to wishlist successfully", response));
    }

    @DeleteMapping("/products/{productId}")
    @Operation(summary = "Remove a product from customer's wishlist")
    public ResponseEntity<ApiResponse<WishlistResponse>> removeFromWishlist(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @PathVariable Long productId
    ) {
        WishlistResponse response = wishlistService.removeFromWishlist(userPrincipal.getId(), productId);
        return ResponseEntity.ok(ApiResponse.success("Product removed from wishlist successfully", response));
    }

    @GetMapping("/products/{productId}/check")
    @Operation(summary = "Check whether a product is in customer's wishlist")
    public ResponseEntity<ApiResponse<Boolean>> isWishlisted(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @PathVariable Long productId
    ) {
        boolean wishlisted = wishlistService.isProductWishlisted(userPrincipal.getId(), productId);
        return ResponseEntity.ok(ApiResponse.success(wishlisted));
    }
}
