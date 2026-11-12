package com.tradetwin.negotiation.model;

import java.math.BigDecimal;
import java.time.Instant;

public class SellerAttemptRecord {
    private String sellerId;
    private String storeName;
    private String productId;
    private String productTitle;
    private String reason;
    private int roundsCompleted;
    private BigDecimal lastOfferPrice;
    private Instant timestamp = Instant.now();

    public SellerAttemptRecord() {}

    public SellerAttemptRecord(String sellerId, String storeName, String productId, String productTitle,
                               String reason, int roundsCompleted, BigDecimal lastOfferPrice, Instant timestamp) {
        this.sellerId = sellerId;
        this.storeName = storeName;
        this.productId = productId;
        this.productTitle = productTitle;
        this.reason = reason;
        this.roundsCompleted = roundsCompleted;
        this.lastOfferPrice = lastOfferPrice;
        this.timestamp = timestamp != null ? timestamp : Instant.now();
    }

    public String getSellerId() { return sellerId; }
    public void setSellerId(String sellerId) { this.sellerId = sellerId; }
    public String getStoreName() { return storeName; }
    public void setStoreName(String storeName) { this.storeName = storeName; }
    public String getProductId() { return productId; }
    public void setProductId(String productId) { this.productId = productId; }
    public String getProductTitle() { return productTitle; }
    public void setProductTitle(String productTitle) { this.productTitle = productTitle; }
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
    public int getRoundsCompleted() { return roundsCompleted; }
    public void setRoundsCompleted(int roundsCompleted) { this.roundsCompleted = roundsCompleted; }
    public BigDecimal getLastOfferPrice() { return lastOfferPrice; }
    public void setLastOfferPrice(BigDecimal lastOfferPrice) { this.lastOfferPrice = lastOfferPrice; }
    public Instant getTimestamp() { return timestamp; }
    public void setTimestamp(Instant timestamp) { this.timestamp = timestamp; }
}
