package com.tradetwin.order.service;

import com.tradetwin.catalog.model.Product;
import com.tradetwin.catalog.service.ProductService;
import com.tradetwin.common.exception.BusinessRuleException;
import com.tradetwin.common.exception.ResourceNotFoundException;
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
import com.tradetwin.order.model.OrderStatusChange;
import com.tradetwin.order.repository.OrderRepository;
import com.tradetwin.payment.model.PaymentRecord;
import com.tradetwin.user.model.User;
import com.tradetwin.user.model.UserRole;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;

@Service
public class OrderService {

    private static final Logger log = LoggerFactory.getLogger(OrderService.class);

    private final OrderRepository orderRepository;
    private final NegotiationRepository negotiationRepository;
    private final ProductService productService;
    private final SseStreamingHub sseStreamingHub;
    private final OrderStateMachine orderStateMachine;

    public OrderService(OrderRepository orderRepository,
                        NegotiationRepository negotiationRepository,
                        ProductService productService,
                        SseStreamingHub sseStreamingHub,
                        OrderStateMachine orderStateMachine) {
        this.orderRepository = orderRepository;
        this.negotiationRepository = negotiationRepository;
        this.productService = productService;
        this.sseStreamingHub = sseStreamingHub;
        this.orderStateMachine = orderStateMachine;
    }

    /**
     * Creates an order from a verified, server-captured agreement.
     * Orders are NEVER marked as PAID or CONFIRMED merely by frontend navigation;
     * they are created strictly after PayPal capture verification.
     */
    public Order createOrderFromAgreement(
            String negotiationId,
            String dealToken,
            PaymentRecord paymentRecord,
            Map<String, String> shippingDetails) {

        Negotiation neg = negotiationRepository.findById(negotiationId)
                .orElseThrow(() -> new ResourceNotFoundException("Negotiation", "id", negotiationId));

        if (neg.getStatus() != NegotiationStatus.AGREED && neg.getStatus() != NegotiationStatus.ACCEPTED) {
            throw new BusinessRuleException("Cannot complete checkout: Negotiation status is " + neg.getStatus());
        }

        AgreementSeal seal = neg.getAgreementSeal();
        if (seal == null) {
            throw new BusinessRuleException("No valid agreement seal found for this negotiation");
        }

        if (!seal.getDealToken().equals(dealToken)) {
            throw new BusinessRuleException("Invalid agreement deal token provided for checkout authorization");
        }

        int qty = seal.getQuantity();
        if (Instant.now().isAfter(seal.getExpiresAt())) {
            seal.setStatus("EXPIRED");
            neg.setStatus(NegotiationStatus.EXPIRED);
            if (seal.isStockReserved()) {
                try {
                    productService.releaseStock(neg.getProductId(), qty);
                } catch (Exception ex) {
                    log.warn("Could not release expired reservation for product {}: {}", neg.getProductId(), ex.getMessage());
                }
                seal.setStockReserved(false);
            }
            negotiationRepository.save(neg);
            throw new BusinessRuleException("Deal agreement lock has expired. Please renegotiate.");
        }

        // Ensure stock is reserved before permanently deducting (supports retry after cancellation)
        if (!seal.isStockReserved()) {
            productService.reserveStock(neg.getProductId(), qty);
            seal.setStockReserved(true);
        }

        // Deduct product inventory permanently
        productService.deductStock(neg.getProductId(), qty);
        seal.setStockReserved(false);

        // Fetch product metadata for rich order record
        Product product = null;
        try {
            product = productService.getProductById(neg.getProductId());
        } catch (Exception e) {
            log.warn("Could not retrieve product metadata for order creation: {}", e.getMessage());
        }

        String productTitle = product != null ? product.getTitle() : "Negotiated Item";
        String productImage = product != null && product.getImages() != null && !product.getImages().isEmpty()
                ? product.getImages().get(0) : "";
        String sellerStoreName = product != null && product.getBrand() != null && !product.getBrand().isBlank()
                ? product.getBrand() : "Seller #" + neg.getSellerId();

        String orderNumber = "TT-" + Instant.now().getEpochSecond() + "-" + (1000 + new Random().nextInt(9000));

        // Initial state audit history
        List<OrderStatusChange> history = new ArrayList<>();
        history.add(new OrderStatusChange(
                OrderStatus.PAYMENT_PENDING,
                OrderStatus.PAID,
                "PAYPAL_CAPTURE",
                paymentRecord != null && paymentRecord.getCaptureId() != null
                        ? "PayPal payment capture confirmed (Capture ID: " + paymentRecord.getCaptureId() + ")"
                        : "PayPal sandbox payment captured and verified",
                Instant.now()
        ));
        history.add(new OrderStatusChange(
                OrderStatus.PAID,
                OrderStatus.CONFIRMED,
                "SYSTEM",
                "Order confirmed and merchant inventory deducted",
                Instant.now()
        ));

        Order order = Order.builder()
                .orderNumber(orderNumber)
                .negotiationId(negotiationId)
                .buyerId(neg.getBuyerId())
                .sellerId(neg.getSellerId())
                .productId(neg.getProductId())
                .productTitle(productTitle)
                .productImage(productImage)
                .sellerStoreName(sellerStoreName)
                .quantity(qty)
                .unitPrice(seal.getUnitPrice() != null ? seal.getUnitPrice() : seal.getFinalPrice())
                .finalAmount(seal.getFinalPrice())
                .currency(seal.getCurrency())
                .dealToken(dealToken)
                .payment(paymentRecord)
                .shippingDetails(shippingDetails != null ? shippingDetails : Map.of())
                .orderStatus(OrderStatus.CONFIRMED)
                .statusHistory(history)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        Order saved = orderRepository.save(order);

        seal.setStatus("PAID");
        neg.setStatus(NegotiationStatus.COMPLETED);
        neg.setUpdatedAt(Instant.now());
        negotiationRepository.save(neg);

        // Emit NEGOTIATION_COMPLETED event
        sseStreamingHub.broadcast(negotiationId, NegotiationStreamEvent.builder()
                .eventType("NEGOTIATION_COMPLETED")
                .negotiationId(negotiationId)
                .speaker("MARKETPLACE")
                .payload(saved)
                .timestamp(Instant.now())
                .build());

        log.info("Order [{}] successfully confirmed for negotiation [{}] at amount: ${}", orderNumber, negotiationId, seal.getFinalPrice());
        return saved;
    }

