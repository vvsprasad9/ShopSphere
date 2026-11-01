package com.shopsphere.entity;

import com.shopsphere.entity.enums.DiscountType;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;

@Entity
@Table(name = "coupons", indexes = {
        @Index(name = "idx_coupon_code", columnList = "code"),
        @Index(name = "idx_coupon_active_dates", columnList = "active, start_date, expiration_date")
}, uniqueConstraints = {
        @UniqueConstraint(columnNames = "code")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Coupon extends BaseEntity {

    @Column(nullable = false, unique = true, length = 50)
    private String code;

    @Enumerated(EnumType.STRING)
    @Column(name = "discount_type", nullable = false, length = 20)
    private DiscountType discountType;

    @Column(name = "discount_value", nullable = false, precision = 12, scale = 2)
    private BigDecimal discountValue;

    @Column(name = "min_order_value", nullable = false, precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal minOrderValue = BigDecimal.ZERO;

    @Column(name = "max_discount_amount", precision = 12, scale = 2)
    private BigDecimal maxDiscountAmount;

    @Column(name = "start_date", nullable = false)
    private LocalDateTime startDate;

    @Column(name = "expiration_date", nullable = false)
    private LocalDateTime expirationDate;

    @Column(name = "usage_limit", nullable = false)
    @Builder.Default
    private Integer usageLimit = 100;

    @Column(name = "per_user_limit", nullable = false)
    @Builder.Default
    private Integer perUserLimit = 1;

    @Column(name = "times_used", nullable = false)
    @Builder.Default
    private Integer timesUsed = 0;

    @Column(nullable = false)
    @Builder.Default
    private boolean active = true;

    public boolean isValidForOrder(BigDecimal orderTotal) {
        LocalDateTime now = LocalDateTime.now();
        if (!active) return false;
        if (now.isBefore(startDate) || now.isAfter(expirationDate)) return false;
        if (timesUsed >= usageLimit) return false;
        return orderTotal == null || minOrderValue == null || orderTotal.compareTo(minOrderValue) >= 0;
    }

    public BigDecimal computeDiscount(BigDecimal subtotal) {
        if (!isValidForOrder(subtotal)) return BigDecimal.ZERO;
        BigDecimal calculated;
        if (discountType == DiscountType.PERCENTAGE) {
            calculated = subtotal.multiply(discountValue).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
            if (maxDiscountAmount != null && calculated.compareTo(maxDiscountAmount) > 0) {
                calculated = maxDiscountAmount;
            }
        } else {
            calculated = discountValue.min(subtotal);
        }
        return calculated;
    }
}
