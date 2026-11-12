package com.tradetwin.deal.model;

import java.util.ArrayList;
import java.util.List;

public class FairDealComparisonResult {
    private boolean hasViableDeal;
    private String summaryExplanation;
    private List<FairDealOfferEvaluation> evaluations = new ArrayList<>();
    private FairDealOfferEvaluation bestOffer;

    public FairDealComparisonResult() {}

    public FairDealComparisonResult(boolean hasViableDeal, String summaryExplanation,
                                    List<FairDealOfferEvaluation> evaluations, FairDealOfferEvaluation bestOffer) {
        this.hasViableDeal = hasViableDeal;
        this.summaryExplanation = summaryExplanation;
        this.evaluations = evaluations != null ? evaluations : new ArrayList<>();
        this.bestOffer = bestOffer;
    }

    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private boolean hasViableDeal;
        private String summaryExplanation;
        private List<FairDealOfferEvaluation> evaluations = new ArrayList<>();
        private FairDealOfferEvaluation bestOffer;

        public Builder hasViableDeal(boolean hasViableDeal) { this.hasViableDeal = hasViableDeal; return this; }
        public Builder summaryExplanation(String summaryExplanation) { this.summaryExplanation = summaryExplanation; return this; }
        public Builder evaluations(List<FairDealOfferEvaluation> evaluations) { this.evaluations = evaluations != null ? evaluations : new ArrayList<>(); return this; }
        public Builder bestOffer(FairDealOfferEvaluation bestOffer) { this.bestOffer = bestOffer; return this; }

        public FairDealComparisonResult build() {
            return new FairDealComparisonResult(hasViableDeal, summaryExplanation, evaluations, bestOffer);
        }
    }

    public boolean isHasViableDeal() { return hasViableDeal; }
    public void setHasViableDeal(boolean hasViableDeal) { this.hasViableDeal = hasViableDeal; }
    public String getSummaryExplanation() { return summaryExplanation; }
    public void setSummaryExplanation(String summaryExplanation) { this.summaryExplanation = summaryExplanation; }
    public List<FairDealOfferEvaluation> getEvaluations() { return evaluations; }
    public void setEvaluations(List<FairDealOfferEvaluation> evaluations) { this.evaluations = evaluations; }
    public FairDealOfferEvaluation getBestOffer() { return bestOffer; }
    public void setBestOffer(FairDealOfferEvaluation bestOffer) { this.bestOffer = bestOffer; }
}
