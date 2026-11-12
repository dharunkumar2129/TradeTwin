package com.tradetwin.deal.service;

import com.tradetwin.catalog.model.NegotiationPolicy;
import com.tradetwin.catalog.model.Product;
import com.tradetwin.deal.model.FairDealComparisonResult;
import com.tradetwin.deal.model.FairDealMetrics;
import com.tradetwin.deal.model.FairDealOfferEvaluation;
import com.tradetwin.deal.model.ScoreBreakdown;
import com.tradetwin.negotiation.model.BuyerPreferences;
import com.tradetwin.negotiation.model.NegotiationOffer;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
public class FairDealEngine {

    /**
     * Legacy evaluateDeal method preserved for backwards compatibility and test parity.
     */
    public FairDealMetrics evaluateDeal(
            BigDecimal proposedPrice,
            BigDecimal listPrice,
            BigDecimal floorPrice,
            BigDecimal buyerTarget,
            BigDecimal buyerCeiling,
            int warrantyMonths,
            int deliveryDays,
            double buyerReputation) {

        BigDecimal marketEstimate = listPrice.add(floorPrice).divide(new BigDecimal("2.0"), 2, RoundingMode.HALF_UP);

        BigDecimal priceRangeBuyer = buyerCeiling.subtract(buyerTarget);
        double buyerPriceFactor = 0.5;
        if (priceRangeBuyer.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal priceDiff = buyerCeiling.subtract(proposedPrice);
            buyerPriceFactor = Math.max(0.0, Math.min(1.0,
                    priceDiff.divide(priceRangeBuyer, 4, RoundingMode.HALF_UP).doubleValue()));
        }
        double warrantyFactor = Math.min(1.0, (double) warrantyMonths / 24.0);
        double deliveryFactor = deliveryDays <= 2 ? 1.0 : (deliveryDays <= 4 ? 0.8 : 0.5);
        double utilityBuyer = (0.60 * buyerPriceFactor) + (0.20 * warrantyFactor) + (0.20 * deliveryFactor);

        BigDecimal marginRange = listPrice.subtract(floorPrice);
        double sellerPriceFactor = 0.5;
        if (marginRange.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal marginSurplus = proposedPrice.subtract(floorPrice);
            sellerPriceFactor = Math.max(0.0, Math.min(1.0,
                    marginSurplus.divide(marginRange, 4, RoundingMode.HALF_UP).doubleValue()));
        }
        double sellerRepFactor = Math.min(1.0, buyerReputation / 100.0);
        double utilitySeller = (0.70 * sellerPriceFactor) + (0.30 * sellerRepFactor);

        BigDecimal totalDiscount = listPrice.subtract(proposedPrice);
        BigDecimal sellerMarginKept = proposedPrice.subtract(floorPrice);
        BigDecimal totalBargainingSurplus = listPrice.subtract(floorPrice);

        double surplusSplitBuyer = 50.0;
        double surplusSplitSeller = 50.0;
        if (totalBargainingSurplus.compareTo(BigDecimal.ZERO) > 0) {
            surplusSplitBuyer = totalDiscount.divide(totalBargainingSurplus, 4, RoundingMode.HALF_UP).doubleValue() * 100.0;
            surplusSplitSeller = 100.0 - surplusSplitBuyer;
        }

        double splitDifference = Math.abs(surplusSplitBuyer - surplusSplitSeller);
        double balanceRatio = 1.0 - (splitDifference / 100.0 * 0.25);

        double combinedUtility = (utilityBuyer + utilitySeller) / 2.0;
        double score = (50.0 + (combinedUtility * 45.0)) * balanceRatio;
        double finalScore = Math.round(Math.max(10.0, Math.min(99.0, score)) * 10.0) / 10.0;

        String explanation = String.format(
                "Equitable agreement: Price of $%s yields a %.1f%% buyer surplus share and retains a $%.2f seller margin above reserve floor.",
                proposedPrice.toPlainString(),
                surplusSplitBuyer,
                sellerMarginKept.doubleValue()
        );

        // Build transparent sub-scores for new metrics dimensions
        ScoreBreakdown breakdown = ScoreBreakdown.builder()
                .landedCostScore(Math.round(buyerPriceFactor * 100.0 * 10.0) / 10.0)
                .brandMatchScore(85.0)
                .specMatchScore(85.0)
                .deliveryScore(Math.round(deliveryFactor * 100.0 * 10.0) / 10.0)
                .warrantyScore(Math.round(warrantyFactor * 100.0 * 10.0) / 10.0)
                .quantityScore(100.0)
                .returnConditionsScore(80.0)
                .constraintComplianceScore(Math.round(sellerPriceFactor * 100.0 * 10.0) / 10.0)
                .build();

        double buyerSat = Math.round(utilityBuyer * 100.0 * 10.0) / 10.0;
        double sellerComp = Math.round(utilitySeller * 100.0 * 10.0) / 10.0;
        double offerQual = Math.round(((buyerSat + sellerComp) / 2.0) * 10.0) / 10.0;

        return FairDealMetrics.builder()
                .fairDealScore(finalScore)
                .overallDealScore(finalScore)
                .priceEquitability(Math.round(balanceRatio * 100.0) / 100.0)
                .utilityBuyer(Math.round(utilityBuyer * 100.0) / 100.0)
                .utilitySeller(Math.round(utilitySeller * 100.0) / 100.0)
                .buyerSatisfactionScore(buyerSat)
                .offerQualityScore(offerQual)
                .sellerPolicyCompliance(sellerComp)
                .surplusSplitPctBuyer(Math.round(surplusSplitBuyer * 10.0) / 10.0)
                .surplusSplitPctSeller(Math.round(surplusSplitSeller * 10.0) / 10.0)
                .totalLandedCost(proposedPrice)
                .marketFairPriceEstimate(marketEstimate)
                .scoreBreakdown(breakdown)
                .passedHardFilters(true)
                .calculationExplanation(explanation)
                .summaryExplanation(explanation)
                .build();
    }

