package com.tradetwin.order.model;

import com.tradetwin.payment.model.PaymentRecord;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Document(collection = "orders")
public class Order {

    @Id
    private String id;

    @Indexed(unique = true)
    private String orderNumber;

    @Indexed
    private String negotiationId;

    @Indexed
    private String buyerId;

    @Indexed
    private String sellerId;

    @Indexed
    private String productId;

    private String productTitle;
    private String productImage;
    private String sellerStoreName;
    private int quantity = 1;
    private BigDecimal unitPrice;
    private BigDecimal finalAmount;
    private String currency = "USD";
    private String dealToken;
    private PaymentRecord payment;
    private Map<String, String> shippingDetails;
    private OrderStatus orderStatus = OrderStatus.CONFIRMED;

    // Fulfilment tracking
    private String carrier;
    private String trackingNumber;
    private String fulfilmentNotes;
    private Instant shippedAt;
    private Instant deliveredAt;
    private List<OrderStatusChange> statusHistory = new ArrayList<>();

    private Instant createdAt = Instant.now();
    private Instant updatedAt = Instant.now();

    public Order() {}

    public Order(String id, String orderNumber, String negotiationId, String buyerId, String sellerId,
                 String productId, String productTitle, String productImage, String sellerStoreName,
                 int quantity, BigDecimal unitPrice, BigDecimal finalAmount, String currency, String dealToken,
                 PaymentRecord payment, Map<String, String> shippingDetails, OrderStatus orderStatus,
                 String carrier, String trackingNumber, String fulfilmentNotes,
                 Instant shippedAt, Instant deliveredAt, List<OrderStatusChange> statusHistory,
                 Instant createdAt, Instant updatedAt) {
        this.id = id;
        this.orderNumber = orderNumber;
        this.negotiationId = negotiationId;
        this.buyerId = buyerId;
        this.sellerId = sellerId;
        this.productId = productId;
        this.productTitle = productTitle;
        this.productImage = productImage;
        this.sellerStoreName = sellerStoreName;
        this.quantity = quantity > 0 ? quantity : 1;
        this.unitPrice = unitPrice != null ? unitPrice : finalAmount;
        this.finalAmount = finalAmount;
        this.currency = currency != null ? currency : "USD";
        this.dealToken = dealToken;
        this.payment = payment;
        this.shippingDetails = shippingDetails;
        this.orderStatus = orderStatus != null ? orderStatus : OrderStatus.CONFIRMED;
        this.carrier = carrier;
        this.trackingNumber = trackingNumber;
        this.fulfilmentNotes = fulfilmentNotes;
        this.shippedAt = shippedAt;
        this.deliveredAt = deliveredAt;
        this.statusHistory = statusHistory != null ? statusHistory : new ArrayList<>();
        this.createdAt = createdAt != null ? createdAt : Instant.now();
        this.updatedAt = updatedAt != null ? updatedAt : Instant.now();
    }

    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private String id;
        private String orderNumber;
        private String negotiationId;
        private String buyerId;
        private String sellerId;
        private String productId;
        private String productTitle;
        private String productImage;
        private String sellerStoreName;
        private int quantity = 1;
        private BigDecimal unitPrice;
        private BigDecimal finalAmount;
        private String currency = "USD";
        private String dealToken;
        private PaymentRecord payment;
        private Map<String, String> shippingDetails;
        private OrderStatus orderStatus = OrderStatus.CONFIRMED;
        private String carrier;
        private String trackingNumber;
        private String fulfilmentNotes;
        private Instant shippedAt;
        private Instant deliveredAt;
        private List<OrderStatusChange> statusHistory = new ArrayList<>();
        private Instant createdAt = Instant.now();
        private Instant updatedAt = Instant.now();

