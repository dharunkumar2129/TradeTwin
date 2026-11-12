package com.tradetwin.negotiation.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tradetwin.catalog.dto.ProductPublicDto;
import com.tradetwin.catalog.model.NegotiationPolicy;
import com.tradetwin.catalog.model.Product;
import com.tradetwin.catalog.repository.ProductRepository;
import com.tradetwin.catalog.service.ProductService;
import com.tradetwin.deal.service.FairDealEngine;
import com.tradetwin.event.model.NegotiationEventDocument;
import com.tradetwin.event.model.NegotiationStreamEvent;
import com.tradetwin.event.service.SseStreamingHub;
import com.tradetwin.negotiation.dto.StartNegotiationRequest;
import com.tradetwin.negotiation.model.*;
import com.tradetwin.negotiation.repository.NegotiationRepository;
import com.tradetwin.seller.model.NegotiationStrategy;
import com.tradetwin.seller.model.SellerProfile;
import com.tradetwin.seller.repository.SellerProfileRepository;
import com.tradetwin.user.model.User;
import com.tradetwin.user.model.UserRole;
import com.tradetwin.user.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.math.BigDecimal;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

class NegotiationFallbackAndConversationTest {

    private NegotiationRepository negotiationRepository;
    private ProductRepository productRepository;
    private SellerProfileRepository sellerProfileRepository;
    private Map<String, Negotiation> negStore;
    private Map<String, Product> prodStore;
    private List<String> broadcastedEvents;
    private NegotiationOrchestrator orchestrator;
    private BuyerNegotiationAgent buyerAgent;
    private SellerNegotiationAgent sellerAgent;

    static class StubProductService extends ProductService {
        private final Map<String, Product> store;

        StubProductService(ProductRepository repo, Map<String, Product> store) {
            super(repo);
            this.store = store;
        }

        @Override
        public Product getProductById(String id) {
            Product p = store.get(id);
            if (p == null) throw new RuntimeException("Product not found: " + id);
            return p;
        }

        @Override
        public synchronized void reserveStock(String productId, int quantity) {
            Product p = getProductById(productId);
            if (p.getAvailableInventory() < quantity) {
                throw new RuntimeException("Out of stock");
            }
            p.setReservedCount(p.getReservedCount() + quantity);
        }

        @Override
        public synchronized void releaseStock(String productId, int quantity) {
            Product p = getProductById(productId);
            p.setReservedCount(Math.max(0, p.getReservedCount() - quantity));
        }
    }

    static class StubUserService extends UserService {
        StubUserService() {
            super(null);
        }

        @Override
        public User getUserById(String id) {
            return User.builder().id(id).email(id + "@tradetwin.com").fullName("Store " + id).role(UserRole.ROLE_BUYER).reputationScore(95.0).build();
        }
    }

    static class RecordingSseHub extends SseStreamingHub {
        private final List<String> eventNames;

        RecordingSseHub(List<String> eventNames) {
            super(null);
            this.eventNames = eventNames;
        }

        @Override
        public void broadcast(String negotiationId, NegotiationStreamEvent event) {
            if (event != null && event.getEventType() != null) {
                eventNames.add(event.getEventType());
            }
        }

        @Override
        public List<NegotiationEventDocument> getPersistedEvents(String negotiationId) {
            return List.of();
        }
    }