    /**
     * Updates an order's fulfilment status strictly through the Order State Machine.
     */
    public Order updateOrderStatus(String orderId, String sellerUserId, UpdateOrderStatusRequest request) {
        Order order = getOrderById(orderId);

        // Verify seller ownership
        if (sellerUserId != null && !sellerUserId.equals(order.getSellerId())) {
            throw new UnauthorizedException("You do not have permission to manage fulfilment for this order.");
        }

        OrderStatus currentStatus = order.getOrderStatus();
        OrderStatus targetStatus = request.getStatus();

        // Enforce state machine transitions
        orderStateMachine.validateTransition(currentStatus, targetStatus);

        // Handle status-specific side effects
        if (targetStatus == OrderStatus.SHIPPED) {
            order.setCarrier(request.getCarrier() != null ? request.getCarrier().trim() : "Standard Carrier");
            order.setTrackingNumber(request.getTrackingNumber() != null ? request.getTrackingNumber().trim() : "TT-TRK-" + System.currentTimeMillis());
            order.setShippedAt(Instant.now());
        } else if (targetStatus == OrderStatus.DELIVERED) {
            order.setDeliveredAt(Instant.now());
        } else if (targetStatus == OrderStatus.CANCELLED || targetStatus == OrderStatus.REFUNDED) {
            // Restock product inventory
            try {
                productService.restockInventory(order.getProductId(), order.getQuantity());
                log.info("Restocked {} units of product [{}] due to order status [{}]", order.getQuantity(), order.getProductId(), targetStatus);
            } catch (Exception ex) {
                log.warn("Failed to restock inventory for order [{}]: {}", orderId, ex.getMessage());
            }
        }

        if (request.getNotes() != null && !request.getNotes().isBlank()) {
            order.setFulfilmentNotes(request.getNotes().trim());
        }

        // Record audit change
        List<OrderStatusChange> history = order.getStatusHistory() != null ? order.getStatusHistory() : new ArrayList<>();
        history.add(new OrderStatusChange(
                currentStatus,
                targetStatus,
                sellerUserId != null ? sellerUserId : "SYSTEM",
                request.getNotes(),
                Instant.now()
        ));
        order.setStatusHistory(history);
        order.setOrderStatus(targetStatus);
        order.setUpdatedAt(Instant.now());

        Order updated = orderRepository.save(order);

        // Broadcast status update event
        sseStreamingHub.broadcast(order.getNegotiationId(), NegotiationStreamEvent.builder()
                .eventType("ORDER_STATUS_UPDATED")
                .negotiationId(order.getNegotiationId())
                .speaker("SELLER")
                .payload(updated)
                .timestamp(Instant.now())
                .build());

        log.info("Order [{}] status updated from [{}] to [{}] by seller [{}]", order.getOrderNumber(), currentStatus, targetStatus, sellerUserId);
        return updated;
    }

