package com.tradetwin.user.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tradetwin.auth.dto.UserProfileDto;
import com.tradetwin.user.dto.UpdateProfileRequest;
import com.tradetwin.user.model.ShippingAddress;
import com.tradetwin.user.model.ShoppingPreferences;
import com.tradetwin.user.model.UserRole;
import com.tradetwin.user.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class UserControllerHttpTest {

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;
    private UsernamePasswordAuthenticationToken authPrincipal;

    static class StubUserService extends UserService {
        public StubUserService() {
            super(null);
        }

        @Override
        public UserProfileDto updateProfile(String email, UpdateProfileRequest request) {
            return UserProfileDto.builder()
                    .id("u1")
                    .email(email)
                    .fullName(request.getFullName())
                    .role(UserRole.ROLE_BUYER)
                    .reputationScore(99.0)
                    .defaultCurrency(request.getDefaultCurrency())
                    .build();
        }

        @Override
        public UserProfileDto getUserProfile(String email) {
            return UserProfileDto.builder()
                    .id("u1")
                    .email(email)
                    .fullName("Alice Walker")
                    .role(UserRole.ROLE_BUYER)
                    .reputationScore(98.5)
                    .defaultCurrency("USD")
                    .build();
        }
    }

    @BeforeEach
    void setUp() {
        UserController userController = new UserController(new StubUserService());
        mockMvc = MockMvcBuilders.standaloneSetup(userController)
                .setCustomArgumentResolvers(new AuthenticationPrincipalArgumentResolver())
                .build();
        objectMapper = new ObjectMapper();

        UserDetails mockUserDetails = org.springframework.security.core.userdetails.User
                .withUsername("buyer@tradetwin.com")
                .password("password123")
                .roles("BUYER")
                .build();
        authPrincipal = new UsernamePasswordAuthenticationToken(mockUserDetails, null, mockUserDetails.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(authPrincipal);
    }

    @Test
    @DisplayName("PUT /api/users/profile updates user profile and returns 200 OK")
    void testUpdateProfileEndpoint() throws Exception {
        UpdateProfileRequest req = UpdateProfileRequest.builder()
                .fullName("Alice Walker Updated")
                .defaultCurrency("EUR")
                .shippingAddress(ShippingAddress.builder().city("Berlin").build())
                .shoppingPreferences(ShoppingPreferences.builder().priceSensitivity("PREMIUM").build())
                .build();

        mockMvc.perform(put("/api/users/profile")
                        .principal(authPrincipal)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.fullName").value("Alice Walker Updated"))
                .andExpect(jsonPath("$.data.defaultCurrency").value("EUR"));
    }

    @Test
    @DisplayName("GET /api/users/profile returns current user profile")
    void testGetProfileEndpoint() throws Exception {
        mockMvc.perform(get("/api/users/profile")
                        .principal(authPrincipal))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.email").value("buyer@tradetwin.com"));
    }
}
