package com.shopsphere.dto.response.review;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductRatingSummaryResponse {

    private Long productId;
    private Double averageRating;
    private Integer totalReviews;
    private Map<Integer, Long> ratingDistribution;
    private Map<Integer, Double> ratingPercentages;
}
