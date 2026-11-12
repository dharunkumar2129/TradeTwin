package com.tradetwin.deal.model;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class FairDealOfferEvaluation {
    private String offerId;
    private String sellerId;
    private String sellerStoreName;
    private String productId;
    private String productTitle;
    private String brand;
    private BigDecimal unitPrice;
    private int quantity;
    private BigDecimal shippingCost;
    private BigDecimal totalLandedCost;
    private int deliveryEstimateDays;
    private int warrantyMonths;
    private List<String> perks = new ArrayList<>();
    private boolean passedHardFilters;
    private String filterFailureReason;
    private double buyerSatisfactionScore;
    private double offerQualityScore;
    private double sellerPolicyCompliance;
    private double overallDealScore;
    private ScoreBreakdown breakdown;
    private String calculationExplanation;

    public FairDealOfferEvaluation() {}

    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private String offerId;
        private String sellerId;
        private String sellerStoreName;
        private String productId;
        private String productTitle;
        private String brand;
        private BigDecimal unitPrice;
        private int quantity = 1;
        private BigDecimal shippingCost;
        private BigDecimal totalLandedCost;
        private int deliveryEstimateDays;
        private int warrantyMonths;
        private List<String> perks = new ArrayList<>();
        private boolean passedHardFilters;
        private String filterFailureReason;
        private double buyerSatisfactionScore;
        private double offerQualityScore;
        private double sellerPolicyCompliance;
        private double overallDealScore;
        private ScoreBreakdown breakdown;
        private String calculationExplanation;

        public Builder offerId(String offerId) { this.offerId = offerId; return this; }
        public Builder sellerId(String sellerId) { this.sellerId = sellerId; return this; }
        public Builder sellerStoreName(String sellerStoreName) { this.sellerStoreName = sellerStoreName; return this; }
        public Builder productId(String productId) { this.productId = productId; return this; }
        public Builder productTitle(String productTitle) { this.productTitle = productTitle; return this; }
        public Builder brand(String brand) { this.brand = brand; return this; }
        public Builder unitPrice(BigDecimal unitPrice) { this.unitPrice = unitPrice; return this; }
        public Builder quantity(int quantity) { this.quantity = quantity; return this; }
        public Builder shippingCost(BigDecimal shippingCost) { this.shippingCost = shippingCost; return this; }
        public Builder totalLandedCost(BigDecimal totalLandedCost) { this.totalLandedCost = totalLandedCost; return this; }
        public Builder deliveryEstimateDays(int deliveryEstimateDays) { this.deliveryEstimateDays = deliveryEstimateDays; return this; }
        public Builder warrantyMonths(int warrantyMonths) { this.warrantyMonths = warrantyMonths; return this; }
        public Builder perks(List<String> perks) { this.perks = perks != null ? perks : new ArrayList<>(); return this; }
        public Builder passedHardFilters(boolean passedHardFilters) { this.passedHardFilters = passedHardFilters; return this; }
        public Builder filterFailureReason(String filterFailureReason) { this.filterFailureReason = filterFailureReason; return this; }
        public Builder buyerSatisfactionScore(double buyerSatisfactionScore) { this.buyerSatisfactionScore = buyerSatisfactionScore; return this; }
        public Builder offerQualityScore(double offerQualityScore) { this.offerQualityScore = offerQualityScore; return this; }
        public Builder sellerPolicyCompliance(double sellerPolicyCompliance) { this.sellerPolicyCompliance = sellerPolicyCompliance; return this; }
        public Builder overallDealScore(double overallDealScore) { this.overallDealScore = overallDealScore; return this; }
        public Builder breakdown(ScoreBreakdown breakdown) { this.breakdown = breakdown; return this; }
        public Builder calculationExplanation(String calculationExplanation) { this.calculationExplanation = calculationExplanation; return this; }

        public FairDealOfferEvaluation build() {
            FairDealOfferEvaluation eval = new FairDealOfferEvaluation();
            eval.offerId = this.offerId;
            eval.sellerId = this.sellerId;
            eval.sellerStoreName = this.sellerStoreName;
            eval.productId = this.productId;
            eval.productTitle = this.productTitle;
            eval.brand = this.brand;
            eval.unitPrice = this.unitPrice;
            eval.quantity = this.quantity;
            eval.shippingCost = this.shippingCost;
            eval.totalLandedCost = this.totalLandedCost;
            eval.deliveryEstimateDays = this.deliveryEstimateDays;
            eval.warrantyMonths = this.warrantyMonths;
            eval.perks = this.perks;
            eval.passedHardFilters = this.passedHardFilters;
            eval.filterFailureReason = this.filterFailureReason;
            eval.buyerSatisfactionScore = this.buyerSatisfactionScore;
            eval.offerQualityScore = this.offerQualityScore;
            eval.sellerPolicyCompliance = this.sellerPolicyCompliance;
            eval.overallDealScore = this.overallDealScore;
            eval.breakdown = this.breakdown;
            eval.calculationExplanation = this.calculationExplanation;
            return eval;
        }
    }

    public String getOfferId() { return offerId; }
    public void setOfferId(String offerId) { this.offerId = offerId; }
    public String getSellerId() { return sellerId; }
    public void setSellerId(String sellerId) { this.sellerId = sellerId; }
    public String getSellerStoreName() { return sellerStoreName; }
    public void setSellerStoreName(String sellerStoreName) { this.sellerStoreName = sellerStoreName; }
    public String getProductId() { return productId; }
    public void setProductId(String productId) { this.productId = productId; }
    public String getProductTitle() { return productTitle; }
    public void setProductTitle(String productTitle) { this.productTitle = productTitle; }
    public String getBrand() { return brand; }
    public void setBrand(String brand) { this.brand = brand; }
    public BigDecimal getUnitPrice() { return unitPrice; }
    public void setUnitPrice(BigDecimal unitPrice) { this.unitPrice = unitPrice; }
    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) { this.quantity = quantity; }
    public BigDecimal getShippingCost() { return shippingCost; }
    public void setShippingCost(BigDecimal shippingCost) { this.shippingCost = shippingCost; }
    public BigDecimal getTotalLandedCost() { return totalLandedCost; }
    public void setTotalLandedCost(BigDecimal totalLandedCost) { this.totalLandedCost = totalLandedCost; }
    public int getDeliveryEstimateDays() { return deliveryEstimateDays; }
    public void setDeliveryEstimateDays(int deliveryEstimateDays) { this.deliveryEstimateDays = deliveryEstimateDays; }
    public int getWarrantyMonths() { return warrantyMonths; }
    public void setWarrantyMonths(int warrantyMonths) { this.warrantyMonths = warrantyMonths; }
    public List<String> getPerks() { return perks; }
    public void setPerks(List<String> perks) { this.perks = perks; }
    public boolean isPassedHardFilters() { return passedHardFilters; }
    public void setPassedHardFilters(boolean passedHardFilters) { this.passedHardFilters = passedHardFilters; }
    public String getFilterFailureReason() { return filterFailureReason; }
    public void setFilterFailureReason(String filterFailureReason) { this.filterFailureReason = filterFailureReason; }
    public double getBuyerSatisfactionScore() { return buyerSatisfactionScore; }
    public void setBuyerSatisfactionScore(double buyerSatisfactionScore) { this.buyerSatisfactionScore = buyerSatisfactionScore; }
    public double getOfferQualityScore() { return offerQualityScore; }
    public void setOfferQualityScore(double offerQualityScore) { this.offerQualityScore = offerQualityScore; }
    public double getSellerPolicyCompliance() { return sellerPolicyCompliance; }
    public void setSellerPolicyCompliance(double sellerPolicyCompliance) { this.sellerPolicyCompliance = sellerPolicyCompliance; }
    public double getOverallDealScore() { return overallDealScore; }
    public void setOverallDealScore(double overallDealScore) { this.overallDealScore = overallDealScore; }
    public ScoreBreakdown getBreakdown() { return breakdown; }
    public void setBreakdown(ScoreBreakdown breakdown) { this.breakdown = breakdown; }
    public String getCalculationExplanation() { return calculationExplanation; }
    public void setCalculationExplanation(String calculationExplanation) { this.calculationExplanation = calculationExplanation; }
}