    @BeforeEach
    void setUp() {
        negStore = new java.util.concurrent.ConcurrentHashMap<>();
        prodStore = new java.util.concurrent.ConcurrentHashMap<>();
        broadcastedEvents = Collections.synchronizedList(new ArrayList<>());

        negotiationRepository = Mockito.mock(NegotiationRepository.class);
        productRepository = Mockito.mock(ProductRepository.class);
        sellerProfileRepository = Mockito.mock(SellerProfileRepository.class);

        when(negotiationRepository.save(any(Negotiation.class))).thenAnswer(inv -> {
            Negotiation n = inv.getArgument(0);
            if (n.getId() == null) n.setId("neg-test-1");
            negStore.put(n.getId(), n);
            return n;
        });
        when(negotiationRepository.findById(anyString())).thenAnswer(inv -> Optional.ofNullable(negStore.get(inv.getArgument(0))));
        when(productRepository.findByActiveTrue()).thenAnswer(inv -> prodStore.values().stream().filter(Product::isActive).toList());

        buyerAgent = new BuyerNegotiationAgent(new ObjectMapper());
        sellerAgent = new SellerNegotiationAgent(new ObjectMapper());

        orchestrator = new NegotiationOrchestrator(
                negotiationRepository,
                productRepository,
                new StubProductService(productRepository, prodStore),
                new StubUserService(),
                new FairDealEngine(),
                new RecordingSseHub(broadcastedEvents),
                buyerAgent,
                sellerAgent,
                sellerProfileRepository
        );
        orchestrator.setPacingDelayMs(0L);
    }

    private Product createSonyListing(String id, String sellerId, String storeName, BigDecimal list, BigDecimal floor, BigDecimal shipping, int deliveryDays, int stock, boolean allowFreeShipping) {
        SellerProfile profile = SellerProfile.builder().userId(sellerId).storeName(storeName).build();
        when(sellerProfileRepository.findByUserId(sellerId)).thenReturn(Optional.of(profile));

        Product p = Product.builder()
                .id(id)
                .sellerId(sellerId)
                .title("Sony WH-CH720N Wireless Noise Canceling Headphones")
                .brand("Sony")
                .sku("SONY-WHCH720N-" + sellerId)
                .category("Audio & Acoustics")
                .condition("Brand New")
                .listPrice(list)
                .shippingCost(shipping)
                .estimatedDeliveryDays(deliveryDays)
                .inventoryCount(stock)
                .reservedCount(0)
                .negotiationPolicy(NegotiationPolicy.builder()
                        .autoNegotiate(true)
                        .floorPrice(floor)
                        .targetPrice(floor.add(new BigDecimal("8.00")))
                        .autoAcceptPriceThreshold(floor.add(new BigDecimal("10.00")))
                        .strategy(NegotiationStrategy.BALANCED)
                        .maxRounds(4)
                        .allowFreeShipping(allowFreeShipping)
                        .allowWarrantySweeteners(true)
                        .maxWarrantyMonthsSweetener(24)
                        .maxDeliveryDaysCompromise(1)
                        .build())
                .active(true)
                .build();
        prodStore.put(id, p);
        return p;
    }

    @Test
    @DisplayName("Conversations are natural, avoid robotic jargon, and respect progressive concession rounds")
    void agentsUseNaturalDialogueAndProgressiveConcessions() {
        Product p = createSonyListing("p-1", "s-1", "NovaTech Electronics", new BigDecimal("100.00"), new BigDecimal("82.00"), BigDecimal.ZERO, 3, 10, true);
        ProductPublicDto dto = new StubProductService(productRepository, prodStore).toPublicDto(p);

        BuyerPreferences pref = BuyerPreferences.builder()
                .targetPrice(new BigDecimal("85.00"))
                .maxCeilingPrice(new BigDecimal("96.00"))
                .desiredDeliveryDays(3)
                .minWarrantyMonths(12)
                .build();

        SellerNegotiationAgent.SellerDecision s1 = sellerAgent.heuristicSellerDecision(
                new SellerNegotiationAgent.SellerDecisionContext("n1", "s-1", dto, p.getNegotiationPolicy(), BigDecimal.ZERO, 10, null, List.of(), 1, 4, 95.0)
        );
        assertFalse(s1.getPublicExplanation().toLowerCase().contains("benchmark"));
        assertFalse(s1.getPublicExplanation().toLowerCase().contains("policy floor"));
        assertFalse(s1.getPublicExplanation().contains("82.00"));
        assertTrue(s1.getCounterUnitPrice().compareTo(new BigDecimal("82.00")) > 0, "Seller should not collapse immediately to floor in round 1");

        BuyerNegotiationAgent.BuyerDecision b2 = buyerAgent.heuristicBuyerDecision(
                new BuyerNegotiationAgent.BuyerDecisionContext("n1", pref,
                        List.of(NegotiationOffer.builder().sellerId("s-1").productId("p-1").unitPrice(new BigDecimal("98.00")).deliveryEstimateDays(3).warrantyMonths(12).build()),
                        List.of(), 2, 4)
        );
        assertEquals("COUNTER", b2.getAction());
        assertFalse(b2.getPublicExplanation().toLowerCase().contains("multi-attribute budget utility"));
        assertFalse(b2.getPublicExplanation().contains("96.00"));
        assertTrue(b2.getProposedPrice().compareTo(new BigDecimal("85.00")) > 0 && b2.getProposedPrice().compareTo(new BigDecimal("96.00")) < 0,
                "Round 2 buyer counter should make moderate concession without jumping straight to ceiling");
    }

