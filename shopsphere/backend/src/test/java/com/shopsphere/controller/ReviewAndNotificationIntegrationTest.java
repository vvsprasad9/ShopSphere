package com.shopsphere.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.shopsphere.dto.request.review.ReviewModerationRequest;
import com.shopsphere.dto.request.review.ReviewRequest;
import com.shopsphere.dto.request.review.UpdateReviewRequest;
import com.shopsphere.entity.*;
import com.shopsphere.entity.enums.*;
import com.shopsphere.repository.*;
import com.shopsphere.security.JwtTokenProvider;
import com.shopsphere.security.UserPrincipal;
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
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class ReviewAndNotificationIntegrationTest {

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
    private ProductRepository productRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private OrderItemRepository orderItemRepository;

    @Autowired
    private ReviewRepository reviewRepository;

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private User customer;
    private User admin;
    private String customerToken;
    private String adminToken;
    private Product testProduct;

    @BeforeEach
    void setUp() {
        Role customerRole = roleRepository.findByName(ERole.ROLE_CUSTOMER)
                .orElseGet(() -> roleRepository.save(new Role(ERole.ROLE_CUSTOMER)));
        Role adminRole = roleRepository.findByName(ERole.ROLE_ADMIN)
                .orElseGet(() -> roleRepository.save(new Role(ERole.ROLE_ADMIN)));

        customer = userRepository.save(User.builder()
                .email("cust.integ@shopsphere.com")
                .password(passwordEncoder.encode("Password123!"))
                .firstName("John")
                .lastName("Doe")
                .phone("555-0100")
                .status(UserStatus.ACTIVE)
                .roles(new HashSet<>(List.of(customerRole)))
                .build());

        admin = userRepository.save(User.builder()
                .email("admin.integ@shopsphere.com")
                .password(passwordEncoder.encode("Password123!"))
                .firstName("Super")
                .lastName("Admin")
                .phone("555-0101")
                .status(UserStatus.ACTIVE)
                .roles(new HashSet<>(List.of(adminRole)))
                .build());

        customerToken = "Bearer " + jwtTokenProvider.generateAccessToken(customer);
        adminToken = "Bearer " + jwtTokenProvider.generateAccessToken(admin);

        Category category = categoryRepository.save(Category.builder()
                .name("Integration Reviews Category")
                .slug("integ-rev-category")
                .active(true)
                .displayOrder(1)
                .build());

        testProduct = productRepository.save(Product.builder()
                .name("Wireless Ergonomic Mouse")
                .slug("wireless-ergo-mouse-integ")
                .sku("SKU-ERGO-MOUSE-1")
                .price(new BigDecimal("1499.00"))
                .category(category)
                .active(true)
                .averageRating(0.0)
                .reviewCount(0)
                .build());
    }

    private void createDeliveredOrder(User user, Product product) {
        AddressSnapshot addressSnapshot = AddressSnapshot.builder()
                .recipientName("Integration Recipient")
                .phoneNumber("9999999999")
                .addressLine1("456 Elm St")
                .city("Metropia")
                .state("MP")
                .postalCode("54321")
                .country("India")
                .build();

        Order order = orderRepository.save(Order.builder()
                .orderNumber("ORD-INT-" + System.nanoTime())
                .user(user)
                .orderStatus(OrderStatus.DELIVERED)
                .subtotal(product.getPrice())
                .totalAmount(product.getPrice())
                .shippingFee(BigDecimal.ZERO)
                .taxAmount(BigDecimal.ZERO)
                .discountAmount(BigDecimal.ZERO)
                .shippingAddress(addressSnapshot)
                .billingAddress(addressSnapshot)
                .build());

        orderItemRepository.save(OrderItem.builder()
                .order(order)
                .product(product)
                .productName(product.getName())
                .productSku(product.getSku())
                .unitPrice(product.getPrice())
                .quantity(1)
                .totalPrice(product.getPrice())
                .build());
    }

    @Test
    @DisplayName("End-to-End Review Lifecycle: Public Read -> Auth Submit -> Conflict Check -> Admin Moderation -> Public View")
    void fullReviewLifecycleTest() throws Exception {
        // 1. Public browsing without token should succeed and return empty reviews
        mockMvc.perform(get("/api/v1/products/{id}/reviews", testProduct.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content", empty()));

        mockMvc.perform(get("/api/v1/products/{id}/reviews/summary", testProduct.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.averageRating", is(0.0)))
                .andExpect(jsonPath("$.data.totalReviews", is(0)));

        // 2. Unauthenticated review submission must be rejected with 401
        ReviewRequest reviewRequest = ReviewRequest.builder()
                .productId(testProduct.getId())
                .rating(5)
                .title("Ergonomic Excellence")
                .comment("Extremely comfortable for all day work!")
                .build();

        mockMvc.perform(post("/api/v1/products/{id}/reviews", testProduct.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(reviewRequest)))
                .andExpect(status().isUnauthorized());

        // 3. Customer submitting review without having purchased product must fail with 400
        mockMvc.perform(post("/api/v1/products/{id}/reviews", testProduct.getId())
                        .header("Authorization", customerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(reviewRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("confirmed or completed orders")));

        // 4. Create a delivered order for the customer
        createDeliveredOrder(customer, testProduct);

        // 5. Customer submits review -> 201 Created with Verified Purchase badge and PENDING status
        MvcResult submitResult = mockMvc.perform(post("/api/v1/products/{id}/reviews", testProduct.getId())
                        .header("Authorization", customerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(reviewRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.isVerifiedPurchase", is(true)))
                .andExpect(jsonPath("$.data.status", is("PENDING")))
                .andExpect(jsonPath("$.data.rating", is(5)))
                .andReturn();

        String responseBody = submitResult.getResponse().getContentAsString();
        Long reviewId = objectMapper.readTree(responseBody).path("data").path("id").asLong();

        // 6. Submitting duplicate review fails with 409 Conflict
        mockMvc.perform(post("/api/v1/products/{id}/reviews", testProduct.getId())
                        .header("Authorization", customerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(reviewRequest)))
                .andExpect(status().isConflict());

        // 7. Public reviews still empty because status is PENDING
        mockMvc.perform(get("/api/v1/products/{id}/reviews", testProduct.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content", empty()));

        // 8. Non-admin attempting moderation gets 403 Forbidden
        ReviewModerationRequest modRequest = ReviewModerationRequest.builder()
                .status(ReviewStatus.APPROVED)
                .reason("Legitimate verified review")
                .build();

        mockMvc.perform(patch("/api/v1/reviews/admin/{reviewId}/moderation", reviewId)
                        .header("Authorization", customerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(modRequest)))
                .andExpect(status().isForbidden());

        // 9. Admin moderates review to APPROVED -> 200 OK
        mockMvc.perform(patch("/api/v1/reviews/admin/{reviewId}/moderation", reviewId)
                        .header("Authorization", adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(modRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status", is("APPROVED")));

        // 10. Public reviews now show the approved review and rating summary is updated
        mockMvc.perform(get("/api/v1/products/{id}/reviews", testProduct.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content", hasSize(1)))
                .andExpect(jsonPath("$.data.content[0].rating", is(5)))
                .andExpect(jsonPath("$.data.content[0].isVerifiedPurchase", is(true)));

        mockMvc.perform(get("/api/v1/products/{id}/reviews/summary", testProduct.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.averageRating", is(5.0)))
                .andExpect(jsonPath("$.data.totalReviews", is(1)));

        // 11. Customer edits their review -> resets status to PENDING
        UpdateReviewRequest updateRequest = UpdateReviewRequest.builder()
                .rating(3)
                .title("Good but wheel squeaks")
                .comment("Wheel started squeaking after several days.")
                .build();

        mockMvc.perform(put("/api/v1/reviews/{reviewId}", reviewId)
                        .header("Authorization", customerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status", is("PENDING")))
                .andExpect(jsonPath("$.data.rating", is(3)));

        // 12. Public reviews are empty again until re-moderation
        mockMvc.perform(get("/api/v1/products/{id}/reviews", testProduct.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content", empty()));
    }

    @Test
    @DisplayName("End-to-End Notification Flow: Query -> Unread Count -> Mark Read -> Mark All Read -> Delete")
    void fullNotificationFlowTest() throws Exception {
        // Setup initial notifications
        Notification n1 = notificationRepository.save(Notification.builder()
                .user(customer)
                .title("Order Confirmed")
                .message("Your order has been confirmed.")
                .type(NotificationType.ORDER_CONFIRMED)
                .isRead(false)
                .build());

        Notification n2 = notificationRepository.save(Notification.builder()
                .user(customer)
                .title("Order Shipped")
                .message("Your order is on the way.")
                .type(NotificationType.ORDER_SHIPPED)
                .isRead(false)
                .build());

        // 1. Get user notifications
        mockMvc.perform(get("/api/v1/notifications")
                        .header("Authorization", customerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content", hasSize(2)));

        // 2. Get unread count
        mockMvc.perform(get("/api/v1/notifications/unread-count")
                        .header("Authorization", customerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.unreadCount", is(2)));

        // 3. Mark single notification as read
        mockMvc.perform(patch("/api/v1/notifications/{id}/read", n1.getId())
                        .header("Authorization", customerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.isRead", is(true)));

        // 4. Verify unread count is now 1
        mockMvc.perform(get("/api/v1/notifications/unread-count")
                        .header("Authorization", customerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.unreadCount", is(1)));

        // 5. Mark all as read
        mockMvc.perform(patch("/api/v1/notifications/read-all")
                        .header("Authorization", customerToken))
                .andExpect(status().isOk());

        // 6. Verify unread count is now 0
        mockMvc.perform(get("/api/v1/notifications/unread-count")
                        .header("Authorization", customerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.unreadCount", is(0)));

        // 7. Delete notification
        mockMvc.perform(delete("/api/v1/notifications/{id}", n2.getId())
                        .header("Authorization", customerToken))
                .andExpect(status().isOk());

        // 8. Verify only 1 notification remains
        mockMvc.perform(get("/api/v1/notifications")
                        .header("Authorization", customerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content", hasSize(1)));
    }
}