    /**
     * Evaluates an individual seller offer against buyer preferences and seller product policy.
     * Enforces mandatory hard filters first. If hard filters fail, scoring is withheld.
     */
    public FairDealOfferEvaluation evaluateOffer(
            NegotiationOffer offer,
            Product product,
            NegotiationPolicy policy,
            BuyerPreferences pref,
            String preferredBrand,
            double buyerReputation) {

        int quantity = offer.getQuantity() > 0 ? offer.getQuantity() : 1;
        BigDecimal unitPrice = offer.getUnitPrice() != null ? offer.getUnitPrice() : BigDecimal.ZERO;
        BigDecimal shippingCost = offer.getShippingCost() != null ? offer.getShippingCost() : (product.getShippingCost() != null ? product.getShippingCost() : BigDecimal.ZERO);
        BigDecimal totalLandedCost = unitPrice.multiply(new BigDecimal(quantity)).add(shippingCost).setScale(2, RoundingMode.HALF_UP);

        BigDecimal floorPrice = policy != null && policy.getFloorPrice() != null
                ? policy.getFloorPrice()
                : product.getListPrice().multiply(new BigDecimal("0.85")).setScale(2, RoundingMode.HALF_UP);

        // ============================================================
        // 1. MANDATORY HARD FILTERS
        // ============================================================

        // Filter 1: Landed Cost <= Buyer Maximum Ceiling Budget
        if (pref != null && pref.getMaxCeilingPrice() != null && totalLandedCost.compareTo(pref.getMaxCeilingPrice()) > 0) {
            return FairDealOfferEvaluation.builder()
                    .offerId(offer.getId())
                    .sellerId(offer.getSellerId())
                    .productId(product.getId())
                    .productTitle(product.getTitle())
                    .brand(product.getBrand())
                    .unitPrice(unitPrice)
                    .quantity(quantity)
                    .shippingCost(shippingCost)
                    .totalLandedCost(totalLandedCost)
                    .deliveryEstimateDays(offer.getDeliveryEstimateDays())
                    .warrantyMonths(offer.getWarrantyMonths())
                    .perks(offer.getPerks())
                    .passedHardFilters(false)
                    .filterFailureReason(String.format("Total landed cost ($%s) strictly exceeds buyer maximum budget ceiling ($%s)",
                            totalLandedCost.toPlainString(), pref.getMaxCeilingPrice().toPlainString()))
                    .buyerSatisfactionScore(0.0)
                    .offerQualityScore(0.0)
                    .sellerPolicyCompliance(0.0)
                    .overallDealScore(0.0)
                    .calculationExplanation("REJECTED: Offer violated mandatory budget constraint.")
                    .build();
        }

        // Filter 2: Quantity Compatibility & Stock Availability
        if (product.getAvailableInventory() < quantity) {
            return FairDealOfferEvaluation.builder()
                    .offerId(offer.getId())
                    .sellerId(offer.getSellerId())
                    .productId(product.getId())
                    .productTitle(product.getTitle())
                    .brand(product.getBrand())
                    .unitPrice(unitPrice)
                    .quantity(quantity)
                    .shippingCost(shippingCost)
                    .totalLandedCost(totalLandedCost)
                    .deliveryEstimateDays(offer.getDeliveryEstimateDays())
                    .warrantyMonths(offer.getWarrantyMonths())
                    .perks(offer.getPerks())
                    .passedHardFilters(false)
                    .filterFailureReason(String.format("Requested quantity (%d) exceeds merchant inventory stock (%d)",
                            quantity, product.getAvailableInventory()))
                    .buyerSatisfactionScore(0.0)
                    .offerQualityScore(0.0)
                    .sellerPolicyCompliance(0.0)
                    .overallDealScore(0.0)
                    .calculationExplanation("REJECTED: Insufficient inventory to fulfill order quantity.")
                    .build();
        }

        // Filter 3: Minimum Order Quantity (MOQ)
        if (product.getMinOrderQuantity() > 1 && quantity < product.getMinOrderQuantity()) {
            return FairDealOfferEvaluation.builder()
                    .offerId(offer.getId())
                    .sellerId(offer.getSellerId())
                    .productId(product.getId())
                    .productTitle(product.getTitle())
                    .brand(product.getBrand())
                    .unitPrice(unitPrice)
                    .quantity(quantity)
                    .shippingCost(shippingCost)
                    .totalLandedCost(totalLandedCost)
                    .deliveryEstimateDays(offer.getDeliveryEstimateDays())
                    .warrantyMonths(offer.getWarrantyMonths())
                    .passedHardFilters(false)
                    .filterFailureReason(String.format("Order quantity (%d) is below merchant minimum order quantity (%d)",
                            quantity, product.getMinOrderQuantity()))
                    .buyerSatisfactionScore(0.0)
                    .offerQualityScore(0.0)
                    .sellerPolicyCompliance(0.0)
                    .overallDealScore(0.0)
                    .calculationExplanation("REJECTED: Below merchant minimum order quantity.")
                    .build();
        }

        // Filter 4: Seller Floor Price Guardrail
        if (unitPrice.compareTo(floorPrice) < 0) {
            return FairDealOfferEvaluation.builder()
                    .offerId(offer.getId())
                    .sellerId(offer.getSellerId())
                    .productId(product.getId())
                    .productTitle(product.getTitle())
                    .brand(product.getBrand())
                    .unitPrice(unitPrice)
                    .quantity(quantity)
                    .shippingCost(shippingCost)
                    .totalLandedCost(totalLandedCost)
                    .deliveryEstimateDays(offer.getDeliveryEstimateDays())
                    .warrantyMonths(offer.getWarrantyMonths())
                    .passedHardFilters(false)
                    .filterFailureReason(String.format("Unit price ($%s) breaches merchant reserve floor ($%s)",
                            unitPrice.toPlainString(), floorPrice.toPlainString()))
                    .buyerSatisfactionScore(0.0)
                    .offerQualityScore(0.0)
                    .sellerPolicyCompliance(0.0)
                    .overallDealScore(0.0)
                    .calculationExplanation("REJECTED: Unit price violates seller private policy floor.")
                    .build();
        }

        // ============================================================
        // 2. TRANSPARENT WEIGHTED SCORING
        // ============================================================

        // 2a. Landed Cost Score (0 - 100)
        BigDecimal targetPrice = pref != null && pref.getTargetPrice() != null ? pref.getTargetPrice() : unitPrice;
        BigDecimal ceilingPrice = pref != null && pref.getMaxCeilingPrice() != null ? pref.getMaxCeilingPrice() : product.getListPrice();
        double landedCostScore;
        if (totalLandedCost.compareTo(targetPrice) <= 0) {
            landedCostScore = 100.0;
        } else {
            BigDecimal range = ceilingPrice.subtract(targetPrice);
            if (range.compareTo(BigDecimal.ZERO) > 0) {
                BigDecimal diff = ceilingPrice.subtract(totalLandedCost);
                landedCostScore = Math.max(0.0, Math.min(100.0,
                        diff.divide(range, 4, RoundingMode.HALF_UP).doubleValue() * 100.0));
            } else {
                landedCostScore = 80.0;
            }
        }

        // 2b. Brand Preference Match (0 - 100)
        double brandMatchScore = 80.0;
        if (preferredBrand != null && !preferredBrand.isBlank()) {
            if (product.getBrand() != null && product.getBrand().equalsIgnoreCase(preferredBrand.trim())) {
                brandMatchScore = 100.0;
            } else if (product.getTitle().toLowerCase().contains(preferredBrand.toLowerCase())) {
                brandMatchScore = 95.0;
            } else {
                brandMatchScore = 60.0;
            }
        }

        // 2c. Product Specification Match (0 - 100)
        double specMatchScore = 85.0;
        if (offer.getPerks() != null && !offer.getPerks().isEmpty()) {
            specMatchScore = Math.min(100.0, specMatchScore + (offer.getPerks().size() * 5.0));
        }

        // 2d. Delivery Time Score (0 - 100)
        int desiredDeliveryDays = pref != null && pref.getDesiredDeliveryDays() > 0 ? pref.getDesiredDeliveryDays() : 4;
        int deliveryDays = offer.getDeliveryEstimateDays() > 0 ? offer.getDeliveryEstimateDays() : (product.getEstimatedDeliveryDays() > 0 ? product.getEstimatedDeliveryDays() : 4);
        double deliveryScore;
        if (deliveryDays <= desiredDeliveryDays) {
            deliveryScore = 100.0;
        } else {
            int extraDays = deliveryDays - desiredDeliveryDays;
            deliveryScore = Math.max(30.0, 100.0 - (extraDays * 15.0));
        }

        // 2e. Warranty Score (0 - 100)
        int warrantyMonths = offer.getWarrantyMonths() > 0 ? offer.getWarrantyMonths() : 12;
        double warrantyScore;
        if (warrantyMonths >= 24) warrantyScore = 100.0;
        else if (warrantyMonths >= 12) warrantyScore = 85.0;
        else if (warrantyMonths >= 6) warrantyScore = 65.0;
        else warrantyScore = 40.0;

        // 2f. Quantity Compatibility & Stock Headroom (0 - 100)
        double quantityScore = 85.0;
        if (product.getAvailableInventory() >= quantity * 5) {
            quantityScore = 100.0;
        } else if (product.getAvailableInventory() >= quantity * 2) {
            quantityScore = 90.0;
        }

        // 2g. Return Conditions Score (0 - 100)
        double returnConditionsScore = 75.0;
        String returnPolicy = product.getReturnPolicy() != null ? product.getReturnPolicy().toLowerCase() : "";
        if (returnPolicy.contains("30") || returnPolicy.contains("free")) {
            returnConditionsScore = 100.0;
        } else if (returnPolicy.contains("14")) {
            returnConditionsScore = 85.0;
        } else if (returnPolicy.contains("final") || returnPolicy.contains("no return")) {
            returnConditionsScore = 40.0;
        }

        // 2h. Negotiation Constraint & Seller Margin Compliance Score (0 - 100)
        BigDecimal listPrice = product.getListPrice();
        BigDecimal marginSpread = listPrice.subtract(floorPrice);
        double constraintComplianceScore = 70.0;
        if (marginSpread.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal marginKept = unitPrice.subtract(floorPrice);
            constraintComplianceScore = Math.max(0.0, Math.min(100.0,
                    marginKept.divide(marginSpread, 4, RoundingMode.HALF_UP).doubleValue() * 100.0));
        }

        // ============================================================
        // 3. COMPOSITE AGGREGATION & EXPLANATION
        // ============================================================

        // Buyer satisfaction score (40% cost, 15% brand, 15% specs, 15% delivery, 15% warranty)
        double buyerSatisfactionScore = (0.40 * landedCostScore)
                + (0.15 * brandMatchScore)
                + (0.15 * specMatchScore)
                + (0.15 * deliveryScore)
                + (0.15 * warrantyScore);
        buyerSatisfactionScore = Math.round(buyerSatisfactionScore * 10.0) / 10.0;

        // Offer quality score (25% brand, 30% specs, 25% warranty, 20% return conditions)
        double offerQualityScore = (0.25 * brandMatchScore)
                + (0.30 * specMatchScore)
                + (0.25 * warrantyScore)
                + (0.20 * returnConditionsScore);
        offerQualityScore = Math.round(offerQualityScore * 10.0) / 10.0;

        // Seller policy compliance score (70% margin compliance, 30% quantity headroom)
        double sellerPolicyCompliance = (0.70 * constraintComplianceScore)
                + (0.30 * quantityScore);
        sellerPolicyCompliance = Math.round(sellerPolicyCompliance * 10.0) / 10.0;

        // Overall deal score: 45% Buyer Satisfaction, 25% Offer Quality, 30% Seller Policy Compliance
        double overallDealScore = (0.45 * buyerSatisfactionScore)
                + (0.25 * offerQualityScore)
                + (0.30 * sellerPolicyCompliance);
        overallDealScore = Math.round(overallDealScore * 10.0) / 10.0;

        ScoreBreakdown breakdown = ScoreBreakdown.builder()
                .landedCostScore(Math.round(landedCostScore * 10.0) / 10.0)
                .brandMatchScore(Math.round(brandMatchScore * 10.0) / 10.0)
                .specMatchScore(Math.round(specMatchScore * 10.0) / 10.0)
                .deliveryScore(Math.round(deliveryScore * 10.0) / 10.0)
                .warrantyScore(Math.round(warrantyScore * 10.0) / 10.0)
                .quantityScore(Math.round(quantityScore * 10.0) / 10.0)
                .returnConditionsScore(Math.round(returnConditionsScore * 10.0) / 10.0)
                .constraintComplianceScore(Math.round(constraintComplianceScore * 10.0) / 10.0)
                .build();

        String explanation = String.format(
                "Landed Cost: $%s (Unit: $%s + Ship: $%s). Buyer Satisfaction: %.1f/100 [Cost(40%%): %.1f, Brand(15%%): %.1f, Specs(15%%): %.1f, Delivery(15%%): %.1f, Warranty(15%%): %.1f] | Offer Quality: %.1f/100 | Seller Compliance: %.1f/100 => Overall Deal Score: %.1f/100.",
                totalLandedCost.toPlainString(),
                unitPrice.toPlainString(),
                shippingCost.toPlainString(),
                buyerSatisfactionScore,
                landedCostScore,
                brandMatchScore,
                specMatchScore,
                deliveryScore,
                warrantyScore,
                offerQualityScore,
                sellerPolicyCompliance,
                overallDealScore
        );

        return FairDealOfferEvaluation.builder()
                .offerId(offer.getId())
                .sellerId(offer.getSellerId())
                .productId(product.getId())
                .productTitle(product.getTitle())
                .brand(product.getBrand())
                .unitPrice(unitPrice)
                .quantity(quantity)
                .shippingCost(shippingCost)
                .totalLandedCost(totalLandedCost)
                .deliveryEstimateDays(deliveryDays)
                .warrantyMonths(warrantyMonths)
                .perks(offer.getPerks())
                .passedHardFilters(true)
                .buyerSatisfactionScore(buyerSatisfactionScore)
                .offerQualityScore(offerQualityScore)
                .sellerPolicyCompliance(sellerPolicyCompliance)
                .overallDealScore(overallDealScore)
                .breakdown(breakdown)
                .calculationExplanation(explanation)
                .build();
    }

