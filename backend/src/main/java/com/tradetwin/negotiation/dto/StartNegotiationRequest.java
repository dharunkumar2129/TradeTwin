package com.tradetwin.negotiation.dto;

import com.tradetwin.negotiation.model.BuyerPreferences;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.ArrayList;
import java.util.List;

public class StartNegotiationRequest {

    @NotBlank(message = "Product ID is required")
    private String productId;

    @Valid
    @NotNull(message = "Buyer preferences are required")
    private BuyerPreferences preferences;

    private String buyerQuery;
    private String intentId;
    private List<String> candidateProductIds = new ArrayList<>();

    public StartNegotiationRequest() {}

    public StartNegotiationRequest(String productId, BuyerPreferences preferences) {
        this.productId = productId;
        this.preferences = preferences;
    }

    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private String productId;
        private BuyerPreferences preferences;
        private String buyerQuery;
        private String intentId;
        private List<String> candidateProductIds = new ArrayList<>();

        public Builder productId(String productId) { this.productId = productId; return this; }
        public Builder preferences(BuyerPreferences preferences) { this.preferences = preferences; return this; }
        public Builder buyerQuery(String buyerQuery) { this.buyerQuery = buyerQuery; return this; }
        public Builder intentId(String intentId) { this.intentId = intentId; return this; }
        public Builder candidateProductIds(List<String> candidateProductIds) { this.candidateProductIds = candidateProductIds; return this; }

        public StartNegotiationRequest build() {
            StartNegotiationRequest r = new StartNegotiationRequest(productId, preferences);
            r.setBuyerQuery(buyerQuery);
            r.setIntentId(intentId);
            r.setCandidateProductIds(candidateProductIds != null ? candidateProductIds : new ArrayList<>());
            return r;
        }
    }

    public String getProductId() { return productId; }
    public void setProductId(String productId) { this.productId = productId; }
    public BuyerPreferences getPreferences() { return preferences; }
    public void setPreferences(BuyerPreferences preferences) { this.preferences = preferences; }
    public String getBuyerQuery() { return buyerQuery; }
    public void setBuyerQuery(String buyerQuery) { this.buyerQuery = buyerQuery; }
    public String getIntentId() { return intentId; }
    public void setIntentId(String intentId) { this.intentId = intentId; }
    public List<String> getCandidateProductIds() { return candidateProductIds; }
    public void setCandidateProductIds(List<String> candidateProductIds) { this.candidateProductIds = candidateProductIds; }
}
