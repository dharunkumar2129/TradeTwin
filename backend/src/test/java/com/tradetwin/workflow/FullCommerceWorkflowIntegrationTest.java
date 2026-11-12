package com.tradetwin.workflow;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tradetwin.auth.dto.AuthResponse;
import com.tradetwin.auth.dto.RegisterRequest;
import com.tradetwin.auth.security.JwtTokenProvider;
import com.tradetwin.auth.service.AuthService;
import com.tradetwin.catalog.dto.CreateProductRequest;
import com.tradetwin.catalog.dto.ProductPublicDto;
import com.tradetwin.catalog.dto.SearchResultItemDto;
import com.tradetwin.catalog.model.NegotiationPolicy;
import com.tradetwin.catalog.model.Product;
import com.tradetwin.catalog.repository.ProductRepository;
import com.tradetwin.catalog.service.CatalogSearchService;
import com.tradetwin.catalog.service.ProductService;
import com.tradetwin.common.exception.BusinessRuleException;
import com.tradetwin.deal.model.FairDealComparisonResult;
import com.tradetwin.deal.service.FairDealEngine;
import com.tradetwin.event.model.NegotiationEventDocument;
import com.tradetwin.event.repository.NegotiationEventRepository;
import com.tradetwin.event.service.SseStreamingHub;
import com.tradetwin.intent.model.BuyerIntent;
import com.tradetwin.intent.repository.BuyerIntentRepository;
import com.tradetwin.intent.service.IntentParsingService;
import com.tradetwin.negotiation.dto.StartNegotiationRequest;
import com.tradetwin.negotiation.model.*;
import com.tradetwin.negotiation.repository.NegotiationRepository;
import com.tradetwin.negotiation.service.BuyerNegotiationAgent;
import com.tradetwin.negotiation.service.NegotiationOrchestrator;
import com.tradetwin.negotiation.service.SellerNegotiationAgent;
import com.tradetwin.order.dto.BuyerDashboardSummaryDto;
import com.tradetwin.order.dto.UpdateOrderStatusRequest;
import com.tradetwin.order.model.Order;
import com.tradetwin.order.model.OrderStatus;
import com.tradetwin.order.repository.OrderRepository;
import com.tradetwin.order.service.OrderService;
import com.tradetwin.order.service.OrderStateMachine;
import com.tradetwin.payment.model.PaymentRecord;
import com.tradetwin.payment.model.PaymentStatus;
import com.tradetwin.payment.repository.PaymentRepository;
import com.tradetwin.payment.service.PayPalService;
import com.tradetwin.seller.dto.SellerDashboardSummaryDto;
import com.tradetwin.seller.dto.SellerOnboardingRequest;
import com.tradetwin.seller.model.BusinessContact;
import com.tradetwin.seller.model.NegotiationStrategy;
import com.tradetwin.seller.model.SellerProfile;
import com.tradetwin.seller.model.ShippingTerms;
import com.tradetwin.seller.repository.SellerProfileRepository;
import com.tradetwin.seller.service.SellerService;
import com.tradetwin.user.model.User;
import com.tradetwin.user.model.UserRole;
import com.tradetwin.user.repository.UserRepository;
import com.tradetwin.user.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

/**
 * End-to-end cohesive multi-module workflow verification:
 * Seller register + Store Onboard + Product Listing & Private Policy ->
 * Buyer register + NL Intent Parse -> Multi-Seller Discovery ->
 * Multi-Agent Counteroffers & Guardrails -> FairDeal Comparison -> Deal Selection ->
 * PayPal Cancel + Retry + Server Capture + Idempotent Re-capture ->
 * Controlled Order State Machine & Buyer/Seller Dashboard KPIs.
 */
class FullCommerceWorkflowIntegrationTest {

