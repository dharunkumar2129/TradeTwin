package com.tradetwin.deal.service;

import com.tradetwin.catalog.model.NegotiationPolicy;
import com.tradetwin.catalog.model.Product;
import com.tradetwin.deal.model.FairDealComparisonResult;
import com.tradetwin.deal.model.FairDealMetrics;
import com.tradetwin.deal.model.FairDealOfferEvaluation;
import com.tradetwin.negotiation.model.BuyerPreferences;
import com.tradetwin.negotiation.model.NegotiationOffer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class FairDealEngineTest {

    private FairDealEngine fairDealEngine;

    @BeforeEach
    void setUp() {
        fairDealEngine = new FairDealEngine();
    }

    @Test
    @DisplayName("Should calculate balanced FairDeal score for equitable midpoint agreement")
    void testEquitableDeal() {
        BigDecimal listPrice = new BigDecimal("1299.99");
        BigDecimal floorPrice = new BigDecimal("1099.00");
        BigDecimal buyerTarget = new BigDecimal("1100.00");
        BigDecimal buyerCeiling = new BigDecimal("1200.00");
        BigDecimal proposedPrice = new BigDecimal("1150.00");

        FairDealMetrics metrics = fairDealEngine.evaluateDeal(
                proposedPrice, listPrice, floorPrice, buyerTarget, buyerCeiling, 24, 2, 95.0
        );

        assertNotNull(metrics);
        assertTrue(metrics.getFairDealScore() >= 65.0, "FairDeal score should be high for balanced agreement: " + metrics.getFairDealScore());
        assertTrue(metrics.getUtilityBuyer() > 0.5, "Buyer utility should be favorable");
        assertTrue(metrics.getUtilitySeller() > 0.4, "Seller utility should be favorable");
        assertEquals(new BigDecimal("1199.50"), metrics.getMarketFairPriceEstimate());
        assertNotNull(metrics.getSummaryExplanation());
    }

    @Test
    @DisplayName("Should award high surplus share to buyer when price is near floor")
    void testBuyerFavorableDeal() {
        BigDecimal listPrice = new BigDecimal("1000.00");
        BigDecimal floorPrice = new BigDecimal("800.00");
        BigDecimal buyerTarget = new BigDecimal("800.00");
        BigDecimal buyerCeiling = new BigDecimal("950.00");
        BigDecimal proposedPrice = new BigDecimal("820.00");

        FairDealMetrics metrics = fairDealEngine.evaluateDeal(
                proposedPrice, listPrice, floorPrice, buyerTarget, buyerCeiling, 12, 3, 90.0
        );

        assertEquals(90.0, metrics.getSurplusSplitPctBuyer(), 0.1);
        assertEquals(10.0, metrics.getSurplusSplitPctSeller(), 0.1);
    }

    @Test
    @DisplayName("Hard Filter: Should reject offer when landed cost exceeds buyer ceiling budget")
    void testHardFilterBudgetExceeded() {
        Product product = Product.builder()
                .id("p1")
                .sellerId("s1")
                .title("Sony WH-1000XM5")
                .brand("Sony")
                .listPrice(new BigDecimal("399.00"))
                .shippingCost(new BigDecimal("15.00"))
                .inventoryCount(10)
                .build();

        BuyerPreferences pref = BuyerPreferences.builder()
                .targetPrice(new BigDecimal("300.00"))
                .maxCeilingPrice(new BigDecimal("350.00")) // Max budget is $350
                .desiredDeliveryDays(3)
                .build();

        NegotiationOffer offer = NegotiationOffer.builder()
                .id("off1")
                .sellerId("s1")
                .productId("p1")
                .unitPrice(new BigDecimal("345.00"))
                .quantity(1)
                .shippingCost(new BigDecimal("15.00")) // Landed cost = 345 + 15 = $360 > $350
                .deliveryEstimateDays(2)
                .warrantyMonths(12)
                .build();

        FairDealOfferEvaluation eval = fairDealEngine.evaluateOffer(
                offer, product, null, pref, "Sony", 95.0
        );

        assertFalse(eval.isPassedHardFilters(), "Landed cost $360.00 must fail hard filter against $350.00 ceiling");
        assertNotNull(eval.getFilterFailureReason());
        assertTrue(eval.getFilterFailureReason().contains("exceeds buyer maximum budget ceiling"));
        assertEquals(0.0, eval.getOverallDealScore());
    }

    @Test
    @DisplayName("Hard Filter: Should reject offer when seller floor price is breached")
    void testHardFilterFloorPriceBreached() {
        Product product = Product.builder()
                .id("p1")
                .sellerId("s1")
                .title("Anker Wireless Charger")
                .listPrice(new BigDecimal("50.00"))
                .shippingCost(BigDecimal.ZERO)
                .inventoryCount(20)
                .build();

        NegotiationPolicy policy = NegotiationPolicy.builder()
                .floorPrice(new BigDecimal("38.00"))
                .build();

        BuyerPreferences pref = BuyerPreferences.builder()
                .targetPrice(new BigDecimal("30.00"))
                .maxCeilingPrice(new BigDecimal("45.00"))
                .build();

        NegotiationOffer offer = NegotiationOffer.builder()
                .id("off2")
                .sellerId("s1")
                .productId("p1")
                .unitPrice(new BigDecimal("35.00")) // Below floor $38.00
                .quantity(1)
                .shippingCost(BigDecimal.ZERO)
                .build();

        FairDealOfferEvaluation eval = fairDealEngine.evaluateOffer(
                offer, product, policy, pref, null, 90.0
        );

        assertFalse(eval.isPassedHardFilters(), "Offer below floor price must fail hard filter");
        assertTrue(eval.getFilterFailureReason().contains("breaches merchant reserve floor"));
    }

    @Test
    @DisplayName("Transparent Scoring: Should compute all 4 composite scores and sub-scores with mathematical explanation")
    void testEvaluateOfferTransparentScoring() {
        Product product = Product.builder()
                .id("p1")
                .sellerId("s1")
                .title("Bose QuietComfort 45")
                .brand("Bose")
                .listPrice(new BigDecimal("329.00"))
                .shippingCost(new BigDecimal("5.00"))
                .inventoryCount(25)
                .returnPolicy("30 days money-back guarantee")
                .build();

        NegotiationPolicy policy = NegotiationPolicy.builder()
                .floorPrice(new BigDecimal("250.00"))
                .build();

        BuyerPreferences pref = BuyerPreferences.builder()
                .targetPrice(new BigDecimal("260.00"))
                .maxCeilingPrice(new BigDecimal("300.00"))
                .desiredDeliveryDays(4)
                .build();

        NegotiationOffer offer = NegotiationOffer.builder()
                .id("off3")
                .sellerId("s1")
                .productId("p1")
                .unitPrice(new BigDecimal("270.00"))
                .quantity(1)
                .shippingCost(new BigDecimal("5.00")) // Landed cost = $275.00
                .deliveryEstimateDays(3)
                .warrantyMonths(24)
                .perks(List.of("Free Travel Case", "Priority Courier"))
                .build();

        FairDealOfferEvaluation eval = fairDealEngine.evaluateOffer(
                offer, product, policy, pref, "Bose", 95.0
        );

        assertTrue(eval.isPassedHardFilters());
        assertNull(eval.getFilterFailureReason());
        assertEquals(new BigDecimal("275.00"), eval.getTotalLandedCost());

        // Verify the 4 required aggregate scores
        assertTrue(eval.getBuyerSatisfactionScore() > 60.0, "Buyer satisfaction score: " + eval.getBuyerSatisfactionScore());
        assertTrue(eval.getOfferQualityScore() > 70.0, "Offer quality score: " + eval.getOfferQualityScore());
        assertTrue(eval.getSellerPolicyCompliance() > 40.0, "Seller policy compliance: " + eval.getSellerPolicyCompliance());
        assertTrue(eval.getOverallDealScore() > 60.0, "Overall deal score: " + eval.getOverallDealScore());

        // Verify sub-score breakdown
        assertNotNull(eval.getBreakdown());
        assertEquals(100.0, eval.getBreakdown().getBrandMatchScore(), "Exact brand match score");
        assertEquals(100.0, eval.getBreakdown().getDeliveryScore(), "Delivery within desired days");
        assertEquals(100.0, eval.getBreakdown().getWarrantyScore(), "24 months warranty score");
        assertEquals(100.0, eval.getBreakdown().getReturnConditionsScore(), "30-day return policy score");

        // Verify transparent calculation explanation
        assertNotNull(eval.getCalculationExplanation());
        assertTrue(eval.getCalculationExplanation().contains("Buyer Satisfaction"));
        assertTrue(eval.getCalculationExplanation().contains("Offer Quality"));
        assertTrue(eval.getCalculationExplanation().contains("Seller Compliance"));
        assertTrue(eval.getCalculationExplanation().contains("Overall Deal Score"));
    }

    @Test
    @DisplayName("Compare Offers: Should rank eligible offers and select best deal")
    void testCompareOffersRankingAndSelection() {
        NegotiationPolicy policyA = NegotiationPolicy.builder()
                .floorPrice(new BigDecimal("230.00"))
                .build();

        Product prodA = Product.builder()
                .id("pA")
                .sellerId("sA")
                .title("Seller A - Sony ANC Headphones")
                .brand("Sony")
                .listPrice(new BigDecimal("300.00"))
                .shippingCost(new BigDecimal("10.00"))
                .inventoryCount(20)
                .negotiationPolicy(policyA)
                .build();

        NegotiationPolicy policyB = NegotiationPolicy.builder()
                .floorPrice(new BigDecimal("220.00"))
                .build();

        Product prodB = Product.builder()
                .id("pB")
                .sellerId("sB")
                .title("Seller B - Sony ANC Headphones Special")
                .brand("Sony")
                .listPrice(new BigDecimal("300.00"))
                .shippingCost(BigDecimal.ZERO) // Free shipping
                .inventoryCount(15)
                .returnPolicy("30 days free returns")
                .negotiationPolicy(policyB)
                .build();

        BuyerPreferences pref = BuyerPreferences.builder()
                .targetPrice(new BigDecimal("240.00"))
                .maxCeilingPrice(new BigDecimal("280.00"))
                .desiredDeliveryDays(4)
                .build();

        NegotiationOffer offerA = NegotiationOffer.builder()
                .id("offA")
                .sellerId("sA")
                .productId("pA")
                .unitPrice(new BigDecimal("260.00"))
                .shippingCost(new BigDecimal("10.00")) // Landed $270
                .deliveryEstimateDays(5)
                .warrantyMonths(12)
                .build();

        NegotiationOffer offerB = NegotiationOffer.builder()
                .id("offB")
                .sellerId("sB")
                .productId("pB")
                .unitPrice(new BigDecimal("245.00"))
                .shippingCost(BigDecimal.ZERO) // Landed $245 (cheaper + faster + longer warranty)
                .deliveryEstimateDays(2)
                .warrantyMonths(24)
                .perks(List.of("Free Carrying Case"))
                .build();

        FairDealComparisonResult result = fairDealEngine.compareOffers(
                List.of(offerA, offerB),
                List.of(prodA, prodB),
                pref,
                "Sony",
                95.0
        );

        assertTrue(result.isHasViableDeal());
        assertNotNull(result.getBestOffer());
        assertEquals("sB", result.getBestOffer().getSellerId(), "Offer B should be ranked higher due to lower landed cost, faster delivery and longer warranty");
        assertNotNull(result.getSummaryExplanation());
    }

    @Test
    @DisplayName("Compare Offers: If no offer satisfies mandatory requirements, return no deal")
    void testCompareOffersNoDealWhenAllFailHardFilters() {
        Product prodA = Product.builder()
                .id("pA")
                .sellerId("sA")
                .title("Headphones A")
                .listPrice(new BigDecimal("200.00"))
                .shippingCost(new BigDecimal("10.00"))
                .inventoryCount(10)
                .build();

        BuyerPreferences pref = BuyerPreferences.builder()
                .targetPrice(new BigDecimal("100.00"))
                .maxCeilingPrice(new BigDecimal("120.00")) // Strict budget ceiling $120
                .build();

        NegotiationOffer offerA = NegotiationOffer.builder()
                .id("offA")
                .sellerId("sA")
                .productId("pA")
                .unitPrice(new BigDecimal("150.00"))
                .shippingCost(new BigDecimal("10.00")) // Landed $160 > $120
                .build();

        FairDealComparisonResult result = fairDealEngine.compareOffers(
                List.of(offerA),
                List.of(prodA),
                pref,
                null,
                90.0
        );

        assertFalse(result.isHasViableDeal(), "Must return no deal when all offers fail mandatory requirements");
        assertNull(result.getBestOffer());
        assertTrue(result.getSummaryExplanation().contains("No Deal"));
    }
}
