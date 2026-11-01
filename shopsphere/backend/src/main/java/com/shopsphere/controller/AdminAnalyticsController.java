package com.shopsphere.controller;

import com.shopsphere.dto.response.ApiResponse;
import com.shopsphere.dto.response.analytics.*;
import com.shopsphere.service.AnalyticsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.time.LocalDate;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/v1/admin/analytics")
@RequiredArgsConstructor
@Tag(name = "Admin Analytics", description = "Endpoints for business intelligence, reporting, and store analytics")
@PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
public class AdminAnalyticsController {

    private final AnalyticsService analyticsService;

    @GetMapping("/overview")
    @Operation(summary = "Get high-level dashboard metrics and period trends")
    public ResponseEntity<ApiResponse<AnalyticsOverviewResponse>> getOverview(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate
    ) {
        AnalyticsOverviewResponse response = analyticsService.getOverview(startDate, endDate);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/sales")
    @Operation(summary = "Get sales trend data bucketed by time interval")
    public ResponseEntity<ApiResponse<List<SalesTrendResponse>>> getSalesTrend(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false, defaultValue = "DAILY") String interval
    ) {
        List<SalesTrendResponse> response = analyticsService.getSalesTrend(startDate, endDate, interval);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/products")
    @Operation(summary = "Get top-selling product performance analytics")
    public ResponseEntity<ApiResponse<List<ProductPerformanceResponse>>> getProductPerformance(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false, defaultValue = "20") int limit
    ) {
        List<ProductPerformanceResponse> response = analyticsService.getProductPerformance(startDate, endDate, limit);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/categories")
    @Operation(summary = "Get category sales and revenue distribution")
    public ResponseEntity<ApiResponse<List<CategorySalesResponse>>> getCategorySales(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate
    ) {
        List<CategorySalesResponse> response = analyticsService.getCategorySales(startDate, endDate);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/inventory")
    @Operation(summary = "Get deduplicated inventory health and stock status table")
    public ResponseEntity<ApiResponse<InventoryHealthResponse>> getInventoryHealth(
            @RequestParam(required = false, defaultValue = "ALL") String status,
            @RequestParam(required = false, defaultValue = "0") int page,
            @RequestParam(required = false, defaultValue = "20") int size
    ) {
        InventoryHealthResponse response = analyticsService.getInventoryHealth(status, page, size);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/customers")
    @Operation(summary = "Get customer acquisition and lifetime purchasing analytics")
    public ResponseEntity<ApiResponse<CustomerAnalyticsResponse>> getCustomerAnalytics(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate
    ) {
        CustomerAnalyticsResponse response = analyticsService.getCustomerAnalytics(startDate, endDate);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/payments")
    @Operation(summary = "Get payment method breakdown, cash collections, and refund analytics")
    public ResponseEntity<ApiResponse<PaymentAnalyticsResponse>> getPaymentAnalytics(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate
    ) {
        PaymentAnalyticsResponse response = analyticsService.getPaymentAnalytics(startDate, endDate);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/coupons")
    @Operation(summary = "Get coupon usage and discount impact analytics")
    public ResponseEntity<ApiResponse<List<CouponAnalyticsResponse>>> getCouponAnalytics(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate
    ) {
        List<CouponAnalyticsResponse> response = analyticsService.getCouponAnalytics(startDate, endDate);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/audit")
    @Operation(summary = "Get paginated administrative audit logs")
    public ResponseEntity<ApiResponse<Page<AuditReportResponse>>> getAuditLogs(
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) String action,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false, defaultValue = "0") int page,
            @RequestParam(required = false, defaultValue = "20") int size
    ) {
        Page<AuditReportResponse> response = analyticsService.getAuditLogs(userId, action, startDate, endDate, page, size);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    // --- CSV Streaming Export Endpoints ---

    @GetMapping("/export/sales")
    @Operation(summary = "Stream sales and orders data in CSV format")
    public void exportSalesCsv(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            HttpServletResponse response
    ) throws IOException {
        response.setContentType("text/csv; charset=UTF-8");
        response.setHeader(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"sales_report.csv\"");
        analyticsService.exportSalesCsv(startDate, endDate, response.getWriter());
    }

    @GetMapping("/export/products")
    @Operation(summary = "Stream product performance data in CSV format")
    public void exportProductsCsv(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            HttpServletResponse response
    ) throws IOException {
        response.setContentType("text/csv; charset=UTF-8");
        response.setHeader(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"product_performance.csv\"");
        analyticsService.exportProductsCsv(startDate, endDate, response.getWriter());
    }

    @GetMapping("/export/inventory")
    @Operation(summary = "Stream inventory health data in CSV format")
    public void exportInventoryCsv(
            @RequestParam(required = false, defaultValue = "ALL") String status,
            HttpServletResponse response
    ) throws IOException {
        response.setContentType("text/csv; charset=UTF-8");
        response.setHeader(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"inventory_health.csv\"");
        analyticsService.exportInventoryCsv(status, response.getWriter());
    }

    @GetMapping("/export/payments")
    @Operation(summary = "Stream payments data in CSV format")
    public void exportPaymentsCsv(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            HttpServletResponse response
    ) throws IOException {
        response.setContentType("text/csv; charset=UTF-8");
        response.setHeader(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"payments_report.csv\"");
        analyticsService.exportPaymentsCsv(startDate, endDate, response.getWriter());
    }

    @GetMapping("/export/customers")
    @Operation(summary = "Stream customers data in CSV format")
    public void exportCustomersCsv(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            HttpServletResponse response
    ) throws IOException {
        response.setContentType("text/csv; charset=UTF-8");
        response.setHeader(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"customers_report.csv\"");
        analyticsService.exportCustomersCsv(startDate, endDate, response.getWriter());
    }
}
