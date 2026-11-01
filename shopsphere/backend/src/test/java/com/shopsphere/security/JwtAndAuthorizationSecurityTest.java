package com.shopsphere.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.shopsphere.dto.request.auth.LoginRequest;
import com.shopsphere.dto.request.auth.RefreshTokenRequest;
import com.shopsphere.dto.request.auth.RegisterRequest;
import com.shopsphere.dto.response.auth.AuthResponse;
import com.shopsphere.entity.RefreshToken;
import com.shopsphere.entity.Role;
import com.shopsphere.entity.User;
import com.shopsphere.entity.enums.ERole;
import com.shopsphere.entity.enums.UserStatus;
import com.shopsphere.repository.RefreshTokenRepository;
import com.shopsphere.repository.RoleRepository;
import com.shopsphere.repository.UserRepository;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class JwtAndAuthorizationSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    private User customerUser;
    private User adminUser;
    private String customerAccessToken;
    private String adminAccessToken;

    @BeforeEach
    void setUp() {
        Role customerRole = roleRepository.findByName(ERole.ROLE_CUSTOMER)
                .orElseGet(() -> roleRepository.save(new Role(ERole.ROLE_CUSTOMER)));
        Role adminRole = roleRepository.findByName(ERole.ROLE_ADMIN)
                .orElseGet(() -> roleRepository.save(new Role(ERole.ROLE_ADMIN)));

        customerUser = userRepository.save(User.builder()
                .email("test.customer@shopsphere.com")
                .password(passwordEncoder.encode("CustomerPass#123"))
                .firstName("John")
                .lastName("Customer")
                .status(UserStatus.ACTIVE)
                .roles(Set.of(customerRole))
                .build());

        adminUser = userRepository.save(User.builder()
                .email("test.admin@shopsphere.com")
                .password(passwordEncoder.encode("AdminPass#123"))
                .firstName("Boss")
                .lastName("Admin")
                .status(UserStatus.ACTIVE)
                .roles(Set.of(customerRole, adminRole))
                .build());

        customerAccessToken = jwtTokenProvider.generateAccessToken(customerUser);
        adminAccessToken = jwtTokenProvider.generateAccessToken(adminUser);
    }

    @Test
    @DisplayName("1. Missing JWT token on protected endpoint returns 401 Unauthorized")
    void testMissingToken_Returns401() throws Exception {
        mockMvc.perform(get("/api/v1/auth/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.error").value("Unauthorized"));
    }

    @Test
    @DisplayName("2. Invalid JWT signature returns 401 Unauthorized")
    void testInvalidJwtSignature_Returns401() throws Exception {
        // Sign token with a different/fake secret key
        SecretKey fakeKey = Keys.hmacShaKeyFor("fake_untrusted_secret_key_123456789012345678901234567890".getBytes(StandardCharsets.UTF_8));
        String tamperedToken = Jwts.builder()
                .subject("test.customer@shopsphere.com")
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + 60000))
                .signWith(fakeKey, Jwts.SIG.HS256)
                .compact();

        mockMvc.perform(get("/api/v1/auth/me")
                        .header("Authorization", "Bearer " + tamperedToken))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("3. Expired JWT access token returns 401 Unauthorized")
    void testExpiredAccessToken_Returns401() throws Exception {
        // Generate token expired in the past
        String expiredToken = jwtTokenProvider.generateTokenForSubject(customerUser.getEmail(), -10000);

        mockMvc.perform(get("/api/v1/auth/me")
                        .header("Authorization", "Bearer " + expiredToken))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("4. Successful refresh token rotation issues fresh tokens")
    void testSuccessfulRefreshAndRotation() throws Exception {
        LoginRequest loginReq = LoginRequest.builder()
                .email("test.customer@shopsphere.com")
                .password("CustomerPass#123")
                .build();

        MvcResult loginResult = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginReq)))
                .andExpect(status().isOk())
                .andReturn();

        AuthResponse auth = objectMapper.readValue(
                objectMapper.readTree(loginResult.getResponse().getContentAsString()).get("data").toString(),
                AuthResponse.class
        );

        String originalRefreshToken = auth.getRefreshToken();
        assertThat(originalRefreshToken).isNotEmpty();

        // Perform token refresh
        RefreshTokenRequest refreshReq = RefreshTokenRequest.builder()
                .refreshToken(originalRefreshToken)
                .build();

        MvcResult refreshResult = mockMvc.perform(post("/api/v1/auth/refresh-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(refreshReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.data.refreshToken").isNotEmpty())
                .andReturn();

        AuthResponse refreshedAuth = objectMapper.readValue(
                objectMapper.readTree(refreshResult.getResponse().getContentAsString()).get("data").toString(),
                AuthResponse.class
        );

        // Verify that rotation issued a brand new refresh token
        assertThat(refreshedAuth.getRefreshToken()).isNotEqualTo(originalRefreshToken);

        // Verify the old token is marked as revoked in DB
        RefreshToken oldToken = refreshTokenRepository.findByToken(originalRefreshToken).orElseThrow();
        assertThat(oldToken.isRevoked()).isTrue();
        assertThat(oldToken.getReplacedByToken()).isEqualTo(refreshedAuth.getRefreshToken());
    }

    @Test
    @DisplayName("5. Refresh token reuse triggers family revocation and returns 401")
    void testRefreshTokenReuse_TriggersFamilyRevocation() throws Exception {
        LoginRequest loginReq = LoginRequest.builder()
                .email("test.customer@shopsphere.com")
                .password("CustomerPass#123")
                .build();

        MvcResult loginResult = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginReq)))
                .andExpect(status().isOk())
                .andReturn();

        AuthResponse auth = objectMapper.readValue(
                objectMapper.readTree(loginResult.getResponse().getContentAsString()).get("data").toString(),
                AuthResponse.class
        );

        String originalRefreshToken = auth.getRefreshToken();

        // Legitimate client rotates the token once
        mockMvc.perform(post("/api/v1/auth/refresh-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new RefreshTokenRequest(originalRefreshToken))))
                .andExpect(status().isOk());

        // Attacker attempts to REUSE the now-replaced original refresh token
        mockMvc.perform(post("/api/v1/auth/refresh-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new RefreshTokenRequest(originalRefreshToken))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Suspicious activity detected: Refresh token already used. Please log in again."));

        // Verify that all tokens for this user family were revoked
        User freshUser = userRepository.findById(customerUser.getId()).orElseThrow();
        refreshTokenRepository.findByUser(freshUser).forEach(token -> {
            assertThat(token.isRevoked()).isTrue();
        });
    }

    @Test
    @DisplayName("6. Logout revokes active refresh token")
    void testLogout_RevokesToken() throws Exception {
        LoginRequest loginReq = LoginRequest.builder()
                .email("test.customer@shopsphere.com")
                .password("CustomerPass#123")
                .build();

        MvcResult loginResult = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginReq)))
                .andExpect(status().isOk())
                .andReturn();

        AuthResponse auth = objectMapper.readValue(
                objectMapper.readTree(loginResult.getResponse().getContentAsString()).get("data").toString(),
                AuthResponse.class
        );

        String token = auth.getRefreshToken();

        // Perform logout
        mockMvc.perform(post("/api/v1/auth/logout")
                        .header("Authorization", "Bearer " + auth.getAccessToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new RefreshTokenRequest(token))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Logged out successfully"));

        // Attempting to refresh with the logged out token should now fail
        mockMvc.perform(post("/api/v1/auth/refresh-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new RefreshTokenRequest(token))))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("7. CUSTOMER attempting to access ADMIN endpoint returns 403 Forbidden")
    void testCustomerAccessingAdminEndpoint_Returns403() throws Exception {
        mockMvc.perform(get("/api/v1/admin/dashboard-stats")
                        .header("Authorization", "Bearer " + customerAccessToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.error").value("Forbidden"));
    }

    @Test
    @DisplayName("8. ADMIN accessing protected ADMIN endpoint returns 200 OK")
    void testAdminAccessingAdminEndpoint_Returns200() throws Exception {
        mockMvc.perform(get("/api/v1/admin/dashboard-stats")
                        .header("Authorization", "Bearer " + adminAccessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.message").value("Authorized: Access granted to Admin Dashboard metrics"));
    }

    @Test
    @DisplayName("9. Authenticated user profile /me returns their own customer data only")
    void testGetCurrentUserProfile() throws Exception {
        mockMvc.perform(get("/api/v1/auth/me")
                        .header("Authorization", "Bearer " + customerAccessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.email").value("test.customer@shopsphere.com"))
                .andExpect(jsonPath("$.data.firstName").value("John"))
                .andExpect(jsonPath("$.data.roles[0]").value("ROLE_CUSTOMER"));
    }
}