    /**
     * Calculates real statistics for the Buyer Dashboard from MongoDB.
     */
    public BuyerDashboardSummaryDto getBuyerDashboardSummary(String buyerId) {
        List<Order> orders = orderRepository.findByBuyerIdOrderByCreatedAtDesc(buyerId);
        List<Negotiation> negotiations = negotiationRepository.findByBuyerIdOrderByCreatedAtDesc(buyerId);

        long totalOrders = orders.size();
        BigDecimal totalSpent = orders.stream()
                .filter(o -> o.getOrderStatus() != OrderStatus.CANCELLED && o.getOrderStatus() != OrderStatus.REFUNDED)
                .map(Order::getFinalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        long activeNegotiations = negotiations.stream()
                .filter(n -> n.getStatus() == NegotiationStatus.NEGOTIATING
                        || n.getStatus() == NegotiationStatus.DISCOVERING
                        || n.getStatus() == NegotiationStatus.COUNTEROFFERED
                        || n.getStatus() == NegotiationStatus.IN_PROGRESS
                        || n.getStatus() == NegotiationStatus.INITIATED)
                .count();

        long agreedDeals = negotiations.stream()
                .filter(n -> n.getStatus() == NegotiationStatus.AGREED
                        || n.getStatus() == NegotiationStatus.ACCEPTED)
                .count();

        long pendingFulfilment = orders.stream()
                .filter(o -> o.getOrderStatus() == OrderStatus.CONFIRMED || o.getOrderStatus() == OrderStatus.PROCESSING)
                .count();

        long shippedOrDelivered = orders.stream()
                .filter(o -> o.getOrderStatus() == OrderStatus.SHIPPED || o.getOrderStatus() == OrderStatus.DELIVERED)
                .count();

        return new BuyerDashboardSummaryDto(
                totalOrders,
                totalSpent,
                activeNegotiations,
                agreedDeals,
                pendingFulfilment,
                shippedOrDelivered,
                "USD",
                orders.stream().limit(10).toList()
        );
    }

    public Order getOrderById(String orderId) {
        return orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order", "id", orderId));
    }

    public List<Order> getOrdersByBuyer(String buyerId) {
        return orderRepository.findByBuyerIdOrderByCreatedAtDesc(buyerId);
    }

    public List<Order> getOrdersBySeller(String sellerId) {
        return orderRepository.findBySellerIdOrderByCreatedAtDesc(sellerId);
    }
}
