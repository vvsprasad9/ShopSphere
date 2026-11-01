package com.shopsphere.controller;

import com.shopsphere.entity.*;
import com.shopsphere.entity.enums.AddressType;
import com.shopsphere.entity.enums.OrderStatus;
import com.shopsphere.entity.enums.UserStatus;
import com.shopsphere.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class AdminAnalyticsControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private ProductRepository productRepository;

    private User testCustomer;

    @BeforeEach
    void setUp() {
        testCustomer = userRepository.save(User.builder()
                .email("ctrl_cust_" + UUID.randomUUID() + "@shopsphere.com")
                .password("SecurePass123!")
                .firstName("Mock")
                .lastName("Customer")
                .status(UserStatus.ACTIVE)
                .build());

        Category cat = categoryRepository.save(Category.builder()
                .name("Cat " + UUID.randomUUID())
                .slug("cat-" + UUID.randomUUID())
                .active(true)
                .build());

        productRepository.save(Product.builder()
                .name("Sample Product")
                .slug("sample-" + UUID.randomUUID())
                .sku("SKU-" + UUID.randomUUID())
                .price(new BigDecimal("500.00"))
                .category(cat)
                .active(true)
                .build());

        Order order = Order.builder()
                .orderNumber("ORD-CTRL-" + UUID.randomUUID().toString().substring(0, 6).toUpperCase())
                .user(testCustomer)
                .orderStatus(OrderStatus.CONFIRMED)
                .subtotal(new BigDecimal("500.00"))
                .discountAmount(BigDecimal.ZERO)
                .shippingFee(new BigDecimal("50.00"))
                .taxAmount(new BigDecimal("90.00"))
                .totalAmount(new BigDecimal("640.00"))
                .shippingAddress(AddressSnapshot.builder()
                        .recipientName("Mock Cust")
                        .phoneNumber("9876543210")
                        .addressLine1("Street 1")
                        .city("Bengaluru")
                        .state("Karnataka")
                        .postalCode("560001")
                        .country("India")
                        .build())
                .build();
        orderRepository.save(order);
    }

    @Test
    @DisplayName("RBAC: Unauthenticated request to /overview returns 401 Unauthorized")
    void testUnauthenticatedAccessDenied() throws Exception {
        mockMvc.perform(get("/api/v1/admin/analytics/overview"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "CUSTOMER")
    @DisplayName("RBAC: Customer role to /overview returns 403 Forbidden")
    void testCustomerRoleForbidden() throws Exception {
        mockMvc.perform(get("/api/v1/admin/analytics/overview"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("RBAC: Admin role to /overview returns 200 OK with expected structure")
    void testAdminRoleCanAccessOverview() throws Exception {
        mockMvc.perform(get("/api/v1/admin/analytics/overview")
                        .param("startDate", LocalDate.now().minusDays(7).toString())
                        .param("endDate", LocalDate.now().toString())
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.financials.grossSales").exists())
                .andExpect(jsonPath("$.data.financials.netSales").exists())
                .andExpect(jsonPath("$.data.financials.collectedRevenue").exists())
                .andExpect(jsonPath("$.data.orders.totalOrders").exists())
                .andExpect(jsonPath("$.data.catalog.totalActiveInventoryUnits").exists())
                .andExpect(jsonPath("$.data.customers.totalCustomers").exists());
    }

    @Test
    @WithMockUser(roles = "MANAGER")
    @DisplayName("RBAC: Manager role to /overview returns 200 OK")
    void testManagerRoleCanAccessOverview() throws Exception {
        mockMvc.perform(get("/api/v1/admin/analytics/overview")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("Endpoints: Sales trend, products, categories, inventory, payments, coupons, audit")
    void testOtherAnalyticsEndpoints() throws Exception {
        // Sales trend
        mockMvc.perform(get("/api/v1/admin/analytics/sales")
                        .param("interval", "DAILY"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)));

        // Products
        mockMvc.perform(get("/api/v1/admin/analytics/products")
                        .param("limit", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)));

        // Categories
        mockMvc.perform(get("/api/v1/admin/analytics/categories"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)));

        // Inventory health
        mockMvc.perform(get("/api/v1/admin/analytics/inventory")
                        .param("status", "ALL")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.summary").exists())
                .andExpect(jsonPath("$.data.items").exists());

        // Payments
        mockMvc.perform(get("/api/v1/admin/analytics/payments"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.cashBreakdown").exists());

        // Customers
        mockMvc.perform(get("/api/v1/admin/analytics/customers"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)));

        // Coupons
        mockMvc.perform(get("/api/v1/admin/analytics/coupons"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)));

        // Audit logs
        mockMvc.perform(get("/api/v1/admin/analytics/audit")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("CSV Export: Sets correct content-type text/csv and attachment header")
    void testCsvExportHeaders() throws Exception {
        mockMvc.perform(get("/api/v1/admin/analytics/export/sales"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", org.hamcrest.Matchers.containsString("text/csv")))
                .andExpect(header().string("Content-Disposition", "attachment; filename=\"sales_report.csv\""));

        mockMvc.perform(get("/api/v1/admin/analytics/export/inventory"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", org.hamcrest.Matchers.containsString("text/csv")))
                .andExpect(header().string("Content-Disposition", "attachment; filename=\"inventory_health.csv\""));
    }
}
