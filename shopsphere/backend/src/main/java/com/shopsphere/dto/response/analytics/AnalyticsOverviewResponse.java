package com.shopsphere.dto.response.analytics;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AnalyticsOverviewResponse {

    private FinancialMetrics financials;
    private OrderMetrics orders;
    private CatalogMetrics catalog;
    private CustomerMetrics customers;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FinancialMetrics {
        private BigDecimal grossSales;
        private BigDecimal netSales;
        private BigDecimal discounts;
        private BigDecimal tax;
        private BigDecimal shippingCharges;
        private BigDecimal collectedRevenue;
        private BigDecimal pendingCollections;
        private BigDecimal pendingCodCollections;
        private BigDecimal pendingOnlineCollections;
        private BigDecimal confirmedRefunds;
        private BigDecimal netCollectedRevenue;
        private BigDecimal averageOrderValue;

        // Period comparisons
        private BigDecimal previousPeriodGrossSales;
        private BigDecimal previousPeriodNetCollectedRevenue;
        private BigDecimal grossSalesChangePercent;
        private BigDecimal netCollectedRevenueChangePercent;
        private BigDecimal orderCountChangePercent;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OrderMetrics {
        private long totalOrders;
        private long pendingOrders;
        private long confirmedOrders;
        private long processingOrders;
        private long shippedOrders;
        private long deliveredOrders;
        private long cancelledOrders;
        private Map<String, Long> statusBreakdown;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CatalogMetrics {
        private long totalProducts;
        private long totalActiveInventoryUnits;
        private long totalReservedUnits;
        private long totalPurchasableUnits;
        private long lowStockItemCount;
        private long outOfStockItemCount;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CustomerMetrics {
        private long totalCustomers;
        private long newCustomersInPeriod;
        private long returningBuyersCount;
    }
}
