package com.tradetwin.negotiation.model;

import com.tradetwin.deal.model.AgreementSeal;
import com.tradetwin.deal.model.FairDealMetrics;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Document(collection = "negotiations")
public class Negotiation {

    @Id
    private String id;

    @Indexed
    private String productId;

    @Indexed
    private String buyerId;

    @Indexed
    private String sellerId;

    private NegotiationStatus status = NegotiationStatus.CREATED;
    private int roundCount = 0;
    private int maxRounds = 3;
    private BuyerPreferences buyerPreferences;
    private OfferProposal currentOffer;
    private List<Turn> turns = new ArrayList<>();
    private List<NegotiationOffer> offers = new ArrayList<>();
    private List<String> participatingSellerIds = new ArrayList<>();
    private List<String> participatingProductIds = new ArrayList<>();
    private FairDealMetrics fairDealMetrics;
    private AgreementSeal agreementSeal;

    // Multi-seller & conversational metadata (additive)
    private String buyerPrompt;
    private String productTitle;
    private String productImage;
    private String currentSellerStoreName;
    private int currentSellerIndex = 0;
    private List<SellerCandidateSummary> sellerQueue = new ArrayList<>();
    private List<SellerAttemptRecord> failedSellerAttempts = new ArrayList<>();
    private String fallbackNotice;
    private String noDealSummary;
    private List<SellerCandidateSummary> alternatives = new ArrayList<>();

    private Instant createdAt = Instant.now();
    private Instant updatedAt = Instant.now();

    public Negotiation() {}

    public Negotiation(String id, String productId, String buyerId, String sellerId,
                       NegotiationStatus status, int roundCount, int maxRounds,
                       BuyerPreferences buyerPreferences, OfferProposal currentOffer,
                       List<Turn> turns, List<NegotiationOffer> offers,
                       List<String> participatingSellerIds, List<String> participatingProductIds,
                       FairDealMetrics fairDealMetrics, AgreementSeal agreementSeal,
                       Instant createdAt, Instant updatedAt) {
        this.id = id;
        this.productId = productId;
        this.buyerId = buyerId;
        this.sellerId = sellerId;
        this.status = status != null ? status : NegotiationStatus.CREATED;
        this.roundCount = roundCount;
        this.maxRounds = maxRounds > 0 ? maxRounds : 3;
        this.buyerPreferences = buyerPreferences;
        this.currentOffer = currentOffer;
        this.turns = turns != null ? turns : new ArrayList<>();
        this.offers = offers != null ? offers : new ArrayList<>();
        this.participatingSellerIds = participatingSellerIds != null ? participatingSellerIds : new ArrayList<>();
        this.participatingProductIds = participatingProductIds != null ? participatingProductIds : new ArrayList<>();
        this.fairDealMetrics = fairDealMetrics;
        this.agreementSeal = agreementSeal;
        this.createdAt = createdAt != null ? createdAt : Instant.now();
        this.updatedAt = updatedAt != null ? updatedAt : Instant.now();
    }

    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private String id;
        private String productId;
        private String buyerId;
        private String sellerId;
        private NegotiationStatus status = NegotiationStatus.CREATED;
        private int roundCount = 0;
        private int maxRounds = 3;
        private BuyerPreferences buyerPreferences;
        private OfferProposal currentOffer;
        private List<Turn> turns = new ArrayList<>();
        private List<NegotiationOffer> offers = new ArrayList<>();
        private List<String> participatingSellerIds = new ArrayList<>();
        private List<String> participatingProductIds = new ArrayList<>();
        private FairDealMetrics fairDealMetrics;
        private AgreementSeal agreementSeal;
        private Instant createdAt = Instant.now();
        private Instant updatedAt = Instant.now();

        public Builder id(String id) { this.id = id; return this; }
        public Builder productId(String productId) { this.productId = productId; return this; }
        public Builder buyerId(String buyerId) { this.buyerId = buyerId; return this; }
        public Builder sellerId(String sellerId) { this.sellerId = sellerId; return this; }
        public Builder status(NegotiationStatus status) { this.status = status; return this; }
        public Builder roundCount(int roundCount) { this.roundCount = roundCount; return this; }
        public Builder maxRounds(int maxRounds) { this.maxRounds = maxRounds; return this; }
        public Builder buyerPreferences(BuyerPreferences buyerPreferences) { this.buyerPreferences = buyerPreferences; return this; }
        public Builder currentOffer(OfferProposal currentOffer) { this.currentOffer = currentOffer; return this; }
        public Builder turns(List<Turn> turns) { this.turns = turns; return this; }
        public Builder offers(List<NegotiationOffer> offers) { this.offers = offers; return this; }
        public Builder participatingSellerIds(List<String> participatingSellerIds) { this.participatingSellerIds = participatingSellerIds; return this; }
        public Builder participatingProductIds(List<String> participatingProductIds) { this.participatingProductIds = participatingProductIds; return this; }
        public Builder fairDealMetrics(FairDealMetrics fairDealMetrics) { this.fairDealMetrics = fairDealMetrics; return this; }
        public Builder agreementSeal(AgreementSeal agreementSeal) { this.agreementSeal = agreementSeal; return this; }
        public Builder createdAt(Instant createdAt) { this.createdAt = createdAt; return this; }
        public Builder updatedAt(Instant updatedAt) { this.updatedAt = updatedAt; return this; }

