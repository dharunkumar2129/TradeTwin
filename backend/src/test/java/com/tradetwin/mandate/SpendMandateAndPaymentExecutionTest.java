package com.tradetwin.mandate;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tradetwin.catalog.model.Product;
import com.tradetwin.catalog.service.ProductService;
import com.tradetwin.common.exception.BusinessRuleException;
import com.tradetwin.deal.model.AgreementSeal;
import com.tradetwin.mandate.dto.SpendMandateDto;
import com.tradetwin.mandate.dto.SpendMandateRequest;
import com.tradetwin.mandate.model.MandateStatus;
import com.tradetwin.mandate.model.MandateValidationResult;
import com.tradetwin.mandate.model.PaymentApprovalPolicy;
import com.tradetwin.mandate.model.SpendMandate;
import com.tradetwin.mandate.repository.SpendMandateRepository;
import com.tradetwin.mandate.service.DailySpendLookup;
import com.tradetwin.mandate.service.SpendMandateService;
import com.tradetwin.negotiation.model.BuyerPreferences;
import com.tradetwin.negotiation.model.Negotiation;
import com.tradetwin.negotiation.model.NegotiationStatus;
import com.tradetwin.negotiation.repository.NegotiationRepository;
import com.tradetwin.order.model.Order;
import com.tradetwin.order.model.OrderStatus;
import com.tradetwin.order.repository.OrderRepository;
import com.tradetwin.order.service.OrderService;
import com.tradetwin.payment.model.PaymentAuditEntry;
import com.tradetwin.payment.model.PaymentRecord;
import com.tradetwin.payment.model.PaymentStatus;
import com.tradetwin.payment.repository.PaymentAuditEntryRepository;
import com.tradetwin.payment.repository.PaymentRepository;
import com.tradetwin.payment.service.DelegatedPaymentAdapter;
import com.tradetwin.payment.service.PayPalService;
import com.tradetwin.payment.service.PayPalVaultDelegatedPaymentAdapter;
import com.tradetwin.payment.service.PaymentAuditService;
import com.tradetwin.payment.service.PaymentExecutionEngine;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;

import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

class SpendMandateAndPaymentExecutionTest {

    private SpendMandateRepository mandateRepository;
    private PaymentAuditEntryRepository auditRepository;
    private NegotiationRepository negotiationRepository;
    private OrderRepository orderRepository;
    private PaymentRepository paymentRepository;
    private StubProductService stubProductService;
    private StubOrderService stubOrderService;

    private BigDecimal simulatedSpentToday = BigDecimal.ZERO;
    private SpendMandateService mandateService;
    private PaymentAuditService auditService;
    private PayPalService payPalService;
    private PayPalVaultDelegatedPaymentAdapter delegatedAdapter;
    private PaymentExecutionEngine executionEngine;
    private ObjectMapper objectMapper;

    static class StubOrderService extends OrderService {
        Order orderToReturn;
        int createCalls = 0;

        StubOrderService() {
            super(null, null, null, null, null);
        }

        @Override
        public Order createOrderFromAgreement(String negotiationId,
                                              String dealToken,
                                              PaymentRecord paymentRecord,
                                              Map<String, String> shippingDetails) {
            createCalls++;
            return orderToReturn;
        }
    }

    static class StubProductService extends ProductService {
        Product productToReturn;
        int reserveCalls = 0;
        int releaseCalls = 0;

        StubProductService() {
            super(null);
        }

        @Override
        public Product getProductById(String id) {
            return productToReturn;
        }

        @Override
        public synchronized void reserveStock(String productId, int quantity) {
            reserveCalls++;
            if (productToReturn != null) {
                productToReturn.setReservedCount(productToReturn.getReservedCount() + quantity);
            }
        }

        @Override
        public synchronized void releaseStock(String productId, int quantity) {
            releaseCalls++;
            if (productToReturn != null) {
                productToReturn.setReservedCount(Math.max(0, productToReturn.getReservedCount() - quantity));
            }
        }
    }

