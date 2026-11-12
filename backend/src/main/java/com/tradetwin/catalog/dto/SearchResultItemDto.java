package com.tradetwin.catalog.dto;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class SearchResultItemDto {

    private ProductPublicDto product;
    private String sellerStoreName;
    private double sellerReputation;
    private BigDecimal unitPrice;
    private BigDecimal shippingCost;
    private BigDecimal estimatedTotalPrice;
    private int estimatedDeliveryDays;
    private String warrantyInfo;
    private int warrantyMonths;
    private String condition;
    private int availableInventory;
    private double matchScore;
    private List<String> matchHighlights = new ArrayList<>();
    private List<String> tradeOffs = new ArrayList<>();
    private boolean eligibleForNegotiation;

    public SearchResultItemDto() {}

    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private ProductPublicDto product;
        private String sellerStoreName;
        private double sellerReputation;
        private BigDecimal unitPrice;
        private BigDecimal shippingCost;
        private BigDecimal estimatedTotalPrice;
        private int estimatedDeliveryDays;
        private String warrantyInfo;
        private int warrantyMonths;
        private String condition;
        private int availableInventory;
        private double matchScore;
        private List<String> matchHighlights = new ArrayList<>();
        private List<String> tradeOffs = new ArrayList<>();
        private boolean eligibleForNegotiation;

        public Builder product(ProductPublicDto product) { this.product = product; return this; }
        public Builder sellerStoreName(String sellerStoreName) { this.sellerStoreName = sellerStoreName; return this; }
        public Builder sellerReputation(double sellerReputation) { this.sellerReputation = sellerReputation; return this; }
        public Builder unitPrice(BigDecimal unitPrice) { this.unitPrice = unitPrice; return this; }
        public Builder shippingCost(BigDecimal shippingCost) { this.shippingCost = shippingCost; return this; }
        public Builder estimatedTotalPrice(BigDecimal estimatedTotalPrice) { this.estimatedTotalPrice = estimatedTotalPrice; return this; }
        public Builder estimatedDeliveryDays(int estimatedDeliveryDays) { this.estimatedDeliveryDays = estimatedDeliveryDays; return this; }
        public Builder warrantyInfo(String warrantyInfo) { this.warrantyInfo = warrantyInfo; return this; }
        public Builder warrantyMonths(int warrantyMonths) { this.warrantyMonths = warrantyMonths; return this; }
        public Builder condition(String condition) { this.condition = condition; return this; }
        public Builder availableInventory(int availableInventory) { this.availableInventory = availableInventory; return this; }
        public Builder matchScore(double matchScore) { this.matchScore = matchScore; return this; }
        public Builder matchHighlights(List<String> matchHighlights) { this.matchHighlights = matchHighlights; return this; }
        public Builder tradeOffs(List<String> tradeOffs) { this.tradeOffs = tradeOffs; return this; }
        public Builder eligibleForNegotiation(boolean eligibleForNegotiation) { this.eligibleForNegotiation = eligibleForNegotiation; return this; }

        public SearchResultItemDto build() {
            SearchResultItemDto dto = new SearchResultItemDto();
            dto.setProduct(this.product);
            dto.setSellerStoreName(this.sellerStoreName);
            dto.setSellerReputation(this.sellerReputation);
            dto.setUnitPrice(this.unitPrice);
            dto.setShippingCost(this.shippingCost);
            dto.setEstimatedTotalPrice(this.estimatedTotalPrice);
            dto.setEstimatedDeliveryDays(this.estimatedDeliveryDays);
            dto.setWarrantyInfo(this.warrantyInfo);
            dto.setWarrantyMonths(this.warrantyMonths);
            dto.setCondition(this.condition);
            dto.setAvailableInventory(this.availableInventory);
            dto.setMatchScore(this.matchScore);
            dto.setMatchHighlights(this.matchHighlights != null ? this.matchHighlights : new ArrayList<>());
            dto.setTradeOffs(this.tradeOffs != null ? this.tradeOffs : new ArrayList<>());
            dto.setEligibleForNegotiation(this.eligibleForNegotiation);
            return dto;
        }
    }

    public ProductPublicDto getProduct() { return product; }
    public void setProduct(ProductPublicDto product) { this.product = product; }
    public String getSellerStoreName() { return sellerStoreName; }
    public void setSellerStoreName(String sellerStoreName) { this.sellerStoreName = sellerStoreName; }
    public double getSellerReputation() { return sellerReputation; }
    public void setSellerReputation(double sellerReputation) { this.sellerReputation = sellerReputation; }
    public BigDecimal getUnitPrice() { return unitPrice; }
    public void setUnitPrice(BigDecimal unitPrice) { this.unitPrice = unitPrice; }
    public BigDecimal getShippingCost() { return shippingCost; }
    public void setShippingCost(BigDecimal shippingCost) { this.shippingCost = shippingCost; }
    public BigDecimal getEstimatedTotalPrice() { return estimatedTotalPrice; }
    public void setEstimatedTotalPrice(BigDecimal estimatedTotalPrice) { this.estimatedTotalPrice = estimatedTotalPrice; }
    public int getEstimatedDeliveryDays() { return estimatedDeliveryDays; }
    public void setEstimatedDeliveryDays(int estimatedDeliveryDays) { this.estimatedDeliveryDays = estimatedDeliveryDays; }
    public String getWarrantyInfo() { return warrantyInfo; }
    public void setWarrantyInfo(String warrantyInfo) { this.warrantyInfo = warrantyInfo; }
    public int getWarrantyMonths() { return warrantyMonths; }
    public void setWarrantyMonths(int warrantyMonths) { this.warrantyMonths = warrantyMonths; }
    public String getCondition() { return condition; }
    public void setCondition(String condition) { this.condition = condition; }
    public int getAvailableInventory() { return availableInventory; }
    public void setAvailableInventory(int availableInventory) { this.availableInventory = availableInventory; }
    public double getMatchScore() { return matchScore; }
    public void setMatchScore(double matchScore) { this.matchScore = matchScore; }
    public List<String> getMatchHighlights() { return matchHighlights; }
    public void setMatchHighlights(List<String> matchHighlights) { this.matchHighlights = matchHighlights; }
    public List<String> getTradeOffs() { return tradeOffs; }
    public void setTradeOffs(List<String> tradeOffs) { this.tradeOffs = tradeOffs; }
    public boolean isEligibleForNegotiation() { return eligibleForNegotiation; }
    public void setEligibleForNegotiation(boolean eligibleForNegotiation) { this.eligibleForNegotiation = eligibleForNegotiation; }
}
