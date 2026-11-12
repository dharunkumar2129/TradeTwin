package com.tradetwin.seller.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Document(collection = "seller_profiles")
public class SellerProfile {

    @Id
    private String id;

    @Indexed(unique = true)
    private String userId;

    private String storeName;

    private String description;

    private String logoUrl;

    private BusinessContact businessContact;

    private List<String> shippingRegions = new ArrayList<>(List.of("United States", "Canada"));

    private ShippingTerms defaultShippingTerms = new ShippingTerms();

    private NegotiationStrategy defaultStrategy = NegotiationStrategy.BALANCED;

    private boolean autoNegotiateEnabled = true;

    private double minimumReputationRequired = 70.0;

    private boolean onboardingCompleted = false;

    private int onboardingStep = 1;

    private Instant createdAt = Instant.now();

    private Instant updatedAt = Instant.now();

    public SellerProfile() {}

    public SellerProfile(String id, String userId, String storeName, String description,
                         String logoUrl, BusinessContact businessContact, List<String> shippingRegions,
                         ShippingTerms defaultShippingTerms, NegotiationStrategy defaultStrategy,
                         boolean autoNegotiateEnabled, double minimumReputationRequired,
                         boolean onboardingCompleted, int onboardingStep,
                         Instant createdAt, Instant updatedAt) {
        this.id = id;
        this.userId = userId;
        this.storeName = storeName;
        this.description = description;
        this.logoUrl = logoUrl;
        this.businessContact = businessContact != null ? businessContact : new BusinessContact();
        this.shippingRegions = shippingRegions != null ? shippingRegions : new ArrayList<>(List.of("United States", "Canada"));
        this.defaultShippingTerms = defaultShippingTerms != null ? defaultShippingTerms : new ShippingTerms();
        this.defaultStrategy = defaultStrategy != null ? defaultStrategy : NegotiationStrategy.BALANCED;
        this.autoNegotiateEnabled = autoNegotiateEnabled;
        this.minimumReputationRequired = minimumReputationRequired;
        this.onboardingCompleted = onboardingCompleted;
        this.onboardingStep = onboardingStep > 0 ? onboardingStep : 1;
        this.createdAt = createdAt != null ? createdAt : Instant.now();
        this.updatedAt = updatedAt != null ? updatedAt : Instant.now();
    }

    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private String id;
        private String userId;
        private String storeName;
        private String description;
        private String logoUrl;
        private BusinessContact businessContact = new BusinessContact();
        private List<String> shippingRegions = new ArrayList<>(List.of("United States", "Canada"));
        private ShippingTerms defaultShippingTerms = new ShippingTerms();
        private NegotiationStrategy defaultStrategy = NegotiationStrategy.BALANCED;
        private boolean autoNegotiateEnabled = true;
        private double minimumReputationRequired = 70.0;
        private boolean onboardingCompleted = false;
        private int onboardingStep = 1;
        private Instant createdAt = Instant.now();
        private Instant updatedAt = Instant.now();

        public Builder id(String id) { this.id = id; return this; }
        public Builder userId(String userId) { this.userId = userId; return this; }
        public Builder storeName(String storeName) { this.storeName = storeName; return this; }
        public Builder description(String description) { this.description = description; return this; }
        public Builder logoUrl(String logoUrl) { this.logoUrl = logoUrl; return this; }
        public Builder businessContact(BusinessContact businessContact) { this.businessContact = businessContact; return this; }
        public Builder shippingRegions(List<String> shippingRegions) { this.shippingRegions = shippingRegions; return this; }
        public Builder defaultShippingTerms(ShippingTerms defaultShippingTerms) { this.defaultShippingTerms = defaultShippingTerms; return this; }
        public Builder defaultStrategy(NegotiationStrategy defaultStrategy) { this.defaultStrategy = defaultStrategy; return this; }
        public Builder autoNegotiateEnabled(boolean autoNegotiateEnabled) { this.autoNegotiateEnabled = autoNegotiateEnabled; return this; }
        public Builder minimumReputationRequired(double minimumReputationRequired) { this.minimumReputationRequired = minimumReputationRequired; return this; }
        public Builder onboardingCompleted(boolean onboardingCompleted) { this.onboardingCompleted = onboardingCompleted; return this; }
        public Builder onboardingStep(int onboardingStep) { this.onboardingStep = onboardingStep; return this; }
        public Builder createdAt(Instant createdAt) { this.createdAt = createdAt; return this; }
        public Builder updatedAt(Instant updatedAt) { this.updatedAt = updatedAt; return this; }

        public SellerProfile build() {
            return new SellerProfile(id, userId, storeName, description, logoUrl, businessContact,
                    shippingRegions, defaultShippingTerms, defaultStrategy, autoNegotiateEnabled,
                    minimumReputationRequired, onboardingCompleted, onboardingStep, createdAt, updatedAt);
        }
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    public String getStoreName() { return storeName; }
    public void setStoreName(String storeName) { this.storeName = storeName; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getLogoUrl() { return logoUrl; }
    public void setLogoUrl(String logoUrl) { this.logoUrl = logoUrl; }
    public BusinessContact getBusinessContact() { return businessContact; }
    public void setBusinessContact(BusinessContact businessContact) { this.businessContact = businessContact; }
    public List<String> getShippingRegions() { return shippingRegions; }
    public void setShippingRegions(List<String> shippingRegions) { this.shippingRegions = shippingRegions; }
    public ShippingTerms getDefaultShippingTerms() { return defaultShippingTerms; }
    public void setDefaultShippingTerms(ShippingTerms defaultShippingTerms) { this.defaultShippingTerms = defaultShippingTerms; }
    public NegotiationStrategy getDefaultStrategy() { return defaultStrategy; }
    public void setDefaultStrategy(NegotiationStrategy defaultStrategy) { this.defaultStrategy = defaultStrategy; }
    public boolean isAutoNegotiateEnabled() { return autoNegotiateEnabled; }
    public void setAutoNegotiateEnabled(boolean autoNegotiateEnabled) { this.autoNegotiateEnabled = autoNegotiateEnabled; }
    public double getMinimumReputationRequired() { return minimumReputationRequired; }
    public void setMinimumReputationRequired(double minimumReputationRequired) { this.minimumReputationRequired = minimumReputationRequired; }
    public boolean isOnboardingCompleted() { return onboardingCompleted; }
    public void setOnboardingCompleted(boolean onboardingCompleted) { this.onboardingCompleted = onboardingCompleted; }
    public int getOnboardingStep() { return onboardingStep; }
    public void setOnboardingStep(int onboardingStep) { this.onboardingStep = onboardingStep; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