        public Negotiation build() {
            return new Negotiation(id, productId, buyerId, sellerId, status, roundCount, maxRounds,
                    buyerPreferences, currentOffer, turns, offers, participatingSellerIds, participatingProductIds,
                    fairDealMetrics, agreementSeal, createdAt, updatedAt);
        }
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getProductId() { return productId; }
    public void setProductId(String productId) { this.productId = productId; }
    public String getBuyerId() { return buyerId; }
    public void setBuyerId(String buyerId) { this.buyerId = buyerId; }
    public String getSellerId() { return sellerId; }
    public void setSellerId(String sellerId) { this.sellerId = sellerId; }
    public NegotiationStatus getStatus() { return status; }
    public void setStatus(NegotiationStatus status) { this.status = status; }
    public int getRoundCount() { return roundCount; }
    public void setRoundCount(int roundCount) { this.roundCount = roundCount; }
    public int getMaxRounds() { return maxRounds; }
    public void setMaxRounds(int maxRounds) { this.maxRounds = maxRounds; }
    public BuyerPreferences getBuyerPreferences() { return buyerPreferences; }
    public void setBuyerPreferences(BuyerPreferences buyerPreferences) { this.buyerPreferences = buyerPreferences; }
    public OfferProposal getCurrentOffer() { return currentOffer; }
    public void setCurrentOffer(OfferProposal currentOffer) { this.currentOffer = currentOffer; }
    public List<Turn> getTurns() { return turns; }
    public void setTurns(List<Turn> turns) { this.turns = turns; }
    public List<NegotiationOffer> getOffers() { return offers; }
    public void setOffers(List<NegotiationOffer> offers) { this.offers = offers; }
    public List<String> getParticipatingSellerIds() { return participatingSellerIds; }
    public void setParticipatingSellerIds(List<String> participatingSellerIds) { this.participatingSellerIds = participatingSellerIds; }
    public List<String> getParticipatingProductIds() { return participatingProductIds; }
    public void setParticipatingProductIds(List<String> participatingProductIds) { this.participatingProductIds = participatingProductIds; }
    public FairDealMetrics getFairDealMetrics() { return fairDealMetrics; }
    public void setFairDealMetrics(FairDealMetrics fairDealMetrics) { this.fairDealMetrics = fairDealMetrics; }
    public AgreementSeal getAgreementSeal() { return agreementSeal; }
    public void setAgreementSeal(AgreementSeal agreementSeal) { this.agreementSeal = agreementSeal; }
    public String getBuyerPrompt() { return buyerPrompt; }
    public void setBuyerPrompt(String buyerPrompt) { this.buyerPrompt = buyerPrompt; }
    public String getProductTitle() { return productTitle; }
    public void setProductTitle(String productTitle) { this.productTitle = productTitle; }
    public String getProductImage() { return productImage; }
    public void setProductImage(String productImage) { this.productImage = productImage; }
    public String getCurrentSellerStoreName() { return currentSellerStoreName; }
    public void setCurrentSellerStoreName(String currentSellerStoreName) { this.currentSellerStoreName = currentSellerStoreName; }
    public int getCurrentSellerIndex() { return currentSellerIndex; }
    public void setCurrentSellerIndex(int currentSellerIndex) { this.currentSellerIndex = currentSellerIndex; }
    public List<SellerCandidateSummary> getSellerQueue() { return sellerQueue; }
    public void setSellerQueue(List<SellerCandidateSummary> sellerQueue) { this.sellerQueue = sellerQueue; }
    public List<SellerAttemptRecord> getFailedSellerAttempts() { return failedSellerAttempts; }
    public void setFailedSellerAttempts(List<SellerAttemptRecord> failedSellerAttempts) { this.failedSellerAttempts = failedSellerAttempts; }
    public String getFallbackNotice() { return fallbackNotice; }
    public void setFallbackNotice(String fallbackNotice) { this.fallbackNotice = fallbackNotice; }
    public String getNoDealSummary() { return noDealSummary; }
    public void setNoDealSummary(String noDealSummary) { this.noDealSummary = noDealSummary; }
    public List<SellerCandidateSummary> getAlternatives() { return alternatives; }
    public void setAlternatives(List<SellerCandidateSummary> alternatives) { this.alternatives = alternatives; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
