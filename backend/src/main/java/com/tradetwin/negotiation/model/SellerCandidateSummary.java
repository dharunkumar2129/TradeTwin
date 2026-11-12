package com.tradetwin.negotiation.model;

import java.math.BigDecimal;

public class SellerCandidateSummary {
    private String sellerId;
    private String storeName;
    private String productId;
    private String productTitle;
    private String brand;
    private BigDecimal listPrice;
    private BigDecimal shippingCost;
    private int estimatedDeliveryDays;
    private int warrantyMonths;
    private int availableInventory;
    private String status; // QUEUED, ACTIVE, AGREED, FAILED, SKIPPED
    private double matchScore;

    public SellerCandidateSummary() {}

    public SellerCandidateSummary(String sellerId, String storeName, String productId, String productTitle,
                                  String brand, BigDecimal listPrice, BigDecimal shippingCost,
                                  int estimatedDeliveryDays, int warrantyMonths, int availableInventory,
                                  String status, double matchScore) {
        this.sellerId = sellerId;
        this.storeName = storeName;
        this.productId = productId;
        this.productTitle = productTitle;
        this.brand = brand;
        this.listPrice = listPrice;
        this.shippingCost = shippingCost;
        this.estimatedDeliveryDays = estimatedDeliveryDays;
        this.warrantyMonths = warrantyMonths;
        this.availableInventory = availableInventory;
        this.status = status;
        this.matchScore = matchScore;
    }

    public String getSellerId() { return sellerId; }
    public void setSellerId(String sellerId) { this.sellerId = sellerId; }
    public String getStoreName() { return storeName; }
    public void setStoreName(String storeName) { this.storeName = storeName; }
    public String getProductId() { return productId; }
    public void setProductId(String productId) { this.productId = productId; }
    public String getProductTitle() { return productTitle; }
    public void setProductTitle(String productTitle) { this.productTitle = productTitle; }
    public String getBrand() { return brand; }
    public void setBrand(String brand) { this.brand = brand; }
    public BigDecimal getListPrice() { return listPrice; }
    public void setListPrice(BigDecimal listPrice) { this.listPrice = listPrice; }
    public BigDecimal getShippingCost() { return shippingCost; }
    public void setShippingCost(BigDecimal shippingCost) { this.shippingCost = shippingCost; }
    public int getEstimatedDeliveryDays() { return estimatedDeliveryDays; }
    public void setEstimatedDeliveryDays(int estimatedDeliveryDays) { this.estimatedDeliveryDays = estimatedDeliveryDays; }
    public int getWarrantyMonths() { return warrantyMonths; }
    public void setWarrantyMonths(int warrantyMonths) { this.warrantyMonths = warrantyMonths; }
    public int getAvailableInventory() { return availableInventory; }
    public void setAvailableInventory(int availableInventory) { this.availableInventory = availableInventory; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public double getMatchScore() { return matchScore; }
    public void setMatchScore(double matchScore) { this.matchScore = matchScore; }
}
