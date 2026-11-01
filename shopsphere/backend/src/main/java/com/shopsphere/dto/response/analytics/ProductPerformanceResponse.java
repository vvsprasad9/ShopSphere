package com.shopsphere.dto.response.analytics;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductPerformanceResponse {

    private Long productId;
    private String productName;
    private String sku;
    private String categoryName;
    private long unitsSold;
    private BigDecimal totalRevenue;
    private int currentPurchasableStock;
    private Double averageRating;
    private Integer reviewCount;
}
