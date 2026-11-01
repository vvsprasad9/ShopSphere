package com.shopsphere.controller;

import com.shopsphere.dto.request.review.ReviewModerationRequest;
import com.shopsphere.dto.request.review.ReviewRequest;
import com.shopsphere.dto.request.review.UpdateReviewRequest;
import com.shopsphere.dto.response.ApiResponse;
import com.shopsphere.dto.response.PageResponse;
import com.shopsphere.dto.response.review.ProductRatingSummaryResponse;
import com.shopsphere.dto.response.review.ReviewResponse;
import com.shopsphere.entity.enums.ReviewStatus;
import com.shopsphere.security.UserPrincipal;
import com.shopsphere.service.ReviewService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@Tag(name = "Product Reviews", description = "Endpoints for submitting, reading, updating and moderating product reviews")
public class ReviewController {

    private final ReviewService reviewService;

    @PostMapping("/api/v1/products/{productId}/reviews")
    @Operation(summary = "Submit a product review (authenticated customer)")
    @SecurityRequirement(name = "BearerAuth")
    public ResponseEntity<ApiResponse<ReviewResponse>> createReview(
            @PathVariable Long productId,
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @Valid @RequestBody ReviewRequest request
    ) {
        ReviewResponse response = reviewService.createReview(userPrincipal.getId(), productId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Review submitted successfully", response));
    }

    @GetMapping("/api/v1/products/{productId}/reviews")
    @Operation(summary = "Get approved reviews for a product (public)")
    public ResponseEntity<ApiResponse<PageResponse<ReviewResponse>>> getProductReviews(
            @PathVariable Long productId,
            @PageableDefault(sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        PageResponse<ReviewResponse> response = reviewService.getProductReviews(productId, pageable);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/api/v1/products/{productId}/reviews/summary")
    @Operation(summary = "Get product rating breakdown and summary (public)")
    public ResponseEntity<ApiResponse<ProductRatingSummaryResponse>> getProductRatingSummary(
            @PathVariable Long productId
    ) {
        ProductRatingSummaryResponse response = reviewService.getProductRatingSummary(productId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/api/v1/reviews/my")
    @Operation(summary = "Get reviews created by the authenticated customer")
    @SecurityRequirement(name = "BearerAuth")
    public ResponseEntity<ApiResponse<PageResponse<ReviewResponse>>> getCustomerReviews(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @PageableDefault(sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        PageResponse<ReviewResponse> response = reviewService.getCustomerReviews(userPrincipal.getId(), pageable);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PutMapping("/api/v1/reviews/{reviewId}")
    @Operation(summary = "Update own product review (resets status to PENDING for re-moderation)")
    @SecurityRequirement(name = "BearerAuth")
    public ResponseEntity<ApiResponse<ReviewResponse>> updateReview(
            @PathVariable Long reviewId,
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @Valid @RequestBody UpdateReviewRequest request
    ) {
        ReviewResponse response = reviewService.updateReview(userPrincipal.getId(), reviewId, request);
        return ResponseEntity.ok(ApiResponse.success("Review updated and submitted for moderation", response));
    }

    @DeleteMapping("/api/v1/reviews/{reviewId}")
    @Operation(summary = "Delete a review (own review or admin)")
    @SecurityRequirement(name = "BearerAuth")
    public ResponseEntity<ApiResponse<Void>> deleteReview(
            @PathVariable Long reviewId,
            @AuthenticationPrincipal UserPrincipal userPrincipal
    ) {
        boolean isAdmin = userPrincipal.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN") || a.getAuthority().equals("ROLE_MANAGER"));
        reviewService.deleteReview(userPrincipal.getId(), reviewId, isAdmin);
        return ResponseEntity.ok(ApiResponse.success("Review deleted successfully", null));
    }

    @GetMapping("/api/v1/reviews/admin")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @Operation(summary = "List reviews for moderation (Admin/Manager only)")
    @SecurityRequirement(name = "BearerAuth")
    public ResponseEntity<ApiResponse<PageResponse<ReviewResponse>>> getAdminReviews(
            @RequestParam(required = false) ReviewStatus status,
            @PageableDefault(sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        PageResponse<ReviewResponse> response = reviewService.getAdminReviews(status, pageable);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PatchMapping("/api/v1/reviews/admin/{reviewId}/moderation")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @Operation(summary = "Moderate review status (Admin/Manager only)")
    @SecurityRequirement(name = "BearerAuth")
    public ResponseEntity<ApiResponse<ReviewResponse>> moderateReview(
            @PathVariable Long reviewId,
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @Valid @RequestBody ReviewModerationRequest request
    ) {
        ReviewResponse response = reviewService.moderateReview(
                userPrincipal.getId(),
                userPrincipal.getEmail(),
                reviewId,
                request
        );
        return ResponseEntity.ok(ApiResponse.success("Review moderated successfully", response));
    }
}
