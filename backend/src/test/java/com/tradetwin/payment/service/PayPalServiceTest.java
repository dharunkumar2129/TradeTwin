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

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class PayPalServiceTest {

    private NegotiationRepository negotiationRepository;
    private StubOrderService stubOrderService;
    private OrderRepository orderRepository;
    private StubProductService stubProductService;
    private PaymentRepository paymentRepository;
    private ObjectMapper objectMapper;
    private PayPalService payPalService;

    static class StubOrderService extends OrderService {
        Order orderToReturn;
        int callCount = 0;

        public StubOrderService() {
            super(null, null, null, null, null);
        }

        @Override
        public Order createOrderFromAgreement(
                String negotiationId,
                String dealToken,
                PaymentRecord paymentRecord,
                Map<String, String> shippingDetails) {
            callCount++;
            return orderToReturn;
        }
    }

    static class StubProductService extends ProductService {
        Product productToReturn;

        public StubProductService() {
            super(null);
        }

        @Override
        public Product getProductById(String id) {
            return productToReturn;
        }
    }

    @BeforeEach
    void setUp() {
        negotiationRepository = Mockito.mock(NegotiationRepository.class);
        stubOrderService = new StubOrderService();
        orderRepository = Mockito.mock(OrderRepository.class);
        stubProductService = new StubProductService();
        paymentRepository = Mockito.mock(PaymentRepository.class);
        objectMapper = new ObjectMapper();

        payPalService = new PayPalService(
                negotiationRepository,
                stubOrderService,
                orderRepository,
                stubProductService,
                paymentRepository,
                objectMapper
        );
    }

    @Test
    @DisplayName("Create Order: Strictly validates deal state, inventory, and server-side pricing")
    void testCreateOrder_Success() {
        AgreementSeal seal = AgreementSeal.builder()
                .dealToken("tok_valid_123")
                .finalPrice(new BigDecimal("120.00"))
                .currency("USD")
                .expiresAt(Instant.now().plus(15, ChronoUnit.MINUTES))
                .build();

        BuyerPreferences pref = BuyerPreferences.builder()
                .maxCeilingPrice(new BigDecimal("150.00"))
                .build();

        Negotiation neg = Negotiation.builder()
                .id("neg-1")
                .buyerId("b1")
                .sellerId("s1")
                .productId("p1")
                .status(NegotiationStatus.ACCEPTED)
                .buyerPreferences(pref)
                .agreementSeal(seal)
                .build();

        Product prod = Product.builder()
                .id("p1")
                .sellerId("s1")
                .title("Test Headphones")
                .active(true)
                .inventoryCount(10)
                .build();

        when(negotiationRepository.findById("neg-1")).thenReturn(Optional.of(neg));
        stubProductService.productToReturn = prod;

        Map<String, Object> result = payPalService.createOrder("neg-1", "tok_valid_123");

        assertNotNull(result);
        assertNotNull(result.get("paypalOrderId"));
        assertEquals(new BigDecimal("120.00"), result.get("amount"));
        assertEquals("USD", result.get("currency"));
        assertEquals("PENDING", result.get("status"));

        ArgumentCaptor<PaymentRecord> captor = ArgumentCaptor.forClass(PaymentRecord.class);
        verify(paymentRepository).save(captor.capture());
        PaymentRecord savedRecord = captor.getValue();
        assertEquals("neg-1", savedRecord.getNegotiationId());
        assertEquals(new BigDecimal("120.00"), savedRecord.getAmount());
        assertEquals(PaymentStatus.PENDING, savedRecord.getStatus());
    }

    @Test
    @DisplayName("Create Order: Fails if deal is not in ACCEPTED or AGREED state")
    void testCreateOrder_ThrowsWhenDealNotAccepted() {
        Negotiation neg = Negotiation.builder()
                .id("neg-1")
                .status(NegotiationStatus.NEGOTIATING)
                .build();

        when(negotiationRepository.findById("neg-1")).thenReturn(Optional.of(neg));

        assertThrows(BusinessRuleException.class, () ->
                payPalService.createOrder("neg-1", "tok_any")
        );
    }

    @Test
    @DisplayName("Create Order: Fails if 15-minute agreement seal lock has expired")
    void testCreateOrder_ThrowsWhenSealExpired() {
        AgreementSeal expiredSeal = AgreementSeal.builder()
                .dealToken("tok_123")
                .finalPrice(new BigDecimal("100.00"))
                .expiresAt(Instant.now().minus(5, ChronoUnit.MINUTES)) // Expired
                .build();

        Negotiation neg = Negotiation.builder()
                .id("neg-1")
                .status(NegotiationStatus.ACCEPTED)
                .agreementSeal(expiredSeal)
                .build();

        when(negotiationRepository.findById("neg-1")).thenReturn(Optional.of(neg));

        assertThrows(BusinessRuleException.class, () ->
                payPalService.createOrder("neg-1", "tok_123")
        );
    }

    @Test
    @DisplayName("Create Order: Fails if inventory is depleted")
    void testCreateOrder_ThrowsWhenInventoryDepleted() {
        AgreementSeal seal = AgreementSeal.builder()
                .dealToken("tok_123")
                .finalPrice(new BigDecimal("100.00"))
                .expiresAt(Instant.now().plus(15, ChronoUnit.MINUTES))
                .build();

        Negotiation neg = Negotiation.builder()
                .id("neg-1")
                .sellerId("s1")
                .productId("p1")
                .status(NegotiationStatus.ACCEPTED)
                .agreementSeal(seal)
                .build();

        Product outOfStockProd = Product.builder()
                .id("p1")
                .sellerId("s1")
                .active(true)
                .inventoryCount(0) // Out of stock
                .build();

        when(negotiationRepository.findById("neg-1")).thenReturn(Optional.of(neg));
        stubProductService.productToReturn = outOfStockProd;

        assertThrows(BusinessRuleException.class, () ->
                payPalService.createOrder("neg-1", "tok_123")
        );
    }

    @Test
    @DisplayName("Capture Order: Idempotency handles duplicate capture requests cleanly without double billing")
    void testCaptureOrder_IdempotencyDuplicateRequest() {
        PaymentRecord completedRecord = PaymentRecord.builder()
                .paypalOrderId("ORDER-DUP-1")
                .negotiationId("neg-dup")
                .status(PaymentStatus.COMPLETED)
                .build();

        Order existingOrder = Order.builder()
                .id("ord-1")
                .orderNumber("TT-ORDER-1")
                .negotiationId("neg-dup")
                .orderStatus(OrderStatus.CONFIRMED)
                .build();

        when(paymentRepository.findByPaypalOrderId("ORDER-DUP-1")).thenReturn(Optional.of(completedRecord));
        when(orderRepository.findByNegotiationId("neg-dup")).thenReturn(Optional.of(existingOrder));

        Order result = payPalService.captureOrder("ORDER-DUP-1", "neg-dup", "tok_123", Map.of());

        assertNotNull(result);
        assertEquals("TT-ORDER-1", result.getOrderNumber());
        assertEquals(0, stubOrderService.callCount, "Order service must not create duplicate order on re-capture");
    }

    @Test
    @DisplayName("Capture Order: Completes payment record, updates status, and completes order settlement")
    void testCaptureOrder_Success() {
        PaymentRecord pendingRecord = PaymentRecord.builder()
                .paypalOrderId("ORDER-123")
                .negotiationId("neg-1")
                .status(PaymentStatus.PENDING)
                .amount(new BigDecimal("95.00"))
                .build();

        AgreementSeal seal = AgreementSeal.builder()
                .dealToken("tok_123")
                .finalPrice(new BigDecimal("95.00"))
                .expiresAt(Instant.now().plus(10, ChronoUnit.MINUTES))
                .build();

        Negotiation neg = Negotiation.builder()
                .id("neg-1")
                .status(NegotiationStatus.ACCEPTED)
                .agreementSeal(seal)
                .build();

        Order expectedOrder = Order.builder()
                .id("ord-1")
                .orderNumber("TT-1001")
                .negotiationId("neg-1")
                .finalAmount(new BigDecimal("95.00"))
                .build();

        when(paymentRepository.findByPaypalOrderId("ORDER-123")).thenReturn(Optional.of(pendingRecord));
        when(negotiationRepository.findById("neg-1")).thenReturn(Optional.of(neg));
        stubOrderService.orderToReturn = expectedOrder;

        Order result = payPalService.captureOrder("ORDER-123", "neg-1", "tok_123", Map.of("fullName", "Alice"));

        assertNotNull(result);
        assertEquals("TT-1001", result.getOrderNumber());
        assertEquals(1, stubOrderService.callCount);

        ArgumentCaptor<PaymentRecord> captor = ArgumentCaptor.forClass(PaymentRecord.class);
        verify(paymentRepository).save(captor.capture());
        PaymentRecord savedRecord = captor.getValue();
        assertEquals(PaymentStatus.COMPLETED, savedRecord.getStatus());
        assertNotNull(savedRecord.getCaptureId());
        assertNotNull(savedRecord.getPaidAt());
    }

    @Test
    @DisplayName("Create Order: Enforces buyer budget boundary and rejects if deal seal exceeds ceiling")
    void testCreateOrder_ThrowsWhenAmountExceedsBuyerCeiling() {
        AgreementSeal seal = AgreementSeal.builder()
                .dealToken("tok_over_budget")
                .finalPrice(new BigDecimal("175.00"))
                .currency("USD")
                .expiresAt(Instant.now().plus(15, ChronoUnit.MINUTES))
                .build();

        BuyerPreferences pref = BuyerPreferences.builder()
                .targetPrice(new BigDecimal("85.00"))
                .maxCeilingPrice(new BigDecimal("100.00"))
                .build();

        Negotiation neg = Negotiation.builder()
                .id("neg-budget-fail")
                .buyerId("b1")
                .sellerId("s1")
                .productId("p1")
                .status(NegotiationStatus.ACCEPTED)
                .buyerPreferences(pref)
                .agreementSeal(seal)
                .build();

        Product prod = Product.builder()
                .id("p1")
                .sellerId("s1")
                .active(true)
                .inventoryCount(5)
                .build();

        when(negotiationRepository.findById("neg-budget-fail")).thenReturn(Optional.of(neg));
        stubProductService.productToReturn = prod;

        assertThrows(BusinessRuleException.class, () ->
                payPalService.createOrder("neg-budget-fail", "tok_over_budget")
        );
    }

    @Test
    @DisplayName("Cancel Order: Marks pending payment as CANCELLED cleanly without creating an order")
    void testCancelOrder_RecordsCancelledPaymentWithoutCompletingOrder() {
        PaymentRecord pendingRecord = PaymentRecord.builder()
                .paypalOrderId("SANDBOX-ORDER-CANCEL1")
                .negotiationId("neg-cancel")
                .status(PaymentStatus.PENDING)
                .amount(new BigDecimal("92.00"))
                .currency("USD")
                .build();

        when(paymentRepository.findByPaypalOrderId("SANDBOX-ORDER-CANCEL1")).thenReturn(Optional.of(pendingRecord));
        when(paymentRepository.save(any(PaymentRecord.class))).thenAnswer(inv -> inv.getArgument(0));

        PaymentRecord cancelled = payPalService.cancelOrder("SANDBOX-ORDER-CANCEL1", "neg-cancel", "Buyer closed PayPal popup");

        assertEquals(PaymentStatus.CANCELLED, cancelled.getStatus());
        assertEquals("Buyer closed PayPal popup", cancelled.getFailureReason());
        assertEquals(0, stubOrderService.callCount, "Cancelled checkout must never create a completed order");
    }

    @Test
    @DisplayName("Record Failure: Stores DECLINED or FAILED audit status cleanly and blocks order completion")
    void testRecordPaymentFailure_RecordsDeclinedOrFailedPayment() {
        PaymentRecord pendingRecord = PaymentRecord.builder()
                .paypalOrderId("SANDBOX-ORDER-DECLINE")
                .negotiationId("neg-decline")
                .status(PaymentStatus.PENDING)
                .amount(new BigDecimal("94.00"))
                .currency("USD")
                .build();

        when(paymentRepository.findByPaypalOrderId("SANDBOX-ORDER-DECLINE")).thenReturn(Optional.of(pendingRecord));
        when(paymentRepository.save(any(PaymentRecord.class))).thenAnswer(inv -> inv.getArgument(0));

        PaymentRecord declined = payPalService.recordPaymentFailure("SANDBOX-ORDER-DECLINE", "neg-decline", "INSTRUMENT_DECLINED by issuer");

        assertEquals(PaymentStatus.DECLINED, declined.getStatus());
        assertTrue(declined.getFailureReason().contains("INSTRUMENT_DECLINED"));
        assertEquals(0, stubOrderService.callCount, "Declined payment must never create an order");
    }

    @Test
    @DisplayName("Cancel Order: Prevents cancelling a payment that was already captured")
    void testCancelOrder_ThrowsIfAlreadyCompleted() {
        PaymentRecord completedRecord = PaymentRecord.builder()
                .paypalOrderId("SANDBOX-ORDER-DONE")
                .negotiationId("neg-done")
                .status(PaymentStatus.COMPLETED)
                .build();

        when(paymentRepository.findByPaypalOrderId("SANDBOX-ORDER-DONE")).thenReturn(Optional.of(completedRecord));

        assertThrows(BusinessRuleException.class, () ->
                payPalService.cancelOrder("SANDBOX-ORDER-DONE", "neg-done", "Late cancel")
        );
    }
}
