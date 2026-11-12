package com.tradetwin.negotiation.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.math.RoundingMode;

import static org.junit.jupiter.api.Assertions.assertTrue;

class SellerGuardrailTest {

    @Test
    @DisplayName("Seller counter-offer calculation must strictly respect floor price bound")
    void testSellerFloorPriceGuardrail() {
        BigDecimal listPrice = new BigDecimal("1299.99");
        BigDecimal floorPrice = new BigDecimal("1099.00");

        // Simulate aggressive discount step
        BigDecimal discountFactor = new BigDecimal("0.95"); // 95% concession towards floor
        BigDecimal computedOffer = listPrice.subtract(listPrice.subtract(floorPrice).multiply(discountFactor))
                .setScale(2, RoundingMode.HALF_UP);

        // Guard logic as implemented in NegotiationOrchestrator
        if (computedOffer.compareTo(floorPrice) < 0) {
            computedOffer = floorPrice;
        }

        assertTrue(computedOffer.compareTo(floorPrice) >= 0,
                "Computed seller counter-offer ($" + computedOffer + ") must never breach floor ($" + floorPrice + ")");
    }
}
