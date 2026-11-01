package com.shopsphere.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.shopsphere.dto.request.cart.AddToCartRequest;
import com.shopsphere.dto.request.order.CancelOrderRequest;
import com.shopsphere.dto.request.order.CheckoutPreviewRequest;
import com.shopsphere.dto.request.order.CheckoutRequest;
import com.shopsphere.dto.request.order.UpdateOrderStatusRequest;
import com.shopsphere.dto.request.payment.MockPaymentRequest;
import com.shopsphere.dto.request.payment.PaymentVerificationRequest;
import com.shopsphere.dto.request.user.AddressRequest;
import com.shopsphere.entity.*;
import com.shopsphere.entity.enums.AddressType;
import com.shopsphere.entity.enums.ERole;
import com.shopsphere.entity.enums.OrderStatus;
import com.shopsphere.entity.enums.PaymentMethod;
import com.shopsphere.entity.enums.PaymentStatus;
import com.shopsphere.entity.enums.UserStatus;
import com.shopsphere.repository.*;
import com.shopsphere.security.JwtTokenProvider;
import com.shopsphere.security.UserPrincipal;
import com.shopsphere.service.AddressService;
import com.shopsphere.service.CartService;
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
class OrderProcessingIntegrationTest {

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
    private InventoryRepository inventoryRepository;

    @Autowired
    private AddressRepository addressRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Autowired
    private CartService cartService;

    @Autowired
    private AddressService addressService;

    private User customer;
    private User admin;
    private String customerToken;
    private String adminToken;
    private Product testProduct;
    private Address testAddress;

    @BeforeEach
    void setUp() {
        Role customerRole = roleRepository.findByName(ERole.ROLE_CUSTOMER)
                .orElseGet(() -> roleRepository.save(Role.builder().name(ERole.ROLE_CUSTOMER).build()));
        Role adminRole = roleRepository.findByName(ERole.ROLE_ADMIN)
                .orElseGet(() -> roleRepository.save(Role.builder().name(ERole.ROLE_ADMIN).build()));

        customer = userRepository.save(User.builder()
                .email("flow.customer@example.com")
                .password(passwordEncoder.encode("Password123!"))
                .firstName("Flow")
                .lastName("Customer")
                .status(UserStatus.ACTIVE)
                .emailVerified(true)
                .roles(new HashSet<>(List.of(customerRole)))
                .build());

        admin = userRepository.save(User.builder()
                .email("flow.admin@example.com")
                .password(passwordEncoder.encode("Password123!"))
                .firstName("Flow")
                .lastName("Admin")
                .status(UserStatus.ACTIVE)
                .emailVerified(true)
                .roles(new HashSet<>(List.of(adminRole)))
                .build());

        customerToken = "Bearer " + jwtTokenProvider.generateAccessToken(customer);
        adminToken = "Bearer " + jwtTokenProvider.generateAccessToken(admin);

        Category category = categoryRepository.save(Category.builder()
                .name("Wearables")
                .slug("wearables-integration-test")
                .active(true)
                .build());

        testProduct = Product.builder()
                .name("Smart Fitness Band")
                .slug("smart-fitness-band-int-test")
                .sku("WR-FB-001")
                .price(new BigDecimal("299.00"))
                .category(category)
                .active(true)
                .build();
        testProduct = productRepository.save(testProduct);

        Inventory inv = Inventory.builder()
                .product(testProduct)
                .quantityAvailable(25)
                .quantityReserved(0)
                .lowStockThreshold(5)
                .build();
        inv = inventoryRepository.save(inv);
        testProduct.setInventory(inv);

        testAddress = addressRepository.save(Address.builder()
                .user(customer)
                .fullName("Flow Customer")
                .phoneNumber("+919123456789")
                .addressLine1("789 Silicon Blvd")
                .city("Pune")
                .state("Maharashtra")
                .postalCode("411001")
                .country("India")
                .addressType(AddressType.HOME)
                .isDefault(true)
                .build());
    }

