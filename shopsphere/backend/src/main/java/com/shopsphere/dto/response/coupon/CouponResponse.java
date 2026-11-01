package com.shopsphere.dto.response.coupon;

import com.shopsphere.entity.Coupon;
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
public class CouponResponse {

    private Long id;
    private String code;
    private String discountType;
    private BigDecimal discountValue;
    private BigDecimal minOrderValue;
    private BigDecimal maxDiscountAmount;
    private LocalDateTime startDate;
    private LocalDateTime expirationDate;
    private Integer usageLimit;
    private Integer perUserLimit;
    private Integer timesUsed;
    private boolean active;
    private boolean isExpired;

    public static CouponResponse fromEntity(Coupon coupon) {
        if (coupon == null) return null;
        boolean expired = LocalDateTime.now().isAfter(coupon.getExpirationDate());
        return CouponResponse.builder()
                .id(coupon.getId())
                .code(coupon.getCode())
                .discountType(coupon.getDiscountType().name())
                .discountValue(coupon.getDiscountValue())
                .minOrderValue(coupon.getMinOrderValue())
                .maxDiscountAmount(coupon.getMaxDiscountAmount())
                .startDate(coupon.getStartDate())
                .expirationDate(coupon.getExpirationDate())
                .usageLimit(coupon.getUsageLimit())
                .perUserLimit(coupon.getPerUserLimit())
                .timesUsed(coupon.getTimesUsed())
                .active(coupon.isActive())
                .isExpired(expired)
                .build();
    }
}