    /**
     * Compares multiple merchant offers, applying hard filters and transparent scoring.
     * If no offer satisfies mandatory requirements, returns hasViableDeal = false (No Deal).
     */
    public FairDealComparisonResult compareOffers(
            List<NegotiationOffer> offers,
            List<Product> products,
            BuyerPreferences buyerPreferences,
            String preferredBrand,
            double buyerReputation) {

        List<FairDealOfferEvaluation> evaluations = new ArrayList<>();

        for (NegotiationOffer offer : offers) {
            Product matchingProduct = products.stream()
                    .filter(p -> p.getId().equals(offer.getProductId()))
                    .findFirst()
                    .orElse(products.isEmpty() ? null : products.get(0));

            if (matchingProduct == null) continue;

            NegotiationPolicy policy = matchingProduct.getNegotiationPolicy();
            FairDealOfferEvaluation eval = evaluateOffer(offer, matchingProduct, policy, buyerPreferences, preferredBrand, buyerReputation);
            evaluations.add(eval);
        }

        // Separate passing offers from non-compliant offers
        List<FairDealOfferEvaluation> passingOffers = evaluations.stream()
                .filter(FairDealOfferEvaluation::isPassedHardFilters)
                .sorted(Comparator.comparingDouble(FairDealOfferEvaluation::getOverallDealScore).reversed())
                .toList();

        if (passingOffers.isEmpty()) {
            return FairDealComparisonResult.builder()
                    .hasViableDeal(false)
                    .summaryExplanation("No Deal: None of the competing merchant offers satisfied mandatory buyer constraints or seller policy boundaries.")
                    .evaluations(evaluations)
                    .bestOffer(null)
                    .build();
        }

        FairDealOfferEvaluation topOffer = passingOffers.get(0);
        String summary = String.format(
                "Viable deal found! Top recommendation from Seller %s for '%s' with an overall FairDeal Score of %.1f/100 (Total Landed Cost: $%s).",
                topOffer.getSellerId(),
                topOffer.getProductTitle(),
                topOffer.getOverallDealScore(),
                topOffer.getTotalLandedCost().toPlainString()
        );

        return FairDealComparisonResult.builder()
                .hasViableDeal(true)
                .summaryExplanation(summary)
                .evaluations(evaluations)
                .bestOffer(topOffer)
                .build();
    }
}
