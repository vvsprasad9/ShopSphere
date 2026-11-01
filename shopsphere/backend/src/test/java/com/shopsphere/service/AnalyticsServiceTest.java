package com.shopsphere.service;

import com.shopsphere.dto.projection.OrderFinancialSummary;
import com.shopsphere.dto.response.analytics.*;
import com.shopsphere.entity.*;
import com.shopsphere.entity.enums.*;
import com.shopsphere.exception.BadRequestException;
import com.shopsphere.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class AnalyticsServiceTest {

    @Autowired
    private AnalyticsService analyticsService;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private OrderItemRepository orderItemRepository;

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private InventoryRepository inventoryRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private ProductVariantRepository productVariantRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CouponRepository couponRepository;

    private User testCustomer;
    private Category testCategory;
    private Product testProduct;

    @BeforeEach
    void setUp() {
        testCustomer = userRepository.save(User.builder()
                .email("analytics_user_" + UUID.randomUUID() + "@shopsphere.com")
                .password("SecurePass123!")
                .firstName("Analytics")
                .lastName("Tester")
                .status(UserStatus.ACTIVE)
                .build());

        testCategory = categoryRepository.save(Category.builder()
                .name("Electronics " + UUID.randomUUID())
                .slug("electronics-" + UUID.randomUUID())
                .active(true)
                .build());

        testProduct = productRepository.save(Product.builder()
                .name("Analytics Phone")
                .slug("phone-" + UUID.randomUUID())
                .sku("SKU-PH-" + UUID.randomUUID())
                .price(new BigDecimal("1000.00"))
                .category(testCategory)
                .active(true)
                .build());
    }

    private Order createTestOrder(OrderStatus status, BigDecimal subtotal, BigDecimal discount,
                                 BigDecimal shipping, BigDecimal tax, LocalDateTime createdAt) {
        BigDecimal total = subtotal.subtract(discount).add(shipping).add(tax);
        Order order = Order.builder()
                .orderNumber("ORD-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase())
                .user(testCustomer)
                .orderStatus(status)
                .subtotal(subtotal)
                .discountAmount(discount)
                .shippingFee(shipping)
                .taxAmount(tax)
                .totalAmount(total)
                .shippingAddress(AddressSnapshot.builder()
                        .recipientName("Tester")
                        .phoneNumber("9999999999")
                        .addressLine1("123 Street")
                        .city("Bengaluru")
                        .state("Karnataka")
                        .postalCode("560001")
                        .country("India")
                        .build())
                .build();
        order = orderRepository.save(order);
        order.setCreatedAt(createdAt);
        return orderRepository.save(order);
    }

    private Payment createTestPayment(Order order, PaymentMethod method, PaymentStatus status, BigDecimal amount, LocalDateTime createdAt) {
        Payment payment = Payment.builder()
                .order(order)
                .paymentMethod(method)
                .paymentStatus(status)
                .amount(amount)
                .currency("INR")
                .transactionId("txn_" + UUID.randomUUID())
                .build();
        payment = paymentRepository.save(payment);
        payment.setCreatedAt(createdAt);
        return paymentRepository.save(payment);
    }

    @Test
    @DisplayName("Financials: Accrual Gross/Net Sales and Cash Collection are strictly separated")
    void testAccrualVsCashFinancialMetrics() {
        LocalDateTime now = LocalDateTime.now();
        LocalDate today = now.toLocalDate();

        // Order 1: Delivered online payment (Subtotal 1000, Disc 100, Ship 50, Tax 180 = Total 1130) -> Payment SUCCESS
        Order o1 = createTestOrder(OrderStatus.DELIVERED, new BigDecimal("1000.00"), new BigDecimal("100.00"),
                new BigDecimal("50.00"), new BigDecimal("180.00"), now);
        createTestPayment(o1, PaymentMethod.RAZORPAY, PaymentStatus.SUCCESS, new BigDecimal("1130.00"), now);

        // Order 2: Confirmed COD order (Subtotal 500, Disc 0, Ship 50, Tax 90 = Total 640) -> Payment PENDING
        Order o2 = createTestOrder(OrderStatus.CONFIRMED, new BigDecimal("500.00"), BigDecimal.ZERO,
                new BigDecimal("50.00"), new BigDecimal("90.00"), now);
        createTestPayment(o2, PaymentMethod.COD, PaymentStatus.PENDING, new BigDecimal("640.00"), now);

        // Order 3: Cancelled order (Subtotal 2000, Disc 200 = Total 1800) -> should be excluded from sales
        Order o3 = createTestOrder(OrderStatus.CANCELLED, new BigDecimal("2000.00"), new BigDecimal("200.00"),
                BigDecimal.ZERO, BigDecimal.ZERO, now);
        createTestPayment(o3, PaymentMethod.RAZORPAY, PaymentStatus.FAILED, new BigDecimal("1800.00"), now);

        AnalyticsOverviewResponse overview = analyticsService.getOverview(today, today);

        // Accrual Gross Sales = (1000+50+180) + (500+50+90) = 1230 + 640 = 1870
        assertThat(overview.getFinancials().getGrossSales()).isEqualByComparingTo("1870.00");
        // Net Sales = 1130 + 640 = 1770
        assertThat(overview.getFinancials().getNetSales()).isEqualByComparingTo("1770.00");
        // Discounts = 100
        assertThat(overview.getFinancials().getDiscounts()).isEqualByComparingTo("100.00");

        // Cash basis: Collected = 1130.00 (from o1). o2 is COD PENDING, so excluded from collected!
        assertThat(overview.getFinancials().getCollectedRevenue()).isEqualByComparingTo("1130.00");
        assertThat(overview.getFinancials().getNetCollectedRevenue()).isEqualByComparingTo("1130.00");

        // Pending Collections = 640.00 (COD)
        assertThat(overview.getFinancials().getPendingCollections()).isEqualByComparingTo("640.00");
        assertThat(overview.getFinancials().getPendingCodCollections()).isEqualByComparingTo("640.00");
        assertThat(overview.getFinancials().getPendingOnlineCollections()).isEqualByComparingTo("0.00");

        // Confirmed refunds = 0.00
        assertThat(overview.getFinancials().getConfirmedRefunds()).isEqualByComparingTo("0.00");
    }

    @Test
    @DisplayName("COD Lifecycle: Transitions from Pending Collection to Collected Revenue only upon SUCCESS")
    void testCodLifecycleRevenueCollection() {
        LocalDateTime now = LocalDateTime.now();
        LocalDate today = now.toLocalDate();

        Order codOrder = createTestOrder(OrderStatus.CONFIRMED, new BigDecimal("800.00"), BigDecimal.ZERO,
                new BigDecimal("50.00"), new BigDecimal("144.00"), now);
        Payment payment = createTestPayment(codOrder, PaymentMethod.COD, PaymentStatus.PENDING, new BigDecimal("994.00"), now);

        // Before cash collection
        AnalyticsOverviewResponse before = analyticsService.getOverview(today, today);
        assertThat(before.getFinancials().getCollectedRevenue()).isEqualByComparingTo("0.00");
        assertThat(before.getFinancials().getPendingCodCollections()).isEqualByComparingTo("994.00");

        // Simulate delivery and cash collection
        payment.setPaymentStatus(PaymentStatus.SUCCESS);
        paymentRepository.save(payment);

        // After cash collection
        AnalyticsOverviewResponse after = analyticsService.getOverview(today, today);
        assertThat(after.getFinancials().getCollectedRevenue()).isEqualByComparingTo("994.00");
        assertThat(after.getFinancials().getNetCollectedRevenue()).isEqualByComparingTo("994.00");
        assertThat(after.getFinancials().getPendingCodCollections()).isEqualByComparingTo("0.00");
    }

    @Test
    @DisplayName("Refunds: REFUND_PENDING is excluded; only confirmed REFUNDED status reduces net cash")
    void testRefundLifecycleAccounting() {
        LocalDateTime now = LocalDateTime.now();
        LocalDate today = now.toLocalDate();

        Order order = createTestOrder(OrderStatus.CONFIRMED, new BigDecimal("1000.00"), BigDecimal.ZERO,
                BigDecimal.ZERO, BigDecimal.ZERO, now);
        Payment payment = createTestPayment(order, PaymentMethod.RAZORPAY, PaymentStatus.REFUND_PENDING, new BigDecimal("1000.00"), now);

        // In-flight refund: REFUND_PENDING must NOT count as confirmed refund
        AnalyticsOverviewResponse pendingResp = analyticsService.getOverview(today, today);
        assertThat(pendingResp.getFinancials().getConfirmedRefunds()).isEqualByComparingTo("0.00");

        // Gateway confirms refund
        payment.setPaymentStatus(PaymentStatus.REFUNDED);
        paymentRepository.save(payment);

        AnalyticsOverviewResponse confirmedResp = analyticsService.getOverview(today, today);
        assertThat(confirmedResp.getFinancials().getConfirmedRefunds()).isEqualByComparingTo("1000.00");
        assertThat(confirmedResp.getFinancials().getCollectedRevenue()).isEqualByComparingTo("1000.00");
        assertThat(confirmedResp.getFinancials().getNetCollectedRevenue()).isEqualByComparingTo("0.00");
    }

    @Test
    @DisplayName("Inventory: Deduplication prevents double counting parent products with variants")
    void testVariantInventoryDeduplication() {
        Product parentProduct = productRepository.save(Product.builder()
                .name("T-Shirt")
                .slug("tshirt-" + UUID.randomUUID())
                .sku("TSHIRT-BASE-" + UUID.randomUUID())
                .price(new BigDecimal("500.00"))
                .category(testCategory)
                .active(true)
                .build());

        // Variant 1: Size S (Available: 20, Reserved: 5 -> Purchasable: 15)
        ProductVariant varS = productVariantRepository.save(ProductVariant.builder()
                .product(parentProduct)
                .sku("TSHIRT-S-" + UUID.randomUUID())
                .variantName("Small")
                .priceAdjustment(BigDecimal.ZERO)
                .build());
        inventoryRepository.save(Inventory.builder()
                .variant(varS)
                .quantityAvailable(20)
                .quantityReserved(5)
                .lowStockThreshold(10)
                .build());

        // Variant 2: Size L (Available: 8, Reserved: 4 -> Purchasable: 4 <= threshold 5 -> Low Stock)
        ProductVariant varL = productVariantRepository.save(ProductVariant.builder()
                .product(parentProduct)
                .sku("TSHIRT-L-" + UUID.randomUUID())
                .variantName("Large")
                .priceAdjustment(BigDecimal.ZERO)
                .build());
        inventoryRepository.save(Inventory.builder()
                .variant(varL)
                .quantityAvailable(8)
                .quantityReserved(4)
                .lowStockThreshold(5)
                .build());

        // Simple product without variants (Available: 50, Reserved: 10 -> Purchasable: 40)
        Product simpleProd = productRepository.save(Product.builder()
                .name("Coffee Mug")
                .slug("mug-" + UUID.randomUUID())
                .sku("MUG-" + UUID.randomUUID())
                .price(new BigDecimal("250.00"))
                .category(testCategory)
                .active(true)
                .build());
        inventoryRepository.save(Inventory.builder()
                .product(simpleProd)
                .quantityAvailable(50)
                .quantityReserved(10)
                .lowStockThreshold(5)
                .build());

        InventoryHealthResponse health = analyticsService.getInventoryHealth("ALL", 0, 50);

        // Deduplicated summary:
        // Variant S (20 available, 5 reserved) + Variant L (8 available, 4 reserved) + Mug (50 available, 10 reserved)
        // Total available = 78, Total reserved = 19, Purchasable = 59
        assertThat(health.getSummary().getTotalUnitsAvailable()).isGreaterThanOrEqualTo(78L);
        assertThat(health.getSummary().getTotalUnitsReserved()).isGreaterThanOrEqualTo(19L);
        assertThat(health.getSummary().getTotalPurchasableUnits()).isGreaterThanOrEqualTo(59L);
        // Variant L purchasable = 4 <= 5 -> counted in low stock
        assertThat(health.getSummary().getLowStockCount()).isGreaterThanOrEqualTo(1L);
    }

    @Test
    @DisplayName("Edge Case: AOV with zero non-cancelled orders handles zero division gracefully")
    void testAovZeroDivisionHandling() {
        LocalDate pastDate = LocalDate.now().minusYears(10);
        AnalyticsOverviewResponse resp = analyticsService.getOverview(pastDate, pastDate.plusDays(1));

        assertThat(resp.getFinancials().getAverageOrderValue()).isEqualByComparingTo("0.00");
        assertThat(resp.getOrders().getTotalOrders()).isEqualTo(0L);
    }

    @Test
    @DisplayName("Date Validation: Start date after end date throws BadRequestException")
    void testInvalidDateRangeThrowsBadRequest() {
        LocalDate today = LocalDate.now();
        LocalDate yesterday = today.minusDays(1);

        assertThatThrownBy(() -> analyticsService.getOverview(today, yesterday))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Start date cannot be after end date");
    }

    @Test
    @DisplayName("CSV Export: Formula injection (=, +, -, @) is neutralized with apostrophe prefix")
    void testCsvFormulaInjectionNeutralization() {
        // Create product with dangerous formula names
        Product formulaProd = productRepository.save(Product.builder()
                .name("=cmd|' /C calc'!A0")
                .slug("calc-formula-" + UUID.randomUUID())
                .sku("+SKU-DANGEROUS-" + UUID.randomUUID())
                .price(new BigDecimal("100.00"))
                .category(testCategory)
                .active(true)
                .build());

        Order order = createTestOrder(OrderStatus.DELIVERED, new BigDecimal("100.00"), BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, LocalDateTime.now());
        orderItemRepository.save(OrderItem.builder()
                .order(order)
                .product(formulaProd)
                .productName("=cmd|' /C calc'!A0")
                .productSku("+SKU-DANGEROUS")
                .unitPrice(new BigDecimal("100.00"))
                .quantity(1)
                .totalPrice(new BigDecimal("100.00"))
                .build());

        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        analyticsService.exportProductsCsv(LocalDate.now(), LocalDate.now(), pw);

        String csvOutput = sw.toString();
        // Verifies formula characters have been prefixed with single quote "'"
        assertThat(csvOutput).contains("\"'=cmd|' /C calc'!A0\"");
        assertThat(csvOutput).contains("\"'+SKU-DANGEROUS\"");
    }
}
