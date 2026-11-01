package com.shopsphere.service;

import com.shopsphere.dto.request.payment.MockPaymentRequest;
import com.shopsphere.entity.*;
import com.shopsphere.entity.enums.OrderStatus;
import com.shopsphere.entity.enums.PaymentMethod;
import com.shopsphere.entity.enums.PaymentStatus;
import com.shopsphere.entity.enums.UserStatus;
import com.shopsphere.exception.BadRequestException;
import com.shopsphere.exception.PaymentException;
import com.shopsphere.payment.mock.MockPaymentGateway;
import com.shopsphere.payment.razorpay.RazorpayPaymentGateway;
import com.shopsphere.repository.CategoryRepository;
import com.shopsphere.repository.OrderRepository;
import com.shopsphere.repository.ProductRepository;
import com.shopsphere.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class Phase10ProductionConfigTest {

    @Autowired
    private PaymentService paymentService;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private ProductRepository productRepository;

    @Test
    @DisplayName("Module 1: MockPaymentGateway throws PaymentException in production profile")
    void testMockPaymentGatewayThrowsInProduction() {
        MockPaymentGateway prodMockGateway = new MockPaymentGateway(true, "prod");

        Order dummyOrder = Order.builder()
                .orderNumber("ORD-PROD-TEST")
                .totalAmount(new BigDecimal("999.00"))
                .build();

        assertThatThrownBy(() -> prodMockGateway.createGatewayOrder(dummyOrder))
                .isInstanceOf(PaymentException.class)
                .hasMessageContaining("Mock payment gateway is disabled in this environment");

        Payment payment = Payment.builder().build();
        assertThatThrownBy(() -> prodMockGateway.verifySignature(payment, "order_123", "pay_123", "mock_sig"))
                .isInstanceOf(PaymentException.class)
                .hasMessageContaining("Mock payment gateway is disabled in this environment");

        assertThatThrownBy(() -> prodMockGateway.processRefund(payment, new BigDecimal("100.00"), "reason"))
                .isInstanceOf(PaymentException.class)
                .hasMessageContaining("Mock payment gateway is disabled in this environment");
    }

    @Test
    @DisplayName("Module 1: PaymentServiceImpl rejects mock payment initiation when activeProfile=prod")
    void testPaymentServiceRejectsMockInitiationInProd() {
        User user = userRepository.save(User.builder()
                .email("prod_test_" + UUID.randomUUID() + "@shopsphere.com")
                .password("Password123!")
                .firstName("Prod")
                .lastName("Tester")
                .status(UserStatus.ACTIVE)
                .build());

        Order order = orderRepository.save(Order.builder()
                .orderNumber("ORD-PROD-" + UUID.randomUUID().toString().substring(0, 6).toUpperCase())
                .user(user)
                .orderStatus(OrderStatus.PENDING)
                .subtotal(new BigDecimal("500.00"))
                .totalAmount(new BigDecimal("500.00"))
                .shippingAddress(AddressSnapshot.builder()
                        .recipientName("Prod")
                        .phoneNumber("9999999999")
                        .addressLine1("Street")
                        .city("City")
                        .state("State")
                        .postalCode("560001")
                        .country("India")
                        .build())
                .build());

        // Temporarily simulate production profile on paymentService
        Object originalProfile = ReflectionTestUtils.getField(paymentService, "activeProfile");
        try {
            ReflectionTestUtils.setField(paymentService, "activeProfile", "prod");

            assertThatThrownBy(() -> paymentService.initiatePayment(order, PaymentMethod.MOCK_TEST))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("Mock payment gateway cannot be enabled or used in production");
        } finally {
            ReflectionTestUtils.setField(paymentService, "activeProfile", originalProfile);
        }
    }

    @Test
    @DisplayName("Module 1: PaymentServiceImpl rejects processMockPayment simulation when activeProfile=prod")
    void testPaymentServiceRejectsProcessMockPaymentInProd() {
        User user = userRepository.save(User.builder()
                .email("prod_sim_" + UUID.randomUUID() + "@shopsphere.com")
                .password("Password123!")
                .firstName("Prod")
                .lastName("Tester")
                .status(UserStatus.ACTIVE)
                .build());

        Order order = orderRepository.save(Order.builder()
                .orderNumber("ORD-SIM-" + UUID.randomUUID().toString().substring(0, 6).toUpperCase())
                .user(user)
                .orderStatus(OrderStatus.PENDING)
                .subtotal(new BigDecimal("500.00"))
                .totalAmount(new BigDecimal("500.00"))
                .shippingAddress(AddressSnapshot.builder()
                        .recipientName("Prod")
                        .phoneNumber("9999999999")
                        .addressLine1("Street")
                        .city("City")
                        .state("State")
                        .postalCode("560001")
                        .country("India")
                        .build())
                .build());

        MockPaymentRequest req = new MockPaymentRequest();
        req.setOrderId(order.getId());
        req.setStatus(PaymentStatus.SUCCESS);

        Object originalProfile = ReflectionTestUtils.getField(paymentService, "activeProfile");
        try {
            ReflectionTestUtils.setField(paymentService, "activeProfile", "prod");

            assertThatThrownBy(() -> paymentService.processMockPayment(user.getId(), req))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("Mock payment simulation cannot be enabled or executed in production");
        } finally {
            ReflectionTestUtils.setField(paymentService, "activeProfile", originalProfile);
        }
    }

    @Test
    @DisplayName("Module 1: RazorpayPaymentGateway in production profile disables placeholder keys and mock fallbacks")
    void testRazorpayRejectsPlaceholdersInProduction() {
        RazorpayPaymentGateway prodGateway = new RazorpayPaymentGateway(
                "rzp_test_placeholder_key",
                "rzp_test_placeholder_secret",
                "secret",
                false,
                "prod"
        );

        assertThat(prodGateway.isProduction()).isTrue();
        assertThat(prodGateway.isPlaceholderCredentials()).isTrue();

        Order order = Order.builder()
                .orderNumber("ORD-RZP-TEST")
                .totalAmount(new BigDecimal("500.00"))
                .build();

        assertThatThrownBy(() -> prodGateway.createGatewayOrder(order))
                .isInstanceOf(PaymentException.class)
                .hasMessageContaining("cannot be placeholder in production environment");
    }
}
