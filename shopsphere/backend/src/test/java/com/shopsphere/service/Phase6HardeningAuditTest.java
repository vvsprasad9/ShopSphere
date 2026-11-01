package com.shopsphere.service;

import com.shopsphere.dto.request.cart.AddToCartRequest;
import com.shopsphere.dto.request.order.CancelOrderRequest;
import com.shopsphere.dto.request.order.CheckoutRequest;
import com.shopsphere.dto.request.payment.PaymentVerificationRequest;
import com.shopsphere.dto.request.user.AddressRequest;
import com.shopsphere.dto.response.order.OrderResponse;
import com.shopsphere.dto.response.order.OrderTrackingResponse;
import com.shopsphere.dto.response.payment.PaymentResponse;
import com.shopsphere.entity.*;
import com.shopsphere.entity.enums.*;
import com.shopsphere.exception.BadRequestException;
import com.shopsphere.exception.ConflictException;
import com.shopsphere.exception.PaymentException;
import com.shopsphere.payment.razorpay.RazorpayPaymentGateway;
import com.shopsphere.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
public class Phase6HardeningAuditTest {

    @Autowired
    private OrderService orderService;

    @Autowired
    private PaymentService paymentService;

    @Autowired
    private CartService cartService;

    @Autowired
    private InventoryService inventoryService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private InventoryRepository inventoryRepository;

    @Autowired
    private AddressRepository addressRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private User customer1;
    private User customer2;
    private Address address1;
    private Address address2;
    private Product productA;
    private Product productB;

    @BeforeEach
    void setUp() {
        Role customerRole = roleRepository.findByName(ERole.ROLE_CUSTOMER)
                .orElseGet(() -> roleRepository.save(Role.builder().name(ERole.ROLE_CUSTOMER).build()));

        String u1Suffix = UUID.randomUUID().toString().substring(0, 8);
        customer1 = userRepository.save(User.builder()
                .email("audit_c1_" + u1Suffix + "@example.com")
                .password(passwordEncoder.encode("Password123!"))
                .firstName("Audit")
                .lastName("Customer1")
                .phone("+919876543210")
                .status(UserStatus.ACTIVE)
                .emailVerified(true)
                .roles(new HashSet<>(Collections.singletonList(customerRole)))
                .build());

        String u2Suffix = UUID.randomUUID().toString().substring(0, 8);
        customer2 = userRepository.save(User.builder()
                .email("audit_c2_" + u2Suffix + "@example.com")
                .password(passwordEncoder.encode("Password123!"))
                .firstName("Audit")
                .lastName("Customer2")
                .phone("+919876543211")
                .status(UserStatus.ACTIVE)
                .emailVerified(true)
                .roles(new HashSet<>(Collections.singletonList(customerRole)))
                .build());

        address1 = addressRepository.save(Address.builder()
                .user(customer1)
                .fullName("Secret Customer One")
                .phoneNumber("+919876543210")
                .addressLine1("123 Private Street, Flat 4B")
                .addressLine2("Opposite Secure Landmark")
                .city("Bengaluru")
                .state("Karnataka")
                .postalCode("560001")
                .country("India")
                .addressType(AddressType.HOME)
                .isDefault(true)
                .build());

        address2 = addressRepository.save(Address.builder()
                .user(customer1)
                .fullName("Alternate Address")
                .phoneNumber("+919876543219")
                .addressLine1("456 Second Road")
                .city("Bengaluru")
                .state("Karnataka")
                .postalCode("560002")
                .country("India")
                .addressType(AddressType.WORK)
                .isDefault(false)
                .build());

        Category category = categoryRepository.save(Category.builder()
                .name("Audit Hardening Category " + u1Suffix)
                .slug("audit-hardening-" + u1Suffix)
                .description("Audit category")
                .active(true)
                .build());

        productA = productRepository.save(Product.builder()
                .name("Hardening Item Alpha " + u1Suffix)
                .slug("item-alpha-" + u1Suffix)
                .sku("SKU-ALPHA-" + u1Suffix)
                .price(new BigDecimal("100.00"))
                .category(category)
                .active(true)
                .build());

        inventoryRepository.save(Inventory.builder()
                .product(productA)
                .quantityAvailable(10)
                .quantityReserved(0)
                .lowStockThreshold(2)
                .build());

        productB = productRepository.save(Product.builder()
                .name("Hardening Item Beta " + u1Suffix)
                .slug("item-beta-" + u1Suffix)
                .sku("SKU-BETA-" + u1Suffix)
                .price(new BigDecimal("200.00"))
                .category(category)
                .active(true)
                .build());

        inventoryRepository.save(Inventory.builder()
                .product(productB)
                .quantityAvailable(10)
                .quantityReserved(0)
                .lowStockThreshold(2)
                .build());
    }

