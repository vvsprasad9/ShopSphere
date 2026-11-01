package com.shopsphere.dto.request.product;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.Collection;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductSearchCriteria {

    private String keyword;
    private Long categoryId;
    private String categorySlug;
    private Collection<Long> categoryIds; // Populated by service to include all category descendants
    private String brand;
    private BigDecimal minPrice;
    private BigDecimal maxPrice;
    private Double minRating;
    private Boolean inStockOnly;
    private Boolean featured;
    @Builder.Default
    private Boolean active = true;
    @Builder.Default
    private String sortBy = "newest";
    @Builder.Default
    private int page = 0;
    @Builder.Default
    private int size = 20;
}
