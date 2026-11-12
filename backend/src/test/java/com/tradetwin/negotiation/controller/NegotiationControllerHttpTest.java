package com.tradetwin.negotiation.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tradetwin.deal.model.FairDealComparisonResult;
import com.tradetwin.deal.model.FairDealOfferEvaluation;
import com.tradetwin.event.model.NegotiationEventDocument;
import com.tradetwin.event.service.SseStreamingHub;
import com.tradetwin.negotiation.dto.SelectDealRequest;
import com.tradetwin.negotiation.model.BuyerPreferences;
import com.tradetwin.negotiation.model.Negotiation;
import com.tradetwin.negotiation.model.NegotiationStatus;
import com.tradetwin.negotiation.service.NegotiationOrchestrator;
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
import java.time.Instant;
import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class NegotiationControllerHttpTest {

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;
    private UsernamePasswordAuthenticationToken authPrincipal;

    static class StubOrchestrator extends NegotiationOrchestrator {
        public StubOrchestrator() {
            super(null, null, null, null, null, null, null, null);
        }

        @Override
        public Negotiation getNegotiationById(String id) {
            return Negotiation.builder()
                    .id(id)
                    .buyerId("buyer-123")
                    .sellerId("seller-A")
                    .productId("prod-A")
                    .status(NegotiationStatus.AGREED)
                    .buyerPreferences(BuyerPreferences.builder()
                            .targetPrice(new BigDecimal("85.00"))
                            .maxCeilingPrice(new BigDecimal("100.00"))
                            .desiredDeliveryDays(3)
                            .requireExtendedWarranty(true)
                            .build())
                    .build();
        }

        @Override
        public FairDealComparisonResult compareOffersForNegotiation(String negotiationId) {
            FairDealOfferEvaluation eval = FairDealOfferEvaluation.builder()
                    .sellerId("seller-A")
                    .productId("prod-A")
                    .productTitle("Sony WH-1000XM5")
                    .unitPrice(new BigDecimal("280.00"))
                    .totalLandedCost(new BigDecimal("285.00"))
                    .passedHardFilters(true)
                    .buyerSatisfactionScore(88.0)
                    .offerQualityScore(90.0)
                    .sellerPolicyCompliance(82.0)
                    .overallDealScore(86.5)
                    .calculationExplanation("Landed Cost: $285.00 vs Target $250.00")
                    .build();

            return FairDealComparisonResult.builder()
                    .hasViableDeal(true)
                    .summaryExplanation("Top recommendation from seller-A")
                    .evaluations(List.of(eval))
                    .bestOffer(eval)
                    .build();
        }

        @Override
        public Negotiation selectAndApproveDeal(String negotiationId, String offerId, String buyerId) {
            return Negotiation.builder()
                    .id(negotiationId)
                    .buyerId(buyerId)
                    .productId(offerId)
                    .sellerId("seller-A")
                    .status(NegotiationStatus.ACCEPTED)
                    .build();
        }
    }

    static class StubUserService extends UserService {
        public StubUserService() {
            super(null);
        }

        @Override
        public User getUserByEmail(String email) {
            return User.builder()
                    .id("buyer-123")
                    .email(email)
                    .role(UserRole.ROLE_BUYER)
                    .build();
        }
    }

    static class StubStreamingHub extends SseStreamingHub {
        public StubStreamingHub() {
            super(null);
        }

        @Override
        public List<NegotiationEventDocument> getPersistedEvents(String negotiationId) {
            NegotiationEventDocument event1 = NegotiationEventDocument.builder()
                    .id("e1")
                    .negotiationId(negotiationId)
                    .eventType("NEGOTIATION_STARTED")
                    .speaker("MARKETPLACE")
                    .turnNumber(0)
                    .timestamp(Instant.now())
                    .build();

            NegotiationEventDocument event2 = NegotiationEventDocument.builder()
                    .id("e2")
                    .negotiationId(negotiationId)
                    .eventType("SELLER_INITIAL_OFFER")
                    .speaker("SELLER_AGENT")
                    .turnNumber(1)
                    .timestamp(Instant.now())
                    .build();

            return List.of(event1, event2);
        }
    }

    @BeforeEach
    void setUp() {
        NegotiationController controller = new NegotiationController(
                new StubOrchestrator(),
                new StubUserService(),
                new StubStreamingHub()
        );

        mockMvc = MockMvcBuilders.standaloneSetup(controller)
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
    @DisplayName("GET /api/negotiations/{id}/events returns persisted event stream")
    void testGetPersistedEventsEndpoint() throws Exception {
        mockMvc.perform(get("/api/negotiations/neg-100/events"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].eventType").value("NEGOTIATION_STARTED"))
                .andExpect(jsonPath("$.data[1].eventType").value("SELLER_INITIAL_OFFER"));
    }

    @Test
    @DisplayName("GET /api/negotiations/{id}/compare-offers returns FairDeal comparison matrix")
    void testCompareOffersEndpoint() throws Exception {
        mockMvc.perform(get("/api/negotiations/neg-100/compare-offers"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.hasViableDeal").value(true))
                .andExpect(jsonPath("$.data.bestOffer.sellerId").value("seller-A"))
                .andExpect(jsonPath("$.data.bestOffer.overallDealScore").value(86.5));
    }

    @Test
    @DisplayName("POST /api/negotiations/{id}/select-deal allows buyer to explicitly approve deal")
    void testSelectDealEndpoint() throws Exception {
        SelectDealRequest req = new SelectDealRequest("prod-A");

        mockMvc.perform(post("/api/negotiations/neg-100/select-deal")
                        .principal(authPrincipal)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("ACCEPTED"))
                .andExpect(jsonPath("$.data.productId").value("prod-A"));
    }

    @Test
    @DisplayName("GET /api/negotiations/{id} masks private maxCeilingPrice for unauthenticated callers")
    void testGetNegotiationById_MasksBuyerMaxCeilingForUnauthorizedCaller() throws Exception {
        SecurityContextHolder.clearContext();

        // Unauthenticated caller must NOT see private maxCeilingPrice
        mockMvc.perform(get("/api/v1/negotiations/neg-100"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.buyerPreferences.targetPrice").value(85.0))
                .andExpect(jsonPath("$.data.buyerPreferences.maxCeilingPrice").doesNotExist());

        // Owning authenticated buyer receives their session configuration
        SecurityContextHolder.getContext().setAuthentication(authPrincipal);
        mockMvc.perform(get("/api/v1/negotiations/neg-100").principal(authPrincipal))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.buyerPreferences.maxCeilingPrice").value(100.0));
    }
}
