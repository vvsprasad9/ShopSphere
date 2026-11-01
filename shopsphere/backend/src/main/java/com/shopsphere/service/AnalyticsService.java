package com.shopsphere.service;

import com.shopsphere.dto.response.analytics.*;
import org.springframework.data.domain.Page;

import java.io.PrintWriter;
import java.time.LocalDate;
import java.util.List;

public interface AnalyticsService {

    AnalyticsOverviewResponse getOverview(LocalDate startDate, LocalDate endDate);

    List<SalesTrendResponse> getSalesTrend(LocalDate startDate, LocalDate endDate, String interval);

    List<ProductPerformanceResponse> getProductPerformance(LocalDate startDate, LocalDate endDate, int limit);

    List<CategorySalesResponse> getCategorySales(LocalDate startDate, LocalDate endDate);

    InventoryHealthResponse getInventoryHealth(String status, int page, int size);

    CustomerAnalyticsResponse getCustomerAnalytics(LocalDate startDate, LocalDate endDate);

    PaymentAnalyticsResponse getPaymentAnalytics(LocalDate startDate, LocalDate endDate);

    List<CouponAnalyticsResponse> getCouponAnalytics(LocalDate startDate, LocalDate endDate);

    Page<AuditReportResponse> getAuditLogs(Long userId, String action, LocalDate startDate, LocalDate endDate, int page, int size);

    void exportSalesCsv(LocalDate startDate, LocalDate endDate, PrintWriter writer);

    void exportProductsCsv(LocalDate startDate, LocalDate endDate, PrintWriter writer);

    void exportInventoryCsv(String status, PrintWriter writer);

    void exportPaymentsCsv(LocalDate startDate, LocalDate endDate, PrintWriter writer);

    void exportCustomersCsv(LocalDate startDate, LocalDate endDate, PrintWriter writer);
}