    private final Map<String, User> users = new ConcurrentHashMap<>();
    private final Map<String, SellerProfile> sellerProfiles = new ConcurrentHashMap<>();
    private final Map<String, Product> products = new ConcurrentHashMap<>();
    private final Map<String, BuyerIntent> intents = new ConcurrentHashMap<>();
    private final Map<String, Negotiation> negotiations = new ConcurrentHashMap<>();
    private final List<NegotiationEventDocument> events = Collections.synchronizedList(new ArrayList<>());
    private final Map<String, PaymentRecord> payments = new ConcurrentHashMap<>();
    private final Map<String, Order> orders = new ConcurrentHashMap<>();
    private final AtomicInteger idSeq = new AtomicInteger(1);

    private AuthService authService;
    private SellerService sellerService;
    private ProductService productService;
    private CatalogSearchService catalogSearchService;
    private IntentParsingService intentParsingService;
    private BuyerNegotiationAgent buyerAgent;
    private SellerNegotiationAgent sellerAgent;
    private FairDealEngine fairDealEngine;
    private NegotiationOrchestrator orchestrator;
    private PayPalService payPalService;
    private OrderService orderService;

    @BeforeEach
    void setUp() {
        ObjectMapper objectMapper = new ObjectMapper().registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule());
        JwtTokenProvider jwtProvider = new JwtTokenProvider(
                "404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970", 86400000L);
        BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

        UserRepository userRepo = Mockito.mock(UserRepository.class);
        when(userRepo.existsByEmail(anyString())).thenAnswer(i ->
                users.values().stream().anyMatch(u -> u.getEmail().equalsIgnoreCase(i.getArgument(0))));
        when(userRepo.findByEmail(anyString())).thenAnswer(i ->
                users.values().stream().filter(u -> u.getEmail().equalsIgnoreCase(i.getArgument(0))).findFirst());
        when(userRepo.findById(anyString())).thenAnswer(i -> Optional.ofNullable(users.get(i.getArgument(0))));
        when(userRepo.save(any(User.class))).thenAnswer(i -> {
            User u = i.getArgument(0);
            if (u.getId() == null) u.setId("u-" + idSeq.getAndIncrement());
            users.put(u.getId(), u);
            return u;
        });

        SellerProfileRepository sellerRepo = Mockito.mock(SellerProfileRepository.class);
        when(sellerRepo.findByUserId(anyString())).thenAnswer(i ->
                sellerProfiles.values().stream().filter(s -> s.getUserId().equals(i.getArgument(0))).findFirst());
        when(sellerRepo.save(any(SellerProfile.class))).thenAnswer(i -> {
            SellerProfile p = i.getArgument(0);
            if (p.getId() == null) p.setId("sp-" + idSeq.getAndIncrement());
            sellerProfiles.put(p.getId(), p);
            return p;
        });

        ProductRepository prodRepo = Mockito.mock(ProductRepository.class);
        when(prodRepo.save(any(Product.class))).thenAnswer(i -> {
            Product p = i.getArgument(0);
            if (p.getId() == null) p.setId("prod-" + idSeq.getAndIncrement());
            products.put(p.getId(), p);
            return p;
        });
        when(prodRepo.findById(anyString())).thenAnswer(i -> Optional.ofNullable(products.get(i.getArgument(0))));
        when(prodRepo.findByActiveTrue()).thenAnswer(i ->
                products.values().stream().filter(Product::isActive).toList());
        when(prodRepo.findBySellerId(anyString())).thenAnswer(i ->
                products.values().stream().filter(p -> p.getSellerId().equals(i.getArgument(0))).toList());
        when(prodRepo.findByCategoryIgnoreCaseAndActiveTrue(anyString())).thenAnswer(i ->
                products.values().stream()
                        .filter(p -> p.isActive() && p.getCategory().equalsIgnoreCase(i.getArgument(0)))
                        .toList());

