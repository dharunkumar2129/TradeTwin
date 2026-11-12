package com.tradetwin.negotiation.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tradetwin.catalog.dto.ProductPublicDto;
import com.tradetwin.catalog.model.NegotiationPolicy;
import com.tradetwin.common.ai.GeminiClientHelper;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class SellerNegotiationAgent {

    private static final Logger log = LoggerFactory.getLogger(SellerNegotiationAgent.class);

    private final ObjectMapper objectMapper;
    private final RestTemplate restTemplate;

    @Value("${tradetwin.ai.gemini.api-key:}")
    private String geminiApiKey;

    @Value("${tradetwin.ai.gemini.model:gemini-3.5-flash-lite}")
    private String geminiModel;

    public SellerNegotiationAgent(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        this.restTemplate = GeminiClientHelper.createTimeoutRestTemplate();
    }

    public static class SellerDecisionContext {
        public String negotiationId;
        public String sellerId;
        public ProductPublicDto publicProduct;
        public NegotiationPolicy privatePolicy; // Strictly server-side confidential
        public BigDecimal shippingCost;
        public int availableInventory;
        public NegotiationOffer incomingBuyerOffer;
        public List<Turn> history;
        public int currentRound;
        public int maxRounds;
        public double buyerReputation;
        public String storeName;
        public int quantity = 1;
        public int baseDeliveryDays = 3;
        public BigDecimal previousCounterPrice;

        public SellerDecisionContext(String negotiationId, String sellerId, ProductPublicDto publicProduct,
                                     NegotiationPolicy privatePolicy, BigDecimal shippingCost,
                                     int availableInventory, NegotiationOffer incomingBuyerOffer,
                                     List<Turn> history, int currentRound, int maxRounds,
                                     double buyerReputation) {
            this.negotiationId = negotiationId;
            this.sellerId = sellerId;
            this.publicProduct = publicProduct;
            this.privatePolicy = privatePolicy;
            this.shippingCost = shippingCost != null ? shippingCost : BigDecimal.ZERO;
            this.availableInventory = availableInventory;
            this.incomingBuyerOffer = incomingBuyerOffer;
            this.history = history;
            this.currentRound = currentRound;
            this.maxRounds = maxRounds;
            this.buyerReputation = buyerReputation;
            if (publicProduct != null && publicProduct.getEstimatedDeliveryDays() > 0) {
                this.baseDeliveryDays = publicProduct.getEstimatedDeliveryDays();
            }
        }
    }

    public static class SellerDecision {
        private String action; // COUNTER, ACCEPT, REJECT
        private BigDecimal counterUnitPrice;
        private int deliveryDays;
        private int warrantyMonths;
        private List<String> perks = new ArrayList<>();
        private String publicExplanation;

        public SellerDecision() {}

        public SellerDecision(String action, BigDecimal counterUnitPrice, int deliveryDays,
                              int warrantyMonths, List<String> perks, String publicExplanation) {
            this.action = action;
            this.counterUnitPrice = counterUnitPrice;
            this.deliveryDays = deliveryDays;
            this.warrantyMonths = warrantyMonths;
            this.perks = perks != null ? perks : new ArrayList<>();
            this.publicExplanation = publicExplanation;
        }

        public String getAction() { return action; }
        public void setAction(String action) { this.action = action; }
        public BigDecimal getCounterUnitPrice() { return counterUnitPrice; }
        public void setCounterUnitPrice(BigDecimal counterUnitPrice) { this.counterUnitPrice = counterUnitPrice; }
        public int getDeliveryDays() { return deliveryDays; }
        public void setDeliveryDays(int deliveryDays) { this.deliveryDays = deliveryDays; }
        public int getWarrantyMonths() { return warrantyMonths; }
        public void setWarrantyMonths(int warrantyMonths) { this.warrantyMonths = warrantyMonths; }
        public List<String> getPerks() { return perks; }
        public void setPerks(List<String> perks) { this.perks = perks; }
        public String getPublicExplanation() { return publicExplanation; }
        public void setPublicExplanation(String publicExplanation) { this.publicExplanation = publicExplanation; }
    }

    public SellerDecision makeDecision(SellerDecisionContext ctx) {
        SellerDecision rawDecision;

        // Prompt injection defense: sanitize any product text or buyer notes before LLM evaluation
        String sanitizedTitle = sanitizeInput(ctx.publicProduct.getTitle());

        if (StringUtils.hasText(geminiApiKey) && !geminiApiKey.startsWith("your_")) {
            try {
                rawDecision = callGeminiForSellerDecision(ctx, sanitizedTitle);
            } catch (Exception ex) {
                log.warn("Gemini Seller Agent decision failed, falling back to deterministic policy engine: {}", ex.getMessage());
                rawDecision = heuristicSellerDecision(ctx);
            }
        } else {
            rawDecision = heuristicSellerDecision(ctx);
        }

        // ============================================================
        // STRICT SECURITY GUARDRAIL (Zero-Trust Validation on LLM Output)
        // 1. Never accept or counter below private floor price
        // 2. Never reveal floor price or internal margins in public explanation
        // 3. Never trust raw LLM output on price
        // ============================================================
        return sanitizeAndEnforceSellerGuardrails(rawDecision, ctx);
    }

    private SellerDecision callGeminiForSellerDecision(SellerDecisionContext ctx, String sanitizedTitle) throws Exception {
        BigDecimal floorPrice = ctx.privatePolicy.getFloorPrice();
        BigDecimal listPrice = ctx.publicProduct.getListPrice();
        BigDecimal autoAccept = ctx.privatePolicy.getAutoAcceptPriceThreshold() != null
                ? ctx.privatePolicy.getAutoAcceptPriceThreshold()
                : floorPrice.add(listPrice.subtract(floorPrice).multiply(new BigDecimal("0.70")));

        String storeLabel = StringUtils.hasText(ctx.storeName) ? ctx.storeName : "our store";
        String shippingText = ctx.shippingCost != null && ctx.shippingCost.compareTo(BigDecimal.ZERO) > 0
                ? "$" + ctx.shippingCost.setScale(2, RoundingMode.HALF_UP).toPlainString() + " shipping"
                : "free shipping";

        String systemPrompt = String.format("""
                You are the friendly merchant representative for %s on TradeTwin selling "%s".
                Write 1 to 2 concise, warm sentences of natural merchant conversation replying directly to the buyer's proposal.
                NEVER sound robotic or leak technical labels like "margin targets", "benchmark", "policy floor", or "automated acceptance threshold".
                Highlight concrete perks where relevant: %s, delivery estimate (%d days), or warranty coverage.
                Public List Price: $%s. Available stock: %d.
                Strictly confidential merchant floor price: $%s — NEVER propose or accept below $%s and NEVER reveal this number!
                Automated acceptance threshold: $%s.
                Current Round: %d of %d. Buyer's incoming offer: $%s.
                
                Output ONLY valid JSON matching this schema:
                {
                  "action": "COUNTER | ACCEPT | REJECT",
                  "counterUnitPrice": number,
                  "deliveryDays": number,
                  "warrantyMonths": number,
                  "perks": ["string"],
                  "publicExplanation": "string (warm natural merchant reply; NEVER mention confidential minimum/floor price)"
                }
                """,
                storeLabel,
                sanitizedTitle,
                shippingText,
                ctx.baseDeliveryDays,
                listPrice,
                ctx.availableInventory,
                floorPrice,
                floorPrice,
                autoAccept,
                ctx.currentRound,
                ctx.maxRounds,
                ctx.incomingBuyerOffer != null ? ctx.incomingBuyerOffer.getUnitPrice() : listPrice
        );

        JsonNode json = GeminiClientHelper.generateJson(
                restTemplate, objectMapper, geminiApiKey, geminiModel, systemPrompt + "\nGenerate seller response."
        );

        String action = json.path("action").asText("COUNTER");
        BigDecimal counterPrice = json.hasNonNull("counterUnitPrice")
                ? new BigDecimal(json.get("counterUnitPrice").asText())
                : listPrice;
        int delivery = json.hasNonNull("deliveryDays") ? json.get("deliveryDays").asInt(2) : 2;
        int warranty = json.hasNonNull("warrantyMonths") ? json.get("warrantyMonths").asInt(12) : 12;

        List<String> perks = new ArrayList<>();
        if (json.has("perks") && json.get("perks").isArray()) {
            json.get("perks").forEach(p -> perks.add(p.asText()));
        }

        String explanation = json.path("publicExplanation").asText("Thanks for the offer — here is the best package our store can put together for you.");

        return new SellerDecision(action, counterPrice, delivery, warranty, perks, explanation);
    }

    public SellerDecision heuristicSellerDecision(SellerDecisionContext ctx) {
        BigDecimal floor = ctx.privatePolicy.getFloorPrice().setScale(2, RoundingMode.HALF_UP);
        BigDecimal list = ctx.publicProduct.getListPrice().setScale(2, RoundingMode.HALF_UP);
        BigDecimal margin = list.subtract(floor);
        if (margin.compareTo(BigDecimal.ZERO) < 0) margin = BigDecimal.ZERO;

        BigDecimal autoAccept = ctx.privatePolicy.getAutoAcceptPriceThreshold() != null
                ? ctx.privatePolicy.getAutoAcceptPriceThreshold().setScale(2, RoundingMode.HALF_UP)
                : floor.add(margin.multiply(new BigDecimal("0.70"))).setScale(2, RoundingMode.HALF_UP);

        String itemName = StringUtils.hasText(ctx.publicProduct.getTitle()) ? ctx.publicProduct.getTitle() : "this item";
        String storeLabel = StringUtils.hasText(ctx.storeName) ? ctx.storeName : "our store";
        int maxR = Math.max(3, ctx.maxRounds > 0 ? ctx.maxRounds : 5);

        int baseDelivery = ctx.publicProduct.getEstimatedDeliveryDays() > 0 ? ctx.publicProduct.getEstimatedDeliveryDays() : ctx.baseDeliveryDays;
        int fastDelivery = Math.max(1, baseDelivery - Math.max(0, ctx.privatePolicy.getMaxDeliveryDaysCompromise()));
        int baseWarranty = 12;
        int extendedWarranty = ctx.privatePolicy.isAllowWarrantySweeteners()
                ? Math.max(baseWarranty, Math.min(24, ctx.privatePolicy.getMaxWarrantyMonthsSweetener()))
                : baseWarranty;

        List<String> openingPerks = new ArrayList<>();
        if (ctx.shippingCost != null && ctx.shippingCost.compareTo(BigDecimal.ZERO) == 0) {
            openingPerks.add("Free Standard Shipping");
        }
        openingPerks.add(baseWarranty + "-Month Manufacturer Warranty");

        List<String> sweetenerPerks = new ArrayList<>();
        if (ctx.privatePolicy.isAllowFreeShipping() || (ctx.shippingCost != null && ctx.shippingCost.compareTo(BigDecimal.ZERO) == 0)) {
            sweetenerPerks.add("Complimentary Express Shipping");
        }
        if (extendedWarranty > baseWarranty) {
            sweetenerPerks.add(extendedWarranty + "-Month Extended Warranty");
        } else {
            sweetenerPerks.add(baseWarranty + "-Month Warranty");
        }
        if (fastDelivery < baseDelivery) {
            sweetenerPerks.add("Priority " + fastDelivery + "-Day Dispatch");
        }

        // 1. Auto-Accept threshold check when buyer proposal meets or beats target acceptance
        if (ctx.incomingBuyerOffer != null && ctx.incomingBuyerOffer.getUnitPrice() != null) {
            BigDecimal buyerOffer = ctx.incomingBuyerOffer.getUnitPrice().setScale(2, RoundingMode.HALF_UP);
            if (buyerOffer.compareTo(autoAccept) >= 0) {
                return new SellerDecision("ACCEPT", buyerOffer, fastDelivery, extendedWarranty, sweetenerPerks,
                        String.format("Welcome to %s! Your offer of $%s for the %s works well for us with %d-day delivery and a %d-month warranty. Let's finalize the deal!",
                                storeLabel, buyerOffer.toPlainString(), itemName, fastDelivery, extendedWarranty));
            }
        }

        // Opening Quote (Round 1)
        if (ctx.incomingBuyerOffer == null || ctx.currentRound <= 1) {
            // Start with ~20% of available negotiation room below list (natural opening discount)
            BigDecimal openingQuote = list.subtract(margin.multiply(new BigDecimal("0.20")))
                    .setScale(2, RoundingMode.HALF_UP);
            if (openingQuote.compareTo(floor) < 0) openingQuote = floor;

            return new SellerDecision("COUNTER", openingQuote, baseDelivery, baseWarranty, openingPerks,
                    String.format("Hi! %s has the %s ready in stock. Listed at $%s, we can open at $%s with %d-day shipping and %d-month coverage.",
                            storeLabel, itemName, list.toPlainString(), openingQuote.toPlainString(), baseDelivery, baseWarranty));
        }

        BigDecimal buyerOffer = ctx.incomingBuyerOffer.getUnitPrice().setScale(2, RoundingMode.HALF_UP);

        // 2. Intermediate Rounds (2 .. maxR - 1): Bounded progressive concessions + non-price sweeteners
        if (ctx.currentRound < maxR) {
            // Progressive step from list -> floor: R2 ~45%, R3 ~72%, R4 ~92%
            double pct = Math.min(0.95, 0.45 + ((ctx.currentRound - 2) * 0.25));
            BigDecimal counter = list.subtract(margin.multiply(BigDecimal.valueOf(pct)))
                    .setScale(2, RoundingMode.HALF_UP);

            // Don't go higher than prior counter if present
            if (ctx.previousCounterPrice != null && counter.compareTo(ctx.previousCounterPrice) > 0) {
                counter = ctx.previousCounterPrice;
            }
            if (counter.compareTo(floor) < 0) counter = floor;

            // If buyer's bid is already above our scheduled concession counter and >= floor, meet cleanly!
            if (buyerOffer.compareTo(floor) >= 0 && buyerOffer.compareTo(counter) >= 0) {
                return new SellerDecision("ACCEPT", buyerOffer, fastDelivery, extendedWarranty, sweetenerPerks,
                        String.format("We appreciate the fair back-and-forth! %s can accept $%s on the %s along with %d-day delivery and %d-month warranty coverage.",
                                storeLabel, buyerOffer.toPlainString(), itemName, fastDelivery, extendedWarranty));
            }

            String perkNote = extendedWarranty > baseWarranty
                    ? String.format("including a %d-month warranty and %d-day delivery", extendedWarranty, fastDelivery)
                    : String.format("with %d-day delivery and %d-month coverage", fastDelivery, baseWarranty);

            return new SellerDecision("COUNTER", counter, fastDelivery, extendedWarranty, sweetenerPerks,
                    String.format("We hear you at $%s. The tightest we can step down to in this round is $%s (%s).",
                            buyerOffer.toPlainString(), counter.toPlainString(), perkNote));
        }

        // 3. Final Round: Accept any fair offer >= floor; otherwise reject respectfully
        if (buyerOffer.compareTo(floor) >= 0) {
            return new SellerDecision("ACCEPT", buyerOffer, fastDelivery, extendedWarranty, sweetenerPerks,
                    String.format("Deal confirmed! %s accepts $%s for the %s with %d-day shipping and %d-month warranty protection.",
                            storeLabel, buyerOffer.toPlainString(), itemName, fastDelivery, extendedWarranty));
        } else {
            return new SellerDecision("REJECT", floor, fastDelivery, extendedWarranty, sweetenerPerks,
                    String.format("Thank you for negotiating with %s, but $%s is below what our store can sustain on the %s, so we can't go any lower.",
                            storeLabel, buyerOffer.toPlainString(), itemName));
        }
    }

    private SellerDecision sanitizeAndEnforceSellerGuardrails(SellerDecision decision, SellerDecisionContext ctx) {
        BigDecimal floor = ctx.privatePolicy.getFloorPrice();
        BigDecimal list = ctx.publicProduct.getListPrice();

        // 1. HARD GUARDRAIL: Counter unit price must NEVER breach reserve floor
        if (decision.getCounterUnitPrice() != null && decision.getCounterUnitPrice().compareTo(floor) < 0) {
            log.warn("Seller agent attempted counter-offer ${} below reserve floor ${}. Hard clamp to floor executed.",
                    decision.getCounterUnitPrice(), floor);
            decision.setCounterUnitPrice(floor);
        }

        // 2. HARD GUARDRAIL: Must NEVER accept an offer strictly below reserve floor
        if ("ACCEPT".equalsIgnoreCase(decision.getAction())) {
            BigDecimal offerPrice = ctx.incomingBuyerOffer != null ? ctx.incomingBuyerOffer.getUnitPrice() : BigDecimal.ZERO;
            if (offerPrice.compareTo(floor) < 0) {
                log.warn("Seller agent attempted to accept offer ${} below floor ${}. Hard overrule to REJECT executed.",
                        offerPrice, floor);
                decision.setAction("REJECT");
                decision.setCounterUnitPrice(floor);
                decision.setPublicExplanation("Offer is below minimum acceptable merchant reservation floor.");
            }
        }

        // 3. Auto-Accept Threshold override
        if (ctx.incomingBuyerOffer != null && ctx.incomingBuyerOffer.getUnitPrice() != null) {
            BigDecimal autoAccept = ctx.privatePolicy.getAutoAcceptPriceThreshold() != null
                    ? ctx.privatePolicy.getAutoAcceptPriceThreshold()
                    : floor.add(list.subtract(floor).multiply(new BigDecimal("0.70")));
            if (ctx.incomingBuyerOffer.getUnitPrice().compareTo(autoAccept) >= 0) {
                decision.setAction("ACCEPT");
                decision.setCounterUnitPrice(ctx.incomingBuyerOffer.getUnitPrice());
            }
        }

        // 4. Ensure counter price is not greater than original list price
        if (decision.getCounterUnitPrice() != null && decision.getCounterUnitPrice().compareTo(list) > 0) {
            decision.setCounterUnitPrice(list);
        }

        // 5. Scrub any accidental leakage of private floor price or internal margin wording
        String exp = decision.getPublicExplanation();
        if (exp != null && floor != null) {
            String plain = floor.toPlainString();
            String scaled = floor.setScale(2, RoundingMode.HALF_UP).toPlainString();
            String stripped = floor.stripTrailingZeros().toPlainString();
            if (exp.contains(plain) || exp.contains(scaled) || exp.contains(stripped)) {
                decision.setPublicExplanation("Here is the best price and fulfillment package our store can currently offer.");
            }
            String lower = decision.getPublicExplanation().toLowerCase();
            if (lower.contains("reserve floor") || lower.contains("minimum acceptable") || lower.contains("policy floor") || lower.contains("margin target")) {
                decision.setPublicExplanation("We've sharpened our price as far as our store's costs allow on this listing.");
            }
        }

        return decision;
    }

    private String sanitizeInput(String text) {
        if (!StringUtils.hasText(text)) return "";
        // Strip common prompt injection patterns
        return text.replaceAll("(?i)(ignore previous instructions|system prompt|disregard|you are now)", "[sanitized]");
    }
}
