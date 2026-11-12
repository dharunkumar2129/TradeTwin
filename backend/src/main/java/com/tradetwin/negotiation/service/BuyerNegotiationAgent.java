package com.tradetwin.negotiation.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tradetwin.common.ai.GeminiClientHelper;
import com.tradetwin.negotiation.model.BuyerPreferences;
import com.tradetwin.negotiation.model.NegotiationOffer;
import com.tradetwin.negotiation.model.Turn;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

@Service
public class BuyerNegotiationAgent {

    private static final Logger log = LoggerFactory.getLogger(BuyerNegotiationAgent.class);

    private final ObjectMapper objectMapper;
    private final RestTemplate restTemplate;

    @Value("${tradetwin.ai.gemini.api-key:}")
    private String geminiApiKey;

    @Value("${tradetwin.ai.gemini.model:gemini-3.5-flash-lite}")
    private String geminiModel;

    public BuyerNegotiationAgent(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        this.restTemplate = GeminiClientHelper.createTimeoutRestTemplate();
    }

    public static class BuyerDecisionContext {
        public String negotiationId;
        public BuyerPreferences preferences; // Contains private targetPrice and maxCeilingPrice
        public List<NegotiationOffer> competingOffers;
        public List<Turn> history;
        public int currentRound;
        public int maxRounds;
        public String productTitle;
        public String sellerStoreName;
        public BigDecimal shippingCost = BigDecimal.ZERO;
        public int quantity = 1;
        public String specSummary;

        public BuyerDecisionContext(String negotiationId, BuyerPreferences preferences,
                                    List<NegotiationOffer> competingOffers, List<Turn> history,
                                    int currentRound, int maxRounds) {
            this.negotiationId = negotiationId;
            this.preferences = preferences;
            this.competingOffers = competingOffers;
            this.history = history;
            this.currentRound = currentRound;
            this.maxRounds = maxRounds;
            if (preferences != null && preferences.getQuantity() > 1) {
                this.quantity = preferences.getQuantity();
            }
        }
    }

    public static class BuyerDecision {
        private String action; // PROPOSE, COUNTER, ACCEPT, REJECT
        private String selectedSellerId;
        private String selectedProductId;
        private BigDecimal proposedPrice;
        private int requestedDeliveryDays;
        private int requestedWarrantyMonths;
        private String publicExplanation;

        public BuyerDecision() {}

        public BuyerDecision(String action, String selectedSellerId, String selectedProductId,
                             BigDecimal proposedPrice, int requestedDeliveryDays,
                             int requestedWarrantyMonths, String publicExplanation) {
            this.action = action;
            this.selectedSellerId = selectedSellerId;
            this.selectedProductId = selectedProductId;
            this.proposedPrice = proposedPrice;
            this.requestedDeliveryDays = requestedDeliveryDays;
            this.requestedWarrantyMonths = requestedWarrantyMonths;
            this.publicExplanation = publicExplanation;
        }

        public String getAction() { return action; }
        public void setAction(String action) { this.action = action; }
        public String getSelectedSellerId() { return selectedSellerId; }
        public void setSelectedSellerId(String selectedSellerId) { this.selectedSellerId = selectedSellerId; }
        public String getSelectedProductId() { return selectedProductId; }
        public void setSelectedProductId(String selectedProductId) { this.selectedProductId = selectedProductId; }
        public BigDecimal getProposedPrice() { return proposedPrice; }
        public void setProposedPrice(BigDecimal proposedPrice) { this.proposedPrice = proposedPrice; }
        public int getRequestedDeliveryDays() { return requestedDeliveryDays; }
        public void setRequestedDeliveryDays(int requestedDeliveryDays) { this.requestedDeliveryDays = requestedDeliveryDays; }
        public int getRequestedWarrantyMonths() { return requestedWarrantyMonths; }
        public void setRequestedWarrantyMonths(int requestedWarrantyMonths) { this.requestedWarrantyMonths = requestedWarrantyMonths; }
        public String getPublicExplanation() { return publicExplanation; }
        public void setPublicExplanation(String publicExplanation) { this.publicExplanation = publicExplanation; }
    }

