package com.shopsphere.controller;

import com.shopsphere.dto.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.core.env.Environment;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/health")
@Tag(name = "Health Check", description = "System liveness and readiness diagnostic endpoints")
public class HealthController {

    private final Environment environment;

    public HealthController(Environment environment) {
        this.environment = environment;
    }

    @GetMapping
    @Operation(summary = "Check backend service health and runtime metadata")
    public ResponseEntity<ApiResponse<Map<String, Object>>> checkHealth() {
        Map<String, Object> details = new LinkedHashMap<>();
        details.put("status", "UP");
        details.put("service", "ShopSphere E-Commerce Backend");
        details.put("version", "1.0.0-SNAPSHOT");
        details.put("timestamp", LocalDateTime.now());
        details.put("activeProfiles", environment.getActiveProfiles());
        details.put("javaVersion", System.getProperty("java.version"));

        return ResponseEntity.ok(ApiResponse.success("ShopSphere backend service is operational", details));
    }
}
