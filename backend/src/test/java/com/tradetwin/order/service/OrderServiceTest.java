package com.tradetwin.order.service;

import com.tradetwin.catalog.model.Product;
import com.tradetwin.catalog.service.ProductService;
import com.tradetwin.common.exception.BusinessRuleException;
import com.tradetwin.common.exception.UnauthorizedException;
import com.tradetwin.deal.model.AgreementSeal;
import com.tradetwin.event.model.NegotiationStreamEvent;
import com.tradetwin.event.service.SseStreamingHub;
import com.tradetwin.negotiation.model.Negotiation;
import com.tradetwin.negotiation.model.NegotiationStatus;
import com.tradetwin.negotiation.repository.NegotiationRepository;
import com.tradetwin.order.dto.BuyerDashboardSummaryDto;
import com.tradetwin.order.dto.UpdateOrderStatusRequest;
import com.tradetwin.order.model.Order;
import com.tradetwin.order.model.OrderStatus;
import com.tradetwin.order.repository.OrderRepository;
import com.tradetwin.payment.model.PaymentRecord;
import com.tradetwin.payment.model.PaymentStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class OrderServiceTest {

    private OrderRepository orderRepository;
    private NegotiationRepository negotiationRepository;
    private StubProductService stubProductService;
    private StubSseStreamingHub stubSseStreamingHub;
    private OrderStateMachine orderStateMachine;
    private OrderService orderService;

    static class StubProductService extends ProductService {
        Product productToReturn;
        int deductCalls = 0;
        int restockCalls = 0;

        public StubProductService() {
            super(null);
        }

        @Override
        public Product getProductById(String id) {
            return productToReturn;
        }

        @Override
        public synchronized void deductStock(String productId, int quantity) {
            deductCalls++;
        }

        @Override
        public synchronized void restockInventory(String productId, int quantity) {
            restockCalls++;
        }

        @Override
        public synchronized void releaseStock(String productId, int quantity) {}
    }

    static class StubSseStreamingHub extends SseStreamingHub {
        int broadcastCount = 0;

        public StubSseStreamingHub() {
            super(null);
        }

        @Override
        public void broadcast(String negotiationId, NegotiationStreamEvent event) {
            broadcastCount++;
        }
    }

    @BeforeEach
    void setUp() {
        orderRepository = Mockito.mock(OrderRepository.class);
        negotiationRepository = Mockito.mock(NegotiationRepository.class);
        stubProductService = new StubProductService();
        stubSseStreamingHub = new StubSseStreamingHub();
        orderStateMachine = new OrderStateMachine();

        orderService = new OrderService(
                orderRepository,
                negotiationRepository,
                stubProductService,
                stubSseStreamingHub,
                orderStateMachine
        );
    }

    @Test
    @DisplayName("Create Order From Agreement: Deducts stock, saves order with PAID->CONFIRMED audit history")
    void testCreateOrderFromAgreement_Success() {
        AgreementSeal seal = AgreementSeal.builder()
                .dealToken("tok_valid")
                .finalPrice(new BigDecimal("180.00"))
                .currency("USD")
                .expiresAt(Instant.now().plus(10, ChronoUnit.MINUTES))
                .build();

        Negotiation neg = Negotiation.builder()
                .id("neg-1")
                .buyerId("b1")
                .sellerId("s1")
                .productId("p1")
                .status(NegotiationStatus.ACCEPTED)
                .agreementSeal(seal)
                .build();

        Product prod = Product.builder()
                .id("p1")
                .sellerId("s1")
                .title("Sony WH-1000XM5")
                .brand("Sony")
                .images(List.of("http://img/1.jpg"))
                .build();

        PaymentRecord payment = PaymentRecord.builder()
                .paypalOrderId("PP-123")
                .captureId("CAP-456")
                .status(PaymentStatus.COMPLETED)
                .amount(new BigDecimal("180.00"))
                .build();

        when(negotiationRepository.findById("neg-1")).thenReturn(Optional.of(neg));
        stubProductService.productToReturn = prod;
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));

        Order result = orderService.createOrderFromAgreement("neg-1", "tok_valid", payment, Map.of("fullName", "Alice"));

        assertNotNull(result);
        assertEquals(OrderStatus.CONFIRMED, result.getOrderStatus());
        assertEquals("Sony WH-1000XM5", result.getProductTitle());
        assertEquals(new BigDecimal("180.00"), result.getFinalAmount());
        assertEquals(1, stubProductService.deductCalls);
        assertEquals(2, result.getStatusHistory().size());
        assertEquals(OrderStatus.PAID, result.getStatusHistory().get(0).getToStatus());
        assertEquals(OrderStatus.CONFIRMED, result.getStatusHistory().get(1).getToStatus());
        assertEquals(1, stubSseStreamingHub.broadcastCount);
    }

    @Test
    @DisplayName("Fulfilment State Machine: Seller can transition CONFIRMED -> PROCESSING -> SHIPPED with tracking")
    void testUpdateOrderStatus_SellerFulfilmentFlow() {
        Order order = Order.builder()
                .id("ord-1")
                .orderNumber("TT-1001")
                .negotiationId("neg-1")
                .sellerId("s1")
                .productId("p1")
                .orderStatus(OrderStatus.CONFIRMED)
                .build();

        when(orderRepository.findById("ord-1")).thenReturn(Optional.of(order));
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));

        // Step 1: PROCESSING
        UpdateOrderStatusRequest procReq = new UpdateOrderStatusRequest(OrderStatus.PROCESSING, null, null, "Packing order");
        Order afterProc = orderService.updateOrderStatus("ord-1", "s1", procReq);
        assertEquals(OrderStatus.PROCESSING, afterProc.getOrderStatus());
        assertEquals("Packing order", afterProc.getFulfilmentNotes());

        // Step 2: SHIPPED
        UpdateOrderStatusRequest shipReq = new UpdateOrderStatusRequest(OrderStatus.SHIPPED, "FedEx", "FX-999888", "Dispatched via Express");
        Order afterShip = orderService.updateOrderStatus("ord-1", "s1", shipReq);
        assertEquals(OrderStatus.SHIPPED, afterShip.getOrderStatus());
        assertEquals("FedEx", afterShip.getCarrier());
        assertEquals("FX-999888", afterShip.getTrackingNumber());
        assertNotNull(afterShip.getShippedAt());
        assertEquals(2, afterShip.getStatusHistory().size());
    }

    @Test
    @DisplayName("Fulfilment Security: Unauthorized seller cannot update someone else's order")
    void testUpdateOrderStatus_UnauthorizedThrowsException() {
        Order order = Order.builder()
                .id("ord-1")
                .sellerId("s1")
                .orderStatus(OrderStatus.CONFIRMED)
                .build();

        when(orderRepository.findById("ord-1")).thenReturn(Optional.of(order));

        UpdateOrderStatusRequest req = new UpdateOrderStatusRequest(OrderStatus.PROCESSING, null, null, null);
        assertThrows(UnauthorizedException.class, () -> orderService.updateOrderStatus("ord-1", "other-seller", req));
    }

    @Test
    @DisplayName("Fulfilment Cancel/Refund: Transitioning to CANCELLED automatically restocks inventory")
    void testUpdateOrderStatus_CancelledRestocksInventory() {
        Order order = Order.builder()
                .id("ord-1")
                .sellerId("s1")
                .productId("p1")
                .quantity(1)
                .orderStatus(OrderStatus.CONFIRMED)
                .build();

        when(orderRepository.findById("ord-1")).thenReturn(Optional.of(order));
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));

        UpdateOrderStatusRequest req = new UpdateOrderStatusRequest(OrderStatus.CANCELLED, null, null, "Buyer requested cancel");
        Order result = orderService.updateOrderStatus("ord-1", "s1", req);

        assertEquals(OrderStatus.CANCELLED, result.getOrderStatus());
        assertEquals(1, stubProductService.restockCalls);
    }

    @Test
    @DisplayName("Buyer Dashboard Summary: Calculates real totals excluding cancelled/refunded orders")
    void testGetBuyerDashboardSummary_RealStatsCalculation() {
        Order ord1 = Order.builder().id("o1").finalAmount(new BigDecimal("100.00")).orderStatus(OrderStatus.DELIVERED).build();
        Order ord2 = Order.builder().id("o2").finalAmount(new BigDecimal("250.00")).orderStatus(OrderStatus.CONFIRMED).build();
        Order ordCancelled = Order.builder().id("o3").finalAmount(new BigDecimal("500.00")).orderStatus(OrderStatus.CANCELLED).build();

        Negotiation activeNeg = Negotiation.builder().id("n1").status(NegotiationStatus.NEGOTIATING).build();
        Negotiation agreedNeg = Negotiation.builder().id("n2").status(NegotiationStatus.AGREED).build();

        when(orderRepository.findByBuyerIdOrderByCreatedAtDesc("b1")).thenReturn(List.of(ord1, ord2, ordCancelled));
        when(negotiationRepository.findByBuyerIdOrderByCreatedAtDesc("b1")).thenReturn(List.of(activeNeg, agreedNeg));

        BuyerDashboardSummaryDto summary = orderService.getBuyerDashboardSummary("b1");

        assertEquals(3, summary.totalOrders());
        assertEquals(new BigDecimal("350.00"), summary.totalSpent());
        assertEquals(1, summary.activeNegotiations());
        assertEquals(1, summary.agreedDeals());
        assertEquals(1, summary.pendingFulfilmentOrders());
        assertEquals(1, summary.shippedOrDeliveredOrders());
    }
}
