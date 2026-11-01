package com.shopsphere.service;

import com.shopsphere.dto.request.cart.AddToCartRequest;
import com.shopsphere.dto.request.order.CancelOrderRequest;
import com.shopsphere.dto.request.order.CheckoutPreviewRequest;
import com.shopsphere.dto.request.order.CheckoutRequest;
import com.shopsphere.dto.request.order.UpdateOrderStatusRequest;
import com.shopsphere.dto.request.user.AddressRequest;
import com.shopsphere.dto.response.PageResponse;
import com.shopsphere.dto.response.order.CheckoutPreviewResponse;
import com.shopsphere.dto.response.order.OrderResponse;
import com.shopsphere.dto.response.order.OrderTrackingResponse;
import com.shopsphere.entity.*;
import com.shopsphere.entity.enums.AddressType;
import com.shopsphere.entity.enums.DiscountType;
import com.shopsphere.entity.enums.ERole;
import com.shopsphere.entity.enums.OrderStatus;
import com.shopsphere.entity.enums.PaymentMethod;
import com.shopsphere.entity.enums.UserStatus;
import com.shopsphere.exception.BadRequestException;
import com.shopsphere.exception.ResourceNotFoundException;
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
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class OrderServiceTest {

    @Autowired
    private OrderService orderService;

    @Autowired
    private CartService cartService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private ProductVariantRepository productVariantRepository;

    @Autowired
    private InventoryRepository inventoryRepository;

    @Autowired
    private AddressRepository addressRepository;

    @Autowired
    private CouponRepository couponRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private User customer;
    private User customer2;
    private Category testCategory;
    private Product testProduct;
    private Address testAddress;
    private Coupon testCoupon;

    @BeforeEach
    void setUp() {
        Role customerRole = roleRepository.findByName(ERole.ROLE_CUSTOMER)
                .orElseGet(() -> roleRepository.save(Role.builder().name(ERole.ROLE_CUSTOMER).build()));

        customer = userRepository.save(User.builder()
                .email("order.customer@example.com")
                .password(passwordEncoder.encode("Password123!"))
                .firstName("John")
                .lastName("Order")
                .status(UserStatus.ACTIVE)
                .emailVerified(true)
                .roles(new HashSet<>(List.of(customerRole)))
                .build());

        customer2 = userRepository.save(User.builder()
                .email("other.customer@example.com")
                .password(passwordEncoder.encode("Password123!"))
                .firstName("Alice")
                .lastName("Other")
                .status(UserStatus.ACTIVE)
                .emailVerified(true)
                .roles(new HashSet<>(List.of(customerRole)))
                .build());

        testCategory = categoryRepository.save(Category.builder()
                .name("Electronics")
                .slug("electronics-order-test")
                .active(true)
                .build());

        testProduct = Product.builder()
                .name("Gaming Laptop")
                .slug("gaming-laptop-order-test")
                .sku("LAP-GM-001")
                .price(new BigDecimal("1000.00"))
                .discountPrice(new BigDecimal("900.00"))
                .category(testCategory)
                .active(true)
                .build();
        testProduct = productRepository.save(testProduct);

        Inventory inv = Inventory.builder()
                .product(testProduct)
                .quantityAvailable(20)
                .quantityReserved(0)
                .lowStockThreshold(5)
                .build();
        inv = inventoryRepository.save(inv);
        testProduct.setInventory(inv);

        testAddress = addressRepository.save(Address.builder()
                .user(customer)
                .fullName("John Order")
                .phoneNumber("+919876543210")
                .addressLine1("123 Tech Park")
                .city("Bengaluru")
                .state("Karnataka")
                .postalCode("560001")
                .country("India")
                .addressType(AddressType.HOME)
                .isDefault(true)
                .build());

        testCoupon = couponRepository.save(Coupon.builder()
                .code("DISCOUNT100")
                .discountType(DiscountType.FIXED)
                .discountValue(new BigDecimal("100.00"))
                .minOrderValue(new BigDecimal("500.00"))
                .startDate(LocalDateTime.now().minusDays(1))
                .expirationDate(LocalDateTime.now().plusDays(10))
                .usageLimit(50)
                .perUserLimit(1)
                .timesUsed(0)
                .active(true)
                .build());
    }

    @Test
    @DisplayName("Checkout preview calculates live totals and taxes correctly")
    void testCheckoutPreview() {
        cartService.addToCart(customer.getId(), AddToCartRequest.builder()
                .productId(testProduct.getId())
                .quantity(2)
                .build());

        CheckoutPreviewResponse preview = orderService.previewCheckout(customer.getId(), CheckoutPreviewRequest.builder()
                .couponCode("DISCOUNT100")
                .shippingAddressId(testAddress.getId())
                .build());

        assertThat(preview.getTotalQuantity()).isEqualTo(2);
        // Subtotal: 900.00 * 2 = 1800.00
        assertThat(preview.getSubtotal()).isEqualByComparingTo("1800.00");
        assertThat(preview.isCouponApplied()).isTrue();
        assertThat(preview.getDiscountAmount()).isEqualByComparingTo("100.00");
        // Taxable: 1700.00, Shipping: Free (>= 500)
        assertThat(preview.getShippingFee()).isEqualByComparingTo("0.00");
        // Tax: 1700.00 * 0.05 = 85.00
        assertThat(preview.getTaxAmount()).isEqualByComparingTo("85.00");
        // Total: 1700 + 0 + 85 = 1785.00
        assertThat(preview.getTotalAmount()).isEqualByComparingTo("1785.00");
        assertThat(preview.isHasStockIssues()).isFalse();
    }

    @Test
    @DisplayName("Successful checkout reserves inventory and clears cart")
    void testSuccessfulCheckoutWithInventoryReservation() {
        cartService.addToCart(customer.getId(), AddToCartRequest.builder()
                .productId(testProduct.getId())
                .quantity(3)
                .build());

        OrderResponse orderResponse = orderService.checkout(customer.getId(), CheckoutRequest.builder()
                .shippingAddressId(testAddress.getId())
                .paymentMethod(PaymentMethod.MOCK_TEST)
                .notes("Handle with care")
                .build());

        assertThat(orderResponse.getId()).isNotNull();
        assertThat(orderResponse.getOrderNumber()).startsWith("ORD-");
        assertThat(orderResponse.getOrderStatus()).isEqualTo("PENDING");
        assertThat(orderResponse.getItems()).hasSize(1);
        assertThat(orderResponse.getItems().get(0).getQuantity()).isEqualTo(3);
        assertThat(orderResponse.getItems().get(0).getUnitPrice()).isEqualByComparingTo("900.00");

        // Verify inventory reservation
        Inventory updatedInv = inventoryRepository.findByProductId(testProduct.getId()).orElseThrow();
        assertThat(updatedInv.getQuantityAvailable()).isEqualTo(20);
        assertThat(updatedInv.getQuantityReserved()).isEqualTo(3);
        assertThat(updatedInv.getPurchasableQuantity()).isEqualTo(17);

        // Verify cart is cleared
        assertThat(cartService.getCart(customer.getId()).getTotalItems()).isEqualTo(0);
    }

    @Test
    @DisplayName("Checkout with empty cart throws BadRequestException")
    void testCheckoutWithEmptyCartThrowsBadRequest() {
        assertThatThrownBy(() -> orderService.checkout(customer.getId(), CheckoutRequest.builder()
                .shippingAddressId(testAddress.getId())
                .paymentMethod(PaymentMethod.MOCK_TEST)
                .build()))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Shopping cart is empty");
    }

    @Test
    @DisplayName("Checkout with insufficient stock throws BadRequestException")
    void testCheckoutWithInsufficientStockThrowsBadRequest() {
        cartService.addToCart(customer.getId(), AddToCartRequest.builder()
                .productId(testProduct.getId())
                .quantity(5)
                .build());

        // Reduce available stock to 4
        Inventory inv = inventoryRepository.findByProductId(testProduct.getId()).orElseThrow();
        inv.setQuantityAvailable(4);
        inventoryRepository.save(inv);

        assertThatThrownBy(() -> orderService.checkout(customer.getId(), CheckoutRequest.builder()
                .shippingAddressId(testAddress.getId())
                .paymentMethod(PaymentMethod.MOCK_TEST)
                .build()))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Insufficient stock");
    }

    @Test
    @DisplayName("Checkout with inactive product throws BadRequestException")
    void testCheckoutWithInactiveProductThrowsBadRequest() {
        cartService.addToCart(customer.getId(), AddToCartRequest.builder()
                .productId(testProduct.getId())
                .quantity(1)
                .build());

        testProduct.setActive(false);
        productRepository.save(testProduct);

        assertThatThrownBy(() -> orderService.checkout(customer.getId(), CheckoutRequest.builder()
                .shippingAddressId(testAddress.getId())
                .paymentMethod(PaymentMethod.MOCK_TEST)
                .build()))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("no longer available");
    }

    @Test
    @DisplayName("Checkout applies coupon discount and enforces usage limits")
    void testCheckoutWithCouponDiscount() {
        cartService.addToCart(customer.getId(), AddToCartRequest.builder()
                .productId(testProduct.getId())
                .quantity(1)
                .build());

        OrderResponse orderResponse = orderService.checkout(customer.getId(), CheckoutRequest.builder()
                .shippingAddressId(testAddress.getId())
                .couponCode("DISCOUNT100")
                .paymentMethod(PaymentMethod.MOCK_TEST)
                .build());

        assertThat(orderResponse.getCouponCode()).isEqualTo("DISCOUNT100");
        assertThat(orderResponse.getDiscountAmount()).isEqualByComparingTo("100.00");

        Coupon updatedCoupon = couponRepository.findByCodeIgnoreCase("DISCOUNT100").orElseThrow();
        assertThat(updatedCoupon.getTimesUsed()).isEqualTo(1);
    }

    @Test
    @DisplayName("Checkout with variant manages independent variant inventory")
    void testCheckoutWithVariantIndependentInventory() {
        ProductVariant variant = productVariantRepository.save(ProductVariant.builder()
                .product(testProduct)
                .variantName("16GB RAM / 1TB SSD")
                .sku("LAP-GM-001-16-1T")
                .priceAdjustment(new BigDecimal("150.00"))
                .active(true)
                .build());

        Inventory variantInv = inventoryRepository.save(Inventory.builder()
                .variant(variant)
                .quantityAvailable(10)
                .quantityReserved(0)
                .lowStockThreshold(2)
                .build());
        variant.setInventory(variantInv);

        cartService.addToCart(customer.getId(), AddToCartRequest.builder()
                .productId(testProduct.getId())
                .variantId(variant.getId())
                .quantity(2)
                .build());

        OrderResponse orderResponse = orderService.checkout(customer.getId(), CheckoutRequest.builder()
                .shippingAddressId(testAddress.getId())
                .paymentMethod(PaymentMethod.MOCK_TEST)
                .build());

        // Variant stock reserved: 2 units
        Inventory updatedVariantInv = inventoryRepository.findByVariantId(variant.getId()).orElseThrow();
        assertThat(updatedVariantInv.getQuantityAvailable()).isEqualTo(10);
        assertThat(updatedVariantInv.getQuantityReserved()).isEqualTo(2);

        // Parent product inventory untouched
        Inventory parentInv = inventoryRepository.findByProductId(testProduct.getId()).orElseThrow();
        assertThat(parentInv.getQuantityReserved()).isEqualTo(0);

        // Price = 900.00 (discountPrice) + 150.00 = 1050.00
        assertThat(orderResponse.getItems().get(0).getUnitPrice()).isEqualByComparingTo("1050.00");
    }

    @Test
    @DisplayName("Checkout idempotency key returns existing order without re-reserving stock")
    void testCheckoutIdempotency() {
        cartService.addToCart(customer.getId(), AddToCartRequest.builder()
                .productId(testProduct.getId())
                .quantity(2)
                .build());

        String key = "idem-key-test-12345";
        OrderResponse order1 = orderService.checkout(customer.getId(), CheckoutRequest.builder()
                .shippingAddressId(testAddress.getId())
                .idempotencyKey(key)
                .paymentMethod(PaymentMethod.MOCK_TEST)
                .build());

        // Re-call checkout with identical key
        OrderResponse order2 = orderService.checkout(customer.getId(), CheckoutRequest.builder()
                .shippingAddressId(testAddress.getId())
                .idempotencyKey(key)
                .paymentMethod(PaymentMethod.MOCK_TEST)
                .build());

        assertThat(order2.getId()).isEqualTo(order1.getId());
        assertThat(order2.getOrderNumber()).isEqualTo(order1.getOrderNumber());

        // Stock reserved only once (2 units)
        Inventory inv = inventoryRepository.findByProductId(testProduct.getId()).orElseThrow();
        assertThat(inv.getQuantityReserved()).isEqualTo(2);
    }

    @Test
    @DisplayName("COD payment method automatically confirms order and commits inventory")
    void testCodPaymentMethodAutoConfirms() {
        cartService.addToCart(customer.getId(), AddToCartRequest.builder()
                .productId(testProduct.getId())
                .quantity(2)
                .build());

        OrderResponse orderResponse = orderService.checkout(customer.getId(), CheckoutRequest.builder()
                .shippingAddressId(testAddress.getId())
                .paymentMethod(PaymentMethod.COD)
                .build());

        assertThat(orderResponse.getOrderStatus()).isEqualTo("CONFIRMED");

        // Inventory committed: available reduced from 20 to 18, reserved remains 0
        Inventory inv = inventoryRepository.findByProductId(testProduct.getId()).orElseThrow();
        assertThat(inv.getQuantityAvailable()).isEqualTo(18);
        assertThat(inv.getQuantityReserved()).isEqualTo(0);
    }

    @Test
    @DisplayName("Cancelling a PENDING order releases reserved inventory")
    void testCancelPendingOrderReleasesStock() {
        cartService.addToCart(customer.getId(), AddToCartRequest.builder()
                .productId(testProduct.getId())
                .quantity(4)
                .build());

        OrderResponse orderResponse = orderService.checkout(customer.getId(), CheckoutRequest.builder()
                .shippingAddressId(testAddress.getId())
                .paymentMethod(PaymentMethod.MOCK_TEST)
                .build());

        Inventory reservedInv = inventoryRepository.findByProductId(testProduct.getId()).orElseThrow();
        assertThat(reservedInv.getQuantityReserved()).isEqualTo(4);

        OrderResponse cancelled = orderService.cancelOrder(customer.getId(), orderResponse.getId(),
                CancelOrderRequest.builder().reason("Found better price").build());

        assertThat(cancelled.getOrderStatus()).isEqualTo("CANCELLED");

        Inventory releasedInv = inventoryRepository.findByProductId(testProduct.getId()).orElseThrow();
        assertThat(releasedInv.getQuantityReserved()).isEqualTo(0);
        assertThat(releasedInv.getPurchasableQuantity()).isEqualTo(20);
    }

    @Test
    @DisplayName("Admin order status transitions validate lifecycle rules")
    void testAdminOrderStatusTransitions() {
        cartService.addToCart(customer.getId(), AddToCartRequest.builder()
                .productId(testProduct.getId())
                .quantity(1)
                .build());

        OrderResponse order = orderService.checkout(customer.getId(), CheckoutRequest.builder()
                .shippingAddressId(testAddress.getId())
                .paymentMethod(PaymentMethod.MOCK_TEST)
                .build());

        // PENDING -> CONFIRMED
        OrderResponse confirmed = orderService.updateOrderStatus(order.getId(),
                UpdateOrderStatusRequest.builder().status(OrderStatus.CONFIRMED).build());
        assertThat(confirmed.getOrderStatus()).isEqualTo("CONFIRMED");

        // CONFIRMED -> PROCESSING
        OrderResponse processing = orderService.updateOrderStatus(order.getId(),
                UpdateOrderStatusRequest.builder().status(OrderStatus.PROCESSING).build());
        assertThat(processing.getOrderStatus()).isEqualTo("PROCESSING");

        // PROCESSING -> SHIPPED with tracking number
        OrderResponse shipped = orderService.updateOrderStatus(order.getId(),
                UpdateOrderStatusRequest.builder()
                        .status(OrderStatus.SHIPPED)
                        .trackingNumber("TRK-BLR-987654")
                        .build());
        assertThat(shipped.getOrderStatus()).isEqualTo("SHIPPED");
        assertThat(shipped.getTrackingNumber()).isEqualTo("TRK-BLR-987654");

        // SHIPPED -> DELIVERED
        OrderResponse delivered = orderService.updateOrderStatus(order.getId(),
                UpdateOrderStatusRequest.builder().status(OrderStatus.DELIVERED).build());
        assertThat(delivered.getOrderStatus()).isEqualTo("DELIVERED");

        // Invalid: DELIVERED -> CANCELLED
        assertThatThrownBy(() -> orderService.updateOrderStatus(order.getId(),
                UpdateOrderStatusRequest.builder().status(OrderStatus.CANCELLED).build()))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Invalid status transition");
    }

    @Test
    @DisplayName("Customer cannot access or cancel another customer's order (IDOR safety)")
    void testCustomerOrderOwnershipSecurity() {
        cartService.addToCart(customer.getId(), AddToCartRequest.builder()
                .productId(testProduct.getId())
                .quantity(1)
                .build());

        OrderResponse order = orderService.checkout(customer.getId(), CheckoutRequest.builder()
                .shippingAddressId(testAddress.getId())
                .paymentMethod(PaymentMethod.MOCK_TEST)
                .build());

        // Customer2 attempts to view Customer1's order
        assertThatThrownBy(() -> orderService.getCustomerOrderById(customer2.getId(), order.getId()))
                .isInstanceOf(ResourceNotFoundException.class);

        // Customer2 attempts to cancel Customer1's order
        assertThatThrownBy(() -> orderService.cancelOrder(customer2.getId(), order.getId(), null))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    @DisplayName("Public trackOrderByNumber returns order status")
    void testTrackOrderByOrderNumber() {
        cartService.addToCart(customer.getId(), AddToCartRequest.builder()
                .productId(testProduct.getId())
                .quantity(1)
                .build());

        OrderResponse order = orderService.checkout(customer.getId(), CheckoutRequest.builder()
                .shippingAddressId(testAddress.getId())
                .paymentMethod(PaymentMethod.MOCK_TEST)
                .build());

        OrderTrackingResponse tracked = orderService.trackOrderByNumber(order.getOrderNumber());
        assertThat(tracked.getOrderNumber()).isEqualTo(order.getOrderNumber());
        assertThat(tracked.getOrderStatus()).isEqualTo(OrderStatus.PENDING);
        assertThat(tracked.getTotalItems()).isEqualTo(1);
        assertThat(tracked.getDeliveryDestination()).isNotNull();
        assertThat(tracked.getItems()).hasSize(1);
    }

    @Test
    @DisplayName("cleanupExpiredReservations releases inventory for expired pending orders")
    void testCleanupExpiredReservations() {
        cartService.addToCart(customer.getId(), AddToCartRequest.builder()
                .productId(testProduct.getId())
                .quantity(3)
                .build());

        OrderResponse order = orderService.checkout(customer.getId(), CheckoutRequest.builder()
                .shippingAddressId(testAddress.getId())
                .paymentMethod(PaymentMethod.MOCK_TEST)
                .build());

        // Backdate reservation expiration
        Order entity = orderRepository.findById(order.getId()).orElseThrow();
        entity.setReservationExpiresAt(LocalDateTime.now().minusMinutes(20));
        orderRepository.save(entity);

        orderService.cleanupExpiredReservations();

        Order expired = orderRepository.findById(order.getId()).orElseThrow();
        assertThat(expired.getOrderStatus()).isEqualTo(OrderStatus.CANCELLED);

        Inventory inv = inventoryRepository.findByProductId(testProduct.getId()).orElseThrow();
        assertThat(inv.getQuantityReserved()).isEqualTo(0);
    }
}
