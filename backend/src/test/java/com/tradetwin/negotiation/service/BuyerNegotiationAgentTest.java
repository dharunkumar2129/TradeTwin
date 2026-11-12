package com.tradetwin.negotiation.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tradetwin.negotiation.model.BuyerPreferences;
import com.tradetwin.negotiation.model.NegotiationOffer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class BuyerNegotiationAgentTest {

    private BuyerNegotiationAgent buyerAgent;
    private BuyerPreferences buyerPreferences;

    @BeforeEach
    void setUp() {
        buyerAgent = new BuyerNegotiationAgent(new ObjectMapper());

        buyerPreferences = BuyerPreferences.builder()
                .targetPrice(new BigDecimal("85.00"))
                .maxCeilingPrice(new BigDecimal("100.00"))
                .desiredDeliveryDays(3)
                .requireExtendedWarranty(true)
                .build();
    }

    @Test
    @DisplayName("Buyer agent strictly enforces private maximum ceiling budget ($100.00)")
    void testCeilingBudgetGuardrailOnProposedPrice() {
        NegotiationOffer expensiveQuote = NegotiationOffer.builder()
                .sellerId("seller-1")
                .productId("prod-1")
                .unitPrice(new BigDecimal("140.00"))
                .build();

        BuyerNegotiationAgent.BuyerDecisionContext ctx = new BuyerNegotiationAgent.BuyerDecisionContext(
                "neg-1", buyerPreferences, List.of(expensiveQuote), List.of(), 2, 3
        );

        BuyerNegotiationAgent.BuyerDecision decision = buyerAgent.makeDecision(ctx);

        assertNotNull(decision);
        assertTrue(decision.getProposedPrice().compareTo(new BigDecimal("100.00")) <= 0,
                "Proposed buyer price must strictly be <= ceiling budget ($100.00), got: " + decision.getProposedPrice());
    }

    @Test
    @DisplayName("Buyer agent overrules ACCEPT if quote violates private maximum ceiling ($100.00)")
    void testOverruleAcceptAboveCeiling() {
        NegotiationOffer overBudgetQuote = NegotiationOffer.builder()
                .sellerId("seller-1")
                .productId("prod-1")
                .unitPrice(new BigDecimal("110.00"))
                .build();

        BuyerNegotiationAgent.BuyerDecisionContext ctx = new BuyerNegotiationAgent.BuyerDecisionContext(
                "neg-1", buyerPreferences, List.of(overBudgetQuote), List.of(), 3, 3
        );

        BuyerNegotiationAgent.BuyerDecision decision = buyerAgent.makeDecision(ctx);

        assertNotNull(decision);
        assertNotEquals("ACCEPT", decision.getAction(), "Buyer agent must NEVER accept an offer above ceiling budget");
    }

    @Test
    @DisplayName("Buyer agent anchors initial opening proposal at target price ($85.00)")
    void testOpeningBidAnchoring() {
        NegotiationOffer openingQuote = NegotiationOffer.builder()
                .sellerId("seller-1")
                .productId("prod-1")
                .unitPrice(new BigDecimal("98.00"))
                .build();

        BuyerNegotiationAgent.BuyerDecisionContext ctx = new BuyerNegotiationAgent.BuyerDecisionContext(
                "neg-1", buyerPreferences, List.of(openingQuote), List.of(), 1, 3
        );

        BuyerNegotiationAgent.BuyerDecision decision = buyerAgent.makeDecision(ctx);

        assertNotNull(decision);
        assertEquals("PROPOSE", decision.getAction());
        assertEquals(new BigDecimal("85.00"), decision.getProposedPrice(), "Opening proposal must be anchored at target price");
    }

    @Test
    @DisplayName("Buyer agent explanation never discloses private ceiling budget ($100.00)")
    void testZeroLeakageOfPrivateCeiling() {
        NegotiationOffer quote = NegotiationOffer.builder()
                .sellerId("seller-1")
                .productId("prod-1")
                .unitPrice(new BigDecimal("98.00"))
                .build();

        BuyerNegotiationAgent.BuyerDecisionContext ctx = new BuyerNegotiationAgent.BuyerDecisionContext(
                "neg-1", buyerPreferences, List.of(quote), List.of(), 2, 3
        );

        BuyerNegotiationAgent.BuyerDecision decision = buyerAgent.makeDecision(ctx);

        assertNotNull(decision.getPublicExplanation());
        assertFalse(decision.getPublicExplanation().contains("100.00"),
                "Private ceiling budget must NEVER be leaked in public reasoning");
    }
}
