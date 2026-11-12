package com.tradetwin.seller.dto;

import com.tradetwin.seller.model.BusinessContact;
import com.tradetwin.seller.model.NegotiationStrategy;
import com.tradetwin.seller.model.ShippingTerms;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.List;

public class SellerOnboardingRequest {

    @NotBlank(message = "Store name is required")
    @Size(max = 100, message = "Store name must be at most 100 characters")
    private String storeName;

    @NotBlank(message = "Store description is required")
    @Size(max = 1000, message = "Description must be at most 1000 characters")
    private String description;

    private String logoUrl;

    private BusinessContact businessContact;

    private List<String> shippingRegions;

    private ShippingTerms defaultShippingTerms;

    private NegotiationStrategy defaultStrategy = NegotiationStrategy.BALANCED;

    private Boolean autoNegotiateEnabled = true;

    private Double minimumReputationRequired = 70.0;

    private Integer onboardingStep = 4;

    private Boolean onboardingCompleted = true;

    public SellerOnboardingRequest() {}

    public SellerOnboardingRequest(String storeName, String description, String logoUrl,
                                   BusinessContact businessContact, List<String> shippingRegions,
                                   ShippingTerms defaultShippingTerms, NegotiationStrategy defaultStrategy,
                                   Boolean autoNegotiateEnabled, Double minimumReputationRequired,
                                   Integer onboardingStep, Boolean onboardingCompleted) {
        this.storeName = storeName;
        this.description = description;
        this.logoUrl = logoUrl;
        this.businessContact = businessContact;
        this.shippingRegions = shippingRegions;
        this.defaultShippingTerms = defaultShippingTerms;
        this.defaultStrategy = defaultStrategy != null ? defaultStrategy : NegotiationStrategy.BALANCED;
        this.autoNegotiateEnabled = autoNegotiateEnabled != null ? autoNegotiateEnabled : true;
        this.minimumReputationRequired = minimumReputationRequired != null ? minimumReputationRequired : 70.0;
        this.onboardingStep = onboardingStep != null ? onboardingStep : 4;
        this.onboardingCompleted = onboardingCompleted != null ? onboardingCompleted : true;
    }

    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private String storeName;
        private String description;
        private String logoUrl;
        private BusinessContact businessContact;
        private List<String> shippingRegions;
        private ShippingTerms defaultShippingTerms;
        private NegotiationStrategy defaultStrategy = NegotiationStrategy.BALANCED;
        private Boolean autoNegotiateEnabled = true;
        private Double minimumReputationRequired = 70.0;
        private Integer onboardingStep = 4;
        private Boolean onboardingCompleted = true;

        public Builder storeName(String storeName) { this.storeName = storeName; return this; }
        public Builder description(String description) { this.description = description; return this; }
        public Builder logoUrl(String logoUrl) { this.logoUrl = logoUrl; return this; }
        public Builder businessContact(BusinessContact businessContact) { this.businessContact = businessContact; return this; }
        public Builder shippingRegions(List<String> shippingRegions) { this.shippingRegions = shippingRegions; return this; }
        public Builder defaultShippingTerms(ShippingTerms defaultShippingTerms) { this.defaultShippingTerms = defaultShippingTerms; return this; }
        public Builder defaultStrategy(NegotiationStrategy defaultStrategy) { this.defaultStrategy = defaultStrategy; return this; }
        public Builder autoNegotiateEnabled(Boolean autoNegotiateEnabled) { this.autoNegotiateEnabled = autoNegotiateEnabled; return this; }
        public Builder minimumReputationRequired(Double minimumReputationRequired) { this.minimumReputationRequired = minimumReputationRequired; return this; }
        public Builder onboardingStep(Integer onboardingStep) { this.onboardingStep = onboardingStep; return this; }
        public Builder onboardingCompleted(Boolean onboardingCompleted) { this.onboardingCompleted = onboardingCompleted; return this; }

        public SellerOnboardingRequest build() {
            return new SellerOnboardingRequest(storeName, description, logoUrl, businessContact,
                    shippingRegions, defaultShippingTerms, defaultStrategy, autoNegotiateEnabled,
                    minimumReputationRequired, onboardingStep, onboardingCompleted);
        }
    }

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
    public Boolean getAutoNegotiateEnabled() { return autoNegotiateEnabled; }
    public void setAutoNegotiateEnabled(Boolean autoNegotiateEnabled) { this.autoNegotiateEnabled = autoNegotiateEnabled; }
    public Double getMinimumReputationRequired() { return minimumReputationRequired; }
    public void setMinimumReputationRequired(Double minimumReputationRequired) { this.minimumReputationRequired = minimumReputationRequired; }
    public Integer getOnboardingStep() { return onboardingStep; }
    public void setOnboardingStep(Integer onboardingStep) { this.onboardingStep = onboardingStep; }
    public Boolean getOnboardingCompleted() { return onboardingCompleted; }
    public void setOnboardingCompleted(Boolean onboardingCompleted) { this.onboardingCompleted = onboardingCompleted; }
}
