package com.shopsphere.dto.response.product;

import com.shopsphere.entity.Product;
import com.shopsphere.entity.ProductImage;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductSummaryResponse {

    private Long id;
    private String name;
    private String slug;
    private String sku;
    private BigDecimal price;
    private BigDecimal discountPrice;
    private String brand;
    private Long categoryId;
    private String categoryName;
    private String primaryImageUrl;
    private Double averageRating;
    private Integer reviewCount;
    private int stockQuantity;
    private boolean inStock;
    private boolean featured;

    public static ProductSummaryResponse fromEntity(Product product) {
        if (product == null) return null;
        int stock = product.getInventory() != null ? product.getInventory().getPurchasableQuantity() : 0;
        String primaryImage = null;
        if (product.getImages() != null && !product.getImages().isEmpty()) {
            primaryImage = product.getImages().stream()
                    .filter(ProductImage::isPrimary)
                    .map(ProductImage::getImageUrl)
                    .findFirst()
                    .orElse(product.getImages().iterator().next().getImageUrl());
        }

        return ProductSummaryResponse.builder()
                .id(product.getId())
                .name(product.getName())
                .slug(product.getSlug())
                .sku(product.getSku())
                .price(product.getPrice())
                .discountPrice(product.getDiscountPrice())
                .brand(product.getBrand())
                .categoryId(product.getCategory() != null ? product.getCategory().getId() : null)
                .categoryName(product.getCategory() != null ? product.getCategory().getName() : null)
                .primaryImageUrl(primaryImage)
                .averageRating(product.getAverageRating())
                .reviewCount(product.getReviewCount())
                .stockQuantity(stock)
                .inStock(stock > 0)
                .featured(product.isFeatured())
                .build();
    }
}
