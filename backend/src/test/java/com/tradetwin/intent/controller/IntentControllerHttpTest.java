package com.tradetwin.intent.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tradetwin.catalog.dto.SearchResultItemDto;
import com.tradetwin.intent.model.BuyerIntent;
import com.tradetwin.intent.service.IntentParsingService;
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
import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class IntentControllerHttpTest {

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;
    private UsernamePasswordAuthenticationToken authPrincipal;

    static class StubUserService extends UserService {
        public StubUserService() { super(null); }
        @Override
        public User getUserByEmail(String email) {
            return User.builder().id("buyer-u1").email(email).role(UserRole.ROLE_BUYER).build();
        }
    }

    static class StubIntentParsingService extends IntentParsingService {
        public StubIntentParsingService() { super(null, null, null); }

        @Override
        public Map<String, Object> parseAndMatch(String rawQuery, String buyerId) {
            BuyerIntent intent = BuyerIntent.builder()
                    .id("intent-123")
                    .buyerId(buyerId)
                    .rawQuery(rawQuery)
                    .productType("Wireless Noise-Cancelling Headphones")
                    .brandPreferences(List.of("Sony"))
                    .maxBudget(new BigDecimal("100.00"))
                    .minWarrantyMonths(12)
                    .maxDeliveryDays(4)
                    .status("DRAFT")
                    .build();

            SearchResultItemDto candidate = SearchResultItemDto.builder()
                    .unitPrice(new BigDecimal("98.00"))
                    .estimatedTotalPrice(new BigDecimal("98.00"))
                    .matchScore(96.0)
                    .sellerStoreName("AudioSphere Official")
                    .build();

            return Map.of(
                    "intent", intent,
                    "candidates", List.of(candidate),
                    "totalMatched", 1,
                    "searchSummary", "Found 1 match"
            );
        }

        @Override
        public BuyerIntent confirmIntent(BuyerIntent intent, String buyerId) {
            intent.setId("intent-confirmed-1");
            intent.setBuyerId(buyerId);
            intent.setStatus("CONFIRMED");
            return intent;
        }

        @Override
        public List<BuyerIntent> getIntentsForBuyer(String buyerId) {
            return List.of(BuyerIntent.builder().id("intent-1").buyerId(buyerId).status("CONFIRMED").build());
        }
    }

    @BeforeEach
    void setUp() {
        IntentController intentController = new IntentController(new StubIntentParsingService(), new StubUserService());
        mockMvc = MockMvcBuilders.standaloneSetup(intentController)
                .setCustomArgumentResolvers(new AuthenticationPrincipalArgumentResolver())
                .build();
        objectMapper = new ObjectMapper().registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule());

        UserDetails mockUserDetails = org.springframework.security.core.userdetails.User
                .withUsername("buyer@tradetwin.com")
                .password("password123")
                .roles("BUYER")
                .build();
        authPrincipal = new UsernamePasswordAuthenticationToken(mockUserDetails, null, mockUserDetails.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(authPrincipal);
    }

    @Test
    @DisplayName("POST /api/intents/parse parses query and returns structured intent with candidate listings")
    void testParseIntentEndpoint() throws Exception {
        Map<String, String> request = Map.of(
                "query", "Find me Sony wireless noise-cancelling headphones under $100"
        );

        mockMvc.perform(post("/api/intents/parse")
                        .principal(authPrincipal)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.intent.productType").value("Wireless Noise-Cancelling Headphones"))
                .andExpect(jsonPath("$.data.intent.brandPreferences[0]").value("Sony"))
                .andExpect(jsonPath("$.data.candidates[0].unitPrice").value(98.00));
    }

    @Test
    @DisplayName("POST /api/intents confirms buyer intent and persists state")
    void testConfirmIntentEndpoint() throws Exception {
        BuyerIntent intent = BuyerIntent.builder()
                .productType("Wireless Noise-Cancelling Headphones")
                .maxBudget(new BigDecimal("100.00"))
                .build();

        mockMvc.perform(post("/api/intents")
                        .principal(authPrincipal)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(intent)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("CONFIRMED"));
    }

    @Test
    @DisplayName("GET /api/intents returns buyer intent history")
    void testGetMyIntents() throws Exception {
        mockMvc.perform(get("/api/intents").principal(authPrincipal))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].id").value("intent-1"));
    }
}
