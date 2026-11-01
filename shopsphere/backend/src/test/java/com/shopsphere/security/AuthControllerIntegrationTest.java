package com.shopsphere.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.shopsphere.dto.request.auth.LoginRequest;
import com.shopsphere.dto.request.auth.RegisterRequest;
import com.shopsphere.dto.request.auth.ResetPasswordRequest;
import com.shopsphere.entity.User;
import com.shopsphere.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class AuthControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Test
    @DisplayName("1. Successful customer registration")
    void testSuccessfulRegistration() throws Exception {
        RegisterRequest request = RegisterRequest.builder()
                .email("new.shopper@shopsphere.com")
                .password("Password@123")
                .firstName("Alice")
                .lastName("Walker")
                .phone("+1234567890")
                .build();

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.data.refreshToken").isNotEmpty())
                .andExpect(jsonPath("$.data.email").value("new.shopper@shopsphere.com"))
                .andExpect(jsonPath("$.data.roles[0]").value("ROLE_CUSTOMER"));

        assertThat(userRepository.existsByEmail("new.shopper@shopsphere.com")).isTrue();
    }

    @Test
    @DisplayName("2. Duplicate email registration should fail with 400 Bad Request")
    void testDuplicateEmailRegistration_Returns400() throws Exception {
        RegisterRequest request = RegisterRequest.builder()
                .email("duplicate@shopsphere.com")
                .password("Password@123")
                .firstName("Bob")
                .lastName("Smith")
                .build();

        // First registration
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        // Duplicate registration attempt
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("An account with email duplicate@shopsphere.com already exists"));
    }

    @Test
    @DisplayName("3. Registration with invalid password should fail validation with 400")
    void testInvalidPassword_Returns400() throws Exception {
        RegisterRequest request = RegisterRequest.builder()
                .email("shortpass@shopsphere.com")
                .password("short") // Less than 8 characters
                .firstName("Dave")
                .lastName("Clark")
                .build();

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Validation Failed"))
                .andExpect(jsonPath("$.fieldErrors.password").isNotEmpty());
    }

    @Test
    @DisplayName("4. Successful login returns valid JWT and refresh token")
    void testSuccessfulLogin() throws Exception {
        // Register user first
        RegisterRequest registerReq = RegisterRequest.builder()
                .email("login.user@shopsphere.com")
                .password("SecurePass#99")
                .firstName("Elena")
                .lastName("Gilbert")
                .build();

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerReq)))
                .andExpect(status().isCreated());

        // Attempt login
        LoginRequest loginReq = LoginRequest.builder()
                .email("login.user@shopsphere.com")
                .password("SecurePass#99")
                .build();

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.data.refreshToken").isNotEmpty())
                .andExpect(jsonPath("$.data.email").value("login.user@shopsphere.com"));
    }

    @Test
    @DisplayName("5. Login with incorrect password should fail with 401 Unauthorized")
    void testIncorrectPassword_Returns401() throws Exception {
        // Register user
        RegisterRequest registerReq = RegisterRequest.builder()
                .email("wrong.pass@shopsphere.com")
                .password("CorrectPass@123")
                .firstName("Fiona")
                .lastName("Gallagher")
                .build();

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerReq)))
                .andExpect(status().isCreated());

        // Login with wrong password
        LoginRequest loginReq = LoginRequest.builder()
                .email("wrong.pass@shopsphere.com")
                .password("WrongPassword999")
                .build();

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginReq)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Invalid email or password"));
    }

    @Test
    @DisplayName("6. Secure forgot password and reset password workflow")
    void testForgotPasswordAndResetPassword() throws Exception {
        RegisterRequest registerReq = RegisterRequest.builder()
                .email("reset.me@shopsphere.com")
                .password("OldPassword#1")
                .firstName("George")
                .lastName("Costanza")
                .build();

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerReq)))
                .andExpect(status().isCreated());

        // Simulate password reset token generation
        User user = userRepository.findByEmail("reset.me@shopsphere.com").orElseThrow();
        user.setPasswordResetToken("valid-reset-token-12345");
        user.setPasswordResetExpiry(LocalDateTime.now().plusHours(1));
        userRepository.save(user);

        // Reset password
        ResetPasswordRequest resetReq = ResetPasswordRequest.builder()
                .token("valid-reset-token-12345")
                .newPassword("BrandNewPassword#2026")
                .build();

        mockMvc.perform(post("/api/v1/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(resetReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        // Verify login with new password works
        LoginRequest loginReq = LoginRequest.builder()
                .email("reset.me@shopsphere.com")
                .password("BrandNewPassword#2026")
                .build();

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginReq)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("7. Verify email address using token")
    void testVerifyEmail() throws Exception {
        RegisterRequest registerReq = RegisterRequest.builder()
                .email("verify.email@shopsphere.com")
                .password("Password#123")
                .firstName("Hannah")
                .lastName("Montana")
                .build();

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerReq)))
                .andExpect(status().isCreated());

        User user = userRepository.findByEmail("verify.email@shopsphere.com").orElseThrow();
        String token = user.getEmailVerificationToken();
        assertThat(token).isNotEmpty();

        mockMvc.perform(get("/api/v1/auth/verify-email")
                        .param("token", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Email verified successfully"));

        User verifiedUser = userRepository.findByEmail("verify.email@shopsphere.com").orElseThrow();
        assertThat(verifiedUser.isEmailVerified()).isTrue();
    }
}