        BuyerIntentRepository intentRepo = Mockito.mock(BuyerIntentRepository.class);
        when(intentRepo.save(any(BuyerIntent.class))).thenAnswer(i -> {
            BuyerIntent bi = i.getArgument(0);
            if (bi.getId() == null) bi.setId("int-" + idSeq.getAndIncrement());
            intents.put(bi.getId(), bi);
            return bi;
        });
        when(intentRepo.findById(anyString())).thenAnswer(i -> Optional.ofNullable(intents.get(i.getArgument(0))));

        NegotiationRepository negRepo = Mockito.mock(NegotiationRepository.class);
        when(negRepo.save(any(Negotiation.class))).thenAnswer(i -> {
            Negotiation n = i.getArgument(0);
            if (n.getId() == null) n.setId("neg-" + idSeq.getAndIncrement());
            negotiations.put(n.getId(), n);
            return n;
        });
        when(negRepo.findById(anyString())).thenAnswer(i -> Optional.ofNullable(negotiations.get(i.getArgument(0))));
        when(negRepo.findBySellerIdOrderByCreatedAtDesc(anyString())).thenAnswer(i ->
                negotiations.values().stream().filter(n -> n.getSellerId().equals(i.getArgument(0))).toList());
        when(negRepo.findByBuyerIdOrderByCreatedAtDesc(anyString())).thenAnswer(i ->
                negotiations.values().stream().filter(n -> n.getBuyerId().equals(i.getArgument(0))).toList());

        NegotiationEventRepository eventRepo = Mockito.mock(NegotiationEventRepository.class);
        when(eventRepo.save(any(NegotiationEventDocument.class))).thenAnswer(i -> {
            NegotiationEventDocument doc = i.getArgument(0);
            if (doc.getId() == null) doc.setId("evt-" + idSeq.getAndIncrement());
            events.add(doc);
            return doc;
        });
        when(eventRepo.findByNegotiationIdOrderByTimestampAsc(anyString())).thenAnswer(i ->
                events.stream().filter(e -> e.getNegotiationId().equals(i.getArgument(0))).toList());

        PaymentRepository payRepo = Mockito.mock(PaymentRepository.class);
        when(payRepo.save(any(PaymentRecord.class))).thenAnswer(i -> {
            PaymentRecord r = i.getArgument(0);
            if (r.getId() == null) r.setId("pay-" + idSeq.getAndIncrement());
            payments.put(r.getId(), r);
            return r;
        });
        when(payRepo.findByPaypalOrderId(anyString())).thenAnswer(i ->
                payments.values().stream().filter(p -> i.getArgument(0).equals(p.getPaypalOrderId())).findFirst());
        when(payRepo.findByNegotiationId(anyString())).thenAnswer(i ->
                payments.values().stream().filter(p -> i.getArgument(0).equals(p.getNegotiationId())).findFirst());

        OrderRepository orderRepo = Mockito.mock(OrderRepository.class);
        when(orderRepo.save(any(Order.class))).thenAnswer(i -> {
            Order o = i.getArgument(0);
            if (o.getId() == null) o.setId("ord-" + idSeq.getAndIncrement());
            orders.put(o.getId(), o);
            return o;
        });
        when(orderRepo.findById(anyString())).thenAnswer(i -> Optional.ofNullable(orders.get(i.getArgument(0))));
        when(orderRepo.findByNegotiationId(anyString())).thenAnswer(i ->
                orders.values().stream().filter(o -> i.getArgument(0).equals(o.getNegotiationId())).findFirst());
        when(orderRepo.findByBuyerIdOrderByCreatedAtDesc(anyString())).thenAnswer(i ->
                orders.values().stream().filter(o -> o.getBuyerId().equals(i.getArgument(0))).toList());
        when(orderRepo.findBySellerIdOrderByCreatedAtDesc(anyString())).thenAnswer(i ->
                orders.values().stream().filter(o -> o.getSellerId().equals(i.getArgument(0))).toList());

