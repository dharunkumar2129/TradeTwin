package com.tradetwin.auth.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tradetwin.auth.dto.AuthResponse;
import com.tradetwin.auth.dto.LoginRequest;
import com.tradetwin.auth.dto.RegisterRequest;
import com.tradetwin.auth.dto.UserProfileDto;
import com.tradetwin.auth.service.AuthService;
import com.tradetwin.user.model.UserRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AuthControllerHttpTest {

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    static class StubAuthService extends AuthService {
        public StubAuthService() {
            super(null, null, null);
        }

        @Override
        public AuthResponse register(RegisterRequest request) {
            return AuthResponse.builder()
                    .token("jwt_sample_token")
                    .id("u1")
                    .email(request.getEmail())
                    .fullName(request.getFullName())
                    .role(request.getRole())
                    .reputationScore(100.0)
                    .defaultCurrency("USD")
                    .build();
        }

        @Override
        public AuthResponse login(LoginRequest request) {
            return AuthResponse.builder()
                    .token("jwt_sample_token")
                    .id("u1")
                    .email(request.getEmail())
                    .fullName("Alice Walker")
                    .role(UserRole.ROLE_BUYER)
                    .reputationScore(100.0)
                    .defaultCurrency("USD")
                    .build();
        }

        @Override
        public UserProfileDto getCurrentUser(String email) {
            return UserProfileDto.builder()
                    .id("u1")
                    .email(email)
                    .fullName("Alice Walker")
                    .role(UserRole.ROLE_BUYER)
                    .reputationScore(100.0)
                    .defaultCurrency("USD")
                    .build();
        }
    }

    @BeforeEach
    void setUp() {
        AuthController authController = new AuthController(new StubAuthService());
        mockMvc = MockMvcBuilders.standaloneSetup(authController).build();
        objectMapper = new ObjectMapper();
    }

    @Test
    @DisplayName("POST /api/auth/register creates new account and returns 201 Created")
    void testRegisterEndpoint() throws Exception {
        RegisterRequest req = RegisterRequest.builder()
                .email("newbuyer@example.com")
                .password("password123")
                .fullName("New Buyer")
                .role(UserRole.ROLE_BUYER)
                .build();

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.token").value("jwt_sample_token"))
                .andExpect(jsonPath("$.data.email").value("newbuyer@example.com"));
    }

    @Test
    @DisplayName("POST /api/auth/login logs in user and returns 200 OK")
    void testLoginEndpoint() throws Exception {
        LoginRequest req = LoginRequest.builder()
                .email("buyer@tradetwin.com")
                .password("password123")
                .build();

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.token").value("jwt_sample_token"));
    }

    @Test
    @DisplayName("POST /api/auth/register fails with 400 Bad Request if fields invalid")
    void testRegisterValidationFailure() throws Exception {
        RegisterRequest req = RegisterRequest.builder()
                .email("not-an-email")
                .password("123") // too short
                .fullName("")
                .build();

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest());
    }
}
