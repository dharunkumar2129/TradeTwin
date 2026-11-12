package com.tradetwin.payment.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tradetwin.catalog.model.Product;
import com.tradetwin.catalog.service.ProductService;
import com.tradetwin.common.exception.BusinessRuleException;
import com.tradetwin.deal.model.AgreementSeal;
import com.tradetwin.negotiation.model.BuyerPreferences;
import com.tradetwin.negotiation.model.Negotiation;
import com.tradetwin.negotiation.model.NegotiationStatus;
import com.tradetwin.negotiation.repository.NegotiationRepository;
import com.tradetwin.order.model.Order;
import com.tradetwin.order.model.OrderStatus;
import com.tradetwin.order.repository.OrderRepository;
import com.tradetwin.order.service.OrderService;
import com.tradetwin.payment.model.PaymentRecord;
import com.tradetwin.payment.model.PaymentStatus;
import com.tradetwin.payment.repository.PaymentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.client.MockRestServiceServer;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.Optional;

import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

class PayPalDiagnosticsAndValidationTest {

    private NegotiationRepository negotiationRepository;
    private OrderRepository orderRepository;
    private PaymentRepository paymentRepository;
    private StubProductService stubProductService;
    private StubOrderService stubOrderService;
    private PayPalService payPalService;
    private MockRestServiceServer mockServer;

    static class StubOrderService extends OrderService {
        Order orderToReturn;
        int callCount = 0;

        StubOrderService() {
            super(null, null, null, null, null);
        }

        @Override
        public Order createOrderFromAgreement(String negotiationId, String dealToken, PaymentRecord paymentRecord, Map<String, String> shippingDetails) {
            callCount++;
            return orderToReturn;
        }
    }

    static class StubProductService extends ProductService {
        Product productToReturn;
        int reserveCount = 0;
        int releaseCount = 0;

        StubProductService() {
            super(null);
        }

        @Override
        public Product getProductById(String id) {
            return productToReturn;
        }

        @Override
        public synchronized void reserveStock(String productId, int quantity) {
            reserveCount++;
            productToReturn.setReservedCount(productToReturn.getReservedCount() + quantity);
        }

        @Override
        public synchronized void releaseStock(String productId, int quantity) {
            releaseCount++;
            productToReturn.setReservedCount(Math.max(0, productToReturn.getReservedCount() - quantity));
        }
    }

