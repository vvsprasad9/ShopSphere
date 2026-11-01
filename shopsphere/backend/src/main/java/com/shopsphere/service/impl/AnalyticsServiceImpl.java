package com.shopsphere.service.impl;

import com.shopsphere.dto.projection.OrderFinancialSummary;
import com.shopsphere.dto.response.analytics.*;
import com.shopsphere.entity.*;
import com.shopsphere.entity.enums.DiscountType;
import com.shopsphere.entity.enums.OrderStatus;
import com.shopsphere.entity.enums.PaymentMethod;
import com.shopsphere.entity.enums.PaymentStatus;
import com.shopsphere.exception.BadRequestException;
import com.shopsphere.repository.*;
import com.shopsphere.service.AnalyticsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.PrintWriter;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AnalyticsServiceImpl implements AnalyticsService {

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final PaymentRepository paymentRepository;
    private final InventoryRepository inventoryRepository;
    private final UserRepository userRepository;
    private final AuditLogRepository auditLogRepository;
    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;

    @Value("${spring.profiles.active:dev}")
    private String activeProfile;

    private boolean isProduction() {
        return activeProfile != null && (activeProfile.equalsIgnoreCase("prod") || activeProfile.equalsIgnoreCase("production"));
    }

    private DateRange parseDateRange(LocalDate startDate, LocalDate endDate) {
        if (startDate == null && endDate == null) {
            endDate = LocalDate.now();
            startDate = endDate.minusDays(29);
        } else if (startDate == null) {
            startDate = endDate.minusDays(29);
        } else if (endDate == null) {
            endDate = startDate.plusDays(29);
        }

        if (startDate.isAfter(endDate)) {
            throw new BadRequestException("Start date cannot be after end date");
        }

        LocalDateTime startInclusive = startDate.atStartOfDay();
        LocalDateTime endExclusive = endDate.plusDays(1).atStartOfDay();

        long days = ChronoUnit.DAYS.between(startDate, endDate) + 1;
        LocalDateTime prevStartInclusive = startInclusive.minusDays(days);
        LocalDateTime prevEndExclusive = startInclusive;

        return new DateRange(startInclusive, endExclusive, prevStartInclusive, prevEndExclusive, days);
    }

    private record DateRange(
            LocalDateTime startInclusive,
            LocalDateTime endExclusive,
            LocalDateTime prevStartInclusive,
            LocalDateTime prevEndExclusive,
            long days
    ) {}

    private BigDecimal calculatePercentageChange(BigDecimal current, BigDecimal previous) {
        if (current == null) current = BigDecimal.ZERO;
        if (previous == null) previous = BigDecimal.ZERO;

        if (previous.compareTo(BigDecimal.ZERO) == 0) {
            if (current.compareTo(BigDecimal.ZERO) == 0) {
                return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
            }
            return BigDecimal.valueOf(100).setScale(2, RoundingMode.HALF_UP);
        }

        return current.subtract(previous)
                .divide(previous, 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100))
                .setScale(2, RoundingMode.HALF_UP);
    }

    @Override
    public AnalyticsOverviewResponse getOverview(LocalDate startDate, LocalDate endDate) {
        DateRange range = parseDateRange(startDate, endDate);

        // 1. Financials from Order summaries
        OrderFinancialSummary summary = orderRepository.getFinancialSummary(range.startInclusive(), range.endExclusive());
        BigDecimal grossSales = summary.getSubtotal().add(summary.getShippingFee()).add(summary.getTaxAmount());
        BigDecimal discounts = summary.getDiscountAmount();
        BigDecimal netSales = summary.getTotalAmount();
        BigDecimal tax = summary.getTaxAmount();
        BigDecimal shipping = summary.getShippingFee();
        long orderCount = summary.getOrderCount();
        BigDecimal aov = orderCount > 0
                ? netSales.divide(BigDecimal.valueOf(orderCount), 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);

        // Previous period financial summary
        OrderFinancialSummary prevSummary = orderRepository.getFinancialSummary(range.prevStartInclusive(), range.prevEndExclusive());
        BigDecimal prevGrossSales = prevSummary.getSubtotal().add(prevSummary.getShippingFee()).add(prevSummary.getTaxAmount());
        long prevOrderCount = prevSummary.getOrderCount();

        // 2. Cash collections from Payments
        List<Object[]> currentPayments = paymentRepository.getPaymentBreakdownBetween(range.startInclusive(), range.endExclusive());
        PaymentBreakdownMetrics currentCash = aggregatePaymentMetrics(currentPayments);

        List<Object[]> prevPayments = paymentRepository.getPaymentBreakdownBetween(range.prevStartInclusive(), range.prevEndExclusive());
        PaymentBreakdownMetrics prevCash = aggregatePaymentMetrics(prevPayments);

        AnalyticsOverviewResponse.FinancialMetrics financialMetrics = AnalyticsOverviewResponse.FinancialMetrics.builder()
                .grossSales(grossSales.setScale(2, RoundingMode.HALF_UP))
                .netSales(netSales.setScale(2, RoundingMode.HALF_UP))
                .discounts(discounts.setScale(2, RoundingMode.HALF_UP))
                .tax(tax.setScale(2, RoundingMode.HALF_UP))
                .shippingCharges(shipping.setScale(2, RoundingMode.HALF_UP))
                .collectedRevenue(currentCash.collectedRevenue.setScale(2, RoundingMode.HALF_UP))
                .pendingCollections(currentCash.pendingCollections.setScale(2, RoundingMode.HALF_UP))
                .pendingCodCollections(currentCash.pendingCod.setScale(2, RoundingMode.HALF_UP))
                .pendingOnlineCollections(currentCash.pendingOnline.setScale(2, RoundingMode.HALF_UP))
                .confirmedRefunds(currentCash.confirmedRefunds.setScale(2, RoundingMode.HALF_UP))
                .netCollectedRevenue(currentCash.netCollectedRevenue.setScale(2, RoundingMode.HALF_UP))
                .averageOrderValue(aov)
                .previousPeriodGrossSales(prevGrossSales.setScale(2, RoundingMode.HALF_UP))
                .previousPeriodNetCollectedRevenue(prevCash.netCollectedRevenue.setScale(2, RoundingMode.HALF_UP))
                .grossSalesChangePercent(calculatePercentageChange(grossSales, prevGrossSales))
                .netCollectedRevenueChangePercent(calculatePercentageChange(currentCash.netCollectedRevenue, prevCash.netCollectedRevenue))
                .orderCountChangePercent(calculatePercentageChange(BigDecimal.valueOf(orderCount), BigDecimal.valueOf(prevOrderCount)))
                .build();

        // 3. Order Status Metrics
        List<Object[]> statusCounts = orderRepository.countOrdersByStatusBetween(range.startInclusive(), range.endExclusive());
        Map<String, Long> statusMap = new HashMap<>();
        for (Object[] row : statusCounts) {
            if (row[0] != null && row[1] != null) {
                statusMap.put(row[0].toString(), ((Number) row[1]).longValue());
            }
        }

        long totalOrders = statusMap.values().stream().mapToLong(Long::longValue).sum();
        AnalyticsOverviewResponse.OrderMetrics orderMetrics = AnalyticsOverviewResponse.OrderMetrics.builder()
                .totalOrders(totalOrders)
                .pendingOrders(statusMap.getOrDefault(OrderStatus.PENDING.name(), 0L))
                .confirmedOrders(statusMap.getOrDefault(OrderStatus.CONFIRMED.name(), 0L))
                .processingOrders(statusMap.getOrDefault(OrderStatus.PROCESSING.name(), 0L))
                .shippedOrders(statusMap.getOrDefault(OrderStatus.SHIPPED.name(), 0L))
                .deliveredOrders(statusMap.getOrDefault(OrderStatus.DELIVERED.name(), 0L))
                .cancelledOrders(statusMap.getOrDefault(OrderStatus.CANCELLED.name(), 0L))
                .statusBreakdown(statusMap)
                .build();

        // 4. Catalog & Deduplicated Inventory Metrics
        List<Object[]> invSummaryList = inventoryRepository.getDeduplicatedInventorySummary();
        long available = 0, reserved = 0, outOfStock = 0, lowStock = 0;
        if (!invSummaryList.isEmpty()) {
            Object[] row = invSummaryList.get(0);
            available = row[0] != null ? ((Number) row[0]).longValue() : 0L;
            reserved = row[1] != null ? ((Number) row[1]).longValue() : 0L;
            outOfStock = row[2] != null ? ((Number) row[2]).longValue() : 0L;
            lowStock = row[3] != null ? ((Number) row[3]).longValue() : 0L;
        }
        long purchasable = Math.max(0, available - reserved);

        AnalyticsOverviewResponse.CatalogMetrics catalogMetrics = AnalyticsOverviewResponse.CatalogMetrics.builder()
                .totalProducts(productRepository.count())
                .totalActiveInventoryUnits(available)
                .totalReservedUnits(reserved)
                .totalPurchasableUnits(purchasable)
                .lowStockItemCount(lowStock)
                .outOfStockItemCount(outOfStock)
                .build();

        // 5. Customer Metrics
        long totalCustomers = userRepository.count();
        long newCustomers = userRepository.countByCreatedAtGreaterThanEqualAndCreatedAtLessThan(range.startInclusive(), range.endExclusive());

        // Count returning buyers who placed an order in this period and have >= 2 lifetime orders
        List<Long> periodUsers = orderRepository.getDistinctUserIdsWithOrdersBetween(range.startInclusive(), range.endExclusive());
        List<Object[]> customerLifetimeCounts = orderRepository.getCustomerTotalOrderCounts();
        Map<Long, Long> lifetimeMap = new HashMap<>();
        for (Object[] row : customerLifetimeCounts) {
            if (row[0] != null && row[1] != null) {
                lifetimeMap.put(((Number) row[0]).longValue(), ((Number) row[1]).longValue());
            }
        }
        long returningBuyers = periodUsers.stream()
                .filter(uId -> lifetimeMap.getOrDefault(uId, 0L) >= 2)
                .count();

        AnalyticsOverviewResponse.CustomerMetrics customerMetrics = AnalyticsOverviewResponse.CustomerMetrics.builder()
                .totalCustomers(totalCustomers)
                .newCustomersInPeriod(newCustomers)
                .returningBuyersCount(returningBuyers)
                .build();

        return AnalyticsOverviewResponse.builder()
                .financials(financialMetrics)
                .orders(orderMetrics)
                .catalog(catalogMetrics)
                .customers(customerMetrics)
                .build();
    }

    private record PaymentBreakdownMetrics(
            BigDecimal collectedRevenue,
            BigDecimal pendingCollections,
            BigDecimal pendingCod,
            BigDecimal pendingOnline,
            BigDecimal confirmedRefunds,
            BigDecimal netCollectedRevenue
    ) {}

    private PaymentBreakdownMetrics aggregatePaymentMetrics(List<Object[]> paymentRows) {
        BigDecimal collected = BigDecimal.ZERO;
        BigDecimal pendingCod = BigDecimal.ZERO;
        BigDecimal pendingOnline = BigDecimal.ZERO;
        BigDecimal refunds = BigDecimal.ZERO;

        boolean filterMock = isProduction();

        for (Object[] row : paymentRows) {
            if (row[0] == null || row[1] == null || row[3] == null) continue;
            PaymentMethod method = (PaymentMethod) row[0];
            PaymentStatus status = (PaymentStatus) row[1];
            BigDecimal amount = new BigDecimal(row[3].toString());

            if (filterMock && method == PaymentMethod.MOCK_TEST) {
                continue;
            }

            if (status == PaymentStatus.SUCCESS) {
                collected = collected.add(amount);
            } else if (status == PaymentStatus.REFUNDED) {
                refunds = refunds.add(amount);
                collected = collected.add(amount); // Gross inflow before refund
            } else if (status == PaymentStatus.PENDING) {
                if (method == PaymentMethod.COD) {
                    pendingCod = pendingCod.add(amount);
                } else {
                    pendingOnline = pendingOnline.add(amount);
                }
            }
        }

        BigDecimal pendingTotal = pendingCod.add(pendingOnline);
        BigDecimal netCollected = collected.subtract(refunds);

        return new PaymentBreakdownMetrics(collected, pendingTotal, pendingCod, pendingOnline, refunds, netCollected);
    }

    @Override
    public List<SalesTrendResponse> getSalesTrend(LocalDate startDate, LocalDate endDate, String interval) {
        DateRange range = parseDateRange(startDate, endDate);

        // Fetch all non-cancelled orders in period
        List<Order> orders = orderRepository.findAll((root, query, cb) -> cb.and(
                cb.notEqual(root.get("orderStatus"), OrderStatus.CANCELLED),
                cb.greaterThanOrEqualTo(root.get("createdAt"), range.startInclusive()),
                cb.lessThan(root.get("createdAt"), range.endExclusive())
        ));

        // Group by requested interval: DAILY, WEEKLY, MONTHLY
        DateTimeFormatter formatter;
        if ("MONTHLY".equalsIgnoreCase(interval)) {
            formatter = DateTimeFormatter.ofPattern("yyyy-MM");
        } else if ("WEEKLY".equalsIgnoreCase(interval)) {
            formatter = DateTimeFormatter.ofPattern("yyyy-'W'w");
        } else {
            formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        }

        Map<String, List<Order>> grouped = orders.stream()
                .collect(Collectors.groupingBy(o -> o.getCreatedAt().format(formatter), TreeMap::new, Collectors.toList()));

        List<SalesTrendResponse> result = new ArrayList<>();
        for (Map.Entry<String, List<Order>> entry : grouped.entrySet()) {
            String label = entry.getKey();
            List<Order> bucketOrders = entry.getValue();

            BigDecimal gross = BigDecimal.ZERO;
            BigDecimal net = BigDecimal.ZERO;
            BigDecimal disc = BigDecimal.ZERO;
            BigDecimal tax = BigDecimal.ZERO;
            BigDecimal ship = BigDecimal.ZERO;
            long count = bucketOrders.size();

            for (Order o : bucketOrders) {
                BigDecimal sub = o.getSubtotal() != null ? o.getSubtotal() : BigDecimal.ZERO;
                BigDecimal sFee = o.getShippingFee() != null ? o.getShippingFee() : BigDecimal.ZERO;
                BigDecimal tAmt = o.getTaxAmount() != null ? o.getTaxAmount() : BigDecimal.ZERO;
                BigDecimal dAmt = o.getDiscountAmount() != null ? o.getDiscountAmount() : BigDecimal.ZERO;
                BigDecimal tot = o.getTotalAmount() != null ? o.getTotalAmount() : BigDecimal.ZERO;

                gross = gross.add(sub).add(sFee).add(tAmt);
                net = net.add(tot);
                disc = disc.add(dAmt);
                tax = tax.add(tAmt);
                ship = ship.add(sFee);
            }

            BigDecimal aov = count > 0
                    ? net.divide(BigDecimal.valueOf(count), 2, RoundingMode.HALF_UP)
                    : BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);

            // Compute payments associated with these orders
            BigDecimal collected = BigDecimal.ZERO;
            BigDecimal refunds = BigDecimal.ZERO;
            for (Order o : bucketOrders) {
                Payment p = o.getPayment();
                if (p != null) {
                    if (isProduction() && p.getPaymentMethod() == PaymentMethod.MOCK_TEST) continue;
                    if (p.getPaymentStatus() == PaymentStatus.SUCCESS) {
                        collected = collected.add(p.getAmount());
                    } else if (p.getPaymentStatus() == PaymentStatus.REFUNDED) {
                        collected = collected.add(p.getAmount());
                        refunds = refunds.add(p.getAmount());
                    }
                }
            }
            BigDecimal netCollected = collected.subtract(refunds);

            result.add(SalesTrendResponse.builder()
                    .periodLabel(label)
                    .orderCount(count)
                    .grossSales(gross.setScale(2, RoundingMode.HALF_UP))
                    .netSales(net.setScale(2, RoundingMode.HALF_UP))
                    .discounts(disc.setScale(2, RoundingMode.HALF_UP))
                    .tax(tax.setScale(2, RoundingMode.HALF_UP))
                    .shippingCharges(ship.setScale(2, RoundingMode.HALF_UP))
                    .collectedRevenue(collected.setScale(2, RoundingMode.HALF_UP))
                    .confirmedRefunds(refunds.setScale(2, RoundingMode.HALF_UP))
                    .netCollectedRevenue(netCollected.setScale(2, RoundingMode.HALF_UP))
                    .averageOrderValue(aov)
                    .build());
        }

        return result;
    }

    @Override
    public List<ProductPerformanceResponse> getProductPerformance(LocalDate startDate, LocalDate endDate, int limit) {
        DateRange range = parseDateRange(startDate, endDate);
        int pageLimit = limit > 0 ? limit : 20;

        List<Object[]> topProducts = orderItemRepository.getTopSellingProducts(
                range.startInclusive(), range.endExclusive(), PageRequest.of(0, pageLimit));

        List<ProductPerformanceResponse> responses = new ArrayList<>();
        for (Object[] row : topProducts) {
            Long productId = row[0] != null ? ((Number) row[0]).longValue() : null;
            String productName = row[1] != null ? row[1].toString() : "Unknown";
            String productSku = row[2] != null ? row[2].toString() : "";
            long unitsSold = row[3] != null ? ((Number) row[3]).longValue() : 0L;
            BigDecimal revenue = row[4] != null ? new BigDecimal(row[4].toString()) : BigDecimal.ZERO;

            Product p = productId != null ? productRepository.findById(productId).orElse(null) : null;
            String categoryName = (p != null && p.getCategory() != null) ? p.getCategory().getName() : "Unassigned";
            Double rating = p != null ? p.getAverageRating() : 0.0;
            Integer reviewCount = p != null ? p.getReviewCount() : 0;

            int purchasableStock = 0;
            if (p != null && p.getInventory() != null) {
                purchasableStock = p.getInventory().getPurchasableQuantity();
            }

            responses.add(ProductPerformanceResponse.builder()
                    .productId(productId)
                    .productName(productName)
                    .sku(productSku)
                    .categoryName(categoryName)
                    .unitsSold(unitsSold)
                    .totalRevenue(revenue.setScale(2, RoundingMode.HALF_UP))
                    .currentPurchasableStock(purchasableStock)
                    .averageRating(rating)
                    .reviewCount(reviewCount)
                    .build());
        }

        return responses;
    }

    @Override
    public List<CategorySalesResponse> getCategorySales(LocalDate startDate, LocalDate endDate) {
        DateRange range = parseDateRange(startDate, endDate);
        List<Object[]> rows = orderItemRepository.getCategorySalesBetween(range.startInclusive(), range.endExclusive());

        BigDecimal grandTotal = BigDecimal.ZERO;
        List<CategorySalesResponse> list = new ArrayList<>();

        for (Object[] row : rows) {
            Long catId = row[0] != null ? ((Number) row[0]).longValue() : null;
            String catName = row[1] != null ? row[1].toString() : "Unknown";
            long units = row[2] != null ? ((Number) row[2]).longValue() : 0L;
            BigDecimal rev = row[3] != null ? new BigDecimal(row[3].toString()) : BigDecimal.ZERO;

            grandTotal = grandTotal.add(rev);
            list.add(CategorySalesResponse.builder()
                    .categoryId(catId)
                    .categoryName(catName)
                    .unitsSold(units)
                    .totalRevenue(rev.setScale(2, RoundingMode.HALF_UP))
                    .percentageOfTotalSales(BigDecimal.ZERO)
                    .build());
        }

        if (grandTotal.compareTo(BigDecimal.ZERO) > 0) {
            for (CategorySalesResponse item : list) {
                BigDecimal pct = item.getTotalRevenue()
                        .divide(grandTotal, 4, RoundingMode.HALF_UP)
                        .multiply(BigDecimal.valueOf(100))
                        .setScale(2, RoundingMode.HALF_UP);
                item.setPercentageOfTotalSales(pct);
            }
        }

        return list;
    }

    @Override
    public InventoryHealthResponse getInventoryHealth(String status, int page, int size) {
        Pageable pageable = PageRequest.of(Math.max(0, page), Math.max(1, size));

        // Deduplicated summary metrics
        List<Object[]> summaryList = inventoryRepository.getDeduplicatedInventorySummary();
        long available = 0, reserved = 0, outOfStock = 0, lowStock = 0;
        if (!summaryList.isEmpty()) {
            Object[] row = summaryList.get(0);
            available = row[0] != null ? ((Number) row[0]).longValue() : 0L;
            reserved = row[1] != null ? ((Number) row[1]).longValue() : 0L;
            outOfStock = row[2] != null ? ((Number) row[2]).longValue() : 0L;
            lowStock = row[3] != null ? ((Number) row[3]).longValue() : 0L;
        }
        long purchasable = Math.max(0, available - reserved);

        InventoryHealthResponse.InventorySummary summary = InventoryHealthResponse.InventorySummary.builder()
                .totalUnitsAvailable(available)
                .totalUnitsReserved(reserved)
                .totalPurchasableUnits(purchasable)
                .lowStockCount(lowStock)
                .outOfStockCount(outOfStock)
                .build();

        // Paginated items by status filter
        Page<Inventory> invPage;
        if ("LOW_STOCK".equalsIgnoreCase(status)) {
            invPage = inventoryRepository.findDeduplicatedLowStockInventories(pageable);
        } else if ("OUT_OF_STOCK".equalsIgnoreCase(status)) {
            invPage = inventoryRepository.findDeduplicatedOutOfStockInventories(pageable);
        } else {
            invPage = inventoryRepository.findDeduplicatedInventories(pageable);
        }

        List<InventoryHealthResponse.InventoryItemDto> itemDtos = invPage.getContent().stream().map(inv -> {
            String itemName = inv.getVariant() != null
                    ? inv.getVariant().getProduct().getName() + " (" + inv.getVariant().getVariantName() + ")"
                    : (inv.getProduct() != null ? inv.getProduct().getName() : "Unknown");

            String sku = inv.getVariant() != null
                    ? inv.getVariant().getSku()
                    : (inv.getProduct() != null ? inv.getProduct().getSku() : "");

            String catName = inv.getVariant() != null && inv.getVariant().getProduct().getCategory() != null
                    ? inv.getVariant().getProduct().getCategory().getName()
                    : (inv.getProduct() != null && inv.getProduct().getCategory() != null ? inv.getProduct().getCategory().getName() : "Unassigned");

            int purch = inv.getPurchasableQuantity();
            String stockStatus = purch <= 0 ? "OUT_OF_STOCK" : (purch <= inv.getLowStockThreshold() ? "LOW_STOCK" : "ADEQUATE");

            return InventoryHealthResponse.InventoryItemDto.builder()
                    .inventoryId(inv.getId())
                    .productId(inv.getProduct() != null ? inv.getProduct().getId() : (inv.getVariant() != null ? inv.getVariant().getProduct().getId() : null))
                    .variantId(inv.getVariant() != null ? inv.getVariant().getId() : null)
                    .itemName(itemName)
                    .sku(sku)
                    .categoryName(catName)
                    .quantityAvailable(inv.getQuantityAvailable())
                    .quantityReserved(inv.getQuantityReserved())
                    .purchasableQuantity(purch)
                    .lowStockThreshold(inv.getLowStockThreshold())
                    .stockStatus(stockStatus)
                    .build();
        }).toList();

        return InventoryHealthResponse.builder()
                .summary(summary)
                .items(new PageImpl<>(itemDtos, pageable, invPage.getTotalElements()))
                .build();
    }

    @Override
    public CustomerAnalyticsResponse getCustomerAnalytics(LocalDate startDate, LocalDate endDate) {
        DateRange range = parseDateRange(startDate, endDate);

        long totalCustomers = userRepository.count();
        long newCustomers = userRepository.countByCreatedAtGreaterThanEqualAndCreatedAtLessThan(range.startInclusive(), range.endExclusive());

        List<Object[]> lifetimeCounts = orderRepository.getCustomerTotalOrderCounts();
        long totalBuyers = lifetimeCounts.size();
        long returningBuyers = 0;
        long oneOrder = 0, twoOrThree = 0, fourPlus = 0;

        for (Object[] row : lifetimeCounts) {
            long count = ((Number) row[1]).longValue();
            if (count >= 2) returningBuyers++;
            if (count == 1) oneOrder++;
            else if (count <= 3) twoOrThree++;
            else fourPlus++;
        }

        Map<String, Long> distribution = new LinkedHashMap<>();
        distribution.put("1 order", oneOrder);
        distribution.put("2-3 orders", twoOrThree);
        distribution.put("4+ orders", fourPlus);

        OrderFinancialSummary summary = orderRepository.getFinancialSummary(range.startInclusive(), range.endExclusive());
        BigDecimal aov = summary.getOrderCount() > 0
                ? summary.getTotalAmount().divide(BigDecimal.valueOf(summary.getOrderCount()), 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);

        return CustomerAnalyticsResponse.builder()
                .totalCustomers(totalCustomers)
                .newCustomersInPeriod(newCustomers)
                .totalBuyers(totalBuyers)
                .returningBuyersCount(returningBuyers)
                .averageOrderValue(aov)
                .orderFrequencyDistribution(distribution)
                .build();
    }

    @Override
    public PaymentAnalyticsResponse getPaymentAnalytics(LocalDate startDate, LocalDate endDate) {
        DateRange range = parseDateRange(startDate, endDate);
        List<Object[]> rows = paymentRepository.getPaymentBreakdownBetween(range.startInclusive(), range.endExclusive());

        PaymentBreakdownMetrics cashMetrics = aggregatePaymentMetrics(rows);

        Map<String, Long> methodCounts = new HashMap<>();
        Map<String, BigDecimal> methodTotals = new HashMap<>();
        Map<String, Long> statusCounts = new HashMap<>();
        Map<String, BigDecimal> statusTotals = new HashMap<>();
        BigDecimal totalVolume = BigDecimal.ZERO;

        boolean filterMock = isProduction();

        for (Object[] row : rows) {
            if (row[0] == null || row[1] == null) continue;
            PaymentMethod method = (PaymentMethod) row[0];
            PaymentStatus status = (PaymentStatus) row[1];
            long count = ((Number) row[2]).longValue();
            BigDecimal amount = new BigDecimal(row[3].toString());

            if (filterMock && method == PaymentMethod.MOCK_TEST) continue;

            methodCounts.merge(method.name(), count, Long::sum);
            methodTotals.merge(method.name(), amount, BigDecimal::add);

            statusCounts.merge(status.name(), count, Long::sum);
            statusTotals.merge(status.name(), amount, BigDecimal::add);

            totalVolume = totalVolume.add(amount);
        }

        List<PaymentAnalyticsResponse.PaymentMethodMetric> methodList = new ArrayList<>();
        for (String m : methodCounts.keySet()) {
            BigDecimal amt = methodTotals.getOrDefault(m, BigDecimal.ZERO);
            BigDecimal pct = totalVolume.compareTo(BigDecimal.ZERO) > 0
                    ? amt.divide(totalVolume, 4, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100)).setScale(2, RoundingMode.HALF_UP)
                    : BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);

            methodList.add(PaymentAnalyticsResponse.PaymentMethodMetric.builder()
                    .paymentMethod(m)
                    .transactionCount(methodCounts.get(m))
                    .totalAmount(amt.setScale(2, RoundingMode.HALF_UP))
                    .percentageOfTotal(pct)
                    .build());
        }

        List<PaymentAnalyticsResponse.PaymentStatusMetric> statusList = new ArrayList<>();
        for (String s : statusCounts.keySet()) {
            statusList.add(PaymentAnalyticsResponse.PaymentStatusMetric.builder()
                    .paymentStatus(s)
                    .transactionCount(statusCounts.get(s))
                    .totalAmount(statusTotals.getOrDefault(s, BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP))
                    .build());
        }

        return PaymentAnalyticsResponse.builder()
                .cashBreakdown(PaymentAnalyticsResponse.CashCollectionBreakdown.builder()
                        .collectedRevenue(cashMetrics.collectedRevenue.setScale(2, RoundingMode.HALF_UP))
                        .pendingCollections(cashMetrics.pendingCollections.setScale(2, RoundingMode.HALF_UP))
                        .pendingCodAmount(cashMetrics.pendingCod.setScale(2, RoundingMode.HALF_UP))
                        .pendingOnlineAmount(cashMetrics.pendingOnline.setScale(2, RoundingMode.HALF_UP))
                        .confirmedRefundAmount(cashMetrics.confirmedRefunds.setScale(2, RoundingMode.HALF_UP))
                        .netCollectedRevenue(cashMetrics.netCollectedRevenue.setScale(2, RoundingMode.HALF_UP))
                        .build())
                .methodDistribution(methodList)
                .statusDistribution(statusList)
                .build();
    }

    @Override
    public List<CouponAnalyticsResponse> getCouponAnalytics(LocalDate startDate, LocalDate endDate) {
        DateRange range = parseDateRange(startDate, endDate);
        List<Object[]> rows = orderRepository.getCouponPerformanceBetween(range.startInclusive(), range.endExclusive());

        List<CouponAnalyticsResponse> list = new ArrayList<>();
        for (Object[] row : rows) {
            Long couponId = row[0] != null ? ((Number) row[0]).longValue() : null;
            String code = row[1] != null ? row[1].toString() : "";
            DiscountType discountType = row[2] != null ? (DiscountType) row[2] : DiscountType.PERCENTAGE;
            BigDecimal discountValue = row[3] != null ? new BigDecimal(row[3].toString()) : BigDecimal.ZERO;
            long timesUsed = row[4] != null ? ((Number) row[4]).longValue() : 0L;
            BigDecimal totalDiscounts = row[5] != null ? new BigDecimal(row[5].toString()) : BigDecimal.ZERO;
            BigDecimal totalRevenue = row[6] != null ? new BigDecimal(row[6].toString()) : BigDecimal.ZERO;
            boolean active = row[7] != null && (Boolean) row[7];
            LocalDateTime expiration = row[8] != null ? (LocalDateTime) row[8] : null;

            list.add(CouponAnalyticsResponse.builder()
                    .couponId(couponId)
                    .code(code)
                    .discountType(discountType.name())
                    .discountValue(discountValue)
                    .timesUsed(timesUsed)
                    .totalDiscountsGiven(totalDiscounts.setScale(2, RoundingMode.HALF_UP))
                    .totalOrderRevenueGenerated(totalRevenue.setScale(2, RoundingMode.HALF_UP))
                    .active(active)
                    .expirationDate(expiration)
                    .build());
        }

        return list;
    }

    @Override
    public Page<AuditReportResponse> getAuditLogs(Long userId, String action, LocalDate startDate, LocalDate endDate, int page, int size) {
        DateRange range = parseDateRange(startDate, endDate);
        Pageable pageable = PageRequest.of(Math.max(0, page), Math.max(1, size));

        Page<AuditLog> logPage;
        if (userId != null && action != null && !action.isBlank()) {
            logPage = auditLogRepository.findByUserIdAndActionAndCreatedAtGreaterThanEqualAndCreatedAtLessThanOrderByCreatedAtDesc(
                    userId, action, range.startInclusive(), range.endExclusive(), pageable);
        } else if (userId != null) {
            logPage = auditLogRepository.findByUserIdAndCreatedAtGreaterThanEqualAndCreatedAtLessThanOrderByCreatedAtDesc(
                    userId, range.startInclusive(), range.endExclusive(), pageable);
        } else if (action != null && !action.isBlank()) {
            logPage = auditLogRepository.findByActionAndCreatedAtGreaterThanEqualAndCreatedAtLessThanOrderByCreatedAtDesc(
                    action, range.startInclusive(), range.endExclusive(), pageable);
        } else {
            logPage = auditLogRepository.findByCreatedAtGreaterThanEqualAndCreatedAtLessThanOrderByCreatedAtDesc(
                    range.startInclusive(), range.endExclusive(), pageable);
        }

        List<AuditReportResponse> dtos = logPage.getContent().stream().map(log -> AuditReportResponse.builder()
                .id(log.getId())
                .userId(log.getUserId())
                .userEmail(log.getUserEmail())
                .action(log.getAction())
                .entityName(log.getEntityName())
                .entityId(log.getEntityId())
                .ipAddress(log.getIpAddress())
                .details(log.getDetails())
                .createdAt(log.getCreatedAt())
                .build()).toList();

        return new PageImpl<>(dtos, pageable, logPage.getTotalElements());
    }

    // --- CSV Streaming and Formula Injection Escaping ---

    private String sanitizeCsv(Object val) {
        if (val == null) return "\"\"";
        String str = val.toString();
        // Prevent CSV Formula Injection
        if (str.startsWith("=") || str.startsWith("+") || str.startsWith("-") || str.startsWith("@") || str.startsWith("\t") || str.startsWith("\r")) {
            str = "'" + str;
        }
        return "\"" + str.replace("\"", "\"\"") + "\"";
    }

    @Override
    public void exportSalesCsv(LocalDate startDate, LocalDate endDate, PrintWriter writer) {
        DateRange range = parseDateRange(startDate, endDate);
        writer.println("Order Number,Order Date,Status,Customer Email,Subtotal,Discount,Shipping,Tax,Total Amount,Payment Method,Payment Status");

        int page = 0;
        int batchSize = 500;
        Page<Order> orderPage;
        do {
            orderPage = orderRepository.findByCreatedAtGreaterThanEqualAndCreatedAtLessThanOrderByCreatedAtDesc(
                    range.startInclusive(), range.endExclusive(), PageRequest.of(page, batchSize));

            for (Order o : orderPage.getContent()) {
                String pMethod = o.getPayment() != null ? o.getPayment().getPaymentMethod().name() : "N/A";
                String pStatus = o.getPayment() != null ? o.getPayment().getPaymentStatus().name() : "N/A";
                String email = o.getUser() != null ? o.getUser().getEmail() : "Guest";

                writer.println(String.join(",",
                        sanitizeCsv(o.getOrderNumber()),
                        sanitizeCsv(o.getCreatedAt()),
                        sanitizeCsv(o.getOrderStatus()),
                        sanitizeCsv(email),
                        sanitizeCsv(o.getSubtotal()),
                        sanitizeCsv(o.getDiscountAmount()),
                        sanitizeCsv(o.getShippingFee()),
                        sanitizeCsv(o.getTaxAmount()),
                        sanitizeCsv(o.getTotalAmount()),
                        sanitizeCsv(pMethod),
                        sanitizeCsv(pStatus)
                ));
            }
            writer.flush();
            page++;
        } while (orderPage.hasNext());
    }

    @Override
    public void exportProductsCsv(LocalDate startDate, LocalDate endDate, PrintWriter writer) {
        List<ProductPerformanceResponse> products = getProductPerformance(startDate, endDate, 1000);
        writer.println("Product ID,Product Name,SKU,Category,Units Sold,Total Revenue,Purchasable Stock,Average Rating,Reviews");

        for (ProductPerformanceResponse p : products) {
            writer.println(String.join(",",
                    sanitizeCsv(p.getProductId()),
                    sanitizeCsv(p.getProductName()),
                    sanitizeCsv(p.getSku()),
                    sanitizeCsv(p.getCategoryName()),
                    sanitizeCsv(p.getUnitsSold()),
                    sanitizeCsv(p.getTotalRevenue()),
                    sanitizeCsv(p.getCurrentPurchasableStock()),
                    sanitizeCsv(p.getAverageRating()),
                    sanitizeCsv(p.getReviewCount())
            ));
        }
        writer.flush();
    }

    @Override
    public void exportInventoryCsv(String status, PrintWriter writer) {
        writer.println("Inventory ID,Product Name,SKU,Category,Available Units,Reserved Units,Purchasable Units,Low Stock Threshold,Stock Status");

        int page = 0;
        int batchSize = 500;
        InventoryHealthResponse resp;
        do {
            resp = getInventoryHealth(status, page, batchSize);
            for (InventoryHealthResponse.InventoryItemDto item : resp.getItems().getContent()) {
                writer.println(String.join(",",
                        sanitizeCsv(item.getInventoryId()),
                        sanitizeCsv(item.getItemName()),
                        sanitizeCsv(item.getSku()),
                        sanitizeCsv(item.getCategoryName()),
                        sanitizeCsv(item.getQuantityAvailable()),
                        sanitizeCsv(item.getQuantityReserved()),
                        sanitizeCsv(item.getPurchasableQuantity()),
                        sanitizeCsv(item.getLowStockThreshold()),
                        sanitizeCsv(item.getStockStatus())
                ));
            }
            writer.flush();
            page++;
        } while (resp.getItems().hasNext());
    }

    @Override
    public void exportPaymentsCsv(LocalDate startDate, LocalDate endDate, PrintWriter writer) {
        DateRange range = parseDateRange(startDate, endDate);
        writer.println("Payment ID,Transaction ID,Order Number,Method,Status,Amount,Currency,Created Date");

        int page = 0;
        int batchSize = 500;
        Page<Payment> payPage;
        do {
            payPage = paymentRepository.findByCreatedAtGreaterThanEqualAndCreatedAtLessThanOrderByCreatedAtDesc(
                    range.startInclusive(), range.endExclusive(), PageRequest.of(page, batchSize));

            for (Payment p : payPage.getContent()) {
                String orderNum = p.getOrder() != null ? p.getOrder().getOrderNumber() : "N/A";
                writer.println(String.join(",",
                        sanitizeCsv(p.getId()),
                        sanitizeCsv(p.getTransactionId()),
                        sanitizeCsv(orderNum),
                        sanitizeCsv(p.getPaymentMethod()),
                        sanitizeCsv(p.getPaymentStatus()),
                        sanitizeCsv(p.getAmount()),
                        sanitizeCsv(p.getCurrency()),
                        sanitizeCsv(p.getCreatedAt())
                ));
            }
            writer.flush();
            page++;
        } while (payPage.hasNext());
    }

    @Override
    public void exportCustomersCsv(LocalDate startDate, LocalDate endDate, PrintWriter writer) {
        DateRange range = parseDateRange(startDate, endDate);
        writer.println("User ID,Customer Name,Email,Phone,Registration Date,Status");

        int page = 0;
        int batchSize = 500;
        Page<User> userPage;
        do {
            userPage = userRepository.findAll(PageRequest.of(page, batchSize));
            for (User u : userPage.getContent()) {
                String name = (u.getFirstName() != null ? u.getFirstName() : "") + " " + (u.getLastName() != null ? u.getLastName() : "");
                writer.println(String.join(",",
                        sanitizeCsv(u.getId()),
                        sanitizeCsv(name.trim()),
                        sanitizeCsv(u.getEmail()),
                        sanitizeCsv(u.getPhone()),
                        sanitizeCsv(u.getCreatedAt()),
                        sanitizeCsv(u.getStatus())
                ));
            }
            writer.flush();
            page++;
        } while (userPage.hasNext());
    }
}
