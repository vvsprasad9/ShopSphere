package com.shopsphere.controller;

import com.shopsphere.dto.request.product.*;
import com.shopsphere.dto.response.ApiResponse;
import com.shopsphere.dto.response.PageResponse;
import com.shopsphere.dto.response.product.ProductImageResponse;
import com.shopsphere.dto.response.product.ProductResponse;
import com.shopsphere.dto.response.product.ProductSummaryResponse;
import com.shopsphere.dto.response.product.ProductVariantResponse;
import com.shopsphere.service.ProductService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/v1/products")
@RequiredArgsConstructor
@Tag(name = "Products", description = "Endpoints for product search, catalog browsing, and inventory/catalog management")
public class ProductController {

    private final ProductService productService;

    @GetMapping
    @Operation(summary = "Search and filter products with pagination and dynamic sorting")
    public ResponseEntity<ApiResponse<PageResponse<ProductSummaryResponse>>> searchProducts(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) String categorySlug,
            @RequestParam(required = false) String brand,
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice,
            @RequestParam(required = false) Double minRating,
            @RequestParam(required = false) Boolean inStockOnly,
            @RequestParam(required = false) Boolean featured,
            @RequestParam(required = false, defaultValue = "true") Boolean active,
            @RequestParam(required = false, defaultValue = "newest") String sortBy,
            @RequestParam(required = false, defaultValue = "0") int page,
            @RequestParam(required = false, defaultValue = "20") int size
    ) {
        ProductSearchCriteria criteria = ProductSearchCriteria.builder()
                .keyword(keyword)
                .categoryId(categoryId)
                .categorySlug(categorySlug)
                .brand(brand)
                .minPrice(minPrice)
                .maxPrice(maxPrice)
                .minRating(minRating)
                .inStockOnly(inStockOnly)
                .featured(featured)
                .active(active)
                .sortBy(sortBy)
                .page(page)
                .size(size)
                .build();

        Pageable pageable = PageRequest.of(page, size);
        PageResponse<ProductSummaryResponse> response = productService.searchProducts(criteria, pageable);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get full product details by ID")
    public ResponseEntity<ApiResponse<ProductResponse>> getProductById(@PathVariable Long id) {
        ProductResponse response = productService.getProductById(id);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/slug/{slug}")
    @Operation(summary = "Get full product details by SEO slug")
    public ResponseEntity<ApiResponse<ProductResponse>> getProductBySlug(@PathVariable String slug) {
        ProductResponse response = productService.getProductBySlug(slug);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/featured")
    @Operation(summary = "Get top featured products for homepage showcase")
    public ResponseEntity<ApiResponse<List<ProductSummaryResponse>>> getFeaturedProducts(
            @RequestParam(required = false, defaultValue = "8") int limit
    ) {
        List<ProductSummaryResponse> response = productService.getFeaturedProducts(limit);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/{id}/related")
    @Operation(summary = "Get related products within the same category hierarchy")
    public ResponseEntity<ApiResponse<List<ProductSummaryResponse>>> getRelatedProducts(
            @PathVariable Long id,
            @RequestParam(required = false, defaultValue = "8") int limit
    ) {
        List<ProductSummaryResponse> response = productService.getRelatedProducts(id, limit);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @Operation(summary = "Create a new product with authoritative inventory (Admin/Manager only)", security = @SecurityRequirement(name = "BearerAuth"))
    public ResponseEntity<ApiResponse<ProductResponse>> createProduct(@Valid @RequestBody ProductCreateRequest request) {
        ProductResponse created = productService.createProduct(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Product created successfully", created));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @Operation(summary = "Update an existing product (Admin/Manager only)", security = @SecurityRequirement(name = "BearerAuth"))
    public ResponseEntity<ApiResponse<ProductResponse>> updateProduct(
            @PathVariable Long id,
            @Valid @RequestBody ProductUpdateRequest request
    ) {
        ProductResponse updated = productService.updateProduct(id, request);
        return ResponseEntity.ok(ApiResponse.success("Product updated successfully", updated));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Delete or soft-delete a product to preserve order history (Admin only)", security = @SecurityRequirement(name = "BearerAuth"))
    public ResponseEntity<ApiResponse<Void>> deleteProduct(@PathVariable Long id) {
        productService.deleteProduct(id);
        return ResponseEntity.ok(ApiResponse.success("Product deleted successfully", null));
    }

    // Image operations
    @PostMapping("/{id}/images")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @Operation(summary = "Add image to product (Admin/Manager only)", security = @SecurityRequirement(name = "BearerAuth"))
    public ResponseEntity<ApiResponse<ProductImageResponse>> addProductImage(
            @PathVariable Long id,
            @Valid @RequestBody ProductImageRequest request
    ) {
        ProductImageResponse response = productService.addProductImage(id, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Product image added successfully", response));
    }

    @DeleteMapping("/{id}/images/{imageId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @Operation(summary = "Delete product image (Admin/Manager only)", security = @SecurityRequirement(name = "BearerAuth"))
    public ResponseEntity<ApiResponse<Void>> deleteProductImage(
            @PathVariable Long id,
            @PathVariable Long imageId
    ) {
        productService.deleteProductImage(id, imageId);
        return ResponseEntity.ok(ApiResponse.success("Product image deleted successfully", null));
    }

    @PatchMapping("/{id}/images/{imageId}/primary")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @Operation(summary = "Set image as primary for product (Admin/Manager only)", security = @SecurityRequirement(name = "BearerAuth"))
    public ResponseEntity<ApiResponse<ProductImageResponse>> setPrimaryProductImage(
            @PathVariable Long id,
            @PathVariable Long imageId
    ) {
        ProductImageResponse response = productService.setPrimaryProductImage(id, imageId);
        return ResponseEntity.ok(ApiResponse.success("Primary image updated successfully", response));
    }

    // Variant operations
    @PostMapping("/{id}/variants")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @Operation(summary = "Add variant with independent inventory (Admin/Manager only)", security = @SecurityRequirement(name = "BearerAuth"))
    public ResponseEntity<ApiResponse<ProductVariantResponse>> addProductVariant(
            @PathVariable Long id,
            @Valid @RequestBody ProductVariantRequest request
    ) {
        ProductVariantResponse response = productService.addProductVariant(id, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Product variant added successfully", response));
    }

    @PutMapping("/{id}/variants/{variantId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @Operation(summary = "Update product variant and independent inventory (Admin/Manager only)", security = @SecurityRequirement(name = "BearerAuth"))
    public ResponseEntity<ApiResponse<ProductVariantResponse>> updateProductVariant(
            @PathVariable Long id,
            @PathVariable Long variantId,
            @Valid @RequestBody ProductVariantRequest request
    ) {
        ProductVariantResponse response = productService.updateProductVariant(id, variantId, request);
        return ResponseEntity.ok(ApiResponse.success("Product variant updated successfully", response));
    }

    @DeleteMapping("/{id}/variants/{variantId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @Operation(summary = "Delete product variant (Admin/Manager only)", security = @SecurityRequirement(name = "BearerAuth"))
    public ResponseEntity<ApiResponse<Void>> deleteProductVariant(
            @PathVariable Long id,
            @PathVariable Long variantId
    ) {
        productService.deleteProductVariant(id, variantId);
        return ResponseEntity.ok(ApiResponse.success("Product variant deleted successfully", null));
    }
}
