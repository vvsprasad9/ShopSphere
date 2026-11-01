package com.shopsphere.dto.response.product;

import com.shopsphere.entity.ProductVariant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductVariantResponse {

    private Long id;
    private String sku;
    private String variantName;
    private BigDecimal priceAdjustment;
    private String attributesJson;
    private boolean active;
    private int stockQuantity;

    public static ProductVariantResponse fromEntity(ProductVariant variant) {
        if (variant == null) return null;
        int stock = variant.getInventory() != null ? variant.getInventory().getPurchasableQuantity() : 0;
        return ProductVariantResponse.builder()
                .id(variant.getId())
                .sku(variant.getSku())
                .variantName(variant.getVariantName())
                .priceAdjustment(variant.getPriceAdjustment())
                .attributesJson(variant.getAttributesJson())
                .active(variant.isActive())
                .stockQuantity(stock)
                .build();
    }
}