    @Test
    @DisplayName("Automatic fallback: when Seller 1 rejects above budget, automatically negotiates with Seller 2 and seals agreement")
    void orchestratorFallsBackToNextEligibleSellerAndSealsAgreement() {
        // Seller 1 has high floor ($106 ask, $103 floor, $5 shipping = min $108 > $100 budget)
        Product overBudget = createSonyListing("p-orbit", "s-orbit", "Orbit Electronics",
                new BigDecimal("112.00"), new BigDecimal("104.00"), new BigDecimal("7.00"), 4, 5, false);
        // Seller 2 can settle cleanly inside buyer $100 budget ($98 ask, $82 floor, $0 shipping)
        Product viable = createSonyListing("p-novatech", "s-novatech", "NovaTech Electronics",
                new BigDecimal("98.00"), new BigDecimal("82.00"), BigDecimal.ZERO, 3, 12, true);

        BuyerPreferences pref = BuyerPreferences.builder()
                .targetPrice(new BigDecimal("88.00"))
                .maxCeilingPrice(new BigDecimal("100.00"))
                .desiredDeliveryDays(4)
                .minWarrantyMonths(12)
                .requiredBrand("Sony")
                .build();

        StartNegotiationRequest req = StartNegotiationRequest.builder()
                .productId(overBudget.getId())
                .candidateProductIds(List.of(overBudget.getId(), viable.getId()))
                .preferences(pref)
                .buyerQuery("Find Sony wireless headphones under $100 with 4-day delivery")
                .build();

        Negotiation init = orchestrator.startNegotiation("buyer-1", req);
        waitForTerminalStatus(init.getId());

        Negotiation completed = negStore.get(init.getId());
        assertNotNull(completed);
        assertEquals(NegotiationStatus.ACCEPTED, completed.getStatus());
        assertEquals("s-novatech", completed.getSellerId(), "Should have fallen back to NovaTech Electronics");
        assertEquals("NovaTech Electronics", completed.getCurrentSellerStoreName());
        assertNotNull(completed.getAgreementSeal());
        assertNotNull(completed.getAgreementSeal().getAgreementId());
        assertTrue(completed.getAgreementSeal().getFinalPrice().compareTo(new BigDecimal("100.00")) <= 0);
        assertTrue(completed.getAgreementSeal().getFinalPrice().compareTo(new BigDecimal("82.00")) >= 0);

        // Prior failed seller trail & events preserved
        assertEquals(1, completed.getFailedSellerAttempts().size());
        assertEquals("Orbit Electronics", completed.getFailedSellerAttempts().get(0).getStoreName());
        assertEquals(0, overBudget.getReservedCount(), "Failed Seller 1 must have zero stock reserved");
        assertEquals(1, viable.getReservedCount(), "Winning Seller 2 must have 1 unit reserved");
        assertTrue(broadcastedEvents.contains("SELLER_FALLBACK_TRIGGERED"));
        assertTrue(broadcastedEvents.contains("NEXT_SELLER_STARTED"));
        assertTrue(broadcastedEvents.contains("DEAL_SELECTED"));
    }

