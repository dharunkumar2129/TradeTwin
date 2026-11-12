package com.tradetwin.negotiation.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public class NegotiationOffer {

    private String id;
    private String negotiationId;
    private String sellerId;
    private String productId;
    private int roundNumber;
    private BigDecimal unitPrice;
    private int quantity = 1;
    private BigDecimal shippingCost = BigDecimal.ZERO;
    private int deliveryEstimateDays = 3;
    private int warrantyMonths = 12;
    private String offerStatus; // PROPOSED, COUNTERED, ACCEPTED, REJECTED
    private String publicExplanation;
    private Instant timestamp = Instant.now();
    private List<String> perks = new ArrayList<>();

    public NegotiationOffer() {}

    public NegotiationOffer(String id, String negotiationId, String sellerId, String productId,
                            int roundNumber, BigDecimal unitPrice, int quantity, BigDecimal shippingCost,
                            int deliveryEstimateDays, int warrantyMonths, String offerStatus,
                            String publicExplanation, Instant timestamp, List<String> perks) {
        this.id = id;
        this.negotiationId = negotiationId;
        this.sellerId = sellerId;
        this.productId = productId;
        this.roundNumber = roundNumber;
        this.unitPrice = unitPrice;
        this.quantity = quantity > 0 ? quantity : 1;
        this.shippingCost = shippingCost != null ? shippingCost : BigDecimal.ZERO;
        this.deliveryEstimateDays = deliveryEstimateDays;
        this.warrantyMonths = warrantyMonths;
        this.offerStatus = offerStatus;
        this.publicExplanation = publicExplanation;
        this.timestamp = timestamp != null ? timestamp : Instant.now();
        this.perks = perks != null ? perks : new ArrayList<>();
    }

    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private String id;
        private String negotiationId;
        private String sellerId;
        private String productId;
        private int roundNumber;
        private BigDecimal unitPrice;
        private int quantity = 1;
        private BigDecimal shippingCost = BigDecimal.ZERO;
        private int deliveryEstimateDays = 3;
        private int warrantyMonths = 12;
        private String offerStatus;
        private String publicExplanation;
        private Instant timestamp = Instant.now();
        private List<String> perks = new ArrayList<>();

        public Builder id(String id) { this.id = id; return this; }
        public Builder negotiationId(String negotiationId) { this.negotiationId = negotiationId; return this; }
        public Builder sellerId(String sellerId) { this.sellerId = sellerId; return this; }
        public Builder productId(String productId) { this.productId = productId; return this; }
        public Builder roundNumber(int roundNumber) { this.roundNumber = roundNumber; return this; }
        public Builder unitPrice(BigDecimal unitPrice) { this.unitPrice = unitPrice; return this; }
        public Builder quantity(int quantity) { this.quantity = quantity; return this; }
        public Builder shippingCost(BigDecimal shippingCost) { this.shippingCost = shippingCost; return this; }
        public Builder deliveryEstimateDays(int deliveryEstimateDays) { this.deliveryEstimateDays = deliveryEstimateDays; return this; }
        public Builder warrantyMonths(int warrantyMonths) { this.warrantyMonths = warrantyMonths; return this; }
        public Builder offerStatus(String offerStatus) { this.offerStatus = offerStatus; return this; }
        public Builder publicExplanation(String publicExplanation) { this.publicExplanation = publicExplanation; return this; }
        public Builder timestamp(Instant timestamp) { this.timestamp = timestamp; return this; }
        public Builder perks(List<String> perks) { this.perks = perks; return this; }

        public NegotiationOffer build() {
            return new NegotiationOffer(id, negotiationId, sellerId, productId, roundNumber,
                    unitPrice, quantity, shippingCost, deliveryEstimateDays, warrantyMonths,
                    offerStatus, publicExplanation, timestamp, perks);
        }
    }

    public BigDecimal calculateTotalCost() {
        if (unitPrice == null) return BigDecimal.ZERO;
        return unitPrice.multiply(BigDecimal.valueOf(quantity)).add(shippingCost != null ? shippingCost : BigDecimal.ZERO);
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getNegotiationId() { return negotiationId; }
    public void setNegotiationId(String negotiationId) { this.negotiationId = negotiationId; }
    public String getSellerId() { return sellerId; }
    public void setSellerId(String sellerId) { this.sellerId = sellerId; }
    public String getProductId() { return productId; }
    public void setProductId(String productId) { this.productId = productId; }
    public int getRoundNumber() { return roundNumber; }
    public void setRoundNumber(int roundNumber) { this.roundNumber = roundNumber; }
    public BigDecimal getUnitPrice() { return unitPrice; }
    public void setUnitPrice(BigDecimal unitPrice) { this.unitPrice = unitPrice; }
    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) { this.quantity = quantity; }
    public BigDecimal getShippingCost() { return shippingCost; }
    public void setShippingCost(BigDecimal shippingCost) { this.shippingCost = shippingCost; }
    public int getDeliveryEstimateDays() { return deliveryEstimateDays; }
    public void setDeliveryEstimateDays(int deliveryEstimateDays) { this.deliveryEstimateDays = deliveryEstimateDays; }
    public int getWarrantyMonths() { return warrantyMonths; }
    public void setWarrantyMonths(int warrantyMonths) { this.warrantyMonths = warrantyMonths; }
    public String getOfferStatus() { return offerStatus; }
    public void setOfferStatus(String offerStatus) { this.offerStatus = offerStatus; }
    public String getPublicExplanation() { return publicExplanation; }
    public void setPublicExplanation(String publicExplanation) { this.publicExplanation = publicExplanation; }
    public Instant getTimestamp() { return timestamp; }
    public void setTimestamp(Instant timestamp) { this.timestamp = timestamp; }
    public List<String> getPerks() { return perks; }
    public void setPerks(List<String> perks) { this.perks = perks; }
}
