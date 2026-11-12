package com.tradetwin.intent.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Document(collection = "buyer_intents")
public class BuyerIntent {

    @Id
    private String id;
    private String buyerId;
    private String rawQuery;
    private String productType;
    private List<String> brandPreferences = new ArrayList<>();
    private Map<String, String> requiredSpecifications = new HashMap<>();
    private List<String> optionalPreferences = new ArrayList<>();

    // Private Financial & Commercial Constraints (Never disclosed to seller agents)
    private BigDecimal maxBudget;
    private BigDecimal targetBudget;
    private int quantity = 1;
    private Integer minWarrantyMonths;
    private Integer maxDeliveryDays;
    private List<String> preferredConditions = new ArrayList<>();
    private String shippingLocation;
    private String currency = "USD";

    // AI Clarification & Status
    private boolean ambiguous;
    private String followUpQuestion;
    private String status = "DRAFT"; // DRAFT, CONFIRMED, ACTIVE, FULFILLED

    // Legacy compatibility fields
    private String category = "Electronics";
    private List<String> keywords = new ArrayList<>();
    private String urgency = "MEDIUM";
    private List<String> keyRequirements = new ArrayList<>();

    private Instant createdAt = Instant.now();
    private Instant updatedAt = Instant.now();

    public BuyerIntent() {}

    public BuyerIntent(String id, String buyerId, String rawQuery, String productType,
                       List<String> brandPreferences, Map<String, String> requiredSpecifications,
                       List<String> optionalPreferences, BigDecimal maxBudget, BigDecimal targetBudget,
                       int quantity, Integer minWarrantyMonths, Integer maxDeliveryDays,
                       List<String> preferredConditions, String shippingLocation, String currency,
                       boolean ambiguous, String followUpQuestion, String status,
                       String category, List<String> keywords, String urgency, List<String> keyRequirements,
                       Instant createdAt, Instant updatedAt) {
        this.id = id;
        this.buyerId = buyerId;
        this.rawQuery = rawQuery;
        this.productType = productType;
        this.brandPreferences = brandPreferences != null ? brandPreferences : new ArrayList<>();
        this.requiredSpecifications = requiredSpecifications != null ? requiredSpecifications : new HashMap<>();
        this.optionalPreferences = optionalPreferences != null ? optionalPreferences : new ArrayList<>();
        this.maxBudget = maxBudget;
        this.targetBudget = targetBudget;
        this.quantity = quantity > 0 ? quantity : 1;
        this.minWarrantyMonths = minWarrantyMonths;
        this.maxDeliveryDays = maxDeliveryDays;
        this.preferredConditions = preferredConditions != null ? preferredConditions : new ArrayList<>();
        this.shippingLocation = shippingLocation;
        this.currency = currency != null ? currency : "USD";
        this.ambiguous = ambiguous;
        this.followUpQuestion = followUpQuestion;
        this.status = status != null ? status : "DRAFT";
        this.category = category != null ? category : "Electronics";
        this.keywords = keywords != null ? keywords : new ArrayList<>();
        this.urgency = urgency != null ? urgency : "MEDIUM";
        this.keyRequirements = keyRequirements != null ? keyRequirements : new ArrayList<>();
        this.createdAt = createdAt != null ? createdAt : Instant.now();
        this.updatedAt = updatedAt != null ? updatedAt : Instant.now();
    }

    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private String id;
        private String buyerId;
        private String rawQuery;
        private String productType;
        private List<String> brandPreferences = new ArrayList<>();
        private Map<String, String> requiredSpecifications = new HashMap<>();
        private List<String> optionalPreferences = new ArrayList<>();
        private BigDecimal maxBudget;
        private BigDecimal targetBudget;
        private int quantity = 1;
        private Integer minWarrantyMonths;
        private Integer maxDeliveryDays;
        private List<String> preferredConditions = new ArrayList<>();
        private String shippingLocation;
        private String currency = "USD";
        private boolean ambiguous;
        private String followUpQuestion;
        private String status = "DRAFT";
        private String category = "Electronics";
        private List<String> keywords = new ArrayList<>();
        private String urgency = "MEDIUM";
        private List<String> keyRequirements = new ArrayList<>();
        private Instant createdAt = Instant.now();
        private Instant updatedAt = Instant.now();