    @Test
    @DisplayName("Audit Hardening 1: Public order tracking sanitizes all PII")
    void testSanitizedPublicOrderTracking() {
        cartService.addToCart(customer1.getId(), AddToCartRequest.builder()
                .productId(productA.getId())
                .quantity(2)
                .build());

        OrderResponse order = orderService.checkout(customer1.getId(), CheckoutRequest.builder()
                .shippingAddressId(address1.getId())
                .paymentMethod(PaymentMethod.MOCK_TEST)
                .build());

        OrderTrackingResponse tracking = orderService.trackOrderByNumber(order.getOrderNumber());

        assertThat(tracking).isNotNull();
        assertThat(tracking.getOrderNumber()).isEqualTo(order.getOrderNumber());
        assertThat(tracking.getOrderStatus()).isEqualTo(OrderStatus.PENDING);
        assertThat(tracking.getTotalItems()).isEqualTo(2);
        assertThat(tracking.getItems()).hasSize(1);
        assertThat(tracking.getItems().get(0).getProductName()).isEqualTo(productA.getName());
        assertThat(tracking.getItems().get(0).getQuantity()).isEqualTo(2);

        // Verify city/state/country only - no street address, recipient name, or phone
        assertThat(tracking.getDeliveryDestination()).isNotNull();
        assertThat(tracking.getDeliveryDestination().getCity()).isEqualTo("Bengaluru");
        assertThat(tracking.getDeliveryDestination().getState()).isEqualTo("Karnataka");
        assertThat(tracking.getDeliveryDestination().getCountry()).isEqualTo("India");
    }

    @Test
    @DisplayName("Audit Hardening 2: COD payment stays PENDING until admin marks collected without touching inventory")
    void testCodPaymentLifecycleAndIndependence() {
        cartService.addToCart(customer1.getId(), AddToCartRequest.builder()
                .productId(productA.getId())
                .quantity(3)
                .build());

        // 1. COD Checkout
        OrderResponse codOrder = orderService.checkout(customer1.getId(), CheckoutRequest.builder()
                .shippingAddressId(address1.getId())
                .paymentMethod(PaymentMethod.COD)
                .build());

        assertThat(codOrder.getOrderStatus()).isEqualTo("CONFIRMED");

        // Inventory committed immediately on COD placement: available 10 -> 7, reserved = 0
        Inventory invAfterCheckout = inventoryRepository.findByProductId(productA.getId()).orElseThrow();
        assertThat(invAfterCheckout.getQuantityAvailable()).isEqualTo(7);
        assertThat(invAfterCheckout.getQuantityReserved()).isEqualTo(0);

        // Payment must remain PENDING
        Payment payment = paymentRepository.findByOrderId(codOrder.getId()).orElseThrow();
        assertThat(payment.getPaymentStatus()).isEqualTo(PaymentStatus.PENDING);
        assertThat(payment.getTransactionId()).startsWith("COD-");

        // 2. Admin collects COD payment
        PaymentResponse collected = paymentService.markCodPaymentCollected(codOrder.getId());
        assertThat(collected.getPaymentStatus()).isEqualTo("SUCCESS");

        // Verify inventory remains untouched
        Inventory invAfterCollection = inventoryRepository.findByProductId(productA.getId()).orElseThrow();
        assertThat(invAfterCollection.getQuantityAvailable()).isEqualTo(7);
        assertThat(invAfterCollection.getQuantityReserved()).isEqualTo(0);

        // 3. Reject non-COD order
        cartService.addToCart(customer1.getId(), AddToCartRequest.builder()
                .productId(productA.getId())
                .quantity(1)
                .build());
        OrderResponse onlineOrder = orderService.checkout(customer1.getId(), CheckoutRequest.builder()
                .shippingAddressId(address1.getId())
                .paymentMethod(PaymentMethod.MOCK_TEST)
                .build());

        assertThatThrownBy(() -> paymentService.markCodPaymentCollected(onlineOrder.getId()))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("not a Cash-On-Delivery order");
    }

