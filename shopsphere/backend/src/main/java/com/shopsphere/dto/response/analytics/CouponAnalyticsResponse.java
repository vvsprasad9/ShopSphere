package com.shopsphere.dto.response.analytics;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CouponAnalyticsResponse {

    private Long couponId;
    private String code;
    private String discountType;
    private BigDecimal discountValue;
    private long timesUsed;
    private BigDecimal totalDiscountsGiven;
    private BigDecimal totalOrderRevenueGenerated;
    private boolean active;
    private LocalDateTime expirationDate;
}
