package com.tradetwin.negotiation.service;

import com.tradetwin.catalog.dto.ProductPublicDto;
import com.tradetwin.catalog.model.NegotiationPolicy;
import com.tradetwin.catalog.model.Product;
import com.tradetwin.catalog.repository.ProductRepository;
import com.tradetwin.catalog.service.ProductService;
import com.tradetwin.common.exception.BusinessRuleException;
import com.tradetwin.common.exception.ResourceNotFoundException;
import com.tradetwin.deal.model.AgreementSeal;
import com.tradetwin.deal.model.FairDealComparisonResult;
import com.tradetwin.deal.model.FairDealMetrics;
import com.tradetwin.deal.service.FairDealEngine;
import com.tradetwin.event.model.NegotiationStreamEvent;
import com.tradetwin.event.service.SseStreamingHub;
import com.tradetwin.negotiation.dto.StartNegotiationRequest;
import com.tradetwin.negotiation.model.*;
import com.tradetwin.negotiation.repository.NegotiationRepository;
import com.tradetwin.seller.model.SellerProfile;
import com.tradetwin.seller.repository.SellerProfileRepository;
import com.tradetwin.user.model.User;
import com.tradetwin.user.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

@Service
public class NegotiationOrchestrator {

    private static final Logger log = LoggerFactory.getLogger(NegotiationOrchestrator.class);
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final int AGREEMENT_LOCK_MINUTES = 10;

    private final NegotiationRepository negotiationRepository;
    private final ProductRepository productRepository;
    private final ProductService productService;
    private final UserService userService;
    private final FairDealEngine fairDealEngine;
    private final SseStreamingHub sseStreamingHub;
    private final BuyerNegotiationAgent buyerAgent;
    private final SellerNegotiationAgent sellerAgent;
    private final SellerProfileRepository sellerProfileRepository;
    private final Set<String> activeExecutions = ConcurrentHashMap.newKeySet();
    private AgreementPostProcessor agreementPostProcessor;

    @Autowired(required = false)
    public void setAgreementPostProcessor(AgreementPostProcessor agreementPostProcessor) {
        this.agreementPostProcessor = agreementPostProcessor;
    }

    @Value("${tradetwin.negotiation.pacing-ms:250}")
    private long pacingDelayMs = 250L;

    public NegotiationOrchestrator(NegotiationRepository negotiationRepository,
                                   ProductRepository productRepository,
                                   ProductService productService,
                                   UserService userService,
                                   FairDealEngine fairDealEngine,
                                   SseStreamingHub sseStreamingHub,
                                   BuyerNegotiationAgent buyerAgent,
                                   SellerNegotiationAgent sellerAgent) {
        this(negotiationRepository, productRepository, productService, userService,
                fairDealEngine, sseStreamingHub, buyerAgent, sellerAgent, null);
    }

    @Autowired
    public NegotiationOrchestrator(NegotiationRepository negotiationRepository,
                                   ProductRepository productRepository,
                                   ProductService productService,
                                   UserService userService,
                                   FairDealEngine fairDealEngine,
                                   SseStreamingHub sseStreamingHub,
                                   BuyerNegotiationAgent buyerAgent,
                                   SellerNegotiationAgent sellerAgent,
                                   SellerProfileRepository sellerProfileRepository) {
        this.negotiationRepository = negotiationRepository;
        this.productRepository = productRepository;
        this.productService = productService;
        this.userService = userService;
        this.fairDealEngine = fairDealEngine;
        this.sseStreamingHub = sseStreamingHub;
        this.buyerAgent = buyerAgent;
        this.sellerAgent = sellerAgent;
        this.sellerProfileRepository = sellerProfileRepository;
    }

    public void setPacingDelayMs(long pacingDelayMs) {
        this.pacingDelayMs = Math.max(0L, pacingDelayMs);
    }

    public Negotiation startNegotiation(String buyerId, StartNegotiationRequest request) {
        Product primaryProduct = productService.getProductById(request.getProductId());
        if (!primaryProduct.isActive()) {
            throw new BusinessRuleException("Product is currently inactive and cannot be negotiated");
        }
        BuyerPreferences pref = request.getPreferences();
        int requestedQty = Math.max(1, pref.getQuantity());
        if (primaryProduct.getAvailableInventory() < requestedQty) {
            throw new BusinessRuleException("Product does not have enough available inventory for requested quantity (" + requestedQty + ")");
        }
        if (primaryProduct.getNegotiationPolicy() != null && !primaryProduct.getNegotiationPolicy().isAutoNegotiate()) {
            throw new BusinessRuleException("Autonomous negotiation is disabled by the seller for this listing");
        }

        if (pref.getTargetPrice().compareTo(pref.getMaxCeilingPrice()) > 0) {
            throw new BusinessRuleException("Buyer target price cannot exceed maximum ceiling price");
        }

        int policyRounds = primaryProduct.getNegotiationPolicy() != null && primaryProduct.getNegotiationPolicy().getMaxRounds() > 0
                ? primaryProduct.getNegotiationPolicy().getMaxRounds()
                : 4;
        int maxRounds = Math.max(3, Math.min(5, policyRounds));

        String buyerPrompt = StringUtils.hasText(request.getBuyerQuery())
                ? request.getBuyerQuery().trim()
                : (StringUtils.hasText(pref.getBuyerPrompt())
                    ? pref.getBuyerPrompt().trim()
                    : String.format("%s · Target $%s · Delivery <= %d days",
                        primaryProduct.getTitle(), pref.getTargetPrice().toPlainString(), Math.max(1, pref.getDesiredDeliveryDays())));
        pref.setBuyerPrompt(buyerPrompt);

        List<Product> rankedQueue = buildSellerCandidateQueue(primaryProduct, pref, request.getCandidateProductIds());
        List<SellerCandidateSummary> queueSummaries = new ArrayList<>();
        List<String> sellerIds = new ArrayList<>();
        List<String> productIds = new ArrayList<>();
        for (int i = 0; i < rankedQueue.size(); i++) {
            Product candidate = rankedQueue.get(i);
            String storeName = resolveStoreName(candidate.getSellerId());
            int candidateWarranty = candidate.getNegotiationPolicy() != null && candidate.getNegotiationPolicy().isAllowWarrantySweeteners()
                    ? Math.max(12, candidate.getNegotiationPolicy().getMaxWarrantyMonthsSweetener())
                    : 12;
            queueSummaries.add(new SellerCandidateSummary(
                    candidate.getSellerId(),
                    storeName,
                    candidate.getId(),
                    candidate.getTitle(),
                    candidate.getBrand(),
                    candidate.getListPrice(),
                    candidate.getShippingCost(),
                    candidate.getEstimatedDeliveryDays(),
                    candidateWarranty,
                    candidate.getAvailableInventory(),
                    i == 0 ? "ACTIVE" : "QUEUED",
                    Math.max(50.0, 96.0 - (i * 4.5))
            ));
            if (!sellerIds.contains(candidate.getSellerId())) {
                sellerIds.add(candidate.getSellerId());
            }
            if (!productIds.contains(candidate.getId())) {
                productIds.add(candidate.getId());
            }
        }

        String primaryImage = primaryProduct.getImages() != null && !primaryProduct.getImages().isEmpty()
                ? primaryProduct.getImages().get(0)
                : "";
        String primaryStoreName = resolveStoreName(primaryProduct.getSellerId());

        Negotiation negotiation = Negotiation.builder()
                .productId(primaryProduct.getId())
                .buyerId(buyerId)
                .sellerId(primaryProduct.getSellerId())
                .status(NegotiationStatus.CREATED)
                .roundCount(0)
                .maxRounds(maxRounds)
                .buyerPreferences(pref)
                .turns(new ArrayList<>())
                .offers(new ArrayList<>())
                .participatingSellerIds(sellerIds)
                .participatingProductIds(productIds)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
        negotiation.setBuyerPrompt(buyerPrompt);
        negotiation.setProductTitle(primaryProduct.getTitle());
        negotiation.setProductImage(primaryImage);
        negotiation.setCurrentSellerStoreName(primaryStoreName);
        negotiation.setCurrentSellerIndex(0);
        negotiation.setSellerQueue(queueSummaries);

        Negotiation saved = negotiationRepository.save(negotiation);

        // Broadcast NEGOTIATION_STARTED (public metadata only; never leak private buyer ceiling)
        sseStreamingHub.broadcast(saved.getId(), NegotiationStreamEvent.builder()
                .eventType("NEGOTIATION_STARTED")
                .negotiationId(saved.getId())
                .speaker("MARKETPLACE")
                .payload(Map.of(
                        "negotiationId", saved.getId(),
                        "productId", saved.getProductId(),
                        "productTitle", primaryProduct.getTitle(),
                        "currentSellerStoreName", primaryStoreName,
                        "queuedSellers", queueSummaries.size(),
                        "status", saved.getStatus().name(),
                        "maxRounds", saved.getMaxRounds()
                ))
                .timestamp(Instant.now())
                .build());

        // Run multi-agent protocol loop with automatic seller fallback asynchronously
        CompletableFuture.runAsync(() -> executeNegotiationProtocol(saved.getId()));

        return saved;
    }

