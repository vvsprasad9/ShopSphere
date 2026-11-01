package com.shopsphere.service;

import com.shopsphere.dto.request.product.*;
import com.shopsphere.dto.response.PageResponse;
import com.shopsphere.dto.response.product.ProductImageResponse;
import com.shopsphere.dto.response.product.ProductResponse;
import com.shopsphere.dto.response.product.ProductSummaryResponse;
import com.shopsphere.dto.response.product.ProductVariantResponse;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface ProductService {

    ProductResponse createProduct(ProductCreateRequest request);

    ProductResponse updateProduct(Long id, ProductUpdateRequest request);

    void deleteProduct(Long id);

    ProductResponse getProductById(Long id);

    ProductResponse getProductBySlug(String slug);

    PageResponse<ProductSummaryResponse> searchProducts(ProductSearchCriteria criteria, Pageable pageable);

    List<ProductSummaryResponse> getFeaturedProducts(int limit);

    List<ProductSummaryResponse> getRelatedProducts(Long productId, int limit);

    ProductImageResponse addProductImage(Long productId, ProductImageRequest request);

    void deleteProductImage(Long productId, Long imageId);

    ProductImageResponse setPrimaryProductImage(Long productId, Long imageId);

    ProductVariantResponse addProductVariant(Long productId, ProductVariantRequest request);

    ProductVariantResponse updateProductVariant(Long productId, Long variantId, ProductVariantRequest request);

    void deleteProductVariant(Long productId, Long variantId);
}
