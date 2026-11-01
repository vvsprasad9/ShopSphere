package com.shopsphere.dto.response.product;

import com.shopsphere.entity.Product;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductResponse {

    private Long id;
    private String name;
    private String slug;
    private String sku;
    private String description;
    private String shortDescription;
    private BigDecimal price;
    private BigDecimal discountPrice;
    private String brand;
    private Long categoryId;
    private String categoryName;
    private String categorySlug;
    private boolean active;
    private boolean featured;
    private Double averageRating;
    private Integer reviewCount;
    private int stockQuantity; // Authoritative stock from Inventory
    private boolean inStock;
    @Builder.Default
    private List<ProductImageResponse> images = new ArrayList<>();
    @Builder.Default
    private List<ProductVariantResponse> variants = new ArrayList<>();
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static ProductResponse fromEntity(Product product) {
        if (product == null) return null;
        int stock = product.getInventory() != null ? product.getInventory().getPurchasableQuantity() : 0;
        return ProductResponse.builder()
                .id(product.getId())
                .name(product.getName())
                .slug(product.getSlug())
                .sku(product.getSku())
                .description(product.getDescription())
                .shortDescription(product.getShortDescription())
                .price(product.getPrice())
                .discountPrice(product.getDiscountPrice())
                .brand(product.getBrand())
                .categoryId(product.getCategory() != null ? product.getCategory().getId() : null)
                .categoryName(product.getCategory() != null ? product.getCategory().getName() : null)
                .categorySlug(product.getCategory() != null ? product.getCategory().getSlug() : null)
                .active(product.isActive())
                .featured(product.isFeatured())
                .averageRating(product.getAverageRating())
                .reviewCount(product.getReviewCount())
                .stockQuantity(stock)
                .inStock(stock > 0)
                .images(product.getImages() != null ?
                        product.getImages().stream().map(ProductImageResponse::fromEntity).collect(Collectors.toList()) :
                        new ArrayList<>())
                .variants(product.getVariants() != null ?
                        product.getVariants().stream().map(ProductVariantResponse::fromEntity).collect(Collectors.toList()) :
                        new ArrayList<>())
                .createdAt(product.getCreatedAt())
                .updatedAt(product.getUpdatedAt())
                .build();
    }
}
