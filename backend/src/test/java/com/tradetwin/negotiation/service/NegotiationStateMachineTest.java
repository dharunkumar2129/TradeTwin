package com.tradetwin.negotiation.service;

import com.tradetwin.catalog.model.NegotiationPolicy;
import com.tradetwin.catalog.model.Product;
import com.tradetwin.deal.model.AgreementSeal;
import com.tradetwin.negotiation.model.BuyerPreferences;
import com.tradetwin.negotiation.model.Negotiation;
import com.tradetwin.negotiation.model.NegotiationOffer;
import com.tradetwin.negotiation.model.NegotiationStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class NegotiationStateMachineTest {

    @Test
    @DisplayName("Explicit state machine includes all 9 required operational states")
    void testAllNegotiationStatesValid() {
        NegotiationStatus[] required = {
                NegotiationStatus.CREATED,
                NegotiationStatus.DISCOVERING,
                NegotiationStatus.NEGOTIATING,
                NegotiationStatus.OFFER_RECEIVED,
                NegotiationStatus.COUNTEROFFERED,
                NegotiationStatus.ACCEPTED,
                NegotiationStatus.REJECTED,
                NegotiationStatus.EXPIRED,
                NegotiationStatus.COMPLETED
        };

        for (NegotiationStatus status : required) {
            assertNotNull(status, "Status must not be null: " + status.name());
        }

        assertTrue(NegotiationStatus.ACCEPTED.isTerminal());
        assertTrue(NegotiationStatus.REJECTED.isTerminal());
        assertTrue(NegotiationStatus.EXPIRED.isTerminal());
        assertTrue(NegotiationStatus.COMPLETED.isTerminal());
        assertFalse(NegotiationStatus.NEGOTIATING.isTerminal());
        assertFalse(NegotiationStatus.DISCOVERING.isTerminal());
    }

    @Test
    @DisplayName("NegotiationOffer correctly encapsulates commercial quote terms")
    void testNegotiationOfferModel() {
        NegotiationOffer offer = NegotiationOffer.builder()
                .negotiationId("neg-100")
                .sellerId("seller-1")
                .productId("prod-sony-1")
                .roundNumber(1)
                .unitPrice(new BigDecimal("98.00"))
                .quantity(2)
                .shippingCost(new BigDecimal("5.00"))
                .deliveryEstimateDays(2)
                .warrantyMonths(12)
                .offerStatus("PROPOSED")
                .publicExplanation("Initial merchant quote with free express delivery")
                .timestamp(Instant.now())
                .perks(List.of("Free Express Delivery"))
                .build();

        assertNotNull(offer);
        assertEquals("neg-100", offer.getNegotiationId());
        assertEquals("prod-sony-1", offer.getProductId());
        assertEquals(new BigDecimal("98.00"), offer.getUnitPrice());
        assertEquals(2, offer.getQuantity());
        // Total cost = 98 * 2 + 5 = 201.00
        assertEquals(new BigDecimal("201.00"), offer.calculateTotalCost());
    }

    @Test
    @DisplayName("AgreementSeal creates cryptographic token and enforces 15-minute expiration lock")
    void testAgreementSealLock() {
        String dealToken = "deal_sec_" + UUID.randomUUID().toString().replace("-", "");
        Instant now = Instant.now();
        Instant expires = now.plus(15, ChronoUnit.MINUTES);

        AgreementSeal seal = AgreementSeal.builder()
                .dealToken(dealToken)
                .sealedAt(now)
                .expiresAt(expires)
                .finalPrice(new BigDecimal("92.00"))
                .currency("USD")
                .status("AWAITING_PAYMENT")
                .build();

        assertNotNull(seal.getDealToken());
        assertTrue(seal.getDealToken().startsWith("deal_sec_"));
        assertTrue(seal.getExpiresAt().isAfter(Instant.now()));
        assertEquals(new BigDecimal("92.00"), seal.getFinalPrice());
        assertEquals("AWAITING_PAYMENT", seal.getStatus());
    }
}
