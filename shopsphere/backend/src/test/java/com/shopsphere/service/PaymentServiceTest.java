package com.shopsphere.service;

import com.shopsphere.dto.request.cart.AddToCartRequest;
import com.shopsphere.dto.request.order.CheckoutRequest;
import com.shopsphere.dto.request.payment.MockPaymentRequest;
import com.shopsphere.dto.request.payment.PaymentRefundRequest;
import com.shopsphere.dto.request.payment.PaymentVerificationRequest;
import com.shopsphere.dto.response.order.OrderResponse;
import com.shopsphere.dto.response.payment.PaymentResponse;
import com.shopsphere.entity.*;
import com.shopsphere.entity.enums.AddressType;
import com.shopsphere.entity.enums.ERole;
import com.shopsphere.entity.enums.OrderStatus;
import com.shopsphere.entity.enums.PaymentMethod;
import com.shopsphere.entity.enums.PaymentStatus;
import com.shopsphere.entity.enums.UserStatus;
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
import java.util.HashSet;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class PaymentServiceTest {

    @Autowired
    private PaymentService paymentService;

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
    private InventoryRepository inventoryRepository;

    @Autowired
    private AddressRepository addressRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private User customer;
    private Product testProduct;
    private Address testAddress;

    @BeforeEach
    void setUp() {
        Role customerRole = roleRepository.findByName(ERole.ROLE_CUSTOMER)
                .orElseGet(() -> roleRepository.save(Role.builder().name(ERole.ROLE_CUSTOMER).build()));

        customer = userRepository.save(User.builder()
                .email("payment.customer@example.com")
                .password(passwordEncoder.encode("Password123!"))
                .firstName("Pay")
                .lastName("Customer")
                .status(UserStatus.ACTIVE)
                .emailVerified(true)
                .roles(new HashSet<>(List.of(customerRole)))
                .build());

        Category category = categoryRepository.save(Category.builder()
                .name("Audio")
                .slug("audio-payment-test")
                .active(true)
                .build());

        testProduct = Product.builder()
                .name("Wireless Headphones")
                .slug("wireless-headphones-payment-test")
                .sku("AUD-HP-001")
                .price(new BigDecimal("500.00"))
                .category(category)
                .active(true)
                .build();
        testProduct = productRepository.save(testProduct);

        Inventory inv = Inventory.builder()
                .product(testProduct)
                .quantityAvailable(15)
                .quantityReserved(0)
                .lowStockThreshold(3)
                .build();
        inv = inventoryRepository.save(inv);
        testProduct.setInventory(inv);

        testAddress = addressRepository.save(Address.builder()
                .user(customer)
                .fullName("Pay Customer")
                .phoneNumber("+919988776655")
                .addressLine1("456 Innovation Way")
                .city("Hyderabad")
                .state("Telangana")
                .postalCode("500081")
                .country("India")
                .addressType(AddressType.HOME)
                .isDefault(true)
                .build());
    }

    private OrderResponse createTestOrder(PaymentMethod method) {
        cartService.addToCart(customer.getId(), AddToCartRequest.builder()
                .productId(testProduct.getId())
                .quantity(2)
                .build());

        return orderService.checkout(customer.getId(), CheckoutRequest.builder()
                .shippingAddressId(testAddress.getId())
                .paymentMethod(method)
                .build());
    }

    @Test
    @DisplayName("Initiate mock payment creates payment entity with CREATED status")
    void testInitiatePaymentMock() {
        OrderResponse orderResponse = createTestOrder(PaymentMethod.MOCK_TEST);
        assertThat(orderResponse.getPayment()).isNotNull();
        assertThat(orderResponse.getPayment().getPaymentStatus()).isEqualTo("CREATED");
        assertThat(orderResponse.getPayment().getRazorpayOrderId()).startsWith("order_mock_");
    }

    @Test
    @DisplayName("Verify payment with valid signature commits inventory and confirms order")
    void testVerifyPaymentSuccessCommitsInventory() {
        OrderResponse orderResponse = createTestOrder(PaymentMethod.MOCK_TEST);
        String razorpayOrderId = orderResponse.getPayment().getRazorpayOrderId();

        PaymentVerificationRequest req = PaymentVerificationRequest.builder()
                .razorpayOrderId(razorpayOrderId)
                .razorpayPaymentId("pay_test_123456")
                .razorpaySignature("sig_mock_signature")
                .build();

        PaymentResponse verified = paymentService.verifyPayment(customer.getId(), req);

        assertThat(verified.getPaymentStatus()).isEqualTo("SUCCESS");
        assertThat(verified.getTransactionId()).isEqualTo("pay_test_123456");

        // Verify order is CONFIRMED
        Order order = orderRepository.findById(orderResponse.getId()).orElseThrow();
        assertThat(order.getOrderStatus()).isEqualTo(OrderStatus.CONFIRMED);

        // Verify inventory committed: available reduced from 15 to 13, reserved 0
        Inventory inv = inventoryRepository.findByProductId(testProduct.getId()).orElseThrow();
        assertThat(inv.getQuantityAvailable()).isEqualTo(13);
        assertThat(inv.getQuantityReserved()).isEqualTo(0);
        assertThat(inv.getPurchasableQuantity()).isEqualTo(13);
    }

    @Test
    @DisplayName("Verify payment with invalid signature throws PaymentException")
    void testVerifyPaymentInvalidSignatureThrowsException() {
        OrderResponse orderResponse = createTestOrder(PaymentMethod.MOCK_TEST);
        String razorpayOrderId = orderResponse.getPayment().getRazorpayOrderId();

        PaymentVerificationRequest req = PaymentVerificationRequest.builder()
                .razorpayOrderId(razorpayOrderId)
                .razorpayPaymentId("pay_test_bad")
                .razorpaySignature("invalid_bogus_signature")
                .build();

        assertThatThrownBy(() -> paymentService.verifyPayment(customer.getId(), req))
                .isInstanceOf(PaymentException.class)
                .hasMessageContaining("Invalid payment signature");

        Payment payment = paymentRepository.findByOrderId(orderResponse.getId()).orElseThrow();
        assertThat(payment.getPaymentStatus()).isEqualTo(PaymentStatus.FAILED);
    }

    @Test
    @DisplayName("Verify payment is idempotent when called multiple times")
    void testVerifyPaymentIdempotency() {
        OrderResponse orderResponse = createTestOrder(PaymentMethod.MOCK_TEST);
        String razorpayOrderId = orderResponse.getPayment().getRazorpayOrderId();

        PaymentVerificationRequest req = PaymentVerificationRequest.builder()
                .razorpayOrderId(razorpayOrderId)
                .razorpayPaymentId("pay_test_idem")
                .razorpaySignature("sig_mock_signature")
                .build();

        PaymentResponse first = paymentService.verifyPayment(customer.getId(), req);
        PaymentResponse second = paymentService.verifyPayment(customer.getId(), req);

        assertThat(second.getId()).isEqualTo(first.getId());
        assertThat(second.getPaymentStatus()).isEqualTo("SUCCESS");

        // Inventory not double-deducted
        Inventory inv = inventoryRepository.findByProductId(testProduct.getId()).orElseThrow();
        assertThat(inv.getQuantityAvailable()).isEqualTo(13);
    }

    @Test
    @DisplayName("Mock payment simulation supports SUCCESS and FAILED outcomes")
    void testMockPaymentSimulation() {
        OrderResponse orderResponse = createTestOrder(PaymentMethod.MOCK_TEST);

        // Simulate Success
        PaymentResponse successResp = paymentService.processMockPayment(customer.getId(),
                MockPaymentRequest.builder()
                        .orderId(orderResponse.getId())
                        .status(PaymentStatus.SUCCESS)
                        .build());

        assertThat(successResp.getPaymentStatus()).isEqualTo("SUCCESS");
        Order order = orderRepository.findById(orderResponse.getId()).orElseThrow();
        assertThat(order.getOrderStatus()).isEqualTo(OrderStatus.CONFIRMED);

        // Inventory committed
        Inventory inv = inventoryRepository.findByProductId(testProduct.getId()).orElseThrow();
        assertThat(inv.getQuantityAvailable()).isEqualTo(13);
    }

    @Test
    @DisplayName("Process payment refund restores inventory and marks payment REFUNDED")
    void testRefundPaymentRestoresInventory() {
        OrderResponse orderResponse = createTestOrder(PaymentMethod.MOCK_TEST);

        // First complete payment
        paymentService.processMockPayment(customer.getId(), MockPaymentRequest.builder()
                .orderId(orderResponse.getId())
                .status(PaymentStatus.SUCCESS)
                .build());

        Inventory invAfterSuccess = inventoryRepository.findByProductId(testProduct.getId()).orElseThrow();
        assertThat(invAfterSuccess.getQuantityAvailable()).isEqualTo(13);

        // Refund payment
        PaymentResponse refundResp = paymentService.refundPayment(customer.getId(),
                PaymentRefundRequest.builder()
                        .orderId(orderResponse.getId())
                        .reason("Customer returned defective unit")
                        .build());

        assertThat(refundResp.getPaymentStatus()).isEqualTo("REFUNDED");
    }

    @Test
    @DisplayName("Webhook processing confirms order and commits inventory idempotently")
    void testWebhookProcessingAndIdempotency() {
        OrderResponse orderResponse = createTestOrder(PaymentMethod.MOCK_TEST);
        String rzpOrderId = orderResponse.getPayment().getRazorpayOrderId();

        String payload = "{" +
                "\"event\":\"payment.captured\"," +
                "\"account_id\":\"acc_test_1\"," +
                "\"created_at\":1600000000," +
                "\"payload\":{" +
                "  \"payment\":{" +
                "    \"entity\":{" +
                "      \"id\":\"pay_webhook_999\"," +
                "      \"order_id\":\"" + rzpOrderId + "\"," +
                "      \"amount\":100000," +
                "      \"status\":\"captured\"" +
                "    }" +
                "  }" +
                "}" +
                "}";

        paymentService.processWebhook(payload, "sig_mock_webhook");

        Payment payment = paymentRepository.findByOrderId(orderResponse.getId()).orElseThrow();
        assertThat(payment.getPaymentStatus()).isEqualTo(PaymentStatus.SUCCESS);

        Order order = orderRepository.findById(orderResponse.getId()).orElseThrow();
        assertThat(order.getOrderStatus()).isEqualTo(OrderStatus.CONFIRMED);

        // Inventory committed
        Inventory inv = inventoryRepository.findByProductId(testProduct.getId()).orElseThrow();
        assertThat(inv.getQuantityAvailable()).isEqualTo(13);

        // Process duplicate webhook - must complete cleanly without re-deducting stock
        paymentService.processWebhook(payload, "sig_mock_webhook");
        Inventory invAfterDuplicate = inventoryRepository.findByProductId(testProduct.getId()).orElseThrow();
        assertThat(invAfterDuplicate.getQuantityAvailable()).isEqualTo(13);
    }
}