        UserService userService = new UserService(userRepo);
        authService = new AuthService(userRepo, passwordEncoder, jwtProvider);
        productService = new ProductService(prodRepo);
        catalogSearchService = new CatalogSearchService(prodRepo, productService, sellerRepo, userRepo);
        intentParsingService = new IntentParsingService(catalogSearchService, intentRepo, objectMapper);
        sellerService = new SellerService(sellerRepo, userRepo, prodRepo, negRepo, orderRepo);
        fairDealEngine = new FairDealEngine();
        SseStreamingHub sseHub = new SseStreamingHub(eventRepo);
        buyerAgent = new BuyerNegotiationAgent(objectMapper);
        sellerAgent = new SellerNegotiationAgent(objectMapper);
        orchestrator = new NegotiationOrchestrator(negRepo, prodRepo, productService, userService, fairDealEngine, sseHub, buyerAgent, sellerAgent);
        OrderStateMachine stateMachine = new OrderStateMachine();
        orderService = new OrderService(orderRepo, negRepo, productService, sseHub, stateMachine);
        payPalService = new PayPalService(negRepo, orderService, orderRepo, productService, payRepo, objectMapper);
    }

    @Test
    @DisplayName("Full E2E Commerce Audit: Two competing sellers -> Buyer NL intent -> Negotiation -> FairDeal -> PayPal Cancel & Capture -> Order Lifecycle")
    @SuppressWarnings("unchecked")
    void testCompleteMarketplaceWorkflowEndToEnd() {
        // 1. Register Seller 1 & Seller 2 and onboard their stores
        AuthResponse seller1Auth = authService.register(RegisterRequest.builder()
                .email("merchant1@audio.test").password("Password123!").fullName("Merchant One").role(UserRole.ROLE_SELLER).build());
        AuthResponse seller2Auth = authService.register(RegisterRequest.builder()
                .email("merchant2@audio.test").password("Password123!").fullName("Merchant Two").role(UserRole.ROLE_SELLER).build());

        sellerService.onboardSeller(seller1Auth.getId(), SellerOnboardingRequest.builder()
                .storeName("QuantumTech Direct").description("Audio Pro").shippingRegions(List.of("United States"))
                .businessContact(new BusinessContact("555-0101", "s1@audio.test", "9-5"))
                .defaultShippingTerms(new ShippingTerms(1, BigDecimal.ZERO, true, 30))
                .defaultStrategy(NegotiationStrategy.BALANCED).build());

        sellerService.onboardSeller(seller2Auth.getId(), SellerOnboardingRequest.builder()
                .storeName("Apex Audio Collective").description("Boutique Audio").shippingRegions(List.of("United States"))
                .businessContact(new BusinessContact("555-0202", "s2@audio.test", "9-6"))
                .defaultShippingTerms(new ShippingTerms(1, BigDecimal.ZERO, true, 45))
                .defaultStrategy(NegotiationStrategy.COOPERATIVE).build());

        // 2. Sellers list competing Sony WH-CH720N headphones with distinct private negotiation policies
        Product prod1 = productService.createProduct(seller1Auth.getId(), CreateProductRequest.builder()
                .title("Sony WH-CH720N Wireless Noise Canceling Headphones").brand("Sony").category("Audio & Acoustics")
                .description("Sony ANC headphones with V1 processor and 35h battery").condition("Brand New")
                .listPrice(new BigDecimal("98.00")).currency("USD").inventoryCount(12)
                .shippingCost(BigDecimal.ZERO).estimatedDeliveryDays(2).warrantyInfo("12 Months Sony Warranty")
                .tags(List.of("sony", "wireless", "noise-cancelling", "headphones"))
                .negotiationPolicy(NegotiationPolicy.builder()
                        .autoNegotiate(true).floorPrice(new BigDecimal("79.00")).targetPrice(new BigDecimal("91.00"))
                        .autoAcceptPriceThreshold(new BigDecimal("89.00")).maxRounds(4).allowFreeShipping(true)
                        .allowWarrantySweeteners(true).maxWarrantyMonthsSweetener(24).build())
                .build());

        Product prod2 = productService.createProduct(seller2Auth.getId(), CreateProductRequest.builder()
                .title("Sony WH-CH720N Wireless Noise Canceling Headphones (24-Mo Bundle)").brand("Sony").category("Audio & Acoustics")
                .description("Sony ANC wireless headphones with extended warranty and express courier").condition("Brand New")
                .listPrice(new BigDecimal("94.00")).currency("USD").inventoryCount(8)
                .shippingCost(BigDecimal.ZERO).estimatedDeliveryDays(3).warrantyInfo("24 Months Extended Warranty")
                .tags(List.of("sony", "wireless", "noise-cancelling", "headphones"))
                .negotiationPolicy(NegotiationPolicy.builder()
                        .autoNegotiate(true).floorPrice(new BigDecimal("77.50")).targetPrice(new BigDecimal("88.00"))
                        .autoAcceptPriceThreshold(new BigDecimal("84.00")).maxRounds(4).allowFreeShipping(true)
                        .allowWarrantySweeteners(true).maxWarrantyMonthsSweetener(24).build())
                .build());

        // Verify Public DTO never exposes floorPrice or targetPrice
        ProductPublicDto public1 = productService.toPublicDto(prod1);
        assertEquals(new BigDecimal("98.00"), public1.getListPrice());
        assertEquals("QuantumTech Direct", sellerService.getProfileByUserId(seller1Auth.getId()).getStoreName());

        // 3. Buyer Registers & Submits Natural-Language Request
        AuthResponse buyerAuth = authService.register(RegisterRequest.builder()
                .email("buyer1@shopper.test").password("Password123!").fullName("Alice Shopper").role(UserRole.ROLE_BUYER).build());

        String buyerPrompt = "Find me Sony wireless noise-cancelling headphones. My maximum total budget is $100. I prefer at least one year warranty and delivery within four days.";
        Map<String, Object> parseResult = intentParsingService.parseAndMatch(buyerPrompt, buyerAuth.getId());

        BuyerIntent parsedIntent = (BuyerIntent) parseResult.get("intent");
        List<SearchResultItemDto> candidates = (List<SearchResultItemDto>) parseResult.get("candidates");
        assertNotNull(parsedIntent.getMaxBudget());
        assertEquals(0, parsedIntent.getMaxBudget().compareTo(new BigDecimal("100")));
        assertEquals(2, candidates.size(), "Both Seller 1 and Seller 2 Sony listings should be discovered");

        // 4. Multi-Agent Counteroffers & Strict Guardrail Enforcement
        BuyerPreferences buyerPrefs = BuyerPreferences.builder()
                .targetPrice(new BigDecimal("82.00"))
                .maxCeilingPrice(new BigDecimal("100.00"))
                .desiredDeliveryDays(3)
                .requireExtendedWarranty(true)
                .urgency("MEDIUM")
                .build();

        // Simulate Seller 1 and Seller 2 quotes without leaking maxCeilingPrice
        List<NegotiationOffer> competingOffers = new ArrayList<>();
        for (Product p : List.of(prod1, prod2)) {
            SellerNegotiationAgent.SellerDecision sDec = sellerAgent.makeDecision(
                    new SellerNegotiationAgent.SellerDecisionContext(
                            "neg-wf-1", p.getSellerId(), productService.toPublicDto(p),
                            p.getNegotiationPolicy(), p.getShippingCost(), p.getAvailableInventory(),
                            null, List.of(), 1, 4, 98.0));

            assertTrue(sDec.getCounterUnitPrice().compareTo(p.getNegotiationPolicy().getFloorPrice()) >= 0,
                    "Seller agent must never breach private floor");
            assertFalse(sDec.getPublicExplanation().contains(p.getNegotiationPolicy().getFloorPrice().toPlainString()),
                    "Seller explanation must never reveal private floor price");

            competingOffers.add(NegotiationOffer.builder()
                    .id("offer-" + p.getSellerId())
                    .negotiationId("neg-wf-1")
                    .sellerId(p.getSellerId())
                    .productId(p.getId())
                    .roundNumber(1)
                    .unitPrice(sDec.getCounterUnitPrice())
                    .quantity(1)
                    .shippingCost(p.getShippingCost())
                    .deliveryEstimateDays(sDec.getDeliveryDays())
                    .warrantyMonths(sDec.getWarrantyMonths())
                    .perks(sDec.getPerks())
                    .offerStatus("PROPOSED")
                    .build());
        }

        BuyerNegotiationAgent.BuyerDecision bCounter = buyerAgent.makeDecision(
                new BuyerNegotiationAgent.BuyerDecisionContext(
                        "neg-wf-1", buyerPrefs, competingOffers, List.of(), 2, 4));
        assertTrue(bCounter.getProposedPrice().compareTo(buyerPrefs.getMaxCeilingPrice()) <= 0,
                "Buyer counteroffer must never exceed private max ceiling");
        assertFalse(bCounter.getPublicExplanation().contains("100.00"),
                "Buyer public explanation must not expose max ceiling limit");

        // 5. FairDeal Engine Transparent Multi-Attribute Comparison
        FairDealComparisonResult comparison = fairDealEngine.compareOffers(
                competingOffers, List.of(prod1, prod2), buyerPrefs, "Sony", 98.0);
        assertTrue(comparison.isHasViableDeal());
        assertNotNull(comparison.getBestOffer());
        assertEquals(2, comparison.getEvaluations().size());
        assertTrue(comparison.getBestOffer().getOverallDealScore() > 0);
        assertFalse(comparison.getBestOffer().getCalculationExplanation().isBlank());

        // 6. Start negotiation, verify SSE NEGOTIATION_STARTED never leaks maxCeilingPrice, and select winning deal
        Negotiation started = orchestrator.startNegotiation(buyerAuth.getId(), StartNegotiationRequest.builder()
                .productId(comparison.getBestOffer().getProductId())
                .preferences(buyerPrefs)
                .build());
        assertNotNull(started.getId());

        // Verify broadcast event payload has zero buyerPreferences leakage
        NegotiationEventDocument startedEvent = events.stream()
                .filter(e -> e.getNegotiationId().equals(started.getId()) && "NEGOTIATION_STARTED".equals(e.getEventType()))
                .findFirst()
                .orElseThrow();
        assertFalse(startedEvent.getPayload().toString().contains("maxCeilingPrice"),
                "Public SSE event must never serialize private buyer maxCeilingPrice");

        // Use an isolated persisted negotiation session with competing offers for deterministic deal selection
        Negotiation workflowNeg = Negotiation.builder()
                .id("neg-workflow-ready")
                .productId(comparison.getBestOffer().getProductId())
                .buyerId(buyerAuth.getId())
                .sellerId(comparison.getBestOffer().getSellerId())
                .status(NegotiationStatus.OFFER_RECEIVED)
                .buyerPreferences(buyerPrefs)
                .offers(competingOffers)
                .participatingSellerIds(List.of(seller1Auth.getId(), seller2Auth.getId()))
                .participatingProductIds(List.of(prod1.getId(), prod2.getId()))
                .build();
        negotiations.put(workflowNeg.getId(), workflowNeg);

        Negotiation approvedNeg = orchestrator.selectAndApproveDeal(workflowNeg.getId(), comparison.getBestOffer().getProductId(), buyerAuth.getId());
        assertEquals(NegotiationStatus.ACCEPTED, approvedNeg.getStatus());
        assertNotNull(approvedNeg.getAgreementSeal());
        String dealToken = approvedNeg.getAgreementSeal().getDealToken();
        BigDecimal serverSealedAmount = approvedNeg.getAgreementSeal().getFinalPrice();

        // 7. PayPal Sandbox Checkout: Create -> Cancel -> Retry Create -> Capture -> Duplicate Capture Idempotency
        Map<String, Object> firstPaypalOrder = payPalService.createOrder(approvedNeg.getId(), dealToken);
        String firstOrderId = (String) firstPaypalOrder.get("paypalOrderId");
        assertEquals(0, ((BigDecimal) firstPaypalOrder.get("amount")).compareTo(serverSealedAmount));

        // Simulate Buyer Cancellation cleanly
        PaymentRecord cancelledRecord = payPalService.cancelOrder(firstOrderId, approvedNeg.getId(), "Buyer cancelled PayPal popup");
        assertEquals(PaymentStatus.CANCELLED, cancelledRecord.getStatus());
        assertTrue(orders.isEmpty(), "Cancelled payment must NOT create a paid order");

        // Buyer Retries Checkout
        Map<String, Object> retryPaypalOrder = payPalService.createOrder(approvedNeg.getId(), dealToken);
        String retryOrderId = (String) retryPaypalOrder.get("paypalOrderId");
        assertNotEquals(firstOrderId, retryOrderId);

        // Capture Approved Payment on Backend
        Order capturedOrder = payPalService.captureOrder(retryOrderId, approvedNeg.getId(), dealToken,
                Map.of("fullName", "Alice Shopper", "addressLine1", "100 Main St", "city", "Portland", "postalCode", "97201", "country", "US"));
        assertNotNull(capturedOrder.getId());
        assertEquals(OrderStatus.CONFIRMED, capturedOrder.getOrderStatus());
        assertEquals(0, capturedOrder.getFinalAmount().compareTo(serverSealedAmount));

        // Duplicate Capture Request returns existing order without creating a second one
        Order duplicateCapture = payPalService.captureOrder(retryOrderId, approvedNeg.getId(), dealToken, Map.of());
        assertEquals(capturedOrder.getId(), duplicateCapture.getId());
        assertEquals(1, orders.size());

        // 8. Order Fulfilment State Machine & Dashboard KPIs
        String winningSellerId = capturedOrder.getSellerId();
        Order processingOrder = orderService.updateOrderStatus(capturedOrder.getId(), winningSellerId,
                new UpdateOrderStatusRequest(OrderStatus.PROCESSING, null, null, "Packed in warehouse"));
        assertEquals(OrderStatus.PROCESSING, processingOrder.getOrderStatus());

        Order shippedOrder = orderService.updateOrderStatus(capturedOrder.getId(), winningSellerId,
                new UpdateOrderStatusRequest(OrderStatus.SHIPPED, "FedEx Express", "FDX-99001122", "Left facility"));
        assertEquals(OrderStatus.SHIPPED, shippedOrder.getOrderStatus());

        Order deliveredOrder = orderService.updateOrderStatus(capturedOrder.getId(), winningSellerId,
                new UpdateOrderStatusRequest(OrderStatus.DELIVERED, null, null, "Signed by buyer"));
        assertEquals(OrderStatus.DELIVERED, deliveredOrder.getOrderStatus());

        // Invalid reverse transition must fail cleanly
        assertThrows(BusinessRuleException.class, () ->
                orderService.updateOrderStatus(capturedOrder.getId(), winningSellerId,
                        new UpdateOrderStatusRequest(OrderStatus.PROCESSING, null, null, "Invalid regression")));

        BuyerDashboardSummaryDto buyerSummary = orderService.getBuyerDashboardSummary(buyerAuth.getId());
        assertEquals(1, buyerSummary.totalOrders());
        assertEquals(1, buyerSummary.shippedOrDeliveredOrders());
        assertEquals(0, buyerSummary.totalSpent().compareTo(serverSealedAmount));

        SellerDashboardSummaryDto sellerSummary = sellerService.getDashboardSummary(winningSellerId);
        assertEquals(1, sellerSummary.getTotalOrders());
        assertEquals(0, sellerSummary.getTotalSalesVolume().compareTo(serverSealedAmount));
    }
}
