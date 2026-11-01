package com.shopsphere.dto.request.coupon;

import com.shopsphere.entity.enums.DiscountType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
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
public class CouponRequest {

    @NotBlank(message = "Coupon code is required")
    private String code;

    @NotNull(message = "Discount type is required")
    private DiscountType discountType;

    @NotNull(message = "Discount value is required")
    @DecimalMin(value = "0.01", message = "Discount value must be greater than zero")
    private BigDecimal discountValue;

    @DecimalMin(value = "0.00", message = "Minimum order value cannot be negative")
    @Builder.Default
    private BigDecimal minOrderValue = BigDecimal.ZERO;

    private BigDecimal maxDiscountAmount;

    @NotNull(message = "Start date is required")
    private LocalDateTime startDate;

    @NotNull(message = "Expiration date is required")
    private LocalDateTime expirationDate;

    @Min(value = 1, message = "Usage limit must be at least 1")
    @Builder.Default
    private Integer usageLimit = 100;

    @Min(value = 1, message = "Per user limit must be at least 1")
    @Builder.Default
    private Integer perUserLimit = 1;

    @Builder.Default
    private boolean active = true;
}