    public Negotiation getNegotiationById(String id) {
        return negotiationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Negotiation", "id", id));
    }

    public List<Negotiation> getNegotiationsForBuyer(String buyerId) {
        return negotiationRepository.findByBuyerIdOrderByCreatedAtDesc(buyerId);
    }

    public FairDealComparisonResult compareOffersForNegotiation(String negotiationId) {
        Negotiation neg = getNegotiationById(negotiationId);
        List<Product> products = new ArrayList<>();
        for (String pid : neg.getParticipatingProductIds()) {
            try {
                products.add(productService.getProductById(pid));
            } catch (Exception ignored) {}
        }
        User buyer = userService.getUserById(neg.getBuyerId());
        return fairDealEngine.compareOffers(
                neg.getOffers(),
                products,
                neg.getBuyerPreferences(),
                products.isEmpty() ? null : products.get(0).getBrand(),
                buyer != null ? buyer.getReputationScore() : 90.0
        );
    }

    public Negotiation selectAndApproveDeal(String negotiationId, String offerId, String buyerId) {
        Negotiation neg = getNegotiationById(negotiationId);
        if (!neg.getBuyerId().equals(buyerId)) {
            throw new BusinessRuleException("Unauthorized: Only the buyer participating in this negotiation can select and approve a deal");
        }

        NegotiationOffer selectedOffer = neg.getOffers().stream()
                .filter(o -> (o.getId() != null && o.getId().equals(offerId))
                        || (o.getProductId() != null && o.getProductId().equals(offerId))
                        || (o.getSellerId() != null && o.getSellerId().equals(offerId)))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("NegotiationOffer", "offerId", offerId));

        Product product = productService.getProductById(selectedOffer.getProductId());
        User buyer = userService.getUserById(buyerId);
        int qty = Math.max(1, selectedOffer.getQuantity());
        BigDecimal shipping = selectedOffer.getShippingCost() != null ? selectedOffer.getShippingCost() : BigDecimal.ZERO;
        BigDecimal unitPrice = selectedOffer.getUnitPrice().setScale(2, RoundingMode.HALF_UP);
        BigDecimal totalPayable = unitPrice.multiply(BigDecimal.valueOf(qty)).add(shipping).setScale(2, RoundingMode.HALF_UP);
        String storeName = resolveStoreName(selectedOffer.getSellerId());

        Turn selectTurn = Turn.builder()
                .turnNumber(neg.getTurns().size() + 1)
                .roundNumber(Math.max(1, neg.getRoundCount()))
                .speaker("BUYER_AGENT")
                .action("ACCEPT")
                .sellerId(selectedOffer.getSellerId())
                .sellerStoreName(storeName)
                .productId(product.getId())
                .productTitle(product.getTitle())
                .proposal(OfferProposal.builder()
                        .price(unitPrice)
                        .unitPrice(unitPrice)
                        .shippingCost(shipping)
                        .totalPayable(totalPayable)
                        .quantity(qty)
                        .currency(product.getCurrency())
                        .deliveryDays(selectedOffer.getDeliveryEstimateDays())
                        .warrantyMonths(selectedOffer.getWarrantyMonths())
                        .offerStatus("ACCEPTED")
                        .roundNumber(Math.max(1, neg.getRoundCount()))
                        .bonusPerks(selectedOffer.getPerks())
                        .build())
                .agentReasoning(String.format("Confirmed %s's offer on the %s at $%s/unit ($%s total landed).",
                        storeName, product.getTitle(), unitPrice.toPlainString(), totalPayable.toPlainString()))
                .timestamp(Instant.now())
                .build();

        recordTurn(neg, selectTurn);
        sealAgreement(neg, product, product.getNegotiationPolicy(), buyer, selectTurn, unitPrice, shipping, qty);

