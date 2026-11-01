package com.shopsphere.controller;

import com.shopsphere.dto.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/v1")
@Tag(name = "Admin Security Diagnostic", description = "Endpoints for verifying role-based authorization")
public class AdminTestController {

    @GetMapping("/admin/dashboard-stats")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Admin-only metrics endpoint")
    public ResponseEntity<ApiResponse<Map<String, String>>> getAdminDashboardStats() {
        return ResponseEntity.ok(ApiResponse.success(Map.of("message", "Authorized: Access granted to Admin Dashboard metrics")));
    }

    @GetMapping("/manager/reports")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @Operation(summary = "Manager and Admin privileged report endpoint")
    public ResponseEntity<ApiResponse<Map<String, String>>> getManagerReports() {
        return ResponseEntity.ok(ApiResponse.success(Map.of("message", "Authorized: Access granted to Manager and Admin reports")));
    }
}
