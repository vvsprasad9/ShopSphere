package com.shopsphere.dto.request.product;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductCreateRequest {

    @NotBlank(message = "Product name is required")
    @Size(max = 200, message = "Product name cannot exceed 200 characters")
    private String name;

    @NotBlank(message = "Product SKU is required")
    @Size(max = 60, message = "SKU cannot exceed 60 characters")
    private String sku;

    private String description;

    @Size(max = 500, message = "Short description cannot exceed 500 characters")
    private String shortDescription;

    @NotNull(message = "Price is required")
    @DecimalMin(value = "0.01", message = "Price must be at least 0.01")
    private BigDecimal price;

    @DecimalMin(value = "0.00", message = "Discount price cannot be negative")
    private BigDecimal discountPrice;

    @Size(max = 100, message = "Brand cannot exceed 100 characters")
    private String brand;

    @NotNull(message = "Category ID is required")
    private Long categoryId;

    /**
     * Initial stock allocated directly into authoritative Inventory entity
     */
    @NotNull(message = "Initial stock quantity is required")
    @Min(value = 0, message = "Stock quantity cannot be negative")
    @Builder.Default
    private Integer initialStockQuantity = 0;

    @Min(value = 0, message = "Low stock threshold cannot be negative")
    @Builder.Default
    private Integer lowStockThreshold = 5;

    @Builder.Default
    private boolean active = true;

    @Builder.Default
    private boolean featured = false;

    @Builder.Default
    private List<String> imageUrls = new ArrayList<>();
}