    @Test
    @DisplayName("Audit Hardening 3: Inverted item ordering prevents pessimistic lock deadlocks")
    void testDeterministicInventoryLockOrdering() throws Exception {
        // Prepare carts with inverse item order
        // Customer 1 cart: [Product A, Product B]
        cartService.addToCart(customer1.getId(), AddToCartRequest.builder().productId(productA.getId()).quantity(1).build());
        cartService.addToCart(customer1.getId(), AddToCartRequest.builder().productId(productB.getId()).quantity(1).build());

        // Customer 2 cart: [Product B, Product A]
        cartService.addToCart(customer2.getId(), AddToCartRequest.builder().productId(productB.getId()).quantity(1).build());
        cartService.addToCart(customer2.getId(), AddToCartRequest.builder().productId(productA.getId()).quantity(1).build());

        Address c2Address = addressRepository.save(Address.builder()
                .user(customer2)
                .fullName("Customer 2")
                .phoneNumber("+919876543211")
                .addressLine1("456 Other Rd")
                .city("Mumbai")
                .state("Maharashtra")
                .postalCode("400001")
                .country("India")
                .addressType(AddressType.HOME)
                .isDefault(true)
                .build());

        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch readyLatch = new CountDownLatch(2);
        CountDownLatch startLatch = new CountDownLatch(1);

        Future<OrderResponse> future1 = executor.submit(() -> {
            readyLatch.countDown();
            startLatch.await();
            return orderService.checkout(customer1.getId(), CheckoutRequest.builder()
                    .shippingAddressId(address1.getId())
                    .paymentMethod(PaymentMethod.MOCK_TEST)
                    .build());
        });

        Future<OrderResponse> future2 = executor.submit(() -> {
            readyLatch.countDown();
            startLatch.await();
            return orderService.checkout(customer2.getId(), CheckoutRequest.builder()
                    .shippingAddressId(c2Address.getId())
                    .paymentMethod(PaymentMethod.MOCK_TEST)
                    .build());
        });

        readyLatch.await(5, TimeUnit.SECONDS);
        startLatch.countDown();

        OrderResponse order1 = future1.get(10, TimeUnit.SECONDS);
        OrderResponse order2 = future2.get(10, TimeUnit.SECONDS);
        executor.shutdown();

        assertThat(order1).isNotNull();
        assertThat(order2).isNotNull();

        // 2 units reserved from Product A (available 10, reserved 2, purchasable 8)
        Inventory invA = inventoryRepository.findByProductId(productA.getId()).orElseThrow();
        assertThat(invA.getQuantityReserved()).isEqualTo(2);
        assertThat(invA.getPurchasableQuantity()).isEqualTo(8);

        // 2 units reserved from Product B (available 10, reserved 2, purchasable 8)
        Inventory invB = inventoryRepository.findByProductId(productB.getId()).orElseThrow();
        assertThat(invB.getQuantityReserved()).isEqualTo(2);
        assertThat(invB.getPurchasableQuantity()).isEqualTo(8);
    }

    @Test
    @DisplayName("Audit Hardening 4: Payment verification on expired/cancelled order initiates refund and avoids inventory commit")
    void testPaymentVerificationOnExpiredOrCancelledOrderTriggersRefund() {
        cartService.addToCart(customer1.getId(), AddToCartRequest.builder()
                .productId(productA.getId())
                .quantity(4)
                .build());

        OrderResponse order = orderService.checkout(customer1.getId(), CheckoutRequest.builder()
                .shippingAddressId(address1.getId())
                .paymentMethod(PaymentMethod.MOCK_TEST)
                .build());

        String rzpOrderId = order.getPayment().getRazorpayOrderId();

        // Cancel order prior to payment verification
        orderService.cancelOrder(customer1.getId(), order.getId(), CancelOrderRequest.builder().reason("Changed mind").build());

        // Stock was released on cancel: reserved = 0, purchasable = 10
        Inventory invAfterCancel = inventoryRepository.findByProductId(productA.getId()).orElseThrow();
        assertThat(invAfterCancel.getQuantityReserved()).isEqualTo(0);
        assertThat(invAfterCancel.getQuantityAvailable()).isEqualTo(10);

        // Late payment verification arrives
        PaymentVerificationRequest verifyRequest = PaymentVerificationRequest.builder()
                .razorpayOrderId(rzpOrderId)
                .razorpayPaymentId("pay_late_test_999")
                .razorpaySignature("sig_mock_signature")
                .build();

        assertThatThrownBy(() -> paymentService.verifyPayment(customer1.getId(), verifyRequest))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("refund has been initiated");

        // Payment status must be REFUNDED
        Payment updatedPayment = paymentRepository.findByRazorpayOrderId(rzpOrderId).orElseThrow();
        assertThat(updatedPayment.getPaymentStatus()).isEqualTo(PaymentStatus.REFUNDED);

        // Stock must NOT be committed
        Inventory invFinal = inventoryRepository.findByProductId(productA.getId()).orElseThrow();
        assertThat(invFinal.getQuantityAvailable()).isEqualTo(10);
        assertThat(invFinal.getQuantityReserved()).isEqualTo(0);
    }