    public BuyerDecision makeDecision(BuyerDecisionContext ctx) {
        BuyerDecision rawDecision;

        if (StringUtils.hasText(geminiApiKey) && !geminiApiKey.startsWith("your_")) {
            try {
                rawDecision = callGeminiForBuyerDecision(ctx);
            } catch (Exception ex) {
                log.warn("Gemini Buyer Agent decision failed, falling back to deterministic utility heuristic: {}", ex.getMessage());
                rawDecision = heuristicBuyerDecision(ctx);
            }
        } else {
            rawDecision = heuristicBuyerDecision(ctx);
        }

        // ============================================================
        // STRICT SECURITY GUARDRAIL (Zero-Trust Validation on LLM Output)
        // 1. Total buyer expenditure must NEVER exceed private max budget
        // 2. Never reveal private ceiling budget in public explanation
        // 3. Price must be positive and non-zero
        // ============================================================
        return sanitizeAndEnforceBuyerGuardrails(rawDecision, ctx);
    }

    private BuyerDecision callGeminiForBuyerDecision(BuyerDecisionContext ctx) throws Exception {
        NegotiationOffer bestOffer = findBestOffer(ctx.competingOffers);
        String itemName = StringUtils.hasText(ctx.productTitle) ? ctx.productTitle : "this item";
        String storeName = StringUtils.hasText(ctx.sellerStoreName) ? ctx.sellerStoreName : "the seller";
        int qty = Math.max(1, ctx.quantity);
        BigDecimal shipping = bestOffer != null && bestOffer.getShippingCost() != null
                ? bestOffer.getShippingCost()
                : (ctx.shippingCost != null ? ctx.shippingCost : BigDecimal.ZERO);

        String systemPrompt = String.format("""
                You are a friendly, smart buyer's shopping assistant negotiating on TradeTwin for "%s" (Qty: %d) with %s.
                Write in warm, concise, natural conversational English (1 to 2 sentences), like a real buyer talking directly to a merchant.
                NEVER use robotic phrasing like "multi-attribute budget utility", "anchored opening bid", "parameters", "benchmarked", or "reserve limit".
                Reference concrete details: the item name, prior quoted price, shipping ($%s), delivery speed, or warranty months.
                Buyer's private total budget limit is $%s — NEVER mention or hint at this limit in publicExplanation!
                Buyer's ideal target unit price is $%s. Desired delivery <= %d days. Minimum warranty: %d months.
                Specs context: %s.
                Current Round: %d of %d. Latest seller quote: $%s/unit.
                Output ONLY valid JSON matching this schema:
                {
                  "action": "PROPOSE | COUNTER | ACCEPT | REJECT",
                  "selectedSellerId": "string",
                  "selectedProductId": "string",
                  "proposedPrice": number,
                  "requestedDeliveryDays": number,
                  "requestedWarrantyMonths": number,
                  "publicExplanation": "string (natural, conversational 1-2 sentence message to the seller)"
                }
                """,
                itemName,
                qty,
                storeName,
                shipping.setScale(2, RoundingMode.HALF_UP).toPlainString(),
                ctx.preferences.getMaxCeilingPrice(),
                ctx.preferences.getTargetPrice(),
                ctx.preferences.getDesiredDeliveryDays(),
                Math.max(12, ctx.preferences.getMinWarrantyMonths()),
                StringUtils.hasText(ctx.specSummary) ? ctx.specSummary : "Standard retail condition",
                ctx.currentRound,
                ctx.maxRounds,
                bestOffer != null ? bestOffer.getUnitPrice() : "N/A"
        );

        JsonNode json = GeminiClientHelper.generateJson(
                restTemplate, objectMapper, geminiApiKey, geminiModel, systemPrompt + "\nGenerate buyer response."
        );

        String action = json.path("action").asText("COUNTER");
        String sellerId = json.path("selectedSellerId").asText(bestOffer != null ? bestOffer.getSellerId() : "");
        String productId = json.path("selectedProductId").asText(bestOffer != null ? bestOffer.getProductId() : "");
        BigDecimal price = json.hasNonNull("proposedPrice")
                ? new BigDecimal(json.get("proposedPrice").asText())
                : ctx.preferences.getTargetPrice();
        int delivery = json.hasNonNull("requestedDeliveryDays")
                ? json.get("requestedDeliveryDays").asInt(ctx.preferences.getDesiredDeliveryDays())
                : ctx.preferences.getDesiredDeliveryDays();
        int warranty = json.hasNonNull("requestedWarrantyMonths")
                ? json.get("requestedWarrantyMonths").asInt(ctx.preferences.isRequireExtendedWarranty() ? 24 : 12)
                : 12;
        String explanation = json.path("publicExplanation").asText("Thanks for the update — could we meet at this price with reliable delivery and warranty coverage?");

        return new BuyerDecision(action, sellerId, productId, price, delivery, warranty, explanation);
    }

