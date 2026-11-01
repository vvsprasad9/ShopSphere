package com.shopsphere.dto.request.product;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductVariantRequest {

    @NotBlank(message = "Variant SKU is required")
    @Size(max = 60, message = "SKU cannot exceed 60 characters")
    private String sku;

    @NotBlank(message = "Variant name is required")
    @Size(max = 120, message = "Variant name cannot exceed 120 characters")
    private String variantName;

    @NotNull(message = "Price adjustment is required")
    @Builder.Default
    private BigDecimal priceAdjustment = BigDecimal.ZERO;

    private String attributesJson;

    @NotNull(message = "Stock quantity is required")
    @Min(value = 0, message = "Stock quantity cannot be negative")
    @Builder.Default
    private Integer stockQuantity = 0;

    @Min(value = 0, message = "Low stock threshold cannot be negative")
    @Builder.Default
    private Integer lowStockThreshold = 5;

    @Builder.Default
    private boolean active = true;
}
