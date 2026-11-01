package com.shopsphere.service;

import com.shopsphere.dto.request.review.ReviewModerationRequest;
import com.shopsphere.dto.request.review.ReviewRequest;
import com.shopsphere.dto.request.review.UpdateReviewRequest;
import com.shopsphere.dto.response.PageResponse;
import com.shopsphere.dto.response.review.ProductRatingSummaryResponse;
import com.shopsphere.dto.response.review.ReviewResponse;
import com.shopsphere.entity.*;
import com.shopsphere.entity.enums.*;
import com.shopsphere.exception.BadRequestException;
import com.shopsphere.exception.ConflictException;
import com.shopsphere.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class ReviewServiceTest {

    @Autowired
    private ReviewService reviewService;

    @Autowired
    private ReviewRepository reviewRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private OrderItemRepository orderItemRepository;

    @Autowired
    private AuditLogRepository auditLogRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private User customerAlice;
    private User customerBob;
    private User adminUser;
    private Product product1;
    private Product product2;

    @BeforeEach
    void setUp() {
        Role customerRole = roleRepository.findByName(ERole.ROLE_CUSTOMER)
                .orElseGet(() -> roleRepository.save(new Role(ERole.ROLE_CUSTOMER)));
        Role adminRole = roleRepository.findByName(ERole.ROLE_ADMIN)
                .orElseGet(() -> roleRepository.save(new Role(ERole.ROLE_ADMIN)));

        customerAlice = userRepository.save(User.builder()
                .email("alice.review@shopsphere.com")
                .password(passwordEncoder.encode("Password123!"))
                .firstName("Alice")
                .lastName("Walker")
                .phone("111-222-3333")
                .status(UserStatus.ACTIVE)
                .roles(new HashSet<>(List.of(customerRole)))
                .build());

        customerBob = userRepository.save(User.builder()
                .email("bob.review@shopsphere.com")
                .password(passwordEncoder.encode("Password123!"))
                .firstName("Bob")
                .lastName("Miller")
                .phone("444-555-6666")
                .status(UserStatus.ACTIVE)
                .roles(new HashSet<>(List.of(customerRole)))
                .build());

        adminUser = userRepository.save(User.builder()
                .email("admin.review@shopsphere.com")
                .password(passwordEncoder.encode("Password123!"))
                .firstName("Admin")
                .lastName("Manager")
                .phone("777-888-9999")
                .status(UserStatus.ACTIVE)
                .roles(new HashSet<>(List.of(adminRole)))
                .build());

        Category category = categoryRepository.save(Category.builder()
                .name("Electronics Review Test")
                .slug("electronics-review-test")
                .active(true)
                .displayOrder(1)
                .build());

        product1 = productRepository.save(Product.builder()
                .name("Wireless Noise Cancelling Earbuds")
                .slug("wireless-earbuds-rev-1")
                .sku("SKU-EARBUDS-REV-1")
                .price(new BigDecimal("2999.00"))
                .category(category)
                .active(true)
                .averageRating(0.0)
                .reviewCount(0)
                .build());

        product2 = productRepository.save(Product.builder()
                .name("Mechanical Gaming Keyboard")
                .slug("mech-keyboard-rev-2")
                .sku("SKU-KEYBOARD-REV-2")
                .price(new BigDecimal("4999.00"))
                .category(category)
                .active(true)
                .averageRating(0.0)
                .reviewCount(0)
                .build());
    }

    private Order createTestOrder(User user, Product product, OrderStatus status) {
        AddressSnapshot addressSnapshot = AddressSnapshot.builder()
                .recipientName("Test Recipient")
                .phoneNumber("9999999999")
                .addressLine1("123 Test St")
                .city("Test City")
                .state("TS")
                .postalCode("123456")
                .country("India")
                .build();

        Order order = orderRepository.save(Order.builder()
                .orderNumber("ORD-REV-" + System.nanoTime())
                .user(user)
                .orderStatus(status)
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

        return order;
    }

    @Test
    @DisplayName("Create Review - Success on DELIVERED order grants Verified Purchase badge and PENDING status")
    void createReview_DeliveredOrder_VerifiedBadgeTrue() {
        createTestOrder(customerAlice, product1, OrderStatus.DELIVERED);

        ReviewRequest request = ReviewRequest.builder()
                .productId(product1.getId())
                .rating(5)
                .title("Fantastic Sound")
                .comment("Incredible clarity and battery life!")
                .build();

        ReviewResponse response = reviewService.createReview(customerAlice.getId(), product1.getId(), request);

        assertThat(response).isNotNull();
        assertThat(response.getRating()).isEqualTo(5);
        assertThat(response.getTitle()).isEqualTo("Fantastic Sound");
        assertThat(response.isVerifiedPurchase()).isTrue();
        assertThat(response.getStatus()).isEqualTo("PENDING");
        assertThat(response.getReviewerName()).isEqualTo("Alice W.");

        // Product rating shouldn't include PENDING review
        Product updatedProduct = productRepository.findById(product1.getId()).orElseThrow();
        assertThat(updatedProduct.getReviewCount()).isEqualTo(0);
        assertThat(updatedProduct.getAverageRating()).isEqualTo(0.0);
    }

    @Test
    @DisplayName("Create Review - Success on CONFIRMED order has no Verified Purchase badge")
    void createReview_ConfirmedOrder_VerifiedBadgeFalse() {
        createTestOrder(customerBob, product1, OrderStatus.CONFIRMED);

        ReviewRequest request = ReviewRequest.builder()
                .productId(product1.getId())
                .rating(4)
                .title("Looking forward to delivery")
                .comment("Order confirmed, excited to receive it.")
                .build();

        ReviewResponse response = reviewService.createReview(customerBob.getId(), product1.getId(), request);

        assertThat(response).isNotNull();
        assertThat(response.getRating()).isEqualTo(4);
        assertThat(response.isVerifiedPurchase()).isFalse(); // Not DELIVERED yet!
        assertThat(response.getStatus()).isEqualTo("PENDING");
    }

    @Test
    @DisplayName("Create Review - Throws BadRequestException when customer has not purchased product")
    void createReview_NoEligibleOrder_ThrowsBadRequestException() {
        ReviewRequest request = ReviewRequest.builder()
                .productId(product2.getId())
                .rating(5)
                .title("Never bought this")
                .comment("Just commenting without purchasing.")
                .build();

        assertThatThrownBy(() -> reviewService.createReview(customerAlice.getId(), product2.getId(), request))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("confirmed or completed orders");
    }

    @Test
    @DisplayName("Create Review - Throws ConflictException when customer attempts duplicate review")
    void createReview_DuplicateReview_ThrowsConflictException() {
        createTestOrder(customerAlice, product1, OrderStatus.DELIVERED);

        ReviewRequest request1 = ReviewRequest.builder()
                .productId(product1.getId())
                .rating(5)
                .title("First Review")
                .comment("Great product!")
                .build();

        reviewService.createReview(customerAlice.getId(), product1.getId(), request1);

        ReviewRequest request2 = ReviewRequest.builder()
                .productId(product1.getId())
                .rating(4)
                .title("Duplicate Review Attempt")
                .comment("Trying to review again.")
                .build();

        assertThatThrownBy(() -> reviewService.createReview(customerAlice.getId(), product1.getId(), request2))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("already reviewed");
    }

    @Test
    @DisplayName("Admin Moderation - Approving reviews recalibrates product rating and records AuditLog")
    void moderateReview_Approve_RecalibratesRating() {
        createTestOrder(customerAlice, product1, OrderStatus.DELIVERED);
        createTestOrder(customerBob, product1, OrderStatus.DELIVERED);

        ReviewResponse rev1 = reviewService.createReview(customerAlice.getId(), product1.getId(), ReviewRequest.builder()
                .productId(product1.getId()).rating(5).title("Best!").comment("Loved it!").build());

        ReviewResponse rev2 = reviewService.createReview(customerBob.getId(), product1.getId(), ReviewRequest.builder()
                .productId(product1.getId()).rating(3).title("Average").comment("It is okay.").build());

        // Moderate both to APPROVED
        reviewService.moderateReview(adminUser.getId(), adminUser.getEmail(), rev1.getId(),
                ReviewModerationRequest.builder().status(ReviewStatus.APPROVED).reason("Quality review").build());

        reviewService.moderateReview(adminUser.getId(), adminUser.getEmail(), rev2.getId(),
                ReviewModerationRequest.builder().status(ReviewStatus.APPROVED).reason("Valid review").build());

        Product product = productRepository.findById(product1.getId()).orElseThrow();
        assertThat(product.getReviewCount()).isEqualTo(2);
        // Average of 5 and 3 is 4.0
        assertThat(product.getAverageRating()).isEqualTo(4.0);

        // Verify audit logs
        long auditCount = auditLogRepository.findAll().stream()
                .filter(a -> "REVIEW_MODERATION".equals(a.getAction()))
                .count();
        assertThat(auditCount).isGreaterThanOrEqualTo(2);
    }

    @Test
    @DisplayName("Product Rating Summary - Accurately computes star distribution and percentages")
    void getProductRatingSummary_ComputesDistribution() {
        createTestOrder(customerAlice, product1, OrderStatus.DELIVERED);
        createTestOrder(customerBob, product1, OrderStatus.DELIVERED);

        ReviewResponse rev1 = reviewService.createReview(customerAlice.getId(), product1.getId(), ReviewRequest.builder()
                .productId(product1.getId()).rating(5).title("A+").comment("Top tier.").build());

        ReviewResponse rev2 = reviewService.createReview(customerBob.getId(), product1.getId(), ReviewRequest.builder()
                .productId(product1.getId()).rating(4).title("Good").comment("Good sound.").build());

        reviewService.moderateReview(adminUser.getId(), adminUser.getEmail(), rev1.getId(),
                ReviewModerationRequest.builder().status(ReviewStatus.APPROVED).build());
        reviewService.moderateReview(adminUser.getId(), adminUser.getEmail(), rev2.getId(),
                ReviewModerationRequest.builder().status(ReviewStatus.APPROVED).build());

        ProductRatingSummaryResponse summary = reviewService.getProductRatingSummary(product1.getId());
        assertThat(summary).isNotNull();
        assertThat(summary.getTotalReviews()).isEqualTo(2);
        assertThat(summary.getAverageRating()).isEqualTo(4.5);
        assertThat(summary.getRatingDistribution().get(5)).isEqualTo(1L);
        assertThat(summary.getRatingDistribution().get(4)).isEqualTo(1L);
        assertThat(summary.getRatingDistribution().get(3)).isEqualTo(0L);
        assertThat(summary.getRatingPercentages().get(5)).isEqualTo(50.0);
        assertThat(summary.getRatingPercentages().get(4)).isEqualTo(50.0);
    }

    @Test
    @DisplayName("Update Review - Editing approved review resets status to PENDING and updates rating")
    void updateReview_ResetsToPending() {
        createTestOrder(customerAlice, product1, OrderStatus.DELIVERED);

        ReviewResponse created = reviewService.createReview(customerAlice.getId(), product1.getId(), ReviewRequest.builder()
                .productId(product1.getId()).rating(5).title("Super").comment("Loved it").build());

        reviewService.moderateReview(adminUser.getId(), adminUser.getEmail(), created.getId(),
                ReviewModerationRequest.builder().status(ReviewStatus.APPROVED).build());

        Product prodBefore = productRepository.findById(product1.getId()).orElseThrow();
        assertThat(prodBefore.getReviewCount()).isEqualTo(1);
        assertThat(prodBefore.getAverageRating()).isEqualTo(5.0);

        // Alice updates her review
        UpdateReviewRequest updateReq = UpdateReviewRequest.builder()
                .rating(2)
                .title("Defective after 1 week")
                .comment("Right earbud stopped charging.")
                .build();

        ReviewResponse updated = reviewService.updateReview(customerAlice.getId(), created.getId(), updateReq);

        assertThat(updated.getStatus()).isEqualTo("PENDING");
        assertThat(updated.getRating()).isEqualTo(2);

        // Product rating excludes PENDING review
        Product prodAfter = productRepository.findById(product1.getId()).orElseThrow();
        assertThat(prodAfter.getReviewCount()).isEqualTo(0);
        assertThat(prodAfter.getAverageRating()).isEqualTo(0.0);
    }

    @Test
    @DisplayName("Update Review - Unauthorized user cannot update other customers' reviews")
    void updateReview_NonOwner_ThrowsBadRequestException() {
        createTestOrder(customerAlice, product1, OrderStatus.DELIVERED);
        ReviewResponse created = reviewService.createReview(customerAlice.getId(), product1.getId(), ReviewRequest.builder()
                .productId(product1.getId()).rating(5).title("Mine").comment("Mine").build());

        UpdateReviewRequest updateReq = UpdateReviewRequest.builder()
                .rating(1)
                .title("Hacked")
                .comment("Hacked review")
                .build();

        assertThatThrownBy(() -> reviewService.updateReview(customerBob.getId(), created.getId(), updateReq))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("only edit your own reviews");
    }

    @Test
    @DisplayName("Delete Review - Customer deletes own review and product metrics update")
    void deleteReview_CustomerOwn_Success() {
        createTestOrder(customerAlice, product1, OrderStatus.DELIVERED);
        ReviewResponse created = reviewService.createReview(customerAlice.getId(), product1.getId(), ReviewRequest.builder()
                .productId(product1.getId()).rating(5).title("Great").comment("Nice").build());

        reviewService.moderateReview(adminUser.getId(), adminUser.getEmail(), created.getId(),
                ReviewModerationRequest.builder().status(ReviewStatus.APPROVED).build());

        Product prodBefore = productRepository.findById(product1.getId()).orElseThrow();
        assertThat(prodBefore.getReviewCount()).isEqualTo(1);

        reviewService.deleteReview(customerAlice.getId(), created.getId(), false);

        assertThat(reviewRepository.findById(created.getId())).isEmpty();
        Product prodAfter = productRepository.findById(product1.getId()).orElseThrow();
        assertThat(prodAfter.getReviewCount()).isEqualTo(0);
    }

    @Test
    @DisplayName("Get Public Reviews - Excludes PENDING, REJECTED, and HIDDEN reviews")
    void getProductReviews_ExcludesNonApproved() {
        createTestOrder(customerAlice, product1, OrderStatus.DELIVERED);
        createTestOrder(customerBob, product1, OrderStatus.DELIVERED);

        ReviewResponse rev1 = reviewService.createReview(customerAlice.getId(), product1.getId(), ReviewRequest.builder()
                .productId(product1.getId()).rating(5).title("Approve me").comment("Quality").build());

        ReviewResponse rev2 = reviewService.createReview(customerBob.getId(), product1.getId(), ReviewRequest.builder()
                .productId(product1.getId()).rating(1).title("Reject me").comment("Spam").build());

        reviewService.moderateReview(adminUser.getId(), adminUser.getEmail(), rev1.getId(),
                ReviewModerationRequest.builder().status(ReviewStatus.APPROVED).build());
        reviewService.moderateReview(adminUser.getId(), adminUser.getEmail(), rev2.getId(),
                ReviewModerationRequest.builder().status(ReviewStatus.REJECTED).reason("Spam").build());

        PageResponse<ReviewResponse> publicReviews = reviewService.getProductReviews(product1.getId(), PageRequest.of(0, 10));

        assertThat(publicReviews.getContent()).hasSize(1);
        assertThat(publicReviews.getContent().get(0).getId()).isEqualTo(rev1.getId());
    }
}