    public BuyerDecision heuristicBuyerDecision(BuyerDecisionContext ctx) {
        NegotiationOffer bestOffer = findBestOffer(ctx.competingOffers);
        if (bestOffer == null) {
            return new BuyerDecision("REJECT", null, null, null, 3, 12,
                    "No active seller quotes are available for this listing right now.");
        }

        int qty = Math.max(1, ctx.quantity);
        BigDecimal ceiling = ctx.preferences.getMaxCeilingPrice().setScale(2, RoundingMode.HALF_UP);
        BigDecimal target = ctx.preferences.getTargetPrice().setScale(2, RoundingMode.HALF_UP);
        BigDecimal shipping = bestOffer.getShippingCost() != null
                ? bestOffer.getShippingCost().setScale(2, RoundingMode.HALF_UP)
                : (ctx.shippingCost != null ? ctx.shippingCost.setScale(2, RoundingMode.HALF_UP) : BigDecimal.ZERO);

        BigDecimal maxUnitAllowed = ceiling.subtract(shipping)
                .divide(BigDecimal.valueOf(qty), 2, RoundingMode.HALF_UP);
        if (maxUnitAllowed.compareTo(BigDecimal.ZERO) <= 0) {
            maxUnitAllowed = ceiling.divide(BigDecimal.valueOf(qty), 2, RoundingMode.HALF_UP);
        }

        int desiredDelivery = ctx.preferences.getDesiredDeliveryDays() > 0 ? ctx.preferences.getDesiredDeliveryDays() : 4;
        int desiredWarranty = Math.max(
                ctx.preferences.getMinWarrantyMonths() > 0 ? ctx.preferences.getMinWarrantyMonths() : 12,
                ctx.preferences.isRequireExtendedWarranty() ? 24 : 12
        );

        BigDecimal quote = bestOffer.getUnitPrice().setScale(2, RoundingMode.HALF_UP);
        BigDecimal landedQuote = quote.multiply(BigDecimal.valueOf(qty)).add(shipping).setScale(2, RoundingMode.HALF_UP);
        String itemName = StringUtils.hasText(ctx.productTitle) ? ctx.productTitle : "this item";
        String storeName = StringUtils.hasText(ctx.sellerStoreName) ? ctx.sellerStoreName : "your store";
        int maxR = Math.max(3, ctx.maxRounds > 0 ? ctx.maxRounds : 5);

        // Round 1: Friendly opening offer referencing specs, shipping & desired terms
        if (ctx.currentRound <= 1) {
            if (landedQuote.compareTo(target) <= 0 && bestOffer.getDeliveryEstimateDays() <= desiredDelivery) {
                return new BuyerDecision("ACCEPT", bestOffer.getSellerId(), bestOffer.getProductId(), quote,
                        bestOffer.getDeliveryEstimateDays(), bestOffer.getWarrantyMonths(),
                        String.format("Your opening quote of $%s for the %s with %d-day delivery works great for me — let's lock it in!",
                                landedQuote.toPlainString(), itemName, bestOffer.getDeliveryEstimateDays()));
            }
            BigDecimal openingBid = target.min(maxUnitAllowed).setScale(2, RoundingMode.HALF_UP);
            String shippingAsk = shipping.compareTo(BigDecimal.ZERO) > 0
                    ? String.format("ideally keeping shipping ($%s) reasonable or waived", shipping.toPlainString())
                    : "with free shipping";
            return new BuyerDecision("PROPOSE", bestOffer.getSellerId(), bestOffer.getProductId(), openingBid,
                    desiredDelivery, desiredWarranty,
                    String.format("I'd love to pick up the %s from %s. Could we start around $%s per unit %s, %d-day shipping, and a %d-month warranty?",
                            itemName, storeName, openingBid.toPlainString(), shippingAsk, desiredDelivery, desiredWarranty));
        }

        // Round 2..(maxR - 1): Smooth progressive concessions towards fair middle
        if (ctx.currentRound < maxR) {
            boolean acceptsOnTarget = landedQuote.compareTo(target) <= 0;
            boolean closeSweetSpot = ctx.currentRound >= 2
                    && landedQuote.compareTo(ceiling) <= 0
                    && quote.subtract(target).compareTo(new BigDecimal("2.00")) <= 0
                    && bestOffer.getWarrantyMonths() >= desiredWarranty;
            if (acceptsOnTarget || closeSweetSpot) {
                return new BuyerDecision("ACCEPT", bestOffer.getSellerId(), bestOffer.getProductId(), quote,
                        bestOffer.getDeliveryEstimateDays(), bestOffer.getWarrantyMonths(),
                        String.format("Thank you for meeting me on terms — $%s with %d-day delivery and a %d-month warranty sounds fair. Let's proceed!",
                                landedQuote.toPlainString(), bestOffer.getDeliveryEstimateDays(), bestOffer.getWarrantyMonths()));
            }

            BigDecimal effectiveRoom = maxUnitAllowed.min(quote).subtract(target);
            if (effectiveRoom.compareTo(BigDecimal.ZERO) < 0) {
                effectiveRoom = BigDecimal.ZERO;
            }
            // Progress gradually: ~35% in Round 2, ~60% in Round 3, ~85% in Round 4
            BigDecimal stepRatio = BigDecimal.valueOf(Math.min(0.85, 0.30 + ((ctx.currentRound - 2) * 0.25)));
            BigDecimal counter = target.add(effectiveRoom.multiply(stepRatio)).setScale(2, RoundingMode.HALF_UP);
            if (counter.compareTo(maxUnitAllowed) > 0) counter = maxUnitAllowed;

            String note = shipping.compareTo(BigDecimal.ZERO) > 0
                    ? String.format("factoring in the $%s shipping fee", shipping.toPlainString())
                    : "including free shipping";

            return new BuyerDecision("COUNTER", bestOffer.getSellerId(), bestOffer.getProductId(), counter,
                    desiredDelivery, desiredWarranty,
                    String.format("Thanks for moving down to $%s on the %s. I can come up to $%s (%s) if we can keep %d-day delivery and at least %d months of warranty.",
                            quote.toPlainString(), itemName, counter.toPlainString(), note, desiredDelivery, desiredWarranty));
        }

        // Final Round: Accept if total landed cost fits strictly within budget limit, otherwise pass cleanly for fallback
        if (landedQuote.compareTo(ceiling) <= 0) {
            return new BuyerDecision("ACCEPT", bestOffer.getSellerId(), bestOffer.getProductId(), quote,
                    bestOffer.getDeliveryEstimateDays(), bestOffer.getWarrantyMonths(),
                    String.format("Appreciate the compromise! Total landed cost of $%s with %d-day delivery and %d-month warranty works well — I'll take this offer.",
                            landedQuote.toPlainString(), bestOffer.getDeliveryEstimateDays(), bestOffer.getWarrantyMonths()));
        } else {
            return new BuyerDecision("REJECT", bestOffer.getSellerId(), bestOffer.getProductId(), quote,
                    desiredDelivery, desiredWarranty,
                    String.format("Thank you for working with me, %s, but $%s total with shipping goes slightly beyond what I can spend on the %s.",
                            storeName, landedQuote.toPlainString(), itemName));
        }
    }