    @Test
    @DisplayName("End-to-end: Add to cart -> Preview -> Checkout -> Verify Payment -> Track -> Admin Status Updates")
    void testEndToEndOrderFlow() throws Exception {
        // 1. Add product to cart
        cartService.addToCart(customer.getId(), AddToCartRequest.builder()
                .productId(testProduct.getId())
                .quantity(2)
                .build());

        // 2. Checkout preview
        CheckoutPreviewRequest previewReq = CheckoutPreviewRequest.builder()
                .shippingAddressId(testAddress.getId())
                .build();

        mockMvc.perform(post("/api/v1/orders/checkout/preview")
                        .header("Authorization", customerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(previewReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.totalQuantity").value(2))
                .andExpect(jsonPath("$.data.subtotal").value(598.00))
                .andExpect(jsonPath("$.data.shippingFee").value(0.00));

        // 3. Checkout
        CheckoutRequest checkoutReq = CheckoutRequest.builder()
                .shippingAddressId(testAddress.getId())
                .paymentMethod(PaymentMethod.MOCK_TEST)
                .notes("Integration test order")
                .build();

        MvcResult checkoutResult = mockMvc.perform(post("/api/v1/orders/checkout")
                        .header("Authorization", customerToken)
                        .header("Idempotency-Key", "flow-key-001")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(checkoutReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.orderNumber", startsWith("ORD-")))
                .andExpect(jsonPath("$.data.orderStatus").value("PENDING"))
                .andReturn();

        String responseJson = checkoutResult.getResponse().getContentAsString();
        Long orderId = objectMapper.readTree(responseJson).at("/data/id").asLong();
        String orderNumber = objectMapper.readTree(responseJson).at("/data/orderNumber").asText();
        String razorpayOrderId = objectMapper.readTree(responseJson).at("/data/payment/razorpayOrderId").asText();

        // Verify stock reserved
        Inventory invAfterCheckout = inventoryRepository.findByProductId(testProduct.getId()).orElseThrow();
        assertThat(invAfterCheckout.getQuantityReserved()).isEqualTo(2);
        assertThat(invAfterCheckout.getPurchasableQuantity()).isEqualTo(23);

        // 4. Verify payment
        PaymentVerificationRequest verifyReq = PaymentVerificationRequest.builder()
                .razorpayOrderId(razorpayOrderId)
                .razorpayPaymentId("pay_int_test_123")
                .razorpaySignature("sig_mock_signature")
                .build();

        mockMvc.perform(post("/api/v1/payments/verify")
                        .header("Authorization", customerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(verifyReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.paymentStatus").value("SUCCESS"));

        // Verify stock committed
        Inventory invAfterPayment = inventoryRepository.findByProductId(testProduct.getId()).orElseThrow();
        assertThat(invAfterPayment.getQuantityAvailable()).isEqualTo(23);
        assertThat(invAfterPayment.getQuantityReserved()).isEqualTo(0);

        // 5. Customer view order by ID
        mockMvc.perform(get("/api/v1/orders/" + orderId)
                        .header("Authorization", customerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.orderStatus").value("CONFIRMED"));

        // 6. Public order tracking (sanitized, zero PII)
        mockMvc.perform(get("/api/v1/orders/track/" + orderNumber))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.orderNumber").value(orderNumber))
                .andExpect(jsonPath("$.data.orderStatus").value("CONFIRMED"))
                .andExpect(jsonPath("$.data.totalItems").value(2))
                .andExpect(jsonPath("$.data.deliveryDestination.city").isNotEmpty())
                .andExpect(jsonPath("$.data.user").doesNotExist())
                .andExpect(jsonPath("$.data.payment").doesNotExist())
                .andExpect(jsonPath("$.data.id").doesNotExist())
                .andExpect(jsonPath("$.data.shippingAddress").doesNotExist())
                .andExpect(jsonPath("$.data.billingAddress").doesNotExist());

        // 7. Admin update status to PROCESSING
        mockMvc.perform(patch("/api/v1/orders/admin/" + orderId + "/status")
                        .header("Authorization", adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(UpdateOrderStatusRequest.builder()
                                .status(OrderStatus.PROCESSING)
                                .build())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.orderStatus").value("PROCESSING"));

        // 8. Admin update status to SHIPPED
        mockMvc.perform(patch("/api/v1/orders/admin/" + orderId + "/status")
                        .header("Authorization", adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(UpdateOrderStatusRequest.builder()
                                .status(OrderStatus.SHIPPED)
                                .trackingNumber("DELHIVERY-INT-7788")
                                .build())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.orderStatus").value("SHIPPED"))
                .andExpect(jsonPath("$.data.trackingNumber").value("DELHIVERY-INT-7788"));

        // 9. Customer attempts to cancel shipped order -> 400 Bad Request
        mockMvc.perform(post("/api/v1/orders/" + orderId + "/cancel")
                        .header("Authorization", customerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(CancelOrderRequest.builder().reason("Too late").build())))
                .andExpect(status().isBadRequest());

        // 10. Admin view all orders
        mockMvc.perform(get("/api/v1/orders/admin")
                        .header("Authorization", adminToken)
                        .param("status", "SHIPPED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content", hasSize(greaterThanOrEqualTo(1))));
    }

    @Test
    @DisplayName("Public Razorpay Webhook endpoint accepts and processes payload")
    void testPublicRazorpayWebhook() throws Exception {
        String payload = "{" +
                "\"event\":\"payment.failed\"," +
                "\"account_id\":\"acc_int_test\"," +
                "\"created_at\":1600000000," +
                "\"payload\":{" +
                "  \"payment\":{" +
                "    \"entity\":{" +
                "      \"id\":\"pay_wh_fail\"," +
                "      \"order_id\":\"order_none\"," +
                "      \"status\":\"failed\"" +
                "    }" +
                "  }" +
                "}" +
                "}";

        mockMvc.perform(post("/api/v1/payments/webhook")
                        .header("X-Razorpay-Signature", "sig_mock_signature")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isOk())
                .andExpect(content().string("Webhook processed successfully"));
    }
}
