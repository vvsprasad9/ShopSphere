package com.shopsphere.service;

import com.shopsphere.dto.request.product.*;
import com.shopsphere.dto.response.product.ProductImageResponse;
import com.shopsphere.dto.response.product.ProductResponse;
import com.shopsphere.dto.response.product.ProductVariantResponse;
import com.shopsphere.entity.*;
import com.shopsphere.exception.BadRequestException;
import com.shopsphere.exception.ResourceNotFoundException;
import com.shopsphere.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class ProductServiceTest {

    @Autowired
    private ProductService productService;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private InventoryRepository inventoryRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private OrderItemRepository orderItemRepository;

    @Autowired
    private ProductImageRepository productImageRepository;

    @Autowired
    private ProductVariantRepository productVariantRepository;

    @Autowired
    private UserRepository userRepository;

    private Category testCategory;

    @BeforeEach
    void setUp() {
        testCategory = categoryRepository.save(Category.builder()
                .name("Audio & Headphones")
                .slug("audio-headphones")
                .active(true)
                .displayOrder(0)
                .build());
    }

    @Test
    @DisplayName("1. Product creation with authoritative inventory initialization")
    void testCreateProductAuthoritativeInventory() {
        ProductCreateRequest request = ProductCreateRequest.builder()
                .name("Noise-Cancelling Headphones Pro")
                .sku("AUDIO-NC-PRO-01")
                .price(BigDecimal.valueOf(299.99))
                .discountPrice(BigDecimal.valueOf(249.99))
                .brand("SoundMaster")
                .categoryId(testCategory.getId())
                .initialStockQuantity(50)
                .lowStockThreshold(10)
                .imageUrls(List.of("https://example.com/img1.jpg", "https://example.com/img2.jpg"))
                .build();

        ProductResponse response = productService.createProduct(request);

        assertThat(response.getId()).isNotNull();
        assertThat(response.getSlug()).isEqualTo("noise-cancelling-headphones-pro");
        assertThat(response.getStockQuantity()).isEqualTo(50);
        assertThat(response.isInStock()).isTrue();
        assertThat(response.getImages()).hasSize(2);
        assertThat(response.getImages().get(0).isPrimary()).isTrue();
        assertThat(response.getImages().get(1).isPrimary()).isFalse();

        // Verify Inventory entity directly in database
        Inventory inv = inventoryRepository.findByProductId(response.getId()).orElse(null);
        assertThat(inv).isNotNull();
        assertThat(inv.getQuantityAvailable()).isEqualTo(50);
        assertThat(inv.getQuantityReserved()).isEqualTo(0);
        assertThat(inv.getPurchasableQuantity()).isEqualTo(50);
    }

    @Test
    @DisplayName("2. Enforce product SKU uniqueness")
    void testEnforceSkuUniqueness() {
        ProductCreateRequest req1 = ProductCreateRequest.builder()
                .name("Product 1")
                .sku("SKU-UNIQUE-TEST")
                .price(BigDecimal.valueOf(100.00))
                .categoryId(testCategory.getId())
                .initialStockQuantity(10)
                .build();
        productService.createProduct(req1);

        ProductCreateRequest req2 = ProductCreateRequest.builder()
                .name("Product 2")
                .sku("SKU-UNIQUE-TEST")
                .price(BigDecimal.valueOf(120.00))
                .categoryId(testCategory.getId())
                .initialStockQuantity(5)
                .build();

        assertThatThrownBy(() -> productService.createProduct(req2))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("SKU already exists");
    }

    @Test
    @DisplayName("3. Product slug collision retry handling")
    void testProductSlugCollisionRetry() {
        ProductCreateRequest req1 = ProductCreateRequest.builder()
                .name("Wireless Gaming Mouse")
                .sku("MOUSE-01")
                .price(BigDecimal.valueOf(59.99))
                .categoryId(testCategory.getId())
                .initialStockQuantity(10)
                .build();
        ProductResponse res1 = productService.createProduct(req1);
        assertThat(res1.getSlug()).isEqualTo("wireless-gaming-mouse");

        ProductCreateRequest req2 = ProductCreateRequest.builder()
                .name("Wireless Gaming Mouse")
                .sku("MOUSE-02")
                .price(BigDecimal.valueOf(69.99))
                .categoryId(testCategory.getId())
                .initialStockQuantity(10)
                .build();
        ProductResponse res2 = productService.createProduct(req2);
        assertThat(res2.getSlug()).isEqualTo("wireless-gaming-mouse-1");
    }

    @Test
    @DisplayName("4. Inventory reservation protection - cannot reduce available below reserved")
    void testInventoryReservationProtection() {
        ProductCreateRequest createReq = ProductCreateRequest.builder()
                .name("Mechanical Keyboard")
                .sku("KB-MECH-01")
                .price(BigDecimal.valueOf(149.99))
                .categoryId(testCategory.getId())
                .initialStockQuantity(20)
                .build();
        ProductResponse product = productService.createProduct(createReq);

        // Simulate reservation of 8 items in authoritative inventory
        Inventory inventory = inventoryRepository.findByProductId(product.getId()).orElseThrow();
        inventory.setQuantityReserved(8);
        inventoryRepository.save(inventory);

        // Attempt to update available stock to 5 (less than 8 reserved)
        ProductUpdateRequest updateReq = ProductUpdateRequest.builder()
                .stockQuantity(5)
                .build();

        assertThatThrownBy(() -> productService.updateProduct(product.getId(), updateReq))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Cannot reduce available stock (5) below currently reserved quantity (8)");

        // Valid update to 10 (>= 8) should succeed
        ProductUpdateRequest validUpdate = ProductUpdateRequest.builder()
                .stockQuantity(10)
                .build();
        ProductResponse updatedResp = productService.updateProduct(product.getId(), validUpdate);
        assertThat(updatedResp.getStockQuantity()).isEqualTo(2); // 10 available - 8 reserved = 2 purchasable
    }

    @Test
    @DisplayName("5. Product soft-deletion when order history exists")
    void testProductSoftDeletionWhenOrdered() {
        ProductCreateRequest createReq = ProductCreateRequest.builder()
                .name("Ordered Product Test")
                .sku("ORD-PROD-01")
                .price(BigDecimal.valueOf(80.00))
                .categoryId(testCategory.getId())
                .initialStockQuantity(10)
                .build();
        ProductResponse product = productService.createProduct(createReq);

        Product productEntity = productRepository.findById(product.getId()).orElseThrow();

        // Simulate an order containing this product
        User testUser = userRepository.save(User.builder()
                .email("order.softdelete@shopsphere.com")
                .password("hash")
                .firstName("Test")
                .lastName("User")
                .build());

        AddressSnapshot addressSnapshot = AddressSnapshot.builder()
                .recipientName("Test User")
                .phoneNumber("1234567890")
                .addressLine1("123 Main St")
                .city("Springfield")
                .state("IL")
                .postalCode("62701")
                .country("USA")
                .build();

        Order dummyOrder = orderRepository.save(Order.builder()
                .orderNumber("ORD-TEST-999")
                .user(testUser)
                .shippingAddress(addressSnapshot)
                .billingAddress(addressSnapshot)
                .subtotal(BigDecimal.valueOf(80.00))
                .totalAmount(BigDecimal.valueOf(80.00))
                .build());

        orderItemRepository.save(OrderItem.builder()
                .order(dummyOrder)
                .product(productEntity)
                .productName(productEntity.getName())
                .productSku(productEntity.getSku())
                .unitPrice(productEntity.getPrice())
                .quantity(1)
                .totalPrice(productEntity.getPrice())
                .build());

        // Deleting should soft-delete (active = false)
        productService.deleteProduct(product.getId());

        Product fetched = productRepository.findById(product.getId()).orElseThrow();
        assertThat(fetched.isActive()).isFalse();
    }

    @Test
    @DisplayName("6. Product hard deletion when no order history exists")
    void testProductHardDeletionWhenUnordered() {
        ProductCreateRequest createReq = ProductCreateRequest.builder()
                .name("Unordered Product Test")
                .sku("UNORD-PROD-01")
                .price(BigDecimal.valueOf(30.00))
                .categoryId(testCategory.getId())
                .initialStockQuantity(5)
                .build();
        ProductResponse product = productService.createProduct(createReq);

        productService.deleteProduct(product.getId());

        assertThat(productRepository.findById(product.getId())).isEmpty();
    }

    @Test
    @DisplayName("7. Product image management and primary image rules")
    void testProductImageRules() {
        ProductCreateRequest createReq = ProductCreateRequest.builder()
                .name("Camera Lens")
                .sku("LENS-01")
                .price(BigDecimal.valueOf(450.00))
                .categoryId(testCategory.getId())
                .initialStockQuantity(5)
                .build();
        ProductResponse product = productService.createProduct(createReq);

        // 1. First image should automatically become primary
        ProductImageResponse img1 = productService.addProductImage(product.getId(), ProductImageRequest.builder()
                .imageUrl("https://example.com/lens1.jpg")
                .isPrimary(false)
                .build());
        assertThat(img1.isPrimary()).isTrue();

        // 2. Second image added without primary remains non-primary
        ProductImageResponse img2 = productService.addProductImage(product.getId(), ProductImageRequest.builder()
                .imageUrl("https://example.com/lens2.jpg")
                .isPrimary(false)
                .build());
        assertThat(img2.isPrimary()).isFalse();

        // 3. Setting img2 as primary unmarks img1
        ProductImageResponse updatedImg2 = productService.setPrimaryProductImage(product.getId(), img2.getId());
        assertThat(updatedImg2.isPrimary()).isTrue();

        ProductImage refreshedImg1 = productImageRepository.findById(img1.getId()).orElseThrow();
        assertThat(refreshedImg1.isPrimary()).isFalse();

        // 4. Deleting primary image promotes remaining image to primary
        productService.deleteProductImage(product.getId(), img2.getId());
        ProductImage promotedImg1 = productImageRepository.findById(img1.getId()).orElseThrow();
        assertThat(promotedImg1.isPrimary()).isTrue();
    }

    @Test
    @DisplayName("8. Product image ownership validation")
    void testImageOwnershipCheck() {
        Product p1 = productRepository.save(Product.builder()
                .name("Product A")
                .sku("SKU-A")
                .slug("product-a")
                .price(BigDecimal.TEN)
                .category(testCategory)
                .build());

        Product p2 = productRepository.save(Product.builder()
                .name("Product B")
                .sku("SKU-B")
                .slug("product-b")
                .price(BigDecimal.TEN)
                .category(testCategory)
                .build());

        ProductImage imgOnP1 = productImageRepository.save(ProductImage.builder()
                .product(p1)
                .imageUrl("https://example.com/a.jpg")
                .isPrimary(true)
                .build());

        // Attempting to delete or set primary on p2 for an image belonging to p1 should throw
        assertThatThrownBy(() -> productService.deleteProductImage(p2.getId(), imgOnP1.getId()))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Image not found on product id: " + p2.getId());
    }

    @Test
    @DisplayName("9. Product variant management with independent variant inventory")
    void testVariantIndependentInventory() {
        ProductCreateRequest createReq = ProductCreateRequest.builder()
                .name("Running Shoes")
                .sku("SHOE-RUN-BASE")
                .price(BigDecimal.valueOf(110.00))
                .categoryId(testCategory.getId())
                .initialStockQuantity(100) // Base product stock
                .build();
        ProductResponse product = productService.createProduct(createReq);

        // Add Variant: Size 10 Red
        ProductVariantRequest vReq1 = ProductVariantRequest.builder()
                .sku("SHOE-RUN-10-RED")
                .variantName("Size: 10, Color: Red")
                .priceAdjustment(BigDecimal.valueOf(10.00))
                .stockQuantity(15) // Independent variant stock
                .lowStockThreshold(3)
                .build();

        ProductVariantResponse vResp1 = productService.addProductVariant(product.getId(), vReq1);
        assertThat(vResp1.getId()).isNotNull();
        assertThat(vResp1.getStockQuantity()).isEqualTo(15);

        // Verify variant inventory is stored independently
        Inventory vInv = inventoryRepository.findByVariantId(vResp1.getId()).orElseThrow();
        assertThat(vInv.getQuantityAvailable()).isEqualTo(15);
        assertThat(vInv.getVariant().getId()).isEqualTo(vResp1.getId());

        // Verify base product inventory remains unchanged (100)
        Inventory baseInv = inventoryRepository.findByProductId(product.getId()).orElseThrow();
        assertThat(baseInv.getQuantityAvailable()).isEqualTo(100);

        // Simulate reservation on variant inventory
        vInv.setQuantityReserved(5);
        inventoryRepository.save(vInv);

        // Attempting to reduce variant stock below 5 reserved should fail
        ProductVariantRequest vUpdate = ProductVariantRequest.builder()
                .sku("SHOE-RUN-10-RED")
                .variantName("Size: 10, Color: Red")
                .priceAdjustment(BigDecimal.valueOf(10.00))
                .stockQuantity(3)
                .build();

        assertThatThrownBy(() -> productService.updateProductVariant(product.getId(), vResp1.getId(), vUpdate))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Cannot reduce available variant stock (3) below currently reserved quantity (5)");
    }
}
