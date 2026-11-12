package com.tradetwin.deal.model;

public class ScoreBreakdown {
    private double landedCostScore;
    private double brandMatchScore;
    private double specMatchScore;
    private double deliveryScore;
    private double warrantyScore;
    private double quantityScore;
    private double returnConditionsScore;
    private double constraintComplianceScore;

    public ScoreBreakdown() {}

    public ScoreBreakdown(double landedCostScore, double brandMatchScore, double specMatchScore,
                          double deliveryScore, double warrantyScore, double quantityScore,
                          double returnConditionsScore, double constraintComplianceScore) {
        this.landedCostScore = landedCostScore;
        this.brandMatchScore = brandMatchScore;
        this.specMatchScore = specMatchScore;
        this.deliveryScore = deliveryScore;
        this.warrantyScore = warrantyScore;
        this.quantityScore = quantityScore;
        this.returnConditionsScore = returnConditionsScore;
        this.constraintComplianceScore = constraintComplianceScore;
    }

    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private double landedCostScore;
        private double brandMatchScore;
        private double specMatchScore;
        private double deliveryScore;
        private double warrantyScore;
        private double quantityScore;
        private double returnConditionsScore;
        private double constraintComplianceScore;

        public Builder landedCostScore(double landedCostScore) { this.landedCostScore = landedCostScore; return this; }
        public Builder brandMatchScore(double brandMatchScore) { this.brandMatchScore = brandMatchScore; return this; }
        public Builder specMatchScore(double specMatchScore) { this.specMatchScore = specMatchScore; return this; }
        public Builder deliveryScore(double deliveryScore) { this.deliveryScore = deliveryScore; return this; }
        public Builder warrantyScore(double warrantyScore) { this.warrantyScore = warrantyScore; return this; }
        public Builder quantityScore(double quantityScore) { this.quantityScore = quantityScore; return this; }
        public Builder returnConditionsScore(double returnConditionsScore) { this.returnConditionsScore = returnConditionsScore; return this; }
        public Builder constraintComplianceScore(double constraintComplianceScore) { this.constraintComplianceScore = constraintComplianceScore; return this; }

        public ScoreBreakdown build() {
            return new ScoreBreakdown(landedCostScore, brandMatchScore, specMatchScore, deliveryScore,
                    warrantyScore, quantityScore, returnConditionsScore, constraintComplianceScore);
        }
    }

    public double getLandedCostScore() { return landedCostScore; }
    public void setLandedCostScore(double landedCostScore) { this.landedCostScore = landedCostScore; }

    public double getBrandMatchScore() { return brandMatchScore; }
    public void setBrandMatchScore(double brandMatchScore) { this.brandMatchScore = brandMatchScore; }

    public double getSpecMatchScore() { return specMatchScore; }
    public void setSpecMatchScore(double specMatchScore) { this.specMatchScore = specMatchScore; }

    public double getDeliveryScore() { return deliveryScore; }
    public void setDeliveryScore(double deliveryScore) { this.deliveryScore = deliveryScore; }

    public double getWarrantyScore() { return warrantyScore; }
    public void setWarrantyScore(double warrantyScore) { this.warrantyScore = warrantyScore; }

    public double getQuantityScore() { return quantityScore; }
    public void setQuantityScore(double quantityScore) { this.quantityScore = quantityScore; }

    public double getReturnConditionsScore() { return returnConditionsScore; }
    public void setReturnConditionsScore(double returnConditionsScore) { this.returnConditionsScore = returnConditionsScore; }

    public double getConstraintComplianceScore() { return constraintComplianceScore; }
    public void setConstraintComplianceScore(double constraintComplianceScore) { this.constraintComplianceScore = constraintComplianceScore; }
}