        return getNegotiationById(negotiationId);
    }

    public List<Product> buildSellerCandidateQueue(Product primaryProduct, BuyerPreferences pref, List<String> requestedIds) {
        int qty = Math.max(1, pref != null ? pref.getQuantity() : 1);
        List<Product> allActive = productRepository.findByActiveTrue();
        Map<String, Product> byId = new LinkedHashMap<>();
        for (Product p : allActive) {
            if (p.getId() != null) {
                byId.put(p.getId(), p);
            }
        }
        if (primaryProduct.getId() != null) {
            byId.putIfAbsent(primaryProduct.getId(), primaryProduct);
        }

        List<Product> ordered = new ArrayList<>();
        Set<String> seenSellers = new HashSet<>();
        Set<String> seenProducts = new HashSet<>();

        // 1. Keep buyer's chosen primary product first
        if (primaryProduct.getSellerId() != null) {
            ordered.add(primaryProduct);
            seenSellers.add(primaryProduct.getSellerId());
            if (primaryProduct.getId() != null) seenProducts.add(primaryProduct.getId());
        }

        // 2. Include any explicitly prioritized candidate IDs passed from frontend comparison matrix
        if (requestedIds != null) {
            for (String pid : requestedIds) {
                Product p = byId.get(pid);
                if (p != null && !seenProducts.contains(p.getId()) && isEligibleFallbackCandidate(primaryProduct, p, pref, qty, seenSellers)) {
                    ordered.add(p);
                    seenSellers.add(p.getSellerId());
                    seenProducts.add(p.getId());
                }
            }
        }

        // 3. Collect and rank all remaining eligible sellers by balanced FairDeal candidate criteria (not price alone)
        List<Product> peerCandidates = new ArrayList<>();
        for (Product p : allActive) {
            if (seenProducts.contains(p.getId())) continue;
            if (isEligibleFallbackCandidate(primaryProduct, p, pref, qty, seenSellers)) {
                peerCandidates.add(p);
            }
        }
        peerCandidates.sort((a, b) -> Double.compare(
                scoreCandidateForQueue(b, primaryProduct, pref, qty),
                scoreCandidateForQueue(a, primaryProduct, pref, qty)
        ));

        for (Product candidate : peerCandidates) {
            if (ordered.size() >= 6) break;
            if (seenSellers.contains(candidate.getSellerId())) continue;
            ordered.add(candidate);
            seenSellers.add(candidate.getSellerId());
            seenProducts.add(candidate.getId());
        }

        return ordered;
    }

    private boolean isEligibleFallbackCandidate(Product primaryProduct, Product candidate, BuyerPreferences pref, int qty, Set<String> seenSellers) {
        if (candidate == null || !candidate.isActive()) return false;
        if (candidate.getSellerId() == null || seenSellers.contains(candidate.getSellerId())) return false;
        if (candidate.getAvailableInventory() < qty) return false;
        if (candidate.getMinOrderQuantity() > 1 && qty < candidate.getMinOrderQuantity()) return false;
        if (candidate.getMaxOrderQuantity() > 0 && qty > candidate.getMaxOrderQuantity()) return false;
        if (candidate.getNegotiationPolicy() != null && !candidate.getNegotiationPolicy().isAutoNegotiate()) return false;

        // Category must match so headphones do not fall back to unrelated gadgets
        if (StringUtils.hasText(primaryProduct.getCategory()) && StringUtils.hasText(candidate.getCategory())
                && !primaryProduct.getCategory().equalsIgnoreCase(candidate.getCategory())) {
            return false;
        }

        // Strict mandatory brand compliance if buyer mandated a specific brand
        if (pref != null && StringUtils.hasText(pref.getRequiredBrand())) {
            String required = pref.getRequiredBrand().trim().toLowerCase(Locale.ROOT);
            String brand = candidate.getBrand() != null ? candidate.getBrand().toLowerCase(Locale.ROOT) : "";
            String title = candidate.getTitle() != null ? candidate.getTitle().toLowerCase(Locale.ROOT) : "";
            if (!brand.contains(required) && !title.contains(required)) {
                return false;
            }
        }

        return true;
    }

    private double scoreCandidateForQueue(Product candidate, Product primaryProduct, BuyerPreferences pref, int qty) {
        double score = 0.0;

        // Exact product / SKU continuity
        if (StringUtils.hasText(primaryProduct.getSku()) && StringUtils.hasText(candidate.getSku())
                && normalizedSkuRoot(primaryProduct.getSku()).equalsIgnoreCase(normalizedSkuRoot(candidate.getSku()))) {
            score += 35.0;
        } else if (candidate.getTitle() != null && primaryProduct.getTitle() != null
                && candidate.getTitle().equalsIgnoreCase(primaryProduct.getTitle())) {
            score += 30.0;
        }

        // Brand match
        String preferredBrand = (pref != null && StringUtils.hasText(pref.getPreferredBrand()))
                ? pref.getPreferredBrand() : primaryProduct.getBrand();
        if (StringUtils.hasText(preferredBrand) && StringUtils.hasText(candidate.getBrand())
                && candidate.getBrand().equalsIgnoreCase(preferredBrand.trim())) {
            score += 20.0;
        }

        // Delivery fit
        int desiredDelivery = pref != null && pref.getDesiredDeliveryDays() > 0 ? pref.getDesiredDeliveryDays() : 4;
        if (candidate.getEstimatedDeliveryDays() <= desiredDelivery) {
            score += 15.0;
        } else {
            score += Math.max(0.0, 15.0 - ((candidate.getEstimatedDeliveryDays() - desiredDelivery) * 4.0));
        }

        // Warranty capability
        int minWarranty = pref != null && pref.getMinWarrantyMonths() > 0 ? pref.getMinWarrantyMonths() : 12;
        int possibleWarranty = candidate.getNegotiationPolicy() != null && candidate.getNegotiationPolicy().isAllowWarrantySweeteners()
                ? Math.max(12, candidate.getNegotiationPolicy().getMaxWarrantyMonthsSweetener())
                : 12;
        if (possibleWarranty >= minWarranty) {
            score += 15.0;
        }

        // Landed cost feasibility vs buyer target/ceiling (balanced weighting, NOT price alone)
        if (pref != null && pref.getMaxCeilingPrice() != null && candidate.getListPrice() != null) {
            BigDecimal shipping = candidate.getShippingCost() != null ? candidate.getShippingCost() : BigDecimal.ZERO;
            BigDecimal estMidUnit = candidate.getNegotiationPolicy() != null && candidate.getNegotiationPolicy().getFloorPrice() != null
                    ? candidate.getListPrice().add(candidate.getNegotiationPolicy().getFloorPrice()).divide(BigDecimal.valueOf(2), 2, RoundingMode.HALF_UP)
                    : candidate.getListPrice();
            BigDecimal estLanded = estMidUnit.multiply(BigDecimal.valueOf(qty)).add(shipping);
            if (estLanded.compareTo(pref.getMaxCeilingPrice()) <= 0) {
                score += 20.0;
            } else {
                score += 4.0;
            }
        }

        // Healthy stock depth bonus
        if (candidate.getAvailableInventory() >= Math.max(2, qty * 2)) {
            score += 5.0;
        }

        return score;
    }

    private String normalizedSkuRoot(String sku) {
        if (sku == null) return "";
        int dash = sku.lastIndexOf('-');
        // Strip merchant suffix when SKUs share prefix like SONY-WHCH720N-S1 / SONY-WHCH720N-S2
        return (dash > 4) ? sku.substring(0, dash) : sku;
    }

    /**
     * Executes multi-round negotiation across the ordered seller candidate queue,
     * automatically falling back to the next eligible seller whenever a seller rejects,
     * runs out of rounds without agreement, loses stock, or hits an unrecoverable error.
     */
    public void executeNegotiationProtocol(String negotiationId) {
        if (!activeExecutions.add(negotiationId)) {
            log.debug("Negotiation [{}] already executing; skipping duplicate invocation", negotiationId);
            return;
        }
        try {
            pause();
            Negotiation neg = getNegotiationById(negotiationId);
            Product primaryProduct = productService.getProductById(neg.getProductId());
            User buyer = userService.getUserById(neg.getBuyerId());
            BuyerPreferences pref = neg.getBuyerPreferences();

            neg.setStatus(NegotiationStatus.DISCOVERING);
            negotiationRepository.save(neg);

            List<Product> candidateProducts = buildSellerCandidateQueue(
                    primaryProduct,
                    pref,
                    neg.getParticipatingProductIds()
            );
            neg.setParticipatingSellerIds(candidateProducts.stream().map(Product::getSellerId).distinct().toList());
            neg.setParticipatingProductIds(candidateProducts.stream().map(Product::getId).distinct().toList());
            negotiationRepository.save(neg);

            // Emit SELLERS_DISCOVERED event
            sseStreamingHub.broadcast(negotiationId, NegotiationStreamEvent.builder()
                    .eventType("SELLERS_DISCOVERED")
                    .negotiationId(negotiationId)
                    .speaker("MARKETPLACE")
                    .payload(candidateProducts.stream().map(productService::toPublicDto).toList())
                    .timestamp(Instant.now())
                    .build());

            Set<String> triedSellerIds = new HashSet<>();
            int totalCandidates = candidateProducts.size();

            for (int idx = 0; idx < candidateProducts.size(); idx++) {
                Product candidate = candidateProducts.get(idx);
                if (triedSellerIds.contains(candidate.getSellerId())) {
                    continue;
                }
                triedSellerIds.add(candidate.getSellerId());

                // Refresh live product inventory and active state right before talking to this seller
                Product liveCandidate;
                try {
                    liveCandidate = productService.getProductById(candidate.getId());
                } catch (Exception e) {
                    recordSellerFailureAndNotify(neg, candidate, resolveStoreName(candidate.getSellerId()),
                            "Listing became unavailable before negotiation started", 0, null, idx, totalCandidates);
                    continue;
                }

                int qty = Math.max(1, pref.getQuantity());
                if (!liveCandidate.isActive() || liveCandidate.getAvailableInventory() < qty) {
                    recordSellerFailureAndNotify(neg, liveCandidate, resolveStoreName(liveCandidate.getSellerId()),
                            "Insufficient live inventory for requested quantity (" + qty + ")", 0, null, idx, totalCandidates);
                    continue;
                }

                String storeName = resolveStoreName(liveCandidate.getSellerId());
                neg.setSellerId(liveCandidate.getSellerId());
                neg.setProductId(liveCandidate.getId());
                neg.setProductTitle(liveCandidate.getTitle());
                neg.setCurrentSellerStoreName(storeName);
                neg.setCurrentSellerIndex(idx);
                updateQueueStatus(neg, liveCandidate.getSellerId(), "ACTIVE");
                neg.setStatus(NegotiationStatus.NEGOTIATING);
                negotiationRepository.save(neg);

                sseStreamingHub.broadcast(negotiationId, NegotiationStreamEvent.builder()
                        .eventType("SELLER_AGENT_STARTED")
                        .negotiationId(negotiationId)
                        .speaker("SELLER_AGENT")
                        .payload(Map.of(
                                "sellerId", liveCandidate.getSellerId(),
                                "storeName", storeName,
                                "productId", liveCandidate.getId(),
                                "productTitle", liveCandidate.getTitle(),
                                "sellerIndex", idx + 1,
                                "totalCandidates", totalCandidates,
                                "remainingSellers", Math.max(0, totalCandidates - idx - 1)
                        ))
                        .timestamp(Instant.now())
                        .build());

                SellerSessionOutcome outcome = runSingleSellerNegotiation(neg, liveCandidate, storeName, buyer, pref, qty);
                if (outcome.agreed) {
                    updateQueueStatus(neg, liveCandidate.getSellerId(), "AGREED");
                    negotiationRepository.save(neg);
                    return;
                }

                // This seller failed -> record attempt and automatically fall back if more candidates exist
                updateQueueStatus(neg, liveCandidate.getSellerId(), "FAILED");
                recordSellerFailureAndNotify(neg, liveCandidate, storeName, outcome.failureReason,
                        outcome.roundsCompleted, outcome.lastOfferPrice, idx, totalCandidates);

                if (idx + 1 < candidateProducts.size()) {
                    Product nextCandidate = candidateProducts.get(idx + 1);
                    String nextStoreName = resolveStoreName(nextCandidate.getSellerId());
                    String transitionMsg = String.format(
                            "Could not reach an agreement with %s (%s). Automatically switching to Seller %d of %d: %s.",
                            storeName, outcome.failureReason, idx + 2, totalCandidates, nextStoreName);
                    neg.setFallbackNotice(transitionMsg);
                    negotiationRepository.save(neg);

                    Map<String, Object> transitionPayload = Map.of(
                            "previousSellerId", liveCandidate.getSellerId(),
                            "previousStoreName", storeName,
                            "nextSellerId", nextCandidate.getSellerId(),
                            "nextStoreName", nextStoreName,
                            "nextProductId", nextCandidate.getId(),
                            "reason", outcome.failureReason,
                            "nextSellerIndex", idx + 2,
                            "totalCandidates", totalCandidates,
                            "remainingSellers", Math.max(0, totalCandidates - idx - 2),
                            "message", transitionMsg
                    );
                    sseStreamingHub.broadcast(negotiationId, NegotiationStreamEvent.builder()
                            .eventType("NEXT_SELLER_STARTED")
                            .negotiationId(negotiationId)
                            .speaker("MARKETPLACE")
                            .payload(transitionPayload)
                            .timestamp(Instant.now())
                            .build());
                    sseStreamingHub.broadcast(negotiationId, NegotiationStreamEvent.builder()
                            .eventType("SELLER_TRANSITION")
                            .negotiationId(negotiationId)
                            .speaker("MARKETPLACE")
                            .payload(transitionPayload)
                            .timestamp(Instant.now())
                            .build());
                    pause();
                }
            }

            // All candidate sellers exhausted without a deal
            handleAllSellersExhausted(neg, primaryProduct, pref);

        } catch (Exception ex) {
            log.error("Fatal exception during multi-agent negotiation execution for {}: ", negotiationId, ex);
            handleFailureState(negotiationId, ex.getMessage());
        } finally {
            activeExecutions.remove(negotiationId);
        }
    }

    private SellerSessionOutcome runSingleSellerNegotiation(
            Negotiation neg,
            Product product,
            String storeName,
            User buyer,
            BuyerPreferences pref,
            int qty) {

        NegotiationPolicy policy = product.getNegotiationPolicy() != null
                ? product.getNegotiationPolicy()
                : NegotiationPolicy.builder().floorPrice(product.getListPrice().multiply(new BigDecimal("0.85")).setScale(2, RoundingMode.HALF_UP)).build();

        int maxRounds = policy.getMaxRounds() > 0 ? Math.max(3, Math.min(5, policy.getMaxRounds())) : Math.max(3, Math.min(5, neg.getMaxRounds()));
        neg.setMaxRounds(maxRounds);
        double buyerRep = buyer != null ? buyer.getReputationScore() : 90.0;
        String currency = StringUtils.hasText(product.getCurrency()) ? product.getCurrency() : "USD";

        BigDecimal baseShipping = product.getShippingCost() != null ? product.getShippingCost().setScale(2, RoundingMode.HALF_UP) : BigDecimal.ZERO;
        BigDecimal currentShipping = baseShipping;
        BigDecimal lastSellerUnitPrice = product.getListPrice().setScale(2, RoundingMode.HALF_UP);
        int currentDeliveryDays = product.getEstimatedDeliveryDays() > 0 ? product.getEstimatedDeliveryDays() : 3;
        int currentWarrantyMonths = 12;
        List<String> currentPerks = new ArrayList<>();

        // Round 1: Opening merchant offer
        neg.setRoundCount(1);
        negotiationRepository.save(neg);

        SellerNegotiationAgent.SellerDecisionContext openCtx = new SellerNegotiationAgent.SellerDecisionContext(
                neg.getId(), product.getSellerId(), productService.toPublicDto(product),
                policy, currentShipping, product.getAvailableInventory(),
                null, neg.getTurns(), 1, maxRounds, buyerRep
        );
        openCtx.storeName = storeName;
        openCtx.quantity = qty;

        SellerNegotiationAgent.SellerDecision openDecision = executeWithTransientRetry(() -> sellerAgent.makeDecision(openCtx));
        lastSellerUnitPrice = openDecision.getCounterUnitPrice().setScale(2, RoundingMode.HALF_UP);
        currentDeliveryDays = openDecision.getDeliveryDays() > 0 ? openDecision.getDeliveryDays() : currentDeliveryDays;
        currentWarrantyMonths = openDecision.getWarrantyMonths() > 0 ? openDecision.getWarrantyMonths() : 12;
        currentPerks = openDecision.getPerks() != null ? openDecision.getPerks() : new ArrayList<>();
        BigDecimal openingTotalPayable = lastSellerUnitPrice.multiply(BigDecimal.valueOf(qty)).add(currentShipping).setScale(2, RoundingMode.HALF_UP);

        NegotiationOffer openingOffer = NegotiationOffer.builder()
                .id("offer-" + UUID.randomUUID().toString().substring(0, 8))
                .negotiationId(neg.getId())
                .sellerId(product.getSellerId())
                .productId(product.getId())
                .roundNumber(1)
                .unitPrice(lastSellerUnitPrice)
                .quantity(qty)
                .shippingCost(currentShipping)
                .deliveryEstimateDays(currentDeliveryDays)
                .warrantyMonths(currentWarrantyMonths)
                .perks(currentPerks)
                .offerStatus("PROPOSED")
                .publicExplanation(openDecision.getPublicExplanation())
                .timestamp(Instant.now())
                .build();
        neg.getOffers().add(openingOffer);

        sseStreamingHub.broadcast(neg.getId(), NegotiationStreamEvent.builder()
                .eventType("SELLER_INITIAL_OFFER")
                .negotiationId(neg.getId())
                .turnNumber(1)
                .speaker("SELLER_AGENT")
                .payload(openingOffer)
                .timestamp(Instant.now())
                .build());

        Turn openTurn = Turn.builder()
                .turnNumber(neg.getTurns().size() + 1)
                .roundNumber(1)
                .speaker("SELLER_AGENT")
                .action("OFFER")
                .sellerId(product.getSellerId())
                .sellerStoreName(storeName)
                .productId(product.getId())
                .productTitle(product.getTitle())
                .proposal(OfferProposal.builder()
                        .price(lastSellerUnitPrice)
                        .unitPrice(lastSellerUnitPrice)
                        .shippingCost(currentShipping)
                        .totalPayable(openingTotalPayable)
                        .quantity(qty)
                        .currency(currency)
                        .deliveryDays(currentDeliveryDays)
                        .warrantyMonths(currentWarrantyMonths)
                        .offerStatus("OFFERED")
                        .roundNumber(1)
                        .bonusPerks(currentPerks)
                        .build())
                .agentReasoning(openDecision.getPublicExplanation())
                .timestamp(Instant.now())
                .build();
        neg.setStatus(NegotiationStatus.OFFER_RECEIVED);
        recordTurn(neg, openTurn);
        broadcastTurn(neg.getId(), openTurn, product, policy, buyerRep);
        pause();

        NegotiationOffer latestSellerOffer = openingOffer;

        // Multi-round negotiation dialogue (Rounds 2 .. maxRounds)
        for (int round = 2; round <= maxRounds; round++) {
            neg.setRoundCount(round);
            negotiationRepository.save(neg);

            final int activeRound = round;
            final BigDecimal roundShipping = currentShipping;
            final NegotiationOffer offerToEvaluate = latestSellerOffer;

            BuyerNegotiationAgent.BuyerDecisionContext buyerCtx = new BuyerNegotiationAgent.BuyerDecisionContext(
                    neg.getId(), pref, List.of(offerToEvaluate), neg.getTurns(), activeRound, maxRounds
            );
            buyerCtx.productTitle = product.getTitle();
            buyerCtx.sellerStoreName = storeName;
            buyerCtx.shippingCost = roundShipping;
            buyerCtx.quantity = qty;
            buyerCtx.specSummary = summarizeSpecs(product);

            BuyerNegotiationAgent.BuyerDecision buyerDecision = executeWithTransientRetry(() -> buyerAgent.makeDecision(buyerCtx));

            if ("ACCEPT".equalsIgnoreCase(buyerDecision.getAction())) {
                BigDecimal agreedUnit = offerToEvaluate.getUnitPrice().setScale(2, RoundingMode.HALF_UP);
                BigDecimal agreedTotal = agreedUnit.multiply(BigDecimal.valueOf(qty)).add(roundShipping).setScale(2, RoundingMode.HALF_UP);
                if (agreedTotal.compareTo(pref.getMaxCeilingPrice()) <= 0 && agreedUnit.compareTo(policy.getFloorPrice()) >= 0) {
                    Turn acceptTurn = Turn.builder()
                            .turnNumber(neg.getTurns().size() + 1)
                            .roundNumber(activeRound)
                            .speaker("BUYER_AGENT")
                            .action("ACCEPT")
                            .sellerId(product.getSellerId())
                            .sellerStoreName(storeName)
                            .productId(product.getId())
                            .productTitle(product.getTitle())
                            .proposal(OfferProposal.builder()
                                    .price(agreedUnit)
                                    .unitPrice(agreedUnit)
                                    .shippingCost(roundShipping)
                                    .totalPayable(agreedTotal)
                                    .quantity(qty)
                                    .currency(currency)
                                    .deliveryDays(offerToEvaluate.getDeliveryEstimateDays())
                                    .warrantyMonths(offerToEvaluate.getWarrantyMonths())
                                    .offerStatus("ACCEPTED")
                                    .roundNumber(activeRound)
                                    .bonusPerks(offerToEvaluate.getPerks())
                                    .build())
                            .agentReasoning(buyerDecision.getPublicExplanation())
                            .timestamp(Instant.now())
                            .build();
                    recordTurn(neg, acceptTurn);
                    if (tryLockAndSeal(neg, product, policy, buyer, acceptTurn, agreedUnit, roundShipping, qty)) {
                        return SellerSessionOutcome.agreed();
                    }
                    return SellerSessionOutcome.failed("Inventory depleted at final lock", activeRound, agreedTotal);
                }
            }

            if ("REJECT".equalsIgnoreCase(buyerDecision.getAction())) {
                Turn rejectTurn = Turn.builder()
                        .turnNumber(neg.getTurns().size() + 1)
                        .roundNumber(activeRound)
                        .speaker("BUYER_AGENT")
                        .action("REJECT")
                        .sellerId(product.getSellerId())
                        .sellerStoreName(storeName)
                        .productId(product.getId())
                        .productTitle(product.getTitle())
                        .proposal(OfferProposal.builder()
                                .price(offerToEvaluate.getUnitPrice())
                                .unitPrice(offerToEvaluate.getUnitPrice())
                                .shippingCost(roundShipping)
                                .totalPayable(offerToEvaluate.calculateTotalCost())
                                .quantity(qty)
                                .currency(currency)
                                .deliveryDays(offerToEvaluate.getDeliveryEstimateDays())
                                .warrantyMonths(offerToEvaluate.getWarrantyMonths())
                                .offerStatus("REJECTED")
                                .roundNumber(activeRound)
                                .bonusPerks(offerToEvaluate.getPerks())
                                .build())
                        .agentReasoning(buyerDecision.getPublicExplanation())
                        .timestamp(Instant.now())
                        .build();
                recordTurn(neg, rejectTurn);
                return SellerSessionOutcome.failed("Landed offer stayed above buyer budget limit", activeRound, offerToEvaluate.calculateTotalCost());
            }

            // Buyer counters
            neg.setStatus(NegotiationStatus.COUNTEROFFERED);
            BigDecimal buyerCounterUnit = buyerDecision.getProposedPrice().setScale(2, RoundingMode.HALF_UP);
            BigDecimal buyerCounterTotal = buyerCounterUnit.multiply(BigDecimal.valueOf(qty)).add(roundShipping).setScale(2, RoundingMode.HALF_UP);
            Turn buyerTurn = Turn.builder()
                    .turnNumber(neg.getTurns().size() + 1)
                    .roundNumber(activeRound)
                    .speaker("BUYER_AGENT")
                    .action(activeRound == 2 ? "PROPOSE" : "COUNTER")
                    .sellerId(product.getSellerId())
                    .sellerStoreName(storeName)
                    .productId(product.getId())
                    .productTitle(product.getTitle())
                    .proposal(OfferProposal.builder()
                            .price(buyerCounterUnit)
                            .unitPrice(buyerCounterUnit)
                            .shippingCost(roundShipping)
                            .totalPayable(buyerCounterTotal)
                            .quantity(qty)
                            .currency(currency)
                            .deliveryDays(buyerDecision.getRequestedDeliveryDays())
                            .warrantyMonths(buyerDecision.getRequestedWarrantyMonths())
                            .offerStatus("COUNTERED")
                            .roundNumber(activeRound)
                            .bonusPerks(List.of("Buyer Protection"))
                            .build())
                    .agentReasoning(buyerDecision.getPublicExplanation())
                    .timestamp(Instant.now())
                    .build();
            recordTurn(neg, buyerTurn);
            broadcastTurn(neg.getId(), buyerTurn, product, policy, buyerRep);
            sseStreamingHub.broadcast(neg.getId(), NegotiationStreamEvent.builder()
                    .eventType("BUYER_COUNTEROFFER")
                    .negotiationId(neg.getId())
                    .turnNumber(buyerTurn.getTurnNumber())
                    .speaker("BUYER_AGENT")
                    .payload(buyerTurn)
                    .timestamp(Instant.now())
                    .build());
            pause();

            // Seller evaluates Buyer counteroffer
            // If policy permits free shipping concession in rounds 2+ and landed cost needs relief, waive shipping!
            if (policy.isAllowFreeShipping() && currentShipping.compareTo(BigDecimal.ZERO) > 0 && activeRound >= 2) {
                currentShipping = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
            }
            final BigDecimal effectiveSellerShipping = currentShipping;
            final BigDecimal priorCounter = lastSellerUnitPrice;

            NegotiationOffer incomingBuyerOffer = NegotiationOffer.builder()
                    .negotiationId(neg.getId())
                    .sellerId(product.getSellerId())
                    .productId(product.getId())
                    .roundNumber(activeRound)
                    .unitPrice(buyerCounterUnit)
                    .quantity(qty)
                    .shippingCost(effectiveSellerShipping)
                    .deliveryEstimateDays(buyerDecision.getRequestedDeliveryDays())
                    .warrantyMonths(buyerDecision.getRequestedWarrantyMonths())
                    .build();

            SellerNegotiationAgent.SellerDecisionContext sellerCtx = new SellerNegotiationAgent.SellerDecisionContext(
                    neg.getId(), product.getSellerId(), productService.toPublicDto(product),
                    policy, effectiveSellerShipping, product.getAvailableInventory(),
                    incomingBuyerOffer, neg.getTurns(), activeRound, maxRounds, buyerRep
            );
            sellerCtx.storeName = storeName;
            sellerCtx.quantity = qty;
            sellerCtx.previousCounterPrice = priorCounter;

            SellerNegotiationAgent.SellerDecision sellerDecision = executeWithTransientRetry(() -> sellerAgent.makeDecision(sellerCtx));

            if ("ACCEPT".equalsIgnoreCase(sellerDecision.getAction())) {
                BigDecimal agreedUnit = buyerCounterUnit;
                BigDecimal agreedTotal = agreedUnit.multiply(BigDecimal.valueOf(qty)).add(effectiveSellerShipping).setScale(2, RoundingMode.HALF_UP);
                if (agreedUnit.compareTo(policy.getFloorPrice()) >= 0 && agreedTotal.compareTo(pref.getMaxCeilingPrice()) <= 0) {
                    Turn sellerAcceptTurn = Turn.builder()
                            .turnNumber(neg.getTurns().size() + 1)
                            .roundNumber(activeRound)
                            .speaker("SELLER_AGENT")
                            .action("ACCEPT")
                            .sellerId(product.getSellerId())
                            .sellerStoreName(storeName)
                            .productId(product.getId())
                            .productTitle(product.getTitle())
                            .proposal(OfferProposal.builder()
                                    .price(agreedUnit)
                                    .unitPrice(agreedUnit)
                                    .shippingCost(effectiveSellerShipping)
                                    .totalPayable(agreedTotal)
                                    .quantity(qty)
                                    .currency(currency)
                                    .deliveryDays(sellerDecision.getDeliveryDays())
                                    .warrantyMonths(sellerDecision.getWarrantyMonths())
                                    .offerStatus("ACCEPTED")
                                    .roundNumber(activeRound)
                                    .bonusPerks(sellerDecision.getPerks())
                                    .build())
                            .agentReasoning(sellerDecision.getPublicExplanation())
                            .timestamp(Instant.now())
                            .build();
                    recordTurn(neg, sellerAcceptTurn);
                    sseStreamingHub.broadcast(neg.getId(), NegotiationStreamEvent.builder()
                            .eventType("SELLER_ACCEPTED")
                            .negotiationId(neg.getId())
                            .turnNumber(sellerAcceptTurn.getTurnNumber())
                            .speaker("SELLER_AGENT")
                            .payload(Map.of("agreedPrice", agreedTotal, "unitPrice", agreedUnit, "sellerId", product.getSellerId(), "storeName", storeName))
                            .timestamp(Instant.now())
                            .build());
                    if (tryLockAndSeal(neg, product, policy, buyer, sellerAcceptTurn, agreedUnit, effectiveSellerShipping, qty)) {
                        return SellerSessionOutcome.agreed();
                    }
                    return SellerSessionOutcome.failed("Inventory depleted at agreement seal", activeRound, agreedTotal);
                }
            }

            if ("REJECT".equalsIgnoreCase(sellerDecision.getAction())) {
                Turn sellerRejectTurn = Turn.builder()
                        .turnNumber(neg.getTurns().size() + 1)
                        .roundNumber(activeRound)
                        .speaker("SELLER_AGENT")
                        .action("REJECT")
                        .sellerId(product.getSellerId())
                        .sellerStoreName(storeName)
                        .productId(product.getId())
                        .productTitle(product.getTitle())
                        .proposal(OfferProposal.builder()
                                .price(lastSellerUnitPrice)
                                .unitPrice(lastSellerUnitPrice)
                                .shippingCost(effectiveSellerShipping)
                                .totalPayable(lastSellerUnitPrice.multiply(BigDecimal.valueOf(qty)).add(effectiveSellerShipping))
                                .quantity(qty)
                                .currency(currency)
                                .deliveryDays(sellerDecision.getDeliveryDays())
                                .warrantyMonths(sellerDecision.getWarrantyMonths())
                                .offerStatus("REJECTED")
                                .roundNumber(activeRound)
                                .bonusPerks(sellerDecision.getPerks())
                                .build())
                        .agentReasoning(sellerDecision.getPublicExplanation())
                        .timestamp(Instant.now())
                        .build();
                recordTurn(neg, sellerRejectTurn);
                return SellerSessionOutcome.failed(
                        "Merchant declined counteroffer below acceptable store margin",
                        activeRound,
                        lastSellerUnitPrice.multiply(BigDecimal.valueOf(qty)).add(effectiveSellerShipping)
                );
            }

            // Seller counters
            lastSellerUnitPrice = sellerDecision.getCounterUnitPrice().setScale(2, RoundingMode.HALF_UP);
            currentDeliveryDays = sellerDecision.getDeliveryDays() > 0 ? sellerDecision.getDeliveryDays() : currentDeliveryDays;
            currentWarrantyMonths = sellerDecision.getWarrantyMonths() > 0 ? sellerDecision.getWarrantyMonths() : currentWarrantyMonths;
            currentPerks = sellerDecision.getPerks() != null ? sellerDecision.getPerks() : currentPerks;
            BigDecimal counterTotal = lastSellerUnitPrice.multiply(BigDecimal.valueOf(qty)).add(effectiveSellerShipping).setScale(2, RoundingMode.HALF_UP);

            NegotiationOffer counterOffer = NegotiationOffer.builder()
                    .id("offer-" + UUID.randomUUID().toString().substring(0, 8))
                    .negotiationId(neg.getId())
                    .sellerId(product.getSellerId())
                    .productId(product.getId())
                    .roundNumber(activeRound)
                    .unitPrice(lastSellerUnitPrice)
                    .quantity(qty)
                    .shippingCost(effectiveSellerShipping)
                    .deliveryEstimateDays(currentDeliveryDays)
                    .warrantyMonths(currentWarrantyMonths)
                    .perks(currentPerks)
                    .offerStatus("COUNTERED")
                    .publicExplanation(sellerDecision.getPublicExplanation())
                    .timestamp(Instant.now())
                    .build();
            neg.getOffers().add(counterOffer);
            latestSellerOffer = counterOffer;

            Turn sellerCounterTurn = Turn.builder()
                    .turnNumber(neg.getTurns().size() + 1)
                    .roundNumber(activeRound)
                    .speaker("SELLER_AGENT")
                    .action("COUNTER")
                    .sellerId(product.getSellerId())
                    .sellerStoreName(storeName)
                    .productId(product.getId())
                    .productTitle(product.getTitle())
                    .proposal(OfferProposal.builder()
                            .price(lastSellerUnitPrice)
                            .unitPrice(lastSellerUnitPrice)
                            .shippingCost(effectiveSellerShipping)
                            .totalPayable(counterTotal)
                            .quantity(qty)
                            .currency(currency)
                            .deliveryDays(currentDeliveryDays)
                            .warrantyMonths(currentWarrantyMonths)
                            .offerStatus("COUNTERED")
                            .roundNumber(activeRound)
                            .bonusPerks(currentPerks)
                            .build())
                    .agentReasoning(sellerDecision.getPublicExplanation())
                    .timestamp(Instant.now())
                    .build();
            recordTurn(neg, sellerCounterTurn);
            broadcastTurn(neg.getId(), sellerCounterTurn, product, policy, buyerRep);
            sseStreamingHub.broadcast(neg.getId(), NegotiationStreamEvent.builder()
                    .eventType("SELLER_COUNTEROFFER")
                    .negotiationId(neg.getId())
                    .turnNumber(sellerCounterTurn.getTurnNumber())
                    .speaker("SELLER_AGENT")
                    .payload(sellerCounterTurn)
                    .timestamp(Instant.now())
                    .build());
            pause();
        }

        // After final seller counter in maxRounds, allow Buyer one final acceptance check if landed cost <= ceiling
        BigDecimal finalLanded = latestSellerOffer.calculateTotalCost().setScale(2, RoundingMode.HALF_UP);
        if (finalLanded.compareTo(pref.getMaxCeilingPrice()) <= 0 && latestSellerOffer.getUnitPrice().compareTo(policy.getFloorPrice()) >= 0) {
            Turn finalAccept = Turn.builder()
                    .turnNumber(neg.getTurns().size() + 1)
                    .roundNumber(maxRounds)
                    .speaker("BUYER_AGENT")
                    .action("ACCEPT")
                    .sellerId(product.getSellerId())
                    .sellerStoreName(storeName)
                    .productId(product.getId())
                    .productTitle(product.getTitle())
                    .proposal(OfferProposal.builder()
                            .price(latestSellerOffer.getUnitPrice())
                            .unitPrice(latestSellerOffer.getUnitPrice())
                            .shippingCost(latestSellerOffer.getShippingCost())
                            .totalPayable(finalLanded)
                            .quantity(qty)
                            .currency(currency)
                            .deliveryDays(latestSellerOffer.getDeliveryEstimateDays())
                            .warrantyMonths(latestSellerOffer.getWarrantyMonths())
                            .offerStatus("ACCEPTED")
                            .roundNumber(maxRounds)
                            .bonusPerks(latestSellerOffer.getPerks())
                            .build())
                    .agentReasoning(String.format("Thank you, %s! That final total of $%s with %d-day shipping works cleanly within my budget.",
                            storeName, finalLanded.toPlainString(), latestSellerOffer.getDeliveryEstimateDays()))
                    .timestamp(Instant.now())
                    .build();
            recordTurn(neg, finalAccept);
            if (tryLockAndSeal(neg, product, policy, buyer, finalAccept, latestSellerOffer.getUnitPrice(), latestSellerOffer.getShippingCost(), qty)) {
                return SellerSessionOutcome.agreed();
            }
            return SellerSessionOutcome.failed("Inventory depleted before confirmation", maxRounds, finalLanded);
        }

        return SellerSessionOutcome.failed(
                String.format("Reached %d-round limit without a match within budget ($%s final landed quote)", maxRounds, finalLanded.toPlainString()),
                maxRounds,
                finalLanded
        );
    }

    private boolean tryLockAndSeal(Negotiation neg, Product product, NegotiationPolicy policy, User buyer,
                                   Turn finalTurn, BigDecimal agreedUnitPrice, BigDecimal shippingCost, int qty) {
        try {
            Product fresh = productService.getProductById(product.getId());
            if (!fresh.isActive() || fresh.getAvailableInventory() < qty) {
                return false;
            }
            sealAgreement(neg, fresh, fresh.getNegotiationPolicy() != null ? fresh.getNegotiationPolicy() : policy,
                    buyer, finalTurn, agreedUnitPrice, shippingCost, qty);
            return neg.getAgreementSeal() != null;
        } catch (Exception ex) {
            log.warn("Could not seal deal on product [{}]: {}", product.getId(), ex.getMessage());
            return false;
        }
    }

    private void sealAgreement(Negotiation neg, Product product, NegotiationPolicy policy, User buyer,
                               Turn finalTurn, BigDecimal agreedUnitPrice, BigDecimal shippingCost, int qty) {
        BigDecimal unit = agreedUnitPrice.setScale(2, RoundingMode.HALF_UP);
        BigDecimal shipping = shippingCost != null ? shippingCost.setScale(2, RoundingMode.HALF_UP) : BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        BigDecimal totalPayable = unit.multiply(BigDecimal.valueOf(qty)).add(shipping).setScale(2, RoundingMode.HALF_UP);

        // Double security guardrail verification
        if (policy != null && unit.compareTo(policy.getFloorPrice()) < 0) {
            log.error("CRITICAL SECURITY ALERT: Attempted to seal deal at ${} below floor ${}. Aborting seal.", unit, policy.getFloorPrice());
            handleFailureState(neg.getId(), "Floor price violation prevented by server guardrail");
            return;
        }

        if (neg.getBuyerPreferences() != null && neg.getBuyerPreferences().getMaxCeilingPrice() != null
                && totalPayable.compareTo(neg.getBuyerPreferences().getMaxCeilingPrice()) > 0) {
            log.error("CRITICAL SECURITY ALERT: Attempted to seal total ${} above ceiling ${}. Aborting seal.", totalPayable, neg.getBuyerPreferences().getMaxCeilingPrice());
            handleFailureState(neg.getId(), "Ceiling price violation prevented by server guardrail");
            return;
        }

        byte[] tokenBytes = new byte[16];
        SECURE_RANDOM.nextBytes(tokenBytes);
        StringBuilder tokenSb = new StringBuilder("deal_sec_");
        for (byte b : tokenBytes) {
            tokenSb.append(String.format("%02x", b));
        }
        String dealToken = tokenSb.toString();
        String agreementId = "AGR-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        Instant expiresAt = Instant.now().plus(AGREEMENT_LOCK_MINUTES, ChronoUnit.MINUTES);

        productService.reserveStock(product.getId(), qty);

        AgreementSeal seal = AgreementSeal.builder()
                .agreementId(agreementId)
                .dealToken(dealToken)
                .sealedAt(Instant.now())
                .expiresAt(expiresAt)
                .reservationExpiresAt(expiresAt)
                .unitPrice(unit)
                .shippingCost(shipping)
                .quantity(qty)
                .finalPrice(totalPayable)
                .currency(StringUtils.hasText(product.getCurrency()) ? product.getCurrency() : "USD")
                .status("AWAITING_PAYMENT")
                .stockReserved(true)
                .build();

        FairDealMetrics finalMetrics = fairDealEngine.evaluateDeal(
                totalPayable, product.getListPrice().multiply(BigDecimal.valueOf(qty)).add(shipping),
                policy != null ? policy.getFloorPrice().multiply(BigDecimal.valueOf(qty)).add(shipping) : totalPayable,
                neg.getBuyerPreferences().getTargetPrice().multiply(BigDecimal.valueOf(qty)),
                neg.getBuyerPreferences().getMaxCeilingPrice(),
                finalTurn.getProposal().getWarrantyMonths(), finalTurn.getProposal().getDeliveryDays(),
                buyer != null ? buyer.getReputationScore() : 90.0
        );
        neg.setAgreementSeal(seal);
        neg.setSellerId(product.getSellerId());
        neg.setProductId(product.getId());
        neg.setProductTitle(product.getTitle());
        neg.setCurrentSellerStoreName(resolveStoreName(product.getSellerId()));
        neg.setFairDealMetrics(finalMetrics);
        neg.setStatus(NegotiationStatus.ACCEPTED);
        negotiationRepository.save(neg);

        sseStreamingHub.broadcast(neg.getId(), NegotiationStreamEvent.builder()
                .eventType("DEAL_SELECTED")
                .negotiationId(neg.getId())
                .turnNumber(finalTurn.getTurnNumber())
                .speaker(finalTurn.getSpeaker())
                .payload(seal)
                .timestamp(Instant.now())
                .build());

        sseStreamingHub.broadcast(neg.getId(), NegotiationStreamEvent.builder()
                .eventType("AGREED")
                .negotiationId(neg.getId())
                .turnNumber(finalTurn.getTurnNumber())
                .speaker(finalTurn.getSpeaker())
                .payload(seal)
                .timestamp(Instant.now())
                .build());

        if (agreementPostProcessor != null) {
            try {
                agreementPostProcessor.onAgreementSealed(neg);
                negotiationRepository.save(neg);
            } catch (Exception ex) {
                log.warn("Non-blocking post-agreement payment preparation note for [{}]: {}", neg.getId(), ex.getMessage());
            }
        }

        log.info("Negotiation [{}] sealed agreement [{}] with seller [{}] at landed total ${}",
                neg.getId(), agreementId, product.getSellerId(), totalPayable);
    }

    private void recordSellerFailureAndNotify(Negotiation neg, Product candidate, String storeName,
                                              String reason, int rounds, BigDecimal lastPrice, int idx, int total) {
        SellerAttemptRecord attempt = new SellerAttemptRecord(
                candidate.getSellerId(),
                storeName,
                candidate.getId(),
                candidate.getTitle(),
                reason,
                rounds,
                lastPrice,
                Instant.now()
        );
        neg.getFailedSellerAttempts().add(attempt);
        negotiationRepository.save(neg);

        sseStreamingHub.broadcast(neg.getId(), NegotiationStreamEvent.builder()
                .eventType("SELLER_FALLBACK_TRIGGERED")
                .negotiationId(neg.getId())
                .speaker("MARKETPLACE")
                .payload(Map.of(
                        "sellerId", candidate.getSellerId(),
                        "storeName", storeName,
                        "productId", candidate.getId(),
                        "reason", reason,
                        "roundsCompleted", rounds,
                        "sellerIndex", idx + 1,
                        "totalCandidates", total
                ))
                .timestamp(Instant.now())
                .build());
    }

    private void handleAllSellersExhausted(Negotiation neg, Product primaryProduct, BuyerPreferences pref) {
        neg.setStatus(NegotiationStatus.REJECTED);
        String summary = String.format(
                "All %d eligible merchant(s) for %s were evaluated, and none could meet all constraints within your total budget.",
                Math.max(1, neg.getFailedSellerAttempts().size()),
                primaryProduct.getTitle()
        );
        neg.setNoDealSummary(summary);
        neg.setFallbackNotice(summary);

        // Populate alternative suggestions in same category or closest landed price
        List<SellerCandidateSummary> alts = new ArrayList<>();
        for (Product p : productRepository.findByActiveTrue()) {
            if (!p.isActive() || p.getAvailableInventory() <= 0) continue;
            if (neg.getParticipatingProductIds().contains(p.getId())) continue;
            if (StringUtils.hasText(primaryProduct.getCategory()) && p.getCategory() != null
                    && !primaryProduct.getCategory().equalsIgnoreCase(p.getCategory())) continue;
            alts.add(new SellerCandidateSummary(
                    p.getSellerId(), resolveStoreName(p.getSellerId()), p.getId(), p.getTitle(), p.getBrand(),
                    p.getListPrice(), p.getShippingCost(), p.getEstimatedDeliveryDays(), 12,
                    p.getAvailableInventory(), "ALTERNATIVE", 75.0
            ));
            if (alts.size() >= 3) break;
        }
        neg.setAlternatives(alts);
        neg.setUpdatedAt(Instant.now());
        negotiationRepository.save(neg);

        sseStreamingHub.broadcast(neg.getId(), NegotiationStreamEvent.builder()
                .eventType("NO_DEAL_EXHAUSTED")
                .negotiationId(neg.getId())
                .speaker("MARKETPLACE")
                .payload(Map.of("summary", summary, "failedAttempts", neg.getFailedSellerAttempts(), "alternatives", alts))
                .timestamp(Instant.now())
                .build());

        sseStreamingHub.broadcast(neg.getId(), NegotiationStreamEvent.builder()
                .eventType("REJECTED")
                .negotiationId(neg.getId())
                .speaker("MARKETPLACE")
                .payload(summary)
                .timestamp(Instant.now())
                .build());
    }

    private void updateQueueStatus(Negotiation neg, String sellerId, String status) {
        if (neg.getSellerQueue() == null) return;
        for (SellerCandidateSummary item : neg.getSellerQueue()) {
            if (sellerId != null && sellerId.equals(item.getSellerId())) {
                item.setStatus(status);
            }
        }
    }

    private String resolveStoreName(String sellerId) {
        if (!StringUtils.hasText(sellerId)) return "Marketplace Merchant";
        if (sellerProfileRepository != null) {
            try {
                Optional<SellerProfile> profile = sellerProfileRepository.findByUserId(sellerId);
                if (profile.isPresent() && StringUtils.hasText(profile.get().getStoreName())) {
                    return profile.get().getStoreName();
                }
            } catch (Exception ignored) {}
        }
        try {
            User u = userService.getUserById(sellerId);
            if (u != null && StringUtils.hasText(u.getFullName())) {
                return u.getFullName();
            }
        } catch (Exception ignored) {}
        return "Merchant " + sellerId;
    }

    private String summarizeSpecs(Product product) {
        if (product.getSpecifications() == null || product.getSpecifications().isEmpty()) {
            return StringUtils.hasText(product.getCondition()) ? product.getCondition() : "Standard specifications";
        }
        List<String> parts = new ArrayList<>();
        int count = 0;
        for (Map.Entry<String, String> entry : product.getSpecifications().entrySet()) {
            if (count++ >= 4) break;
            parts.add(entry.getKey() + ": " + entry.getValue());
        }
        return String.join(", ", parts);
    }

    private <T> T executeWithTransientRetry(Supplier<T> supplier) {
        RuntimeException last = null;
        for (int attempt = 1; attempt <= 2; attempt++) {
            try {
                return supplier.get();
            } catch (RuntimeException ex) {
                last = ex;
                log.warn("Transient agent step failure (attempt {}/2): {}", attempt, ex.getMessage());
            }
        }
        throw last != null ? last : new RuntimeException("Agent invocation failed");
    }

    private void pause() {
        if (pacingDelayMs > 0) {
            try {
                Thread.sleep(pacingDelayMs);
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
            }
        }
    }

    private static class SellerSessionOutcome {
        final boolean agreed;
        final String failureReason;
        final int roundsCompleted;
        final BigDecimal lastOfferPrice;

        private SellerSessionOutcome(boolean agreed, String failureReason, int roundsCompleted, BigDecimal lastOfferPrice) {
            this.agreed = agreed;
            this.failureReason = failureReason;
            this.roundsCompleted = roundsCompleted;
            this.lastOfferPrice = lastOfferPrice;
        }

        static SellerSessionOutcome agreed() { return new SellerSessionOutcome(true, null, 0, null); }
        static SellerSessionOutcome failed(String reason, int rounds, BigDecimal price) {
            return new SellerSessionOutcome(false, reason, rounds, price);
        }
    }

    private void handleFailureState(String negotiationId, String reason) {
        try {
            Negotiation neg = getNegotiationById(negotiationId);
            neg.setStatus(NegotiationStatus.REJECTED);
            neg.setUpdatedAt(Instant.now());
            negotiationRepository.save(neg);

            sseStreamingHub.broadcast(negotiationId, NegotiationStreamEvent.builder()
                    .eventType("SELLER_REJECTED")
                    .negotiationId(negotiationId)
                    .speaker("MARKETPLACE")
                    .payload(reason)
                    .timestamp(Instant.now())
                    .build());

            sseStreamingHub.broadcast(negotiationId, NegotiationStreamEvent.builder()
                    .eventType("REJECTED")
                    .negotiationId(negotiationId)
                    .speaker("MARKETPLACE")
                    .payload("Negotiation concluded with failure state: " + reason)
                    .timestamp(Instant.now())
                    .build());
        } catch (Exception ignored) {}
    }

    private void recordTurn(Negotiation neg, Turn turn) {
        neg.getTurns().add(turn);
        neg.setCurrentOffer(turn.getProposal());
        neg.setUpdatedAt(Instant.now());
        negotiationRepository.save(neg);
    }

    private void broadcastTurn(String negotiationId, Turn turn, Product product, NegotiationPolicy policy, double reputation) {
        Negotiation neg = getNegotiationById(negotiationId);
        FairDealMetrics metrics = fairDealEngine.evaluateDeal(
                turn.getProposal().getPrice(),
                product.getListPrice(),
                policy != null ? policy.getFloorPrice() : turn.getProposal().getPrice(),
                neg.getBuyerPreferences().getTargetPrice(),
                neg.getBuyerPreferences().getMaxCeilingPrice(),
                turn.getProposal().getWarrantyMonths(),
                turn.getProposal().getDeliveryDays(),
                reputation
        );
        neg.setFairDealMetrics(metrics);
        negotiationRepository.save(neg);

        sseStreamingHub.broadcast(negotiationId, NegotiationStreamEvent.builder()
                .eventType("OFFER_PROPOSED")
                .negotiationId(negotiationId)
                .turnNumber(turn.getTurnNumber())
                .speaker(turn.getSpeaker())
                .payload(turn)
                .timestamp(Instant.now())
                .build());

        sseStreamingHub.broadcast(negotiationId, NegotiationStreamEvent.builder()
                .eventType("FAIRDEAL_METRIC")
                .negotiationId(negotiationId)
                .turnNumber(turn.getTurnNumber())
                .speaker("FAIRDEAL_ENGINE")
                .payload(metrics)
                .timestamp(Instant.now())
                .build());
    }

    private void broadcastThinking(String negotiationId, String speaker, String message) {
        sseStreamingHub.broadcast(negotiationId, NegotiationStreamEvent.builder()
                .eventType("AGENT_THINKING")
                .negotiationId(negotiationId)
                .speaker(speaker)
                .payload(message)
                .timestamp(Instant.now())
                .build());
    }
}