    @BeforeEach
    void setUp() {
        mandateRepository = Mockito.mock(SpendMandateRepository.class);
        auditRepository = Mockito.mock(PaymentAuditEntryRepository.class);
        negotiationRepository = Mockito.mock(NegotiationRepository.class);
        orderRepository = Mockito.mock(OrderRepository.class);
        paymentRepository = Mockito.mock(PaymentRepository.class);
        stubProductService = new StubProductService();
        stubOrderService = new StubOrderService();
        objectMapper = new ObjectMapper();
        simulatedSpentToday = BigDecimal.ZERO;

        DailySpendLookup dailyLookup = (buyerId, currency) -> simulatedSpentToday;
        mandateService = new SpendMandateService(mandateRepository, dailyLookup);
        auditService = new PaymentAuditService(auditRepository);

        payPalService = new PayPalService(
                negotiationRepository,
                stubOrderService,
                orderRepository,
                stubProductService,
                paymentRepository,
                objectMapper,
                new RestTemplate()
        );
        payPalService.setSpendMandateService(mandateService);
        payPalService.setPaymentAuditService(auditService);

        delegatedAdapter = new PayPalVaultDelegatedPaymentAdapter(payPalService, objectMapper, payPalService.getRestTemplate());
        executionEngine = new PaymentExecutionEngine(
                mandateService,
                payPalService,
                delegatedAdapter,
                auditService,
                negotiationRepository,
                stubProductService,
                paymentRepository,
                stubOrderService,
                orderRepository
        );

        when(mandateRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(auditRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(paymentRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(negotiationRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
    }

    private Product sampleProduct() {
        return Product.builder()
                .id("prod-1")
                .sellerId("seller-1")
                .title("Sony WH-1000XM5 Wireless Headphones")
                .brand("Sony")
                .category("Audio")
                .active(true)
                .inventoryCount(5)
                .reservedCount(0)
                .listPrice(new BigDecimal("350.00"))
                .build();
    }

    private Negotiation sampleNegotiation(String id, BigDecimal finalPrice, int qty) {
        AgreementSeal seal = AgreementSeal.builder()
                .agreementId("AGR-TEST-1")
                .dealToken("tok-1")
                .finalPrice(finalPrice)
                .unitPrice(finalPrice.divide(BigDecimal.valueOf(qty)))
                .shippingCost(BigDecimal.ZERO)
                .quantity(qty)
                .currency("USD")
                .status("AWAITING_PAYMENT")
                .stockReserved(true)
                .expiresAt(Instant.now().plus(15, ChronoUnit.MINUTES))
                .build();

        Negotiation neg = Negotiation.builder()
                .id(id)
                .buyerId("buyer-1")
                .sellerId("seller-1")
                .productId("prod-1")
                .status(NegotiationStatus.ACCEPTED)
                .buyerPreferences(BuyerPreferences.builder().maxCeilingPrice(new BigDecimal("400.00")).build())
                .agreementSeal(seal)
                .build();
        neg.setProductTitle("Sony WH-1000XM5 Wireless Headphones");
        neg.setCurrentSellerStoreName("SoundVault Official");
        return neg;
    }

    private SpendMandate activeMandate(PaymentApprovalPolicy policy) {
        SpendMandate m = new SpendMandate();
        m.setId("man-1");
        m.setBuyerId("buyer-1");
        m.setName("Electronics Buyer Cap");
        m.setMaxAmountPerTransaction(new BigDecimal("300.00"));
        m.setDailySpendingLimit(new BigDecimal("500.00"));
        m.setCurrency("USD");
        m.setAllowedCategories(List.of("Audio", "Electronics"));
        m.setPreferredBrands(List.of("Sony", "Bose"));
        m.setMaxQuantity(2);
        m.setApprovedSellerIds(List.of("seller-1", "seller-2"));
        m.setExpiresAt(Instant.now().plus(7, ChronoUnit.DAYS));
        m.setPaymentApprovalPolicy(policy);
        m.setStatus(MandateStatus.ACTIVE);
        return m;
    }

    @Test
    @DisplayName("Mandate security: rejects ordinary PayPal OAuth access token as delegated credential")
    void testMandateRejectsOrdinaryOAuthToken() {
        SpendMandateRequest req = new SpendMandateRequest(
                "Bad Delegated Attempt",
                new BigDecimal("250.00"),
                new BigDecimal("500.00"),
                "USD",
                List.of("Audio"),
                List.of("Sony"),
                2,
                List.of("seller-1"),
                Instant.now().plus(2, ChronoUnit.DAYS),
                PaymentApprovalPolicy.DELEGATED_AUTONOMOUS_PAYMENT,
                "A21AAI12345OAuthAccessToken",
                "seller-1",
                "USD"
        );

        BusinessRuleException ex = assertThrows(BusinessRuleException.class,
                () -> mandateService.upsertMandate("buyer-1", null, req));
        assertTrue(ex.getMessage().contains("ordinary PayPal OAuth access tokens"));
    }

    @Test
    @DisplayName("Mandate enforcement: transaction limit & daily limit block createOrder + record audit + release stock")
    void testTransactionAndDailyLimitsBlockExecution() {
        Product prod = sampleProduct();
        prod.setReservedCount(1);
        stubProductService.productToReturn = prod;

        SpendMandate mandate = activeMandate(PaymentApprovalPolicy.SUPERVISED_PAYPAL_CHECKOUT);
        mandate.setMaxAmountPerTransaction(new BigDecimal("250.00"));
        when(mandateRepository.findByBuyerIdOrderByUpdatedAtDesc("buyer-1"))
                .thenReturn(List.of(mandate));

        Negotiation negOverTx = sampleNegotiation("neg-over-tx", new BigDecimal("280.00"), 1);
        when(negotiationRepository.findById("neg-over-tx")).thenReturn(Optional.of(negOverTx));

        BusinessRuleException ex = assertThrows(BusinessRuleException.class,
                () -> payPalService.createOrder("neg-over-tx", "tok-1"));
        assertTrue(ex.getMessage().contains("Spend Mandate"));
        assertEquals(1, stubProductService.releaseCalls);
        verify(auditRepository, atLeastOnce()).save(argThat(a ->
                "BLOCKED_MANDATE_VIOLATION".equals(a.getFinalStatus()) && !a.isBudgetCompliant()
        ));

        // Now test cumulative daily spending limit breach: maxPerTx=300, daily=500, spentToday=320, tx=200 => 520 > 500
        mandate.setMaxAmountPerTransaction(new BigDecimal("300.00"));
        simulatedSpentToday = new BigDecimal("320.00");
        MandateValidationResult dailyCheck = mandateService.validateDealForBuyer(
                "buyer-1", prod, new BigDecimal("200.00"), 1, "USD"
        );
        assertFalse(dailyCheck.compliant());
        assertTrue(dailyCheck.violations().stream().anyMatch(v -> v.contains("exceeds daily spending limit")));
    }

    @Test
    @DisplayName("Mandate enforcement: expired mandate transitions to EXPIRED and rejects deal")
    void testExpiredMandateTransitionsAndBlocks() {
        Product prod = sampleProduct();
        SpendMandate expired = activeMandate(PaymentApprovalPolicy.SUPERVISED_PAYPAL_CHECKOUT);
        expired.setExpiresAt(Instant.now().minus(1, ChronoUnit.HOURS));

        when(mandateRepository.findByBuyerIdOrderByUpdatedAtDesc("buyer-1"))
                .thenReturn(List.of(expired));

        MandateValidationResult result = mandateService.validateDealForBuyer(
                "buyer-1", prod, new BigDecimal("199.00"), 1, "USD"
        );
        assertFalse(result.compliant());
        assertTrue(result.violations().stream().anyMatch(v -> v.contains("expired")));
        assertEquals(MandateStatus.EXPIRED, expired.getStatus());
        verify(mandateRepository).save(expired);
    }

    @Test
    @DisplayName("Mandate enforcement: enforces category, preferred brand, quantity cap, and approved sellers")
    void testCategoryBrandQuantityAndSellerRestrictions() {
        SpendMandate mandate = activeMandate(PaymentApprovalPolicy.SUPERVISED_PAYPAL_CHECKOUT);
        when(mandateRepository.findByBuyerIdOrderByUpdatedAtDesc("buyer-1"))
                .thenReturn(List.of(mandate));

        Product wrongAll = Product.builder()
                .id("prod-x")
                .sellerId("unapproved-seller")
                .brand("GenericBrand")
                .category("Furniture")
                .build();

        MandateValidationResult result = mandateService.validateDealForBuyer(
                "buyer-1", wrongAll, new BigDecimal("200.00"), 3, "USD"
        );
        assertFalse(result.compliant());
        assertEquals(4, result.violations().size());
        assertTrue(result.violations().stream().anyMatch(v -> v.contains("category")));
        assertTrue(result.violations().stream().anyMatch(v -> v.contains("brand")));
        assertTrue(result.violations().stream().anyMatch(v -> v.contains("Order quantity (3)")));
        assertTrue(result.violations().stream().anyMatch(v -> v.contains("not in the approved sellers list")));
    }

    @Test
    @DisplayName("Supervised PayPal Checkout: auto-prepares order on seal, handles cancel, duplicate capture, and verified capture")
    void testSupervisedCheckoutAutoPrepareCancelDuplicateAndSuccess() {
        Product prod = sampleProduct();
        stubProductService.productToReturn = prod;
        SpendMandate mandate = activeMandate(PaymentApprovalPolicy.SUPERVISED_PAYPAL_CHECKOUT);
        when(mandateRepository.findByBuyerIdOrderByUpdatedAtDesc("buyer-1"))
                .thenReturn(List.of(mandate));

        Negotiation neg = sampleNegotiation("neg-sup-1", new BigDecimal("250.00"), 1);
        when(negotiationRepository.findById("neg-sup-1")).thenReturn(Optional.of(neg));
        when(paymentRepository.findByNegotiationId("neg-sup-1")).thenReturn(Optional.empty());

        // 1. Auto-prepare PayPal order immediately after agent agreement
        executionEngine.onAgreementSealed(neg);

        String preparedOrderId = neg.getAgreementSeal().getPreparedPaypalOrderId();
        assertNotNull(preparedOrderId);
        assertEquals("SUPERVISED_PAYPAL_CHECKOUT", neg.getAgreementSeal().getPaymentExecutionMode());

        // Simulate saved pending payment record returned on subsequent createOrder lookup
        PaymentRecord pendingRec = PaymentRecord.builder()
                .id("pay-1")
                .negotiationId("neg-sup-1")
                .paypalOrderId(preparedOrderId)
                .amount(new BigDecimal("250.00"))
                .currency("USD")
                .status(PaymentStatus.PENDING)
                .build();
        when(paymentRepository.findByPaypalOrderId(preparedOrderId)).thenReturn(Optional.of(pendingRec));
        when(paymentRepository.findByNegotiationId("neg-sup-1")).thenReturn(Optional.of(pendingRec));

        // 2. Reuses prepared order without creating duplicates
        Map<String, Object> createResp = payPalService.createOrder("neg-sup-1", "tok-1");
        assertEquals(preparedOrderId, createResp.get("paypalOrderId"));
        assertEquals("PENDING", createResp.get("status"));
        verify(paymentRepository, times(1)).save(any());

        // 3. Cancellation releases reservation and writes CANCELLED audit
        PaymentRecord cancelledRec = payPalService.cancelOrder(preparedOrderId, "neg-sup-1", "Buyer cancelled checkout");
        assertEquals(PaymentStatus.CANCELLED, cancelledRec.getStatus());
        verify(auditRepository, atLeastOnce()).save(argThat(a -> "CANCELLED".equals(a.getFinalStatus())));

        // 4. Reset pending status and capture cleanly
        pendingRec.setStatus(PaymentStatus.PENDING);
        neg.getAgreementSeal().setStockReserved(true);
        Order createdOrder = Order.builder()
                .id("ord-1")
                .orderNumber("TT-1001")
                .orderStatus(OrderStatus.CONFIRMED)
                .finalAmount(new BigDecimal("250.00"))
                .build();
        stubOrderService.orderToReturn = createdOrder;

        Order captured = payPalService.captureOrder(preparedOrderId, "neg-sup-1", "tok-1", Map.of("fullName", "Buyer One"));
        assertEquals("TT-1001", captured.getOrderNumber());
        assertEquals(PaymentStatus.COMPLETED, pendingRec.getStatus());
        verify(auditRepository, atLeastOnce()).save(argThat(a ->
                "COMPLETED".equals(a.getFinalStatus()) &&
                        preparedOrderId.equals(a.getPaypalOrderId()) &&
                        a.getPaypalCaptureId() != null
        ));

        // 5. Duplicate capture idempotency: returns existing order without calling OrderService again
        pendingRec.setStatus(PaymentStatus.COMPLETED);
        when(orderRepository.findByNegotiationId("neg-sup-1")).thenReturn(Optional.of(createdOrder));
        Order dupResult = payPalService.captureOrder(preparedOrderId, "neg-sup-1", "tok-1", Map.of());
        assertEquals("TT-1001", dupResult.getOrderNumber());
        assertEquals(1, stubOrderService.createCalls);
    }

    @Test
    @DisplayName("Delegated Autonomous Payment: refuses simulation when Vault access unavailable")
    void testDelegatedPaymentClearlyReportsUnavailableAccess() {
        Product prod = sampleProduct();
        prod.setReservedCount(1);
        stubProductService.productToReturn = prod;

        SpendMandate mandate = activeMandate(PaymentApprovalPolicy.DELEGATED_AUTONOMOUS_PAYMENT);
        mandate.setDelegatedVaultTokenId("vault-tok-xyz-123");
        when(mandateRepository.findByBuyerIdOrderByUpdatedAtDesc("buyer-1"))
                .thenReturn(List.of(mandate));

        Negotiation neg = sampleNegotiation("neg-del-1", new BigDecimal("240.00"), 1);
        when(negotiationRepository.findById("neg-del-1")).thenReturn(Optional.of(neg));

        // delegatedVaultEnabled defaults to false (ordinary sandbox accounts lack Vault v3 access)
        delegatedAdapter.setDelegatedVaultEnabled(false);

        BusinessRuleException ex = assertThrows(BusinessRuleException.class,
                () -> executionEngine.executeDelegatedPayment("neg-del-1", "tok-1", "buyer-1", Map.of()));
        assertTrue(ex.getMessage().contains("Vault v3 / Reference Transactions"));
        assertEquals(1, stubProductService.releaseCalls);
        verify(auditRepository, atLeastOnce()).save(argThat(a ->
                "DELEGATED_ACCESS_UNAVAILABLE".equals(a.getFinalStatus()) &&
                        "DELEGATED_PAYPAL_VAULT_V3".equals(a.getAuthorizationMethod())
        ));
    }

    @Test
    @DisplayName("Delegated Autonomous Payment: executes PayPal Vault v3 order when authorized & enforces merchant restriction")
    void testDelegatedPaymentAuthorizedVaultExecutionAndMerchantRestriction() {
        Product prod = sampleProduct();
        stubProductService.productToReturn = prod;

        SpendMandate mandate = activeMandate(PaymentApprovalPolicy.DELEGATED_AUTONOMOUS_PAYMENT);
        mandate.setDelegatedVaultTokenId("vault_pm_valid_9988");
        mandate.setDelegatedMerchantId("other-merchant");
        mandate.setDelegatedMaxCurrency("USD");
        when(mandateRepository.findByBuyerIdOrderByUpdatedAtDesc("buyer-1"))
                .thenReturn(List.of(mandate));

        ReflectionTestUtils.setField(payPalService, "clientId", "test-sandbox-client");
        ReflectionTestUtils.setField(payPalService, "clientSecret", "test-sandbox-secret");
        ReflectionTestUtils.setField(payPalService, "baseUrl", "https://api-m.sandbox.paypal.com");
        delegatedAdapter.setDelegatedVaultEnabled(true);
        delegatedAdapter.setBaseUrl("https://api-m.sandbox.paypal.com");

        // 1. Merchant restriction blocks seller-1 when delegated token is scoped to other-merchant
        DelegatedPaymentAdapter.DelegatedCapabilityStatus capRestricted =
                delegatedAdapter.inspectCapability(mandate, new BigDecimal("240.00"), "USD", "seller-1");
        assertFalse(capRestricted.eligible());
        assertEquals("MERCHANT_RESTRICTION_VIOLATION", capRestricted.reasonCode());

        // 2. Allow seller-1 and verify real PayPal Vault v3 HTTP payload via MockRestServiceServer
        mandate.setDelegatedMerchantId("seller-1");
        Negotiation neg = sampleNegotiation("neg-del-ok", new BigDecimal("240.00"), 1);
        when(negotiationRepository.findById("neg-del-ok")).thenReturn(Optional.of(neg));

        Order settledOrder = Order.builder()
                .id("ord-del-1")
                .orderNumber("TT-DEL-2026")
                .orderStatus(OrderStatus.CONFIRMED)
                .finalAmount(new BigDecimal("240.00"))
                .build();
        stubOrderService.orderToReturn = settledOrder;

        MockRestServiceServer mockServer = MockRestServiceServer.bindTo(payPalService.getRestTemplate()).build();
        mockServer.expect(requestTo("https://api-m.sandbox.paypal.com/v1/oauth2/token"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess("{\"access_token\":\"A21AA_TOKEN\",\"expires_in\":3600}", MediaType.APPLICATION_JSON));

        mockServer.expect(requestTo("https://api-m.sandbox.paypal.com/v2/checkout/orders"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().string(containsString("\"vault_id\":\"vault_pm_valid_9988\"")))
                .andRespond(withStatus(HttpStatus.CREATED)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body("""
                              {
                                "id": "PP-VAULT-ORD-1",
                                "status": "COMPLETED",
                                "payer": { "payer_id": "PAYER-VAULT-1", "email_address": "buyer@example.com" },
                                "purchase_units": [
                                  {
                                    "payments": {
                                      "captures": [
                                        {
                                          "id": "PP-VAULT-CAP-1",
                                          "status": "COMPLETED",
                                          "amount": { "currency_code": "USD", "value": "240.00" }
                                        }
                                      ]
                                    }
                                  }
                                ]
                              }
                              """));

        Order result = executionEngine.executeDelegatedPayment("neg-del-ok", "tok-1", "buyer-1", Map.of("fullName", "Buyer One"));
        assertEquals("TT-DEL-2026", result.getOrderNumber());
        mockServer.verify();
        verify(auditRepository, atLeastOnce()).save(argThat(a ->
                "COMPLETED".equals(a.getFinalStatus()) &&
                        "DELEGATED_PAYPAL_VAULT_V3".equals(a.getAuthorizationMethod()) &&
                        "PP-VAULT-ORD-1".equals(a.getPaypalOrderId()) &&
                        "PP-VAULT-CAP-1".equals(a.getPaypalCaptureId())
        ));
    }
}
