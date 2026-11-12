package com.tradetwin.seller.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tradetwin.seller.dto.SellerOnboardingRequest;
import com.tradetwin.seller.model.BusinessContact;
import com.tradetwin.seller.model.NegotiationStrategy;
import com.tradetwin.seller.model.SellerProfile;
import com.tradetwin.seller.model.ShippingTerms;
import com.tradetwin.seller.service.SellerService;
import com.tradetwin.user.model.User;
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

import java.math.BigDecimal;
import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class SellerControllerHttpTest {

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;
    private UsernamePasswordAuthenticationToken authPrincipal;

    static class StubUserService extends UserService {
        public StubUserService() {
            super(null);
        }

        @Override
        public User getUserByEmail(String email) {
            return User.builder()
                    .id("seller-u1")
                    .email(email)
                    .role(UserRole.ROLE_SELLER)
                    .build();
        }
    }

    static class StubSellerService extends SellerService {
        public StubSellerService() {
            super(null, null, null, null, null);
        }

        @Override
        public SellerProfile onboardSeller(String userId, SellerOnboardingRequest request) {
            return SellerProfile.builder()
                    .userId(userId)
                    .storeName(request.getStoreName())
                    .description(request.getDescription())
                    .onboardingCompleted(true)
                    .onboardingStep(4)
                    .build();
        }

        @Override
        public SellerProfile getProfileByUserId(String userId) {
            return SellerProfile.builder()
                    .userId(userId)
                    .storeName("QuantumTech Direct")
                    .build();
        }

        @Override
        public com.tradetwin.seller.dto.SellerDashboardSummaryDto getDashboardSummary(String sellerId) {
            return com.tradetwin.seller.dto.SellerDashboardSummaryDto.builder()
                    .totalProducts(5)
                    .activeListings(5)
                    .totalInventory(75)
                    .reservedInventory(1)
                    .lowStockCount(1)
                    .negotiationsReceived(3)
                    .acceptedDeals(1)
                    .totalOrders(1)
                    .totalSalesVolume(new BigDecimal("1219.00"))
                    .build();
        }
    }

    @BeforeEach
    void setUp() {
        SellerController sellerController = new SellerController(new StubSellerService(), new StubUserService());
        mockMvc = MockMvcBuilders.standaloneSetup(sellerController)
                .setCustomArgumentResolvers(new AuthenticationPrincipalArgumentResolver())
                .build();
        objectMapper = new ObjectMapper();

        UserDetails mockUserDetails = org.springframework.security.core.userdetails.User
                .withUsername("seller@tradetwin.com")
                .password("password123")
                .roles("SELLER")
                .build();
        authPrincipal = new UsernamePasswordAuthenticationToken(mockUserDetails, null, mockUserDetails.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(authPrincipal);
    }

    @Test
    @DisplayName("POST /api/sellers/onboard onboards seller and returns 201 Created")
    void testOnboardSeller() throws Exception {
        SellerOnboardingRequest req = SellerOnboardingRequest.builder()
                .storeName("QuantumTech Direct")
                .description("Top notch developer laptops")
                .logoUrl("https://example.com/logo.png")
                .businessContact(new BusinessContact("+123456", "support@store.com", "9-5 EST"))
                .shippingRegions(List.of("United States", "Canada"))
                .defaultShippingTerms(new ShippingTerms(2, new BigDecimal("100.00"), true, 30))
                .defaultStrategy(NegotiationStrategy.BALANCED)
                .autoNegotiateEnabled(true)
                .minimumReputationRequired(70.0)
                .onboardingCompleted(true)
                .onboardingStep(4)
                .build();

        mockMvc.perform(post("/api/sellers/onboard")
                        .principal(authPrincipal)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.storeName").value("QuantumTech Direct"))
                .andExpect(jsonPath("$.data.onboardingCompleted").value(true));
    }

    @Test
    @DisplayName("GET /api/sellers/me returns authenticated seller profile")
    void testGetMySellerProfile() throws Exception {
        mockMvc.perform(get("/api/sellers/me")
                        .principal(authPrincipal))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.storeName").value("QuantumTech Direct"));
    }

    @Test
    @DisplayName("GET /api/sellers/dashboard-summary returns seller metrics and activity")
    void testGetDashboardSummary() throws Exception {
        mockMvc.perform(get("/api/sellers/dashboard-summary")
                        .principal(authPrincipal))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.totalProducts").value(5))
                .andExpect(jsonPath("$.data.totalSalesVolume").value(1219.00));
    }
}
