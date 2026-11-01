package com.shopsphere.service;

import com.shopsphere.dto.request.auth.LoginRequest;
import com.shopsphere.dto.request.auth.RegisterRequest;
import com.shopsphere.dto.request.auth.ResetPasswordRequest;
import com.shopsphere.dto.response.auth.AuthResponse;
import com.shopsphere.dto.response.user.UserResponse;

public interface AuthService {

    AuthResponse register(RegisterRequest request);

    AuthResponse login(LoginRequest request);

    AuthResponse refreshToken(String refreshTokenString);

    void logout(String refreshTokenString, Long userId);

    void forgotPassword(String email);

    void resetPassword(ResetPasswordRequest request);

    void verifyEmail(String token);

    UserResponse getCurrentUser(Long userId);
}