        public Builder id(String id) { this.id = id; return this; }
        public Builder buyerId(String buyerId) { this.buyerId = buyerId; return this; }
        public Builder rawQuery(String rawQuery) { this.rawQuery = rawQuery; return this; }
        public Builder productType(String productType) { this.productType = productType; return this; }
        public Builder brandPreferences(List<String> brandPreferences) { this.brandPreferences = brandPreferences; return this; }
        public Builder requiredSpecifications(Map<String, String> requiredSpecifications) { this.requiredSpecifications = requiredSpecifications; return this; }
        public Builder optionalPreferences(List<String> optionalPreferences) { this.optionalPreferences = optionalPreferences; return this; }
        public Builder maxBudget(BigDecimal maxBudget) { this.maxBudget = maxBudget; return this; }
        public Builder targetBudget(BigDecimal targetBudget) { this.targetBudget = targetBudget; return this; }
        public Builder quantity(int quantity) { this.quantity = quantity; return this; }
        public Builder minWarrantyMonths(Integer minWarrantyMonths) { this.minWarrantyMonths = minWarrantyMonths; return this; }
        public Builder maxDeliveryDays(Integer maxDeliveryDays) { this.maxDeliveryDays = maxDeliveryDays; return this; }
        public Builder preferredConditions(List<String> preferredConditions) { this.preferredConditions = preferredConditions; return this; }
        public Builder shippingLocation(String shippingLocation) { this.shippingLocation = shippingLocation; return this; }
        public Builder currency(String currency) { this.currency = currency; return this; }
        public Builder ambiguous(boolean ambiguous) { this.ambiguous = ambiguous; return this; }
        public Builder followUpQuestion(String followUpQuestion) { this.followUpQuestion = followUpQuestion; return this; }
        public Builder status(String status) { this.status = status; return this; }
        public Builder category(String category) { this.category = category; return this; }
        public Builder keywords(List<String> keywords) { this.keywords = keywords; return this; }
        public Builder urgency(String urgency) { this.urgency = urgency; return this; }
        public Builder keyRequirements(List<String> keyRequirements) { this.keyRequirements = keyRequirements; return this; }
        public Builder createdAt(Instant createdAt) { this.createdAt = createdAt; return this; }
        public Builder updatedAt(Instant updatedAt) { this.updatedAt = updatedAt; return this; }

        public BuyerIntent build() {
            return new BuyerIntent(id, buyerId, rawQuery, productType, brandPreferences, requiredSpecifications,
                    optionalPreferences, maxBudget, targetBudget, quantity, minWarrantyMonths, maxDeliveryDays,
                    preferredConditions, shippingLocation, currency, ambiguous, followUpQuestion, status,
                    category, keywords, urgency, keyRequirements, createdAt, updatedAt);
        }
    }

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getBuyerId() { return buyerId; }
    public void setBuyerId(String buyerId) { this.buyerId = buyerId; }
    public String getRawQuery() { return rawQuery; }
    public void setRawQuery(String rawQuery) { this.rawQuery = rawQuery; }
    public String getProductType() { return productType; }
    public void setProductType(String productType) { this.productType = productType; }
    public List<String> getBrandPreferences() { return brandPreferences; }
    public void setBrandPreferences(List<String> brandPreferences) { this.brandPreferences = brandPreferences; }
    public Map<String, String> getRequiredSpecifications() { return requiredSpecifications; }
    public void setRequiredSpecifications(Map<String, String> requiredSpecifications) { this.requiredSpecifications = requiredSpecifications; }
    public List<String> getOptionalPreferences() { return optionalPreferences; }
    public void setOptionalPreferences(List<String> optionalPreferences) { this.optionalPreferences = optionalPreferences; }
    public BigDecimal getMaxBudget() { return maxBudget; }
    public void setMaxBudget(BigDecimal maxBudget) { this.maxBudget = maxBudget; }
    public BigDecimal getTargetBudget() { return targetBudget; }
    public void setTargetBudget(BigDecimal targetBudget) { this.targetBudget = targetBudget; }
    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) { this.quantity = quantity; }
    public Integer getMinWarrantyMonths() { return minWarrantyMonths; }
    public void setMinWarrantyMonths(Integer minWarrantyMonths) { this.minWarrantyMonths = minWarrantyMonths; }
    public Integer getMaxDeliveryDays() { return maxDeliveryDays; }
    public void setMaxDeliveryDays(Integer maxDeliveryDays) { this.maxDeliveryDays = maxDeliveryDays; }
    public List<String> getPreferredConditions() { return preferredConditions; }
    public void setPreferredConditions(List<String> preferredConditions) { this.preferredConditions = preferredConditions; }
    public String getShippingLocation() { return shippingLocation; }
    public void setShippingLocation(String shippingLocation) { this.shippingLocation = shippingLocation; }
    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }
    public boolean isAmbiguous() { return ambiguous; }
    public void setAmbiguous(boolean ambiguous) { this.ambiguous = ambiguous; }
    public String getFollowUpQuestion() { return followUpQuestion; }
    public void setFollowUpQuestion(String followUpQuestion) { this.followUpQuestion = followUpQuestion; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public List<String> getKeywords() { return keywords; }
    public void setKeywords(List<String> keywords) { this.keywords = keywords; }
    public String getUrgency() { return urgency; }
    public void setUrgency(String urgency) { this.urgency = urgency; }
    public List<String> getKeyRequirements() { return keyRequirements; }
    public void setKeyRequirements(List<String> keyRequirements) { this.keyRequirements = keyRequirements; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