        public Builder id(String id) { this.id = id; return this; }
        public Builder orderNumber(String orderNumber) { this.orderNumber = orderNumber; return this; }
        public Builder negotiationId(String negotiationId) { this.negotiationId = negotiationId; return this; }
        public Builder buyerId(String buyerId) { this.buyerId = buyerId; return this; }
        public Builder sellerId(String sellerId) { this.sellerId = sellerId; return this; }
        public Builder productId(String productId) { this.productId = productId; return this; }
        public Builder productTitle(String productTitle) { this.productTitle = productTitle; return this; }
        public Builder productImage(String productImage) { this.productImage = productImage; return this; }
        public Builder sellerStoreName(String sellerStoreName) { this.sellerStoreName = sellerStoreName; return this; }
        public Builder quantity(int quantity) { this.quantity = quantity; return this; }
        public Builder unitPrice(BigDecimal unitPrice) { this.unitPrice = unitPrice; return this; }
        public Builder finalAmount(BigDecimal finalAmount) { this.finalAmount = finalAmount; return this; }
        public Builder currency(String currency) { this.currency = currency; return this; }
        public Builder dealToken(String dealToken) { this.dealToken = dealToken; return this; }
        public Builder payment(PaymentRecord payment) { this.payment = payment; return this; }
        public Builder shippingDetails(Map<String, String> shippingDetails) { this.shippingDetails = shippingDetails; return this; }
        public Builder orderStatus(OrderStatus orderStatus) { this.orderStatus = orderStatus; return this; }
        public Builder carrier(String carrier) { this.carrier = carrier; return this; }
        public Builder trackingNumber(String trackingNumber) { this.trackingNumber = trackingNumber; return this; }
        public Builder fulfilmentNotes(String fulfilmentNotes) { this.fulfilmentNotes = fulfilmentNotes; return this; }
        public Builder shippedAt(Instant shippedAt) { this.shippedAt = shippedAt; return this; }
        public Builder deliveredAt(Instant deliveredAt) { this.deliveredAt = deliveredAt; return this; }
        public Builder statusHistory(List<OrderStatusChange> statusHistory) { this.statusHistory = statusHistory; return this; }
        public Builder createdAt(Instant createdAt) { this.createdAt = createdAt; return this; }
        public Builder updatedAt(Instant updatedAt) { this.updatedAt = updatedAt; return this; }

        public Order build() {
            return new Order(
                    id, orderNumber, negotiationId, buyerId, sellerId, productId,
                    productTitle, productImage, sellerStoreName, quantity, unitPrice,
                    finalAmount, currency, dealToken, payment, shippingDetails, orderStatus,
                    carrier, trackingNumber, fulfilmentNotes, shippedAt, deliveredAt,
                    statusHistory, createdAt, updatedAt
            );
        }
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getOrderNumber() { return orderNumber; }
    public void setOrderNumber(String orderNumber) { this.orderNumber = orderNumber; }
    public String getNegotiationId() { return negotiationId; }
    public void setNegotiationId(String negotiationId) { this.negotiationId = negotiationId; }
    public String getBuyerId() { return buyerId; }
    public void setBuyerId(String buyerId) { this.buyerId = buyerId; }
    public String getSellerId() { return sellerId; }
    public void setSellerId(String sellerId) { this.sellerId = sellerId; }
    public String getProductId() { return productId; }
    public void setProductId(String productId) { this.productId = productId; }
    public String getProductTitle() { return productTitle; }
    public void setProductTitle(String productTitle) { this.productTitle = productTitle; }
    public String getProductImage() { return productImage; }
    public void setProductImage(String productImage) { this.productImage = productImage; }
    public String getSellerStoreName() { return sellerStoreName; }
    public void setSellerStoreName(String sellerStoreName) { this.sellerStoreName = sellerStoreName; }
    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) { this.quantity = quantity; }
    public BigDecimal getUnitPrice() { return unitPrice; }
    public void setUnitPrice(BigDecimal unitPrice) { this.unitPrice = unitPrice; }
    public BigDecimal getFinalAmount() { return finalAmount; }
    public void setFinalAmount(BigDecimal finalAmount) { this.finalAmount = finalAmount; }
    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }
    public String getDealToken() { return dealToken; }
    public void setDealToken(String dealToken) { this.dealToken = dealToken; }
    public PaymentRecord getPayment() { return payment; }
    public void setPayment(PaymentRecord payment) { this.payment = payment; }
    public Map<String, String> getShippingDetails() { return shippingDetails; }
    public void setShippingDetails(Map<String, String> shippingDetails) { this.shippingDetails = shippingDetails; }
    public OrderStatus getOrderStatus() { return orderStatus; }
    public void setOrderStatus(OrderStatus orderStatus) { this.orderStatus = orderStatus; }
    public String getCarrier() { return carrier; }
    public void setCarrier(String carrier) { this.carrier = carrier; }
    public String getTrackingNumber() { return trackingNumber; }
    public void setTrackingNumber(String trackingNumber) { this.trackingNumber = trackingNumber; }
    public String getFulfilmentNotes() { return fulfilmentNotes; }
    public void setFulfilmentNotes(String fulfilmentNotes) { this.fulfilmentNotes = fulfilmentNotes; }
    public Instant getShippedAt() { return shippedAt; }
    public void setShippedAt(Instant shippedAt) { this.shippedAt = shippedAt; }
    public Instant getDeliveredAt() { return deliveredAt; }
    public void setDeliveredAt(Instant deliveredAt) { this.deliveredAt = deliveredAt; }
    public List<OrderStatusChange> getStatusHistory() { return statusHistory; }
    public void setStatusHistory(List<OrderStatusChange> statusHistory) { this.statusHistory = statusHistory; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
