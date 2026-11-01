package com.shopsphere.controller;

import com.shopsphere.dto.request.auth.*;
import com.shopsphere.dto.response.ApiResponse;
import com.shopsphere.dto.response.auth.AuthResponse;
import com.shopsphere.dto.response.user.UserResponse;
import com.shopsphere.security.UserPrincipal;
import com.shopsphere.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
@Tag(name = "Authentication", description = "User registration, login, JWT rotation, and password management")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    @Operation(summary = "Register a new customer account")
    public ResponseEntity<ApiResponse<AuthResponse>> register(@Valid @RequestBody RegisterRequest request) {
        AuthResponse response = authService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Account created successfully", response));
    }

    @PostMapping("/login")
    @Operation(summary = "Authenticate with email and password")
    public ResponseEntity<ApiResponse<AuthResponse>> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletResponse servletResponse
    ) {
        AuthResponse response = authService.login(request);
        attachRefreshTokenCookie(servletResponse, response.getRefreshToken());
        return ResponseEntity.ok(ApiResponse.success("Login successful", response));
    }

    @PostMapping("/refresh-token")
    @Operation(summary = "Rotate refresh token and issue new JWT access token")
    public ResponseEntity<ApiResponse<AuthResponse>> refreshToken(
            @RequestBody(required = false) RefreshTokenRequest request,
            HttpServletRequest servletRequest,
            HttpServletResponse servletResponse
    ) {
        String token = request != null && request.getRefreshToken() != null ?
                request.getRefreshToken() : extractRefreshTokenFromCookie(servletRequest);

        AuthResponse response = authService.refreshToken(token);
        attachRefreshTokenCookie(servletResponse, response.getRefreshToken());
        return ResponseEntity.ok(ApiResponse.success("Token refreshed successfully", response));
    }

    @PostMapping("/logout")
    @Operation(summary = "Revoke active refresh token and invalidate session")
    public ResponseEntity<ApiResponse<String>> logout(
            @RequestBody(required = false) RefreshTokenRequest request,
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            HttpServletRequest servletRequest,
            HttpServletResponse servletResponse
    ) {
        String token = request != null && request.getRefreshToken() != null ?
                request.getRefreshToken() : extractRefreshTokenFromCookie(servletRequest);

        Long userId = userPrincipal != null ? userPrincipal.getId() : null;
        authService.logout(token, userId);
        clearRefreshTokenCookie(servletResponse);
        return ResponseEntity.ok(ApiResponse.success("Logged out successfully", null));
    }

    @PostMapping("/forgot-password")
    @Operation(summary = "Initiate password reset email workflow")
    public ResponseEntity<ApiResponse<String>> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        authService.forgotPassword(request.getEmail());
        return ResponseEntity.ok(ApiResponse.success("If an account exists, a password reset link has been dispatched", null));
    }

    @PostMapping("/reset-password")
    @Operation(summary = "Reset password using verification token")
    public ResponseEntity<ApiResponse<String>> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        authService.resetPassword(request);
        return ResponseEntity.ok(ApiResponse.success("Password has been reset successfully. Please log in with your new password.", null));
    }

    @GetMapping("/verify-email")
    @Operation(summary = "Verify account email address")
    public ResponseEntity<ApiResponse<String>> verifyEmail(@RequestParam("token") String token) {
        authService.verifyEmail(token);
        return ResponseEntity.ok(ApiResponse.success("Email verified successfully", null));
    }

    @GetMapping("/me")
    @Operation(summary = "Get profile of currently authenticated user")
    public ResponseEntity<ApiResponse<UserResponse>> getCurrentUser(@AuthenticationPrincipal UserPrincipal userPrincipal) {
        if (userPrincipal == null) {
            throw new com.shopsphere.exception.UnauthorizedException("User is not authenticated");
        }
        UserResponse response = authService.getCurrentUser(userPrincipal.getId());
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    private void attachRefreshTokenCookie(HttpServletResponse response, String refreshToken) {
        Cookie cookie = new Cookie("shopsphere_refresh", refreshToken);
        cookie.setHttpOnly(true);
        cookie.setSecure(false); // set true in production HTTPS
        cookie.setPath("/api/v1/auth");
        cookie.setMaxAge(7 * 24 * 60 * 60); // 7 days
        response.addCookie(cookie);
    }

    private void clearRefreshTokenCookie(HttpServletResponse response) {
        Cookie cookie = new Cookie("shopsphere_refresh", "");
        cookie.setHttpOnly(true);
        cookie.setPath("/api/v1/auth");
        cookie.setMaxAge(0);
        response.addCookie(cookie);
    }

    private String extractRefreshTokenFromCookie(HttpServletRequest request) {
        if (request.getCookies() != null) {
            for (Cookie c : request.getCookies()) {
                if ("shopsphere_refresh".equals(c.getName())) {
                    return c.getValue();
                }
            }
        }
        return null;
    }
}
