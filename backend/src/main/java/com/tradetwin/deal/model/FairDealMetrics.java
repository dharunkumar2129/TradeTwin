package com.tradetwin.deal.model;

import java.math.BigDecimal;

public class FairDealMetrics {
    private double fairDealScore;
    private double priceEquitability;
    private double utilityBuyer;
    private double utilitySeller;
    private double surplusSplitPctBuyer;
    private double surplusSplitPctSeller;
    private BigDecimal marketFairPriceEstimate;
    private String summaryExplanation;

    // Enhanced FairDeal evaluation dimensions
    private double buyerSatisfactionScore;
    private double offerQualityScore;
    private double sellerPolicyCompliance;
    private double overallDealScore;
    private BigDecimal totalLandedCost;
    private ScoreBreakdown scoreBreakdown;
    private boolean passedHardFilters = true;
    private String filterFailureReason;
    private String calculationExplanation;

    public FairDealMetrics() {}

    public FairDealMetrics(double fairDealScore, double priceEquitability, double utilityBuyer,
                           double utilitySeller, double surplusSplitPctBuyer, double surplusSplitPctSeller,
                           BigDecimal marketFairPriceEstimate, String summaryExplanation) {
        this.fairDealScore = fairDealScore;
        this.priceEquitability = priceEquitability;
        this.utilityBuyer = utilityBuyer;
        this.utilitySeller = utilitySeller;
        this.surplusSplitPctBuyer = surplusSplitPctBuyer;
        this.surplusSplitPctSeller = surplusSplitPctSeller;
        this.marketFairPriceEstimate = marketFairPriceEstimate;
        this.summaryExplanation = summaryExplanation;
        this.overallDealScore = fairDealScore;
        this.buyerSatisfactionScore = Math.round(utilityBuyer * 100.0 * 10.0) / 10.0;
        this.sellerPolicyCompliance = Math.round(utilitySeller * 100.0 * 10.0) / 10.0;
        this.offerQualityScore = Math.round(((utilityBuyer + utilitySeller) / 2.0 * 100.0) * 10.0) / 10.0;
    }

    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private double fairDealScore;
        private double priceEquitability;
        private double utilityBuyer;
        private double utilitySeller;
        private double surplusSplitPctBuyer;
        private double surplusSplitPctSeller;
        private BigDecimal marketFairPriceEstimate;
        private String summaryExplanation;

        private double buyerSatisfactionScore;
        private double offerQualityScore;
        private double sellerPolicyCompliance;
        private double overallDealScore;
        private BigDecimal totalLandedCost;
        private ScoreBreakdown scoreBreakdown;
        private boolean passedHardFilters = true;
        private String filterFailureReason;
        private String calculationExplanation;

        public Builder fairDealScore(double fairDealScore) { this.fairDealScore = fairDealScore; return this; }
        public Builder priceEquitability(double priceEquitability) { this.priceEquitability = priceEquitability; return this; }
        public Builder utilityBuyer(double utilityBuyer) { this.utilityBuyer = utilityBuyer; return this; }
        public Builder utilitySeller(double utilitySeller) { this.utilitySeller = utilitySeller; return this; }
        public Builder surplusSplitPctBuyer(double surplusSplitPctBuyer) { this.surplusSplitPctBuyer = surplusSplitPctBuyer; return this; }
        public Builder surplusSplitPctSeller(double surplusSplitPctSeller) { this.surplusSplitPctSeller = surplusSplitPctSeller; return this; }
        public Builder marketFairPriceEstimate(BigDecimal marketFairPriceEstimate) { this.marketFairPriceEstimate = marketFairPriceEstimate; return this; }
        public Builder summaryExplanation(String summaryExplanation) { this.summaryExplanation = summaryExplanation; return this; }

        public Builder buyerSatisfactionScore(double buyerSatisfactionScore) { this.buyerSatisfactionScore = buyerSatisfactionScore; return this; }
        public Builder offerQualityScore(double offerQualityScore) { this.offerQualityScore = offerQualityScore; return this; }
        public Builder sellerPolicyCompliance(double sellerPolicyCompliance) { this.sellerPolicyCompliance = sellerPolicyCompliance; return this; }
        public Builder overallDealScore(double overallDealScore) { this.overallDealScore = overallDealScore; return this; }
        public Builder totalLandedCost(BigDecimal totalLandedCost) { this.totalLandedCost = totalLandedCost; return this; }
        public Builder scoreBreakdown(ScoreBreakdown scoreBreakdown) { this.scoreBreakdown = scoreBreakdown; return this; }
        public Builder passedHardFilters(boolean passedHardFilters) { this.passedHardFilters = passedHardFilters; return this; }
        public Builder filterFailureReason(String filterFailureReason) { this.filterFailureReason = filterFailureReason; return this; }
        public Builder calculationExplanation(String calculationExplanation) { this.calculationExplanation = calculationExplanation; return this; }

