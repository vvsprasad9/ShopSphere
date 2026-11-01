package com.shopsphere.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.shopsphere.dto.request.cart.AddToCartRequest;
import com.shopsphere.dto.request.cart.UpdateCartItemRequest;
import com.shopsphere.dto.request.product.ProductCreateRequest;
import com.shopsphere.dto.request.user.AddressRequest;
import com.shopsphere.dto.request.user.ChangePasswordRequest;
import com.shopsphere.dto.request.user.UpdateProfileRequest;
import com.shopsphere.dto.response.product.ProductResponse;
import com.shopsphere.entity.Category;
import com.shopsphere.entity.Role;
import com.shopsphere.entity.User;
import com.shopsphere.entity.enums.AddressType;
import com.shopsphere.entity.enums.ERole;
import com.shopsphere.entity.enums.UserStatus;
import com.shopsphere.repository.CategoryRepository;
import com.shopsphere.repository.RoleRepository;
import com.shopsphere.repository.UserRepository;
import com.shopsphere.security.JwtTokenProvider;
import com.shopsphere.service.AddressService;
import com.shopsphere.service.CartService;
import com.shopsphere.service.ProductService;
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
import java.util.Set;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class CustomerModulesIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Autowired
    private ProductService productService;

    @Autowired
    private AddressService addressService;

    @Autowired
    private CartService cartService;

    private User alice;
    private User bob;
    private String aliceToken;
    private String bobToken;

    private ProductResponse testProduct;

    @BeforeEach
    void setUp() {
        Role customerRole = roleRepository.findByName(ERole.ROLE_CUSTOMER)
                .orElseGet(() -> roleRepository.save(new Role(ERole.ROLE_CUSTOMER)));

        alice = userRepository.save(User.builder()
                .email("alice.test@shopsphere.com")
                .password(passwordEncoder.encode("Alice#12345"))
                .firstName("Alice")
                .lastName("Wonderland")
                .status(UserStatus.ACTIVE)
                .roles(new java.util.HashSet<>(java.util.List.of(customerRole)))
                .build());

        bob = userRepository.save(User.builder()
                .email("bob.test@shopsphere.com")
                .password(passwordEncoder.encode("Bob#12345"))
                .firstName("Bob")
                .lastName("Builder")
                .status(UserStatus.ACTIVE)
                .roles(new java.util.HashSet<>(java.util.List.of(customerRole)))
                .build());

        aliceToken = jwtTokenProvider.generateAccessToken(alice);
        bobToken = jwtTokenProvider.generateAccessToken(bob);

        Category category = categoryRepository.save(Category.builder()
                .name("Integration Test Category")
                .slug("integration-test-category")
                .active(true)
                .build());

        testProduct = productService.createProduct(ProductCreateRequest.builder()
                .name("4K Ultra Monitor")
                .sku("MON-4K-ULTRA")
                .price(BigDecimal.valueOf(499.99))
                .categoryId(category.getId())
                .initialStockQuantity(15)
                .build());
    }

    // --- SECURITY & UNAUTHENTICATED ACCESS ---

    @Test
    @DisplayName("1. Unauthenticated requests to customer endpoints return 401 Unauthorized")
    void testUnauthenticatedCustomerEndpoints_Returns401() throws Exception {
        mockMvc.perform(get("/api/v1/users/profile")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/v1/addresses")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/v1/cart")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/v1/wishlist")).andExpect(status().isUnauthorized());
    }

    // --- MODULE 1: PROFILE HTTP TESTS ---

    @Test
    @DisplayName("2. Customer profile retrieval and update via HTTP")
    void testProfileHttpFlow() throws Exception {
        // GET profile
        mockMvc.perform(get("/api/v1/users/profile")
                        .header("Authorization", "Bearer " + aliceToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.email").value("alice.test@shopsphere.com"))
                .andExpect(jsonPath("$.data.firstName").value("Alice"));

        // PUT profile
        UpdateProfileRequest updateReq = UpdateProfileRequest.builder()
                .firstName("AliceUpdated")
                .lastName("WonderlandUpdated")
                .phone("555-444-3333")
                .build();

        mockMvc.perform(put("/api/v1/users/profile")
                        .header("Authorization", "Bearer " + aliceToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.firstName").value("AliceUpdated"))
                .andExpect(jsonPath("$.data.phone").value("555-444-3333"));
    }

    @Test
    @DisplayName("3. Change password with correct and incorrect credentials via HTTP")
    void testChangePasswordHttp() throws Exception {
        // Wrong current password -> 400
        ChangePasswordRequest wrongReq = ChangePasswordRequest.builder()
                .currentPassword("WrongPassword#1")
                .newPassword("NewPass#12345")
                .build();

        mockMvc.perform(put("/api/v1/users/change-password")
                        .header("Authorization", "Bearer " + aliceToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(wrongReq)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Current password does not match"));

        // Correct password -> 200
        ChangePasswordRequest correctReq = ChangePasswordRequest.builder()
                .currentPassword("Alice#12345")
                .newPassword("NewAlice#12345")
                .build();

        mockMvc.perform(put("/api/v1/users/change-password")
                        .header("Authorization", "Bearer " + aliceToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(correctReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    // --- MODULE 2: ADDRESS HTTP & IDOR TESTS ---

    @Test
    @DisplayName("4. Customer address creation, retrieval, and IDOR isolation")
    void testAddressHttpAndIdor() throws Exception {
        AddressRequest req = AddressRequest.builder()
                .fullName("Alice Address")
                .phoneNumber("123-456-7890")
                .addressLine1("123 Fantasy Way")
                .city("Wonderland")
                .state("CA")
                .postalCode("90210")
                .country("USA")
                .addressType(AddressType.HOME)
                .build();

        // Alice creates address
        String responseContent = mockMvc.perform(post("/api/v1/addresses")
                        .header("Authorization", "Bearer " + aliceToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.isDefault").value(true))
                .andReturn().getResponse().getContentAsString();

        Long aliceAddressId = objectMapper.readTree(responseContent).path("data").path("id").asLong();

        // Alice can view her address
        mockMvc.perform(get("/api/v1/addresses/{id}", aliceAddressId)
                        .header("Authorization", "Bearer " + aliceToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.fullName").value("Alice Address"));

        // Bob attempting to view Alice's address returns 404 (IDOR Protection)
        mockMvc.perform(get("/api/v1/addresses/{id}", aliceAddressId)
                        .header("Authorization", "Bearer " + bobToken))
                .andExpect(status().isNotFound());

        // Bob attempting to delete Alice's address returns 404 (IDOR Protection)
        mockMvc.perform(delete("/api/v1/addresses/{id}", aliceAddressId)
                        .header("Authorization", "Bearer " + bobToken))
                .andExpect(status().isNotFound());
    }

    // --- MODULE 3: CART HTTP & IDOR TESTS ---

    @Test
    @DisplayName("5. Customer cart operations and customer-to-customer data isolation")
    void testCartHttpAndDataIsolation() throws Exception {
        AddToCartRequest addReq = AddToCartRequest.builder()
                .productId(testProduct.getId())
                .quantity(2)
                .build();

        // Alice adds item to her cart
        String addResponse = mockMvc.perform(post("/api/v1/cart/items")
                        .header("Authorization", "Bearer " + aliceToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(addReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalItems").value(2))
                .andReturn().getResponse().getContentAsString();

        Long aliceCartItemId = objectMapper.readTree(addResponse).path("data").path("items").get(0).path("id").asLong();

        // Bob views his cart -> should be empty (Data Isolation)
        mockMvc.perform(get("/api/v1/cart")
                        .header("Authorization", "Bearer " + bobToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalItems").value(0))
                .andExpect(jsonPath("$.data.items", hasSize(0)));

        // Bob attempting to update Alice's cart item returns 404 (IDOR Protection)
        UpdateCartItemRequest updateReq = UpdateCartItemRequest.builder().quantity(5).build();
        mockMvc.perform(put("/api/v1/cart/items/{itemId}", aliceCartItemId)
                        .header("Authorization", "Bearer " + bobToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateReq)))
                .andExpect(status().isNotFound());

        // Bob attempting to delete Alice's cart item returns 404 (IDOR Protection)
        mockMvc.perform(delete("/api/v1/cart/items/{itemId}", aliceCartItemId)
                        .header("Authorization", "Bearer " + bobToken))
                .andExpect(status().isNotFound());

        // Alice updates quantity -> 200 OK
        mockMvc.perform(put("/api/v1/cart/items/{itemId}", aliceCartItemId)
                        .header("Authorization", "Bearer " + aliceToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalItems").value(5));

        // Alice clears cart -> 200 OK
        mockMvc.perform(delete("/api/v1/cart")
                        .header("Authorization", "Bearer " + aliceToken))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/cart")
                        .header("Authorization", "Bearer " + aliceToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalItems").value(0));
    }

    // --- MODULE 4: WISHLIST HTTP TESTS ---

    @Test
    @DisplayName("6. Customer wishlist operations and check endpoint via HTTP")
    void testWishlistHttpFlow() throws Exception {
        // Alice adds product to wishlist
        mockMvc.perform(post("/api/v1/wishlist/products/{productId}", testProduct.getId())
                        .header("Authorization", "Bearer " + aliceToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalItems").value(1));

        // Check isWishlisted
        mockMvc.perform(get("/api/v1/wishlist/products/{productId}/check", testProduct.getId())
                        .header("Authorization", "Bearer " + aliceToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value(true));

        // Bob checking same product returns false (Data Isolation)
        mockMvc.perform(get("/api/v1/wishlist/products/{productId}/check", testProduct.getId())
                        .header("Authorization", "Bearer " + bobToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value(false));

        // Alice removes product from wishlist
        mockMvc.perform(delete("/api/v1/wishlist/products/{productId}", testProduct.getId())
                        .header("Authorization", "Bearer " + aliceToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalItems").value(0));
    }
}
