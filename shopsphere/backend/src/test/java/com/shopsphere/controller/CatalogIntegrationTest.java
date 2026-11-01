package com.shopsphere.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.shopsphere.dto.request.category.CategoryRequest;
import com.shopsphere.dto.request.product.ProductCreateRequest;
import com.shopsphere.dto.request.product.ProductImageRequest;
import com.shopsphere.dto.request.product.ProductVariantRequest;
import com.shopsphere.entity.*;
import com.shopsphere.entity.enums.ERole;
import com.shopsphere.entity.enums.UserStatus;
import com.shopsphere.repository.*;
import com.shopsphere.security.JwtTokenProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class CatalogIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private InventoryRepository inventoryRepository;

    private String customerToken;
    private String adminToken;
    private String managerToken;

    private Category parentCategory;
    private Category childCategory;
    private Product product1;
    private Product product2;
    private Product outOfStockProduct;

    @BeforeEach
    void setUp() {
        Role customerRole = roleRepository.findByName(ERole.ROLE_CUSTOMER)
                .orElseGet(() -> roleRepository.save(new Role(ERole.ROLE_CUSTOMER)));
        Role adminRole = roleRepository.findByName(ERole.ROLE_ADMIN)
                .orElseGet(() -> roleRepository.save(new Role(ERole.ROLE_ADMIN)));
        Role managerRole = roleRepository.findByName(ERole.ROLE_MANAGER)
                .orElseGet(() -> roleRepository.save(new Role(ERole.ROLE_MANAGER)));

        User customer = userRepository.save(User.builder()
                .email("catalog.customer@shopsphere.com")
                .password(passwordEncoder.encode("Pass#123"))
                .firstName("Customer")
                .lastName("User")
                .status(UserStatus.ACTIVE)
                .roles(Set.of(customerRole))
                .build());

        User admin = userRepository.save(User.builder()
                .email("catalog.admin@shopsphere.com")
                .password(passwordEncoder.encode("Admin#123"))
                .firstName("Admin")
                .lastName("User")
                .status(UserStatus.ACTIVE)
                .roles(Set.of(adminRole))
                .build());

        User manager = userRepository.save(User.builder()
                .email("catalog.manager@shopsphere.com")
                .password(passwordEncoder.encode("Manager#123"))
                .firstName("Manager")
                .lastName("User")
                .status(UserStatus.ACTIVE)
                .roles(Set.of(managerRole))
                .build());

        customerToken = jwtTokenProvider.generateAccessToken(customer);
        adminToken = jwtTokenProvider.generateAccessToken(admin);
        managerToken = jwtTokenProvider.generateAccessToken(manager);

        // Seed categories
        parentCategory = categoryRepository.save(Category.builder()
                .name("Electronics")
                .slug("electronics")
                .active(true)
                .displayOrder(1)
                .build());

        childCategory = categoryRepository.save(Category.builder()
                .name("Laptops")
                .slug("laptops")
                .parentCategory(parentCategory)
                .active(true)
                .displayOrder(1)
                .build());

        parentCategory.getSubCategories().add(childCategory);

        // Seed products
        product1 = productRepository.save(Product.builder()
                .name("MacBook Pro 16 M3 Max")
                .slug("macbook-pro-16-m3-max")
                .sku("LAP-MBP-16-M3")
                .description("Supercharged laptop with M3 Max chip")
                .brand("Apple")
                .price(BigDecimal.valueOf(2499.00))
                .discountPrice(BigDecimal.valueOf(2299.00))
                .category(childCategory)
                .active(true)
                .featured(true)
                .averageRating(4.9)
                .reviewCount(15)
                .build());

        Inventory inv1 = inventoryRepository.save(Inventory.builder()
                .product(product1)
                .quantityAvailable(25)
                .quantityReserved(0)
                .lowStockThreshold(5)
                .build());
        product1.setInventory(inv1);
        productRepository.save(product1);

        product2 = productRepository.save(Product.builder()
                .name("Dell XPS 15 OLED")
                .slug("dell-xps-15-oled")
                .sku("LAP-DELL-XPS-15")
                .description("InfinityEdge high-performance laptop")
                .brand("Dell")
                .price(BigDecimal.valueOf(1799.00))
                .category(childCategory)
                .active(true)
                .featured(false)
                .averageRating(4.5)
                .reviewCount(8)
                .build());

        Inventory inv2 = inventoryRepository.save(Inventory.builder()
                .product(product2)
                .quantityAvailable(10)
                .quantityReserved(0)
                .lowStockThreshold(2)
                .build());
        product2.setInventory(inv2);
        productRepository.save(product2);

        outOfStockProduct = productRepository.save(Product.builder()
                .name("Lenovo ThinkPad X1 Carbon")
                .slug("lenovo-thinkpad-x1-carbon")
                .sku("LAP-LEN-X1")
                .description("Ultralight business laptop")
                .brand("Lenovo")
                .price(BigDecimal.valueOf(1499.00))
                .category(childCategory)
                .active(true)
                .build());

        Inventory inv3 = inventoryRepository.save(Inventory.builder()
                .product(outOfStockProduct)
                .quantityAvailable(0)
                .quantityReserved(0)
                .lowStockThreshold(2)
                .build());
        outOfStockProduct.setInventory(inv3);
        productRepository.save(outOfStockProduct);
    }

    @Test
    @DisplayName("1. Public category tree browsing without authentication")
    void testPublicCategoryTree() throws Exception {
        mockMvc.perform(get("/api/v1/categories/tree"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data", hasSize(greaterThanOrEqualTo(1))))
                .andExpect(jsonPath("$.data[?(@.slug == 'electronics')].subCategories[0].slug").value("laptops"));
    }

    @Test
    @DisplayName("2. Public product search and keyword filtering")
    void testPublicProductSearchKeyword() throws Exception {
        mockMvc.perform(get("/api/v1/products")
                        .param("keyword", "MacBook"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content", hasSize(1)))
                .andExpect(jsonPath("$.data.content[0].name").value("MacBook Pro 16 M3 Max"));
    }

    @Test
    @DisplayName("3. Price range filtering")
    void testPriceRangeFiltering() throws Exception {
        // Effective price for MacBook is 2299 (discount), Dell is 1799
        mockMvc.perform(get("/api/v1/products")
                        .param("minPrice", "1500.00")
                        .param("maxPrice", "2000.00"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content", hasSize(1)))
                .andExpect(jsonPath("$.data.content[0].name").value("Dell XPS 15 OLED"));
    }

    @Test
    @DisplayName("4. In-stock only filter excludes zero purchasable stock")
    void testInStockOnlyFiltering() throws Exception {
        mockMvc.perform(get("/api/v1/products")
                        .param("inStockOnly", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[*].sku", not(hasItem("LAP-LEN-X1"))))
                .andExpect(jsonPath("$.data.content[*].sku", hasItems("LAP-MBP-16-M3", "LAP-DELL-XPS-15")));
    }

    @Test
    @DisplayName("5. Category descendant filtering - querying parent category matches child products")
    void testCategoryDescendantFiltering() throws Exception {
        // Filtering by parentCategory "electronics" should include products in childCategory "laptops"
        mockMvc.perform(get("/api/v1/products")
                        .param("categoryId", parentCategory.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content", hasSize(3)));
    }

    @Test
    @DisplayName("6. Sorting by price ascending and descending")
    void testSortingByPrice() throws Exception {
        mockMvc.perform(get("/api/v1/products")
                        .param("sortBy", "price_asc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].price").value(1499.00));

        mockMvc.perform(get("/api/v1/products")
                        .param("sortBy", "price_desc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].price").value(2499.00));
    }

    @Test
    @DisplayName("7. Public product details by ID and by Slug")
    void testProductDetailsByIdAndSlug() throws Exception {
        mockMvc.perform(get("/api/v1/products/{id}", product1.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("MacBook Pro 16 M3 Max"))
                .andExpect(jsonPath("$.data.stockQuantity").value(25))
                .andExpect(jsonPath("$.data.inStock").value(true));

        mockMvc.perform(get("/api/v1/products/slug/{slug}", product1.getSlug()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.sku").value("LAP-MBP-16-M3"));
    }

    @Test
    @DisplayName("8. Unauthenticated requests to administrative endpoints return 401")
    void testUnauthenticatedAdminEndpoints_Returns401() throws Exception {
        CategoryRequest catReq = CategoryRequest.builder().name("Test Cat").build();
        mockMvc.perform(post("/api/v1/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(catReq)))
                .andExpect(status().isUnauthorized());

        ProductCreateRequest prodReq = ProductCreateRequest.builder()
                .name("Test Prod")
                .sku("SKU-TEST")
                .price(BigDecimal.TEN)
                .categoryId(childCategory.getId())
                .build();
        mockMvc.perform(post("/api/v1/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(prodReq)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("9. Customer role accessing Admin/Manager endpoints returns 403 Forbidden")
    void testCustomerRole_Returns403() throws Exception {
        CategoryRequest catReq = CategoryRequest.builder().name("Unauthorized Cat").build();
        mockMvc.perform(post("/api/v1/categories")
                        .header("Authorization", "Bearer " + customerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(catReq)))
                .andExpect(status().isForbidden());

        ProductCreateRequest prodReq = ProductCreateRequest.builder()
                .name("Unauthorized Prod")
                .sku("SKU-UNAUTH")
                .price(BigDecimal.TEN)
                .categoryId(childCategory.getId())
                .build();
        mockMvc.perform(post("/api/v1/products")
                        .header("Authorization", "Bearer " + customerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(prodReq)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("10. Admin role creates category successfully (201 Created)")
    void testAdminCreateCategory() throws Exception {
        CategoryRequest catReq = CategoryRequest.builder()
                .name("Monitors & Displays")
                .parentCategoryId(parentCategory.getId())
                .displayOrder(3)
                .build();

        mockMvc.perform(post("/api/v1/categories")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(catReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.slug").value("monitors-displays"))
                .andExpect(jsonPath("$.data.parentCategoryId").value(parentCategory.getId()));
    }

    @Test
    @DisplayName("11. Manager or Admin creates product with authoritative inventory (201 Created)")
    void testManagerCreateProduct() throws Exception {
        ProductCreateRequest createReq = ProductCreateRequest.builder()
                .name("Sony WH-1000XM5")
                .sku("AUDIO-SONY-XM5")
                .price(BigDecimal.valueOf(399.99))
                .discountPrice(BigDecimal.valueOf(349.99))
                .brand("Sony")
                .categoryId(parentCategory.getId())
                .initialStockQuantity(40)
                .lowStockThreshold(5)
                .imageUrls(List.of("https://example.com/xm5.jpg"))
                .build();

        mockMvc.perform(post("/api/v1/products")
                        .header("Authorization", "Bearer " + managerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.sku").value("AUDIO-SONY-XM5"))
                .andExpect(jsonPath("$.data.stockQuantity").value(40))
                .andExpect(jsonPath("$.data.images", hasSize(1)))
                .andExpect(jsonPath("$.data.images[0].primary").value(true));
    }

    @Test
    @DisplayName("12. Admin/Manager variant and image operations")
    void testVariantAndImageEndpoints() throws Exception {
        // Add variant
        ProductVariantRequest vReq = ProductVariantRequest.builder()
                .sku("LAP-MBP-16-64GB")
                .variantName("64GB Unified Memory")
                .priceAdjustment(BigDecimal.valueOf(400.00))
                .stockQuantity(8)
                .build();

        mockMvc.perform(post("/api/v1/products/{id}/variants", product1.getId())
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(vReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.sku").value("LAP-MBP-16-64GB"))
                .andExpect(jsonPath("$.data.stockQuantity").value(8));

        // Add image
        ProductImageRequest imgReq = ProductImageRequest.builder()
                .imageUrl("https://example.com/macbook-side.jpg")
                .isPrimary(false)
                .build();

        mockMvc.perform(post("/api/v1/products/{id}/images", product1.getId())
                        .header("Authorization", "Bearer " + managerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(imgReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.imageUrl").value("https://example.com/macbook-side.jpg"));
    }
}
