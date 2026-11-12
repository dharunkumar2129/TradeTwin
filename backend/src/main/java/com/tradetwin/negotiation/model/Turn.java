package com.tradetwin.negotiation.model;

import java.time.Instant;

public class Turn {
    private int turnNumber;
    private int roundNumber;
    private String speaker;
    private String action;
    private String sellerId;
    private String sellerStoreName;
    private String productId;
    private String productTitle;
    private OfferProposal proposal;
    private String agentReasoning;
    private Instant timestamp = Instant.now();

    public Turn() {}

    public Turn(int turnNumber, String speaker, String action, OfferProposal proposal, String agentReasoning, Instant timestamp) {
        this.turnNumber = turnNumber;
        this.speaker = speaker;
        this.action = action;
        this.proposal = proposal;
        this.agentReasoning = agentReasoning;
        this.timestamp = timestamp != null ? timestamp : Instant.now();
    }

    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private int turnNumber;
        private int roundNumber;
        private String speaker;
        private String action;
        private String sellerId;
        private String sellerStoreName;
        private String productId;
        private String productTitle;
        private OfferProposal proposal;
        private String agentReasoning;
        private Instant timestamp = Instant.now();

        public Builder turnNumber(int turnNumber) { this.turnNumber = turnNumber; return this; }
        public Builder roundNumber(int roundNumber) { this.roundNumber = roundNumber; return this; }
        public Builder speaker(String speaker) { this.speaker = speaker; return this; }
        public Builder action(String action) { this.action = action; return this; }
        public Builder sellerId(String sellerId) { this.sellerId = sellerId; return this; }
        public Builder sellerStoreName(String sellerStoreName) { this.sellerStoreName = sellerStoreName; return this; }
        public Builder productId(String productId) { this.productId = productId; return this; }
        public Builder productTitle(String productTitle) { this.productTitle = productTitle; return this; }
        public Builder proposal(OfferProposal proposal) { this.proposal = proposal; return this; }
        public Builder agentReasoning(String agentReasoning) { this.agentReasoning = agentReasoning; return this; }
        public Builder timestamp(Instant timestamp) { this.timestamp = timestamp; return this; }

        public Turn build() {
            Turn t = new Turn(turnNumber, speaker, action, proposal, agentReasoning, timestamp);
            t.setRoundNumber(roundNumber);
            t.setSellerId(sellerId);
            t.setSellerStoreName(sellerStoreName);
            t.setProductId(productId);
            t.setProductTitle(productTitle);
            return t;
        }
    }

    public int getTurnNumber() { return turnNumber; }
    public void setTurnNumber(int turnNumber) { this.turnNumber = turnNumber; }
    public int getRoundNumber() { return roundNumber; }
    public void setRoundNumber(int roundNumber) { this.roundNumber = roundNumber; }
    public String getSpeaker() { return speaker; }
    public void setSpeaker(String speaker) { this.speaker = speaker; }
    public String getAction() { return action; }
    public void setAction(String action) { this.action = action; }
    public String getSellerId() { return sellerId; }
    public void setSellerId(String sellerId) { this.sellerId = sellerId; }
    public String getSellerStoreName() { return sellerStoreName; }
    public void setSellerStoreName(String sellerStoreName) { this.sellerStoreName = sellerStoreName; }
    public String getProductId() { return productId; }
    public void setProductId(String productId) { this.productId = productId; }
    public String getProductTitle() { return productTitle; }
    public void setProductTitle(String productTitle) { this.productTitle = productTitle; }
    public OfferProposal getProposal() { return proposal; }
    public void setProposal(OfferProposal proposal) { this.proposal = proposal; }
    public String getAgentReasoning() { return agentReasoning; }
    public void setAgentReasoning(String agentReasoning) { this.agentReasoning = agentReasoning; }
    public Instant getTimestamp() { return timestamp; }
    public void setTimestamp(Instant timestamp) { this.timestamp = timestamp; }
}