        public FairDealMetrics build() {
            FairDealMetrics metrics = new FairDealMetrics(fairDealScore, priceEquitability, utilityBuyer,
                    utilitySeller, surplusSplitPctBuyer, surplusSplitPctSeller, marketFairPriceEstimate, summaryExplanation);
            metrics.buyerSatisfactionScore = this.buyerSatisfactionScore > 0 ? this.buyerSatisfactionScore : Math.round(utilityBuyer * 100.0 * 10.0) / 10.0;
            metrics.offerQualityScore = this.offerQualityScore > 0 ? this.offerQualityScore : Math.round(((utilityBuyer + utilitySeller) / 2.0 * 100.0) * 10.0) / 10.0;
            metrics.sellerPolicyCompliance = this.sellerPolicyCompliance > 0 ? this.sellerPolicyCompliance : Math.round(utilitySeller * 100.0 * 10.0) / 10.0;
            metrics.overallDealScore = this.overallDealScore > 0 ? this.overallDealScore : fairDealScore;
            metrics.totalLandedCost = this.totalLandedCost;
            metrics.scoreBreakdown = this.scoreBreakdown;
            metrics.passedHardFilters = this.passedHardFilters;
            metrics.filterFailureReason = this.filterFailureReason;
            metrics.calculationExplanation = this.calculationExplanation;
            return metrics;
        }
    }

    public double getFairDealScore() { return fairDealScore; }
    public void setFairDealScore(double fairDealScore) { this.fairDealScore = fairDealScore; }
    public double getPriceEquitability() { return priceEquitability; }
    public void setPriceEquitability(double priceEquitability) { this.priceEquitability = priceEquitability; }
    public double getUtilityBuyer() { return utilityBuyer; }
    public void setUtilityBuyer(double utilityBuyer) { this.utilityBuyer = utilityBuyer; }
    public double getUtilitySeller() { return utilitySeller; }
    public void setUtilitySeller(double utilitySeller) { this.utilitySeller = utilitySeller; }
    public double getSurplusSplitPctBuyer() { return surplusSplitPctBuyer; }
    public void setSurplusSplitPctBuyer(double surplusSplitPctBuyer) { this.surplusSplitPctBuyer = surplusSplitPctBuyer; }
    public double getSurplusSplitPctSeller() { return surplusSplitPctSeller; }
    public void setSurplusSplitPctSeller(double surplusSplitPctSeller) { this.surplusSplitPctSeller = surplusSplitPctSeller; }
    public BigDecimal getMarketFairPriceEstimate() { return marketFairPriceEstimate; }
    public void setMarketFairPriceEstimate(BigDecimal marketFairPriceEstimate) { this.marketFairPriceEstimate = marketFairPriceEstimate; }
    public String getSummaryExplanation() { return summaryExplanation; }
    public void setSummaryExplanation(String summaryExplanation) { this.summaryExplanation = summaryExplanation; }

    public double getBuyerSatisfactionScore() { return buyerSatisfactionScore; }
    public void setBuyerSatisfactionScore(double buyerSatisfactionScore) { this.buyerSatisfactionScore = buyerSatisfactionScore; }
    public double getOfferQualityScore() { return offerQualityScore; }
    public void setOfferQualityScore(double offerQualityScore) { this.offerQualityScore = offerQualityScore; }
    public double getSellerPolicyCompliance() { return sellerPolicyCompliance; }
    public void setSellerPolicyCompliance(double sellerPolicyCompliance) { this.sellerPolicyCompliance = sellerPolicyCompliance; }
    public double getOverallDealScore() { return overallDealScore; }
    public void setOverallDealScore(double overallDealScore) { this.overallDealScore = overallDealScore; }
    public BigDecimal getTotalLandedCost() { return totalLandedCost; }
    public void setTotalLandedCost(BigDecimal totalLandedCost) { this.totalLandedCost = totalLandedCost; }
    public ScoreBreakdown getScoreBreakdown() { return scoreBreakdown; }
    public void setScoreBreakdown(ScoreBreakdown scoreBreakdown) { this.scoreBreakdown = scoreBreakdown; }
    public boolean isPassedHardFilters() { return passedHardFilters; }
    public void setPassedHardFilters(boolean passedHardFilters) { this.passedHardFilters = passedHardFilters; }
    public String getFilterFailureReason() { return filterFailureReason; }
    public void setFilterFailureReason(String filterFailureReason) { this.filterFailureReason = filterFailureReason; }
    public String getCalculationExplanation() { return calculationExplanation; }
    public void setCalculationExplanation(String calculationExplanation) { this.calculationExplanation = calculationExplanation; }
}
