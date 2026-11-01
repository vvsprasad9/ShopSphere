package com.shopsphere.dto.response.review;

import com.shopsphere.entity.Review;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReviewResponse {

    private Long id;
    private Long productId;
    private String productName;
    private Long userId;
    private String reviewerName;
    private Integer rating;
    private String title;
    private String comment;
    @com.fasterxml.jackson.annotation.JsonProperty("isVerifiedPurchase")
    private boolean isVerifiedPurchase;
    private String status;
    private String moderationNotes;
    private LocalDateTime moderatedAt;
    private Long moderatedBy;
    private LocalDateTime createdAt;

    public static ReviewResponse fromEntity(Review review) {
        if (review == null) return null;
        String reviewer = "Customer";
        if (review.getUser() != null) {
            String first = review.getUser().getFirstName() != null ? review.getUser().getFirstName() : "";
            String last = (review.getUser().getLastName() != null && !review.getUser().getLastName().isBlank())
                    ? " " + review.getUser().getLastName().charAt(0) + "." : "";
            reviewer = (first + last).trim();
            if (reviewer.isEmpty()) {
                reviewer = "Customer";
            }
        }
        return ReviewResponse.builder()
                .id(review.getId())
                .productId(review.getProduct() != null ? review.getProduct().getId() : null)
                .productName(review.getProduct() != null ? review.getProduct().getName() : null)
                .userId(review.getUser() != null ? review.getUser().getId() : null)
                .reviewerName(reviewer)
                .rating(review.getRating())
                .title(review.getTitle())
                .comment(review.getComment())
                .isVerifiedPurchase(review.isVerifiedPurchase())
                .status(review.getStatus() != null ? review.getStatus().name() : null)
                .moderationNotes(review.getModerationNotes())
                .moderatedAt(review.getModeratedAt())
                .moderatedBy(review.getModeratedBy())
                .createdAt(review.getCreatedAt())
                .build();
    }
}