    @BeforeEach
    void setUp() {
        negotiationRepository = Mockito.mock(NegotiationRepository.class);
        orderRepository = Mockito.mock(OrderRepository.class);
        paymentRepository = Mockito.mock(PaymentRepository.class);
        stubProductService = new StubProductService();
        stubOrderService = new StubOrderService();

        payPalService = new PayPalService(
                negotiationRepository,
                stubOrderService,
                orderRepository,
                stubProductService,
                paymentRepository,
                new ObjectMapper()
        );
        ReflectionTestUtils.setField(payPalService, "clientId", "live-test-client-id");
        ReflectionTestUtils.setField(payPalService, "clientSecret", "live-test-client-secret");
        ReflectionTestUtils.setField(payPalService, "baseUrl", "https://api-m.sandbox.paypal.com");
        ReflectionTestUtils.setField(payPalService, "currency", "USD");

        mockServer = MockRestServiceServer.createServer(payPalService.getRestTemplate());
        when(paymentRepository.save(any(PaymentRecord.class))).thenAnswer(inv -> inv.getArgument(0));
        when(negotiationRepository.save(any(Negotiation.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    private Negotiation buildActiveNegotiation(BigDecimal price, BigDecimal shipping, int qty) {
        AgreementSeal seal = AgreementSeal.builder()
                .agreementId("AGR-TEST0001")
                .dealToken("deal_sec_valid1234567890")
                .sealedAt(Instant.now())
                .expiresAt(Instant.now().plus(10, ChronoUnit.MINUTES))
                .unitPrice(price.subtract(shipping))
                .shippingCost(shipping)
                .quantity(qty)
                .finalPrice(price)
                .currency("USD")
                .status("AWAITING_PAYMENT")
                .stockReserved(true)
                .build();

        return Negotiation.builder()
                .id("neg-pp-1")
                .productId("prod-sony-1")
                .buyerId("buyer-1")
                .sellerId("seller-1")
                .status(NegotiationStatus.ACCEPTED)
                .buyerPreferences(BuyerPreferences.builder()
                        .targetPrice(new BigDecimal("85.00"))
                        .maxCeilingPrice(new BigDecimal("105.00"))
                        .build())
                .agreementSeal(seal)
                .build();
    }

    private Product buildProduct(int inventory, int reserved) {
        return Product.builder()
                .id("prod-sony-1")
                .sellerId("seller-1")
                .title("Sony WH-CH720N Wireless Noise Canceling Headphones With Extra Long Catalog Description Over 127 Characters To Verify Truncation Safety")
                .currency("USD")
                .inventoryCount(inventory)
                .reservedCount(reserved)
                .active(true)
                .build();
    }

    @Test
    @DisplayName("Live createOrder 422 must surface sanitized PayPal error details, never return simulated fake order, and release stock")
    void createOrder_livePayPal422_surfacesDiagnosticsAndReleasesStock() {
        Negotiation neg = buildActiveNegotiation(new BigDecimal("96.000"), new BigDecimal("5.00"), 1);
        Product prod = buildProduct(10, 1);
        stubProductService.productToReturn = prod;
        when(negotiationRepository.findById("neg-pp-1")).thenReturn(Optional.of(neg));

        mockServer.expect(requestTo("https://api-m.sandbox.paypal.com/v1/oauth2/token"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess("{\"access_token\":\"tok_test_123\",\"expires_in\":3600}", MediaType.APPLICATION_JSON));

        String paypal422 = """
                {
                  "name": "UNPROCESSABLE_ENTITY",
                  "message": "The requested action could not be performed, semantically incorrect, or failed business validation.",
                  "debug_id": "pp-dbg-create-422",
                  "details": [
                    {
                      "field": "/purchase_units/0/amount/value",
                      "issue": "DECIMAL_PRECISION",
                      "description": "Value exceeds supported precision."
                    }
                  ]
                }
                """;
        mockServer.expect(requestTo("https://api-m.sandbox.paypal.com/v2/checkout/orders"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withStatus(HttpStatus.UNPROCESSABLE_ENTITY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(paypal422));

        BusinessRuleException ex = assertThrows(BusinessRuleException.class,
                () -> payPalService.createOrder("neg-pp-1", "deal_sec_valid1234567890"));

        assertTrue(ex.getMessage().contains("422"));
        assertTrue(ex.getMessage().contains("DECIMAL_PRECISION"));
        assertTrue(ex.getDetails().stream().anyMatch(d -> d.contains("pp-dbg-create-422")));
        assertEquals(1, stubProductService.releaseCount, "Reserved stock must be released when createOrder fails");
        assertFalse(neg.getAgreementSeal().isStockReserved());

        ArgumentCaptor<PaymentRecord> savedCap = ArgumentCaptor.forClass(PaymentRecord.class);
        verify(paymentRepository).save(savedCap.capture());
        assertEquals(PaymentStatus.FAILED, savedCap.getValue().getStatus());
        assertEquals("CREATE_ORDER", savedCap.getValue().getFailureStage());
        assertEquals("DECIMAL_PRECISION", savedCap.getValue().getPaypalIssue());
        assertEquals("pp-dbg-create-422", savedCap.getValue().getDebugId());
        mockServer.verify();
    }

    @Test
    @DisplayName("Live createOrder formats 2-decimal breakdown and truncates description to <= 127 chars")
    void createOrder_validatesBreakdownAndDescriptionLength() {
        Negotiation neg = buildActiveNegotiation(new BigDecimal("96.500"), new BigDecimal("5.00"), 1);
        neg.getAgreementSeal().setStockReserved(false); // Verify re-reserve on retry!
        Product prod = buildProduct(10, 0);
        stubProductService.productToReturn = prod;
        when(negotiationRepository.findById("neg-pp-1")).thenReturn(Optional.of(neg));

        mockServer.expect(requestTo("https://api-m.sandbox.paypal.com/v1/oauth2/token"))
                .andRespond(withSuccess("{\"access_token\":\"tok_test_123\",\"expires_in\":3600}", MediaType.APPLICATION_JSON));

        mockServer.expect(requestTo("https://api-m.sandbox.paypal.com/v2/checkout/orders"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(jsonPath("$.purchase_units[0].amount.value").value("96.50"))
                .andExpect(jsonPath("$.purchase_units[0].amount.breakdown.item_total.value").value("91.50"))
                .andExpect(jsonPath("$.purchase_units[0].amount.breakdown.shipping.value").value("5.00"))
                .andExpect(jsonPath("$.purchase_units[0].description", new org.hamcrest.TypeSafeMatcher<String>() {
                    @Override
                    protected boolean matchesSafely(String s) {
                        return s != null && !s.isEmpty() && s.length() <= 127;
                    }

                    @Override
                    public void describeTo(org.hamcrest.Description description) {
                        description.appendText("non-empty description with length <= 127");
                    }
                }))
                .andRespond(withSuccess("{\"id\":\"5O190127TN364715T\",\"status\":\"CREATED\"}", MediaType.APPLICATION_JSON));

        Map<String, Object> result = payPalService.createOrder("neg-pp-1", "deal_sec_valid1234567890");
        assertEquals("5O190127TN364715T", result.get("paypalOrderId"));
        assertFalse((Boolean) result.get("simulated"));
        assertEquals(1, stubProductService.reserveCount, "Must re-reserve stock when retrying on active agreement");
        assertTrue(neg.getAgreementSeal().isStockReserved());
        mockServer.verify();
    }

    @Test
    @DisplayName("Live captureOrder detects unapproved CREATED PayPal order BEFORE calling /capture and returns diagnostic 422")
    void captureOrder_unapprovedOrder_stopsBeforeBlindCaptureAndReleasesStock() {
        Negotiation neg = buildActiveNegotiation(new BigDecimal("94.00"), BigDecimal.ZERO, 1);
        stubProductService.productToReturn = buildProduct(5, 1);
        when(negotiationRepository.findById("neg-pp-1")).thenReturn(Optional.of(neg));

        PaymentRecord pending = PaymentRecord.builder()
                .negotiationId("neg-pp-1")
                .paypalOrderId("PAYID-UNAPPROVED-1")
                .status(PaymentStatus.PENDING)
                .build();
        when(paymentRepository.findByPaypalOrderId("PAYID-UNAPPROVED-1")).thenReturn(Optional.of(pending));

        mockServer.expect(requestTo("https://api-m.sandbox.paypal.com/v1/oauth2/token"))
                .andRespond(withSuccess("{\"access_token\":\"tok_test_123\",\"expires_in\":3600}", MediaType.APPLICATION_JSON));

        mockServer.expect(requestTo("https://api-m.sandbox.paypal.com/v2/checkout/orders/PAYID-UNAPPROVED-1"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("{\"id\":\"PAYID-UNAPPROVED-1\",\"status\":\"CREATED\"}", MediaType.APPLICATION_JSON));

        BusinessRuleException ex = assertThrows(BusinessRuleException.class, () ->
                payPalService.captureOrder("PAYID-UNAPPROVED-1", "neg-pp-1", "deal_sec_valid1234567890", Map.of()));

        assertTrue(ex.getMessage().contains("ORDER_NOT_APPROVED"));
        assertEquals(1, stubProductService.releaseCount);
        assertEquals(0, stubOrderService.callCount);
        mockServer.verify();
    }

    @Test
    @DisplayName("Live captureOrder verifies status APPROVED -> captures with PayPal-Request-Id -> checks amount & currency -> creates order")
    void captureOrder_approvedOrder_capturesVerifiesAmountAndCompletes() {
        Negotiation neg = buildActiveNegotiation(new BigDecimal("94.00"), BigDecimal.ZERO, 1);
        stubProductService.productToReturn = buildProduct(5, 1);
        when(negotiationRepository.findById("neg-pp-1")).thenReturn(Optional.of(neg));

        Order expectedOrder = Order.builder().id("ord-1").orderNumber("TT-999").orderStatus(OrderStatus.CONFIRMED).build();
        stubOrderService.orderToReturn = expectedOrder;

        mockServer.expect(requestTo("https://api-m.sandbox.paypal.com/v1/oauth2/token"))
                .andRespond(withSuccess("{\"access_token\":\"tok_test_123\",\"expires_in\":3600}", MediaType.APPLICATION_JSON));

        mockServer.expect(requestTo("https://api-m.sandbox.paypal.com/v2/checkout/orders/PAYID-OK-1"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("{\"id\":\"PAYID-OK-1\",\"status\":\"APPROVED\"}", MediaType.APPLICATION_JSON));

        String captureResponse = """
                {
                  "id": "PAYID-OK-1",
                  "status": "COMPLETED",
                  "payer": { "payer_id": "BUYER123", "email_address": "sb-buyer@example.com" },
                  "purchase_units": [{
                    "payments": {
                      "captures": [{
                        "id": "CAP-REAL-999",
                        "status": "COMPLETED",
                        "amount": { "currency_code": "USD", "value": "94.00" }
                      }]
                    }
                  }]
                }
                """;
        mockServer.expect(requestTo("https://api-m.sandbox.paypal.com/v2/checkout/orders/PAYID-OK-1/capture"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("PayPal-Request-Id", containsString("TT-CAP-PAYID-OK-1")))
                .andRespond(withSuccess(captureResponse, MediaType.APPLICATION_JSON));

        Order created = payPalService.captureOrder("PAYID-OK-1", "neg-pp-1", "deal_sec_valid1234567890", Map.of("city", "Austin"));
        assertNotNull(created);
        assertEquals("ord-1", created.getId());
        assertEquals(1, stubOrderService.callCount);

        ArgumentCaptor<PaymentRecord> cap = ArgumentCaptor.forClass(PaymentRecord.class);
        verify(paymentRepository).save(cap.capture());
        assertEquals(PaymentStatus.COMPLETED, cap.getValue().getStatus());
        assertEquals("CAP-REAL-999", cap.getValue().getCaptureId());
        mockServer.verify();
    }

    @Test
    @DisplayName("Live captureOrder handles 422 INSTRUMENT_DECLINED cleanly without faking order completion")
    void captureOrder_live422InstrumentDeclined_recordsDiagnosticsWithoutFakingSuccess() {
        Negotiation neg = buildActiveNegotiation(new BigDecimal("94.00"), BigDecimal.ZERO, 1);
        stubProductService.productToReturn = buildProduct(5, 1);
        when(negotiationRepository.findById("neg-pp-1")).thenReturn(Optional.of(neg));

        mockServer.expect(requestTo("https://api-m.sandbox.paypal.com/v1/oauth2/token"))
                .andRespond(withSuccess("{\"access_token\":\"tok_test_123\",\"expires_in\":3600}", MediaType.APPLICATION_JSON));

        mockServer.expect(requestTo("https://api-m.sandbox.paypal.com/v2/checkout/orders/PAYID-DECLINE-1"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("{\"id\":\"PAYID-DECLINE-1\",\"status\":\"APPROVED\"}", MediaType.APPLICATION_JSON));

        String declined422 = """
                {
                  "name": "UNPROCESSABLE_ENTITY",
                  "message": "The requested action could not be performed.",
                  "debug_id": "pp-dbg-declined-1",
                  "details": [{
                    "issue": "INSTRUMENT_DECLINED",
                    "description": "The instrument presented was either declined by the processor or bank."
                  }]
                }
                """;
        mockServer.expect(requestTo("https://api-m.sandbox.paypal.com/v2/checkout/orders/PAYID-DECLINE-1/capture"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withStatus(HttpStatus.UNPROCESSABLE_ENTITY).contentType(MediaType.APPLICATION_JSON).body(declined422));

        BusinessRuleException ex = assertThrows(BusinessRuleException.class, () ->
                payPalService.captureOrder("PAYID-DECLINE-1", "neg-pp-1", "deal_sec_valid1234567890", Map.of()));

        assertTrue(ex.getMessage().contains("INSTRUMENT_DECLINED"));
        assertTrue(ex.getDetails().stream().anyMatch(d -> d.contains("pp-dbg-declined-1")));
        assertEquals(0, stubOrderService.callCount);
        assertEquals(1, stubProductService.releaseCount);
        mockServer.verify();
    }
}
