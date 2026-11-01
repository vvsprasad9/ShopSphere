package com.shopsphere.service;

import com.shopsphere.dto.request.review.ReviewModerationRequest;
import com.shopsphere.dto.request.review.ReviewRequest;
import com.shopsphere.dto.request.review.UpdateReviewRequest;
import com.shopsphere.dto.response.PageResponse;
import com.shopsphere.dto.response.review.ProductRatingSummaryResponse;
import com.shopsphere.dto.response.review.ReviewResponse;
import com.shopsphere.entity.enums.ReviewStatus;
import org.springframework.data.domain.Pageable;

public interface ReviewService {

    ReviewResponse createReview(Long userId, Long productId, ReviewRequest request);

    PageResponse<ReviewResponse> getProductReviews(Long productId, Pageable pageable);

    ProductRatingSummaryResponse getProductRatingSummary(Long productId);

    PageResponse<ReviewResponse> getCustomerReviews(Long userId, Pageable pageable);

    ReviewResponse updateReview(Long userId, Long reviewId, UpdateReviewRequest request);

    void deleteReview(Long userId, Long reviewId, boolean isAdmin);

    PageResponse<ReviewResponse> getAdminReviews(ReviewStatus status, Pageable pageable);

    ReviewResponse moderateReview(Long adminUserId, String adminEmail, Long reviewId, ReviewModerationRequest request);
}
