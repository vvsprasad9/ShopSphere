package com.shopsphere.service.impl;

import com.shopsphere.dto.request.product.*;
import com.shopsphere.dto.response.PageResponse;
import com.shopsphere.dto.response.product.ProductImageResponse;
import com.shopsphere.dto.response.product.ProductResponse;
import com.shopsphere.dto.response.product.ProductSummaryResponse;
import com.shopsphere.dto.response.product.ProductVariantResponse;
import com.shopsphere.entity.Category;
import com.shopsphere.entity.Inventory;
import com.shopsphere.entity.Product;
import com.shopsphere.entity.ProductImage;
import com.shopsphere.entity.ProductVariant;
import com.shopsphere.exception.BadRequestException;
import com.shopsphere.exception.ResourceNotFoundException;
import com.shopsphere.repository.*;
import com.shopsphere.repository.specification.ProductSpecification;
import com.shopsphere.service.CategoryService;
import com.shopsphere.service.ProductService;
import com.shopsphere.util.SlugUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProductServiceImpl implements ProductService {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final InventoryRepository inventoryRepository;
    private final ProductImageRepository productImageRepository;
    private final ProductVariantRepository productVariantRepository;
    private final OrderItemRepository orderItemRepository;
    private final CategoryService categoryService;

    @Override
    @Transactional
    public ProductResponse createProduct(ProductCreateRequest request) {
        log.info("Creating product with SKU: {}", request.getSku());

        // Validate SKU uniqueness
        if (productRepository.existsBySku(request.getSku().trim())) {
            throw new BadRequestException("Product with SKU already exists: " + request.getSku());
        }

        // Validate Category
        Category category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Category not found with id: " + request.getCategoryId()));

        if (!category.isActive()) {
            throw new BadRequestException("Cannot assign product to an inactive category");
        }

        // Generate unique slug
        String slug = generateUniqueSlug(request.getName(), null);

        Product product = Product.builder()
                .name(request.getName().trim())
                .slug(slug)
                .sku(request.getSku().trim().toUpperCase())
                .description(request.getDescription())
                .shortDescription(request.getShortDescription())
                .price(request.getPrice())
                .discountPrice(request.getDiscountPrice())
                .brand(request.getBrand() != null ? request.getBrand().trim() : null)
                .category(category)
                .active(request.isActive())
                .featured(request.isFeatured())
                .averageRating(0.0)
                .reviewCount(0)
                .build();

        Product savedProduct = productRepository.save(product);

        // Authoritative Inventory initialization
        int initialStock = request.getInitialStockQuantity() != null ? request.getInitialStockQuantity() : 0;
        int lowStockThreshold = request.getLowStockThreshold() != null ? request.getLowStockThreshold() : 5;

        Inventory inventory = Inventory.builder()
                .product(savedProduct)
                .quantityAvailable(initialStock)
                .quantityReserved(0)
                .lowStockThreshold(lowStockThreshold)
                .build();

        inventoryRepository.save(inventory);
        savedProduct.setInventory(inventory);

        // Process initial images if provided
        if (request.getImageUrls() != null && !request.getImageUrls().isEmpty()) {
            int order = 0;
            for (String imageUrl : request.getImageUrls()) {
                if (imageUrl != null && !imageUrl.isBlank()) {
                    ProductImage image = ProductImage.builder()
                            .product(savedProduct)
                            .imageUrl(imageUrl.trim())
                            .altText(savedProduct.getName())
                            .isPrimary(order == 0)
                            .displayOrder(order)
                            .build();
                    productImageRepository.save(image);
                    savedProduct.getImages().add(image);
                    order++;
                }
            }
        }

        log.info("Product created successfully with id: {}, slug: {}", savedProduct.getId(), savedProduct.getSlug());
        return ProductResponse.fromEntity(savedProduct);
    }

    @Override
    @Transactional
    public ProductResponse updateProduct(Long id, ProductUpdateRequest request) {
        log.info("Updating product with id: {}", id);

        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + id));

        // Update name and slug if changed
        if (request.getName() != null && !request.getName().isBlank() && !product.getName().equalsIgnoreCase(request.getName().trim())) {
            String newSlug = generateUniqueSlug(request.getName().trim(), id);
            product.setSlug(newSlug);
            product.setName(request.getName().trim());
        }

        if (request.getDescription() != null) {
            product.setDescription(request.getDescription());
        }
        if (request.getShortDescription() != null) {
            product.setShortDescription(request.getShortDescription());
        }
        if (request.getPrice() != null) {
            product.setPrice(request.getPrice());
        }
        if (request.getDiscountPrice() != null) {
            product.setDiscountPrice(request.getDiscountPrice());
        }
        if (request.getBrand() != null) {
            product.setBrand(request.getBrand().trim());
        }
        if (request.getActive() != null) {
            product.setActive(request.getActive());
        }
        if (request.getFeatured() != null) {
            product.setFeatured(request.getFeatured());
        }

        // Update Category if provided
        if (request.getCategoryId() != null && (product.getCategory() == null || !request.getCategoryId().equals(product.getCategory().getId()))) {
            Category category = categoryRepository.findById(request.getCategoryId())
                    .orElseThrow(() -> new ResourceNotFoundException("Category not found with id: " + request.getCategoryId()));
            product.setCategory(category);
        }

        // Authoritative Inventory Update:
        // Inventory updates must never reduce available stock below already reserved stock.
        if (request.getStockQuantity() != null || request.getLowStockThreshold() != null) {
            Inventory inventory = inventoryRepository.findByProductId(id)
                    .orElseGet(() -> Inventory.builder()
                            .product(product)
                            .quantityAvailable(0)
                            .quantityReserved(0)
                            .lowStockThreshold(5)
                            .build());

            if (request.getStockQuantity() != null) {
                int reserved = inventory.getQuantityReserved() != null ? inventory.getQuantityReserved() : 0;
                if (request.getStockQuantity() < reserved) {
                    throw new BadRequestException("Cannot reduce available stock (" + request.getStockQuantity() +
                            ") below currently reserved quantity (" + reserved + ")");
                }
                inventory.setQuantityAvailable(request.getStockQuantity());
            }

            if (request.getLowStockThreshold() != null) {
                inventory.setLowStockThreshold(request.getLowStockThreshold());
            }

            inventoryRepository.save(inventory);
            product.setInventory(inventory);
        }

        Product updated = productRepository.save(product);
        log.info("Product updated successfully with id: {}", updated.getId());
        return ProductResponse.fromEntity(updated);
    }

    @Override
    @Transactional
    public void deleteProduct(Long id) {
        log.info("Attempting to delete product with id: {}", id);

        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + id));

        // Product deletion must preserve historical order integrity
        boolean hasOrders = orderItemRepository.existsByProductId(id);
        if (hasOrders) {
            log.info("Product id: {} has associated order items. Soft-deleting (active=false) to preserve order history.", id);
            product.setActive(false);
            productRepository.save(product);
        } else {
            log.info("Product id: {} has no order history. Removing entity.", id);
            productRepository.delete(product);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public ProductResponse getProductById(Long id) {
        Product product = productRepository.findByIdWithDetails(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + id));
        return ProductResponse.fromEntity(product);
    }

    @Override
    @Transactional(readOnly = true)
    public ProductResponse getProductBySlug(String slug) {
        Product product = productRepository.findBySlugWithDetails(slug)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with slug: " + slug));
        return ProductResponse.fromEntity(product);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<ProductSummaryResponse> searchProducts(ProductSearchCriteria criteria, Pageable pageable) {
        // Category hierarchy filtering: include all descendants efficiently
        if (criteria.getCategorySlug() != null && !criteria.getCategorySlug().isBlank()) {
            categoryRepository.findBySlug(criteria.getCategorySlug().trim())
                    .ifPresent(category -> {
                        Set<Long> descendantIds = categoryService.getAllDescendantCategoryIds(category.getId());
                        criteria.setCategoryIds(descendantIds);
                    });
        } else if (criteria.getCategoryId() != null) {
            Set<Long> descendantIds = categoryService.getAllDescendantCategoryIds(criteria.getCategoryId());
            criteria.setCategoryIds(descendantIds);
        }

        // Apply dynamic sorting
        Pageable sortedPageable = applySorting(criteria.getSortBy(), pageable);

        Specification<Product> spec = ProductSpecification.buildSpecification(criteria);
        Page<Product> page = productRepository.findAll(spec, sortedPageable);

        List<ProductSummaryResponse> content = page.getContent().stream()
                .map(ProductSummaryResponse::fromEntity)
                .collect(Collectors.toList());

        return PageResponse.<ProductSummaryResponse>builder()
                .content(content)
                .pageNumber(page.getNumber())
                .pageSize(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .isFirst(page.isFirst())
                .isLast(page.isLast())
                .hasNext(page.hasNext())
                .hasPrevious(page.hasPrevious())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProductSummaryResponse> getFeaturedProducts(int limit) {
        List<Product> products = productRepository.findTop8ByFeaturedTrueAndActiveTrueOrderByCreatedAtDesc();
        return products.stream()
                .limit(limit > 0 ? limit : 8)
                .map(ProductSummaryResponse::fromEntity)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProductSummaryResponse> getRelatedProducts(Long productId, int limit) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + productId));

        if (product.getCategory() == null) {
            return Collections.emptyList();
        }

        Set<Long> relatedCategoryIds = categoryService.getAllDescendantCategoryIds(product.getCategory().getId());
        List<Product> related = productRepository.findTop8ByCategoryIdInAndIdNotAndActiveTrueOrderByCreatedAtDesc(relatedCategoryIds, productId);

        return related.stream()
                .limit(limit > 0 ? limit : 8)
                .map(ProductSummaryResponse::fromEntity)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public ProductImageResponse addProductImage(Long productId, ProductImageRequest request) {
        log.info("Adding image to product id: {}", productId);

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + productId));

        long imageCount = productImageRepository.countByProductId(productId);
        boolean shouldBePrimary = request.isPrimary() || imageCount == 0;

        if (shouldBePrimary) {
            unmarkCurrentPrimaryImage(productId);
        }

        ProductImage image = ProductImage.builder()
                .product(product)
                .imageUrl(request.getImageUrl().trim())
                .altText(request.getAltText() != null ? request.getAltText().trim() : product.getName())
                .isPrimary(shouldBePrimary)
                .displayOrder(request.getDisplayOrder() != null ? request.getDisplayOrder() : (int) imageCount)
                .build();

        ProductImage saved = productImageRepository.save(image);
        return ProductImageResponse.fromEntity(saved);
    }

    @Override
    @Transactional
    public void deleteProductImage(Long productId, Long imageId) {
        log.info("Deleting image id: {} from product id: {}", imageId, productId);

        ProductImage image = productImageRepository.findByIdAndProductId(imageId, productId)
                .orElseThrow(() -> new ResourceNotFoundException("Image not found on product id: " + productId));

        boolean wasPrimary = image.isPrimary();
        productImageRepository.delete(image);

        // If primary image was removed, assign next available image as primary
        if (wasPrimary) {
            List<ProductImage> remaining = productImageRepository.findByProductIdOrderByDisplayOrderAsc(productId);
            if (!remaining.isEmpty()) {
                ProductImage nextPrimary = remaining.get(0);
                nextPrimary.setPrimary(true);
                productImageRepository.save(nextPrimary);
            }
        }
    }

    @Override
    @Transactional
    public ProductImageResponse setPrimaryProductImage(Long productId, Long imageId) {
        log.info("Setting image id: {} as primary for product id: {}", imageId, productId);

        ProductImage image = productImageRepository.findByIdAndProductId(imageId, productId)
                .orElseThrow(() -> new ResourceNotFoundException("Image not found on product id: " + productId));

        unmarkCurrentPrimaryImage(productId);

        image.setPrimary(true);
        ProductImage updated = productImageRepository.save(image);
        return ProductImageResponse.fromEntity(updated);
    }

    @Override
    @Transactional
    public ProductVariantResponse addProductVariant(Long productId, ProductVariantRequest request) {
        log.info("Adding variant SKU: {} to product id: {}", request.getSku(), productId);

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + productId));

        if (productVariantRepository.existsBySku(request.getSku().trim().toUpperCase())) {
            throw new BadRequestException("Variant with SKU already exists: " + request.getSku());
        }

        ProductVariant variant = ProductVariant.builder()
                .product(product)
                .sku(request.getSku().trim().toUpperCase())
                .variantName(request.getVariantName().trim())
                .priceAdjustment(request.getPriceAdjustment())
                .attributesJson(request.getAttributesJson())
                .active(request.isActive())
                .build();

        ProductVariant savedVariant = productVariantRepository.save(variant);

        // Variant inventory remains independent from parent product inventory
        int stockQuantity = request.getStockQuantity() != null ? request.getStockQuantity() : 0;
        int lowStockThreshold = request.getLowStockThreshold() != null ? request.getLowStockThreshold() : 5;

        Inventory variantInventory = Inventory.builder()
                .variant(savedVariant)
                .quantityAvailable(stockQuantity)
                .quantityReserved(0)
                .lowStockThreshold(lowStockThreshold)
                .build();

        inventoryRepository.save(variantInventory);
        savedVariant.setInventory(variantInventory);

        return ProductVariantResponse.fromEntity(savedVariant);
    }

    @Override
    @Transactional
    public ProductVariantResponse updateProductVariant(Long productId, Long variantId, ProductVariantRequest request) {
        log.info("Updating variant id: {} on product id: {}", variantId, productId);

        ProductVariant variant = productVariantRepository.findByIdAndProductId(variantId, productId)
                .orElseThrow(() -> new ResourceNotFoundException("Variant not found on product id: " + productId));

        if (productVariantRepository.existsBySkuAndIdNot(request.getSku().trim().toUpperCase(), variantId)) {
            throw new BadRequestException("Variant SKU already in use by another variant: " + request.getSku());
        }

        variant.setSku(request.getSku().trim().toUpperCase());
        variant.setVariantName(request.getVariantName().trim());
        variant.setPriceAdjustment(request.getPriceAdjustment());
        variant.setAttributesJson(request.getAttributesJson());
        variant.setActive(request.isActive());

        // Update independent variant inventory:
        // Inventory updates must never reduce available stock below already reserved stock.
        if (request.getStockQuantity() != null || request.getLowStockThreshold() != null) {
            Inventory variantInventory = inventoryRepository.findByVariantId(variantId)
                    .orElseGet(() -> Inventory.builder()
                            .variant(variant)
                            .quantityAvailable(0)
                            .quantityReserved(0)
                            .lowStockThreshold(5)
                            .build());

            if (request.getStockQuantity() != null) {
                int reserved = variantInventory.getQuantityReserved() != null ? variantInventory.getQuantityReserved() : 0;
                if (request.getStockQuantity() < reserved) {
                    throw new BadRequestException("Cannot reduce available variant stock (" + request.getStockQuantity() +
                            ") below currently reserved quantity (" + reserved + ")");
                }
                variantInventory.setQuantityAvailable(request.getStockQuantity());
            }

            if (request.getLowStockThreshold() != null) {
                variantInventory.setLowStockThreshold(request.getLowStockThreshold());
            }

            inventoryRepository.save(variantInventory);
            variant.setInventory(variantInventory);
        }

        ProductVariant updated = productVariantRepository.save(variant);
        return ProductVariantResponse.fromEntity(updated);
    }

    @Override
    @Transactional
    public void deleteProductVariant(Long productId, Long variantId) {
        log.info("Deleting variant id: {} from product id: {}", variantId, productId);

        ProductVariant variant = productVariantRepository.findByIdAndProductId(variantId, productId)
                .orElseThrow(() -> new ResourceNotFoundException("Variant not found on product id: " + productId));

        inventoryRepository.findByVariantId(variantId)
                .ifPresent(inventoryRepository::delete);

        productVariantRepository.delete(variant);
    }

    private void unmarkCurrentPrimaryImage(Long productId) {
        productImageRepository.findByProductIdAndIsPrimaryTrue(productId)
                .ifPresent(currentPrimary -> {
                    currentPrimary.setPrimary(false);
                    productImageRepository.save(currentPrimary);
                });
    }

    private Pageable applySorting(String sortBy, Pageable pageable) {
        if (sortBy == null || sortBy.isBlank()) {
            return PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), Sort.by(Sort.Direction.DESC, "createdAt"));
        }

        Sort sort = switch (sortBy.toLowerCase().trim()) {
            case "price_asc" -> Sort.by(Sort.Direction.ASC, "price");
            case "price_desc" -> Sort.by(Sort.Direction.DESC, "price");
            case "rating_desc" -> Sort.by(Sort.Direction.DESC, "averageRating");
            case "name_asc" -> Sort.by(Sort.Direction.ASC, "name");
            case "popular" -> Sort.by(Sort.Direction.DESC, "reviewCount");
            case "newest" -> Sort.by(Sort.Direction.DESC, "createdAt");
            default -> Sort.by(Sort.Direction.DESC, "createdAt");
        };

        return PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), sort);
    }

    private String generateUniqueSlug(String name, Long currentProductId) {
        String baseSlug = SlugUtils.toSlug(name);
        if (baseSlug.isBlank()) {
            baseSlug = "product";
        }

        String candidateSlug = baseSlug;
        int counter = 1;

        while (true) {
            boolean exists;
            if (currentProductId == null) {
                exists = productRepository.existsBySlug(candidateSlug);
            } else {
                exists = productRepository.existsBySlugAndIdNot(candidateSlug, currentProductId);
            }

            if (!exists) {
                return candidateSlug;
            }

            candidateSlug = baseSlug + "-" + counter;
            counter++;
        }
    }
}