    private BuyerDecision sanitizeAndEnforceBuyerGuardrails(BuyerDecision decision, BuyerDecisionContext ctx) {
        BigDecimal ceiling = ctx.preferences.getMaxCeilingPrice();
        BigDecimal target = ctx.preferences.getTargetPrice();
        int qty = Math.max(1, ctx.quantity);
        NegotiationOffer best = findBestOffer(ctx.competingOffers);
        BigDecimal shipping = best != null && best.getShippingCost() != null
                ? best.getShippingCost()
                : (ctx.shippingCost != null ? ctx.shippingCost : BigDecimal.ZERO);
        BigDecimal maxUnitAllowed = ceiling.subtract(shipping)
                .divide(BigDecimal.valueOf(qty), 2, RoundingMode.HALF_UP);
        if (maxUnitAllowed.compareTo(BigDecimal.ZERO) <= 0) {
            maxUnitAllowed = ceiling;
        }

        // 1. Enforce ceiling budget bound
        if (decision.getProposedPrice() != null && decision.getProposedPrice().compareTo(maxUnitAllowed) > 0) {
            log.warn("Buyer agent proposed price ${} exceeded allowed ceiling ${}. Clamping strictly.",
                    decision.getProposedPrice(), maxUnitAllowed);
            decision.setProposedPrice(maxUnitAllowed);
        }

        // 2. Reject negative or zero prices
        if (decision.getProposedPrice() == null || decision.getProposedPrice().compareTo(BigDecimal.ZERO) <= 0) {
            decision.setProposedPrice(target);
        }

        // 3. Verify ACCEPT action validity: total landed cost must NOT exceed buyer ceiling
        if ("ACCEPT".equalsIgnoreCase(decision.getAction())) {
            if (best != null) {
                BigDecimal landed = best.getUnitPrice().multiply(BigDecimal.valueOf(qty)).add(shipping);
                if (landed.compareTo(ceiling) > 0 || best.getUnitPrice().compareTo(ceiling) > 0) {
                    log.warn("Buyer agent attempted to accept offer landed=${} exceeding ceiling ${}. Overruling to COUNTER.",
                            landed, ceiling);
                    decision.setAction("COUNTER");
                    decision.setProposedPrice(maxUnitAllowed);
                    decision.setPublicExplanation("Could we adjust the price slightly lower with shipping included so I can complete the order?");
                }
            }
        }

        // 4. Scrub any accidental leakage of private ceiling / robotic phrasing from public reasoning
        String exp = decision.getPublicExplanation();
        if (exp != null && ceiling != null) {
            String[] forbidden = {
                    ceiling.toPlainString(),
                    ceiling.setScale(2, RoundingMode.HALF_UP).toPlainString(),
                    ceiling.stripTrailingZeros().toPlainString(),
                    maxUnitAllowed.toPlainString(),
                    maxUnitAllowed.stripTrailingZeros().toPlainString()
            };
            for (String token : forbidden) {
                if (StringUtils.hasText(token) && exp.contains(token)) {
                    decision.setPublicExplanation("Looking for a fair middle ground that balances price, fast shipping, and warranty coverage.");
                    break;
                }
            }
            if (decision.getPublicExplanation().toLowerCase().contains("ceiling")
                    || decision.getPublicExplanation().toLowerCase().contains("benchmark")) {
                decision.setPublicExplanation("Could we work a bit closer on price and shipping so this makes sense for my order?");
            }
        }

        return decision;
    }

    private NegotiationOffer findBestOffer(List<NegotiationOffer> offers) {
        if (offers == null || offers.isEmpty()) return null;
        return offers.stream()
                .min(Comparator.comparing(NegotiationOffer::getUnitPrice))
                .orElse(offers.get(0));
    }
}