    @Test
    @DisplayName("Audit Hardening 5: Idempotency fingerprinting returns existing order on same payload and 409 on different payload")
    void testIdempotencyRequestFingerprinting() {
        String key = "idem-audit-" + UUID.randomUUID();

        cartService.addToCart(customer1.getId(), AddToCartRequest.builder()
                .productId(productA.getId())
                .quantity(1)
                .build());

        CheckoutRequest request = CheckoutRequest.builder()
                .idempotencyKey(key)
                .shippingAddressId(address1.getId())
                .paymentMethod(PaymentMethod.MOCK_TEST)
                .build();

        // 1. Initial checkout
        OrderResponse firstOrder = orderService.checkout(customer1.getId(), request);
        assertThat(firstOrder).isNotNull();

        // 2. Exact same payload retry with same key -> returns identical order
        OrderResponse retriedOrder = orderService.checkout(customer1.getId(), request);
        assertThat(retriedOrder.getId()).isEqualTo(firstOrder.getId());
        assertThat(retriedOrder.getOrderNumber()).isEqualTo(firstOrder.getOrderNumber());

        // 3. Different payload with same key -> throws ConflictException (409)
        cartService.addToCart(customer1.getId(), AddToCartRequest.builder()
                .productId(productB.getId())
                .quantity(2)
                .build());

        CheckoutRequest conflictingRequest = CheckoutRequest.builder()
                .idempotencyKey(key)
                .shippingAddressId(address2.getId()) // Different shipping address
                .paymentMethod(PaymentMethod.COD)     // Different payment method
                .build();

        assertThatThrownBy(() -> orderService.checkout(customer1.getId(), conflictingRequest))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("Idempotency key reuse detected with differing request payload");
    }

    @Test
    @DisplayName("Audit Hardening 6: Razorpay gateway in production profile strictly rejects mock signatures and placeholder credentials")
    void testRazorpayProductionSecurity() {
        RazorpayPaymentGateway prodGateway = new RazorpayPaymentGateway(
                "rzp_test_placeholder_key",
                "rzp_test_placeholder_secret",
                "rzp_test_placeholder_webhook",
                true,
                "prod"
        );

        assertThat(prodGateway.isProduction()).isTrue();

        // Must reject placeholder credentials in production
        Order dummyOrder = Order.builder().orderNumber("ORD-PROD-TEST").totalAmount(new BigDecimal("500.00")).build();
        assertThatThrownBy(() -> prodGateway.createGatewayOrder(dummyOrder))
                .isInstanceOf(PaymentException.class)
                .hasMessageContaining("credentials cannot be placeholder in production");

        // Must reject mock signatures in production
        boolean verified = prodGateway.verifySignature(null, "order_rzp_mock_123", "pay_mock_123", "sig_mock_signature");
        assertThat(verified).isFalse();

        // Must reject mock webhook signatures in production
        boolean webhookVerified = prodGateway.verifyWebhookSignature("{\"event\":\"payment.captured\"}", "mock_sig");
        assertThat(webhookVerified).isFalse();
    }

    @Test
    @DisplayName("Audit Hardening 7: Concurrent inventory reservation strictly prevents overselling (5 stock, 4 threads request 2 each -> exactly 2 succeed)")
    void testConcurrentInventoryReservationOversellingPrevention() throws Exception {
        // Set available to exactly 5, reserved 0
        Inventory inv = inventoryRepository.findByProductId(productA.getId()).orElseThrow();
        inv.setQuantityAvailable(5);
        inv.setQuantityReserved(0);
        inventoryRepository.save(inv);

        int threads = 4;
        ExecutorService executor = Executors.newFixedThreadPool(threads);
        CountDownLatch readyLatch = new CountDownLatch(threads);
        CountDownLatch startLatch = new CountDownLatch(1);

        AtomicInteger successes = new AtomicInteger(0);
        AtomicInteger failures = new AtomicInteger(0);

        List<Future<?>> futures = new ArrayList<>();
        for (int i = 0; i < threads; i++) {
            futures.add(executor.submit(() -> {
                readyLatch.countDown();
                try {
                    startLatch.await();
                    inventoryService.reserveStock(productA, null, 2);
                    successes.incrementAndGet();
                } catch (BadRequestException e) {
                    failures.incrementAndGet();
                } catch (Exception e) {
                    failures.incrementAndGet();
                }
            }));
        }

        readyLatch.await(5, TimeUnit.SECONDS);
        startLatch.countDown();

        for (Future<?> f : futures) {
            f.get(10, TimeUnit.SECONDS);
        }
        executor.shutdown();

        // Exactly 2 threads can reserve 2 each (4 total). 3rd and 4th thread must fail (only 1 available).
        assertThat(successes.get()).isEqualTo(2);
        assertThat(failures.get()).isEqualTo(2);

        Inventory finalInv = inventoryRepository.findByProductId(productA.getId()).orElseThrow();
        assertThat(finalInv.getQuantityAvailable()).isEqualTo(5);
        assertThat(finalInv.getQuantityReserved()).isEqualTo(4);
        assertThat(finalInv.getPurchasableQuantity()).isEqualTo(1);
    }
}
