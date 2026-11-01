package com.shopsphere.service.impl;

import com.shopsphere.dto.request.review.ReviewModerationRequest;
import com.shopsphere.dto.request.review.ReviewRequest;
import com.shopsphere.dto.request.review.UpdateReviewRequest;
import com.shopsphere.dto.response.PageResponse;
import com.shopsphere.dto.response.review.ProductRatingSummaryResponse;
import com.shopsphere.dto.response.review.ReviewResponse;
import com.shopsphere.entity.AuditLog;
import com.shopsphere.entity.Product;
import com.shopsphere.entity.Review;
import com.shopsphere.entity.User;
import com.shopsphere.entity.enums.OrderStatus;
import com.shopsphere.entity.enums.ReviewStatus;
import com.shopsphere.exception.BadRequestException;
import com.shopsphere.exception.ConflictException;
import com.shopsphere.exception.ResourceNotFoundException;
import com.shopsphere.repository.*;
import com.shopsphere.service.ReviewService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReviewServiceImpl implements ReviewService {

    private final ReviewRepository reviewRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    private final OrderItemRepository orderItemRepository;
    private final AuditLogRepository auditLogRepository;

    private static final List<OrderStatus> ELIGIBLE_ORDER_STATUSES = List.of(
            OrderStatus.CONFIRMED,
            OrderStatus.PROCESSING,
            OrderStatus.SHIPPED,
            OrderStatus.OUT_FOR_DELIVERY,
            OrderStatus.DELIVERED
    );

    @Override
    @Transactional
    public ReviewResponse createReview(Long userId, Long productId, ReviewRequest request) {
        log.info("User {} submitting review for product {}", userId, productId);

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + productId));

        if (reviewRepository.existsByProductIdAndUserId(productId, userId)) {
            throw new ConflictException("You have already reviewed this product");
        }

        boolean hasEligibleOrder = orderItemRepository.hasUserOrderedProductWithStatuses(userId, productId, ELIGIBLE_ORDER_STATUSES);
        if (!hasEligibleOrder) {
            throw new BadRequestException("You can only review products from confirmed or completed orders");
        }

        // Verified purchase badge strictly requires DELIVERED order status (and unrefunded payment)
        boolean isVerifiedPurchase = orderItemRepository.hasUserDeliveredOrderForProduct(userId, productId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        Review review = Review.builder()
                .product(product)
                .user(user)
                .rating(request.getRating())
                .title(request.getTitle())
                .comment(request.getComment())
                .isVerifiedPurchase(isVerifiedPurchase)
                .status(ReviewStatus.PENDING)
                .build();

        Review saved = reviewRepository.saveAndFlush(review);
        recalculateProductRating(productId);

        log.info("Review created successfully with id {} in PENDING status for product {}", saved.getId(), productId);
        return ReviewResponse.fromEntity(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<ReviewResponse> getProductReviews(Long productId, Pageable pageable) {
        if (!productRepository.existsById(productId)) {
            throw new ResourceNotFoundException("Product not found with id: " + productId);
        }

        Page<Review> page = reviewRepository.findByProductIdAndStatus(productId, ReviewStatus.APPROVED, pageable);
        return PageResponse.fromPage(page.map(ReviewResponse::fromEntity));
    }

    @Override
    @Transactional(readOnly = true)
    public ProductRatingSummaryResponse getProductRatingSummary(Long productId) {
        if (!productRepository.existsById(productId)) {
            throw new ResourceNotFoundException("Product not found with id: " + productId);
        }

        Double avg = reviewRepository.calculateAverageRatingForProduct(productId, ReviewStatus.APPROVED);
        Integer total = reviewRepository.countApprovedReviewsForProduct(productId, ReviewStatus.APPROVED);
        List<Object[]> distributionList = reviewRepository.getRatingDistributionForProduct(productId, ReviewStatus.APPROVED);

        Map<Integer, Long> distribution = new LinkedHashMap<>();
        for (int i = 5; i >= 1; i--) {
            distribution.put(i, 0L);
        }

        if (distributionList != null) {
            for (Object[] row : distributionList) {
                if (row != null && row.length >= 2 && row[0] instanceof Integer r && row[1] instanceof Long cnt) {
                    if (distribution.containsKey(r)) {
                        distribution.put(r, cnt);
                    }
                }
            }
        }

        int totalReviews = total != null ? total : 0;
        Map<Integer, Double> percentages = new LinkedHashMap<>();
        for (int i = 5; i >= 1; i--) {
            long count = distribution.get(i);
            double pct = totalReviews > 0 ? (count * 100.0) / totalReviews : 0.0;
            percentages.put(i, Math.round(pct * 10.0) / 10.0);
        }

        double roundedAvg = avg != null ? Math.round(avg * 10.0) / 10.0 : 0.0;

        return ProductRatingSummaryResponse.builder()
                .productId(productId)
                .averageRating(roundedAvg)
                .totalReviews(totalReviews)
                .ratingDistribution(distribution)
                .ratingPercentages(percentages)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<ReviewResponse> getCustomerReviews(Long userId, Pageable pageable) {
        Page<Review> page = reviewRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable);
        return PageResponse.fromPage(page.map(ReviewResponse::fromEntity));
    }

    @Override
    @Transactional
    public ReviewResponse updateReview(Long userId, Long reviewId, UpdateReviewRequest request) {
        log.info("User {} updating review id: {}", userId, reviewId);

        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new ResourceNotFoundException("Review not found with id: " + reviewId));

        if (review.getUser() == null || !review.getUser().getId().equals(userId)) {
            throw new BadRequestException("You can only edit your own reviews");
        }

        review.setRating(request.getRating());
        review.setTitle(request.getTitle());
        review.setComment(request.getComment());
        // Reset to PENDING for re-moderation
        review.setStatus(ReviewStatus.PENDING);

        Review saved = reviewRepository.saveAndFlush(review);
        recalculateProductRating(review.getProduct().getId());

        log.info("Review {} updated and reset to PENDING status", reviewId);
        return ReviewResponse.fromEntity(saved);
    }

    @Override
    @Transactional
    public void deleteReview(Long userId, Long reviewId, boolean isAdmin) {
        log.info("Deleting review id: {} by user: {} (isAdmin: {})", reviewId, userId, isAdmin);

        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new ResourceNotFoundException("Review not found with id: " + reviewId));

        if (!isAdmin && (review.getUser() == null || !review.getUser().getId().equals(userId))) {
            throw new BadRequestException("You are not authorized to delete this review");
        }

        Long productId = review.getProduct().getId();
        reviewRepository.delete(review);
        reviewRepository.flush();
        recalculateProductRating(productId);

        log.info("Review {} deleted successfully", reviewId);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<ReviewResponse> getAdminReviews(ReviewStatus status, Pageable pageable) {
        Page<Review> page;
        if (status != null) {
            page = reviewRepository.findByStatus(status, pageable);
        } else {
            page = reviewRepository.findAll(pageable);
        }
        return PageResponse.fromPage(page.map(ReviewResponse::fromEntity));
    }

    @Override
    @Transactional
    public ReviewResponse moderateReview(Long adminUserId, String adminEmail, Long reviewId, ReviewModerationRequest request) {
        log.info("Admin {} moderating review id: {} to status: {}", adminUserId, reviewId, request.getStatus());

        if (request.getStatus() == null) {
            throw new BadRequestException("Review status is required for moderation");
        }

        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new ResourceNotFoundException("Review not found with id: " + reviewId));

        review.setStatus(request.getStatus());
        review.setModerationNotes(request.getReason());
        review.setModeratedAt(LocalDateTime.now());
        review.setModeratedBy(adminUserId);

        Review saved = reviewRepository.saveAndFlush(review);

        // Record audit log entry
        AuditLog auditLog = AuditLog.builder()
                .userId(adminUserId)
                .userEmail(adminEmail)
                .action("REVIEW_MODERATION")
                .entityName("Review")
                .entityId(String.valueOf(reviewId))
                .details("Status changed to " + request.getStatus() + (request.getReason() != null ? ". Reason: " + request.getReason() : ""))
                .build();
        auditLogRepository.save(auditLog);

        recalculateProductRating(review.getProduct().getId());

        log.info("Review {} moderated to status {}", reviewId, request.getStatus());
        return ReviewResponse.fromEntity(saved);
    }

    private void recalculateProductRating(Long productId) {
        Double avg = reviewRepository.calculateAverageRatingForProduct(productId, ReviewStatus.APPROVED);
        Integer count = reviewRepository.countApprovedReviewsForProduct(productId, ReviewStatus.APPROVED);

        double roundedAvg = avg != null ? Math.round(avg * 10.0) / 10.0 : 0.0;
        int safeCount = count != null ? count : 0;

        Product product = productRepository.findById(productId).orElse(null);
        if (product != null) {
            product.setAverageRating(roundedAvg);
            product.setReviewCount(safeCount);
            productRepository.save(product);
        }
    }
}