    @Test
    @DisplayName("Strict mandatory brand enforcement prevents fallback from switching Sony to competing brand")
    void fallbackRespectsMandatoryBrandFilter() {
        Product sonyHighFloor = createSonyListing("p-sony-1", "s-orbit", "Orbit Electronics",
                new BigDecimal("115.00"), new BigDecimal("108.00"), BigDecimal.ZERO, 3, 5, false);

        // Non-Sony cheap alternative in same category should NOT be added when requiredBrand="Sony"
        Product boseCheap = Product.builder()
                .id("p-bose").sellerId("s-bose").title("Bose Noise Canceling Headphones").brand("Bose")
                .category("Audio & Acoustics").listPrice(new BigDecimal("95.00")).shippingCost(BigDecimal.ZERO)
                .estimatedDeliveryDays(2).inventoryCount(10)
                .negotiationPolicy(NegotiationPolicy.builder().autoNegotiate(true).floorPrice(new BigDecimal("78.00")).maxRounds(4).build())
                .active(true).build();
        prodStore.put("p-bose", boseCheap);

        BuyerPreferences pref = BuyerPreferences.builder()
                .targetPrice(new BigDecimal("85.00"))
                .maxCeilingPrice(new BigDecimal("100.00"))
                .requiredBrand("Sony")
                .build();

        List<Product> queue = orchestrator.buildSellerCandidateQueue(sonyHighFloor, pref, List.of());
        assertEquals(1, queue.size());
        assertEquals("p-sony-1", queue.get(0).getId());
    }

    @Test
    @DisplayName("Exhausting all eligible sellers terminates cleanly with NO_DEAL_EXHAUSTED and zero locked stock")
    void exhaustingAllSellersReturnsNoDealSummaryWithoutLockingStock() {
        Product p1 = createSonyListing("p-1", "s-1", "Store One", new BigDecimal("120.00"), new BigDecimal("112.00"), BigDecimal.ZERO, 3, 5, false);
        Product p2 = createSonyListing("p-2", "s-2", "Store Two", new BigDecimal("125.00"), new BigDecimal("115.00"), BigDecimal.ZERO, 3, 5, false);

        BuyerPreferences pref = BuyerPreferences.builder()
                .targetPrice(new BigDecimal("80.00"))
                .maxCeilingPrice(new BigDecimal("95.00"))
                .requiredBrand("Sony")
                .build();

        Negotiation init = orchestrator.startNegotiation("buyer-1", StartNegotiationRequest.builder()
                .productId(p1.getId())
                .candidateProductIds(List.of(p1.getId(), p2.getId()))
                .preferences(pref)
                .build());
        waitForTerminalStatus(init.getId());

        Negotiation exhausted = negStore.get(init.getId());
        assertEquals(NegotiationStatus.REJECTED, exhausted.getStatus());
        assertNull(exhausted.getAgreementSeal());
        assertNotNull(exhausted.getNoDealSummary());
        assertEquals(2, exhausted.getFailedSellerAttempts().size());
        assertEquals(0, p1.getReservedCount());
        assertEquals(0, p2.getReservedCount());
        assertTrue(broadcastedEvents.contains("NO_DEAL_EXHAUSTED"));
    }

    private void waitForTerminalStatus(String negotiationId) {
        for (int i = 0; i < 200; i++) {
            Negotiation n = negStore.get(negotiationId);
            if (n != null && n.getStatus() != null && n.getStatus().isTerminal()) {
                return;
            }
            try {
                Thread.sleep(20);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException(e);
            }
        }
        fail("Negotiation " + negotiationId + " did not reach a terminal status in time");
    }
}
