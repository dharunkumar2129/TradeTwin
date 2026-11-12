package com.tradetwin.catalog.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;

public class ProductPublicDto {
    private String id;
    private String sellerId;
    private String title;
    private String brand;
    private String sku;
    private String description;
    private String category;
    private String condition;
    private BigDecimal listPrice;
    private String currency;
    private int availableInventory;
    private int minOrderQuantity;
    private int maxOrderQuantity;
    private BigDecimal shippingCost;
    private int estimatedDeliveryDays;
    private String warrantyInfo;
    private String returnPolicy;
    private Map<String, String> specifications;
    private boolean autoNegotiate;
    private int maxNegotiationRounds;
    private boolean allowWarrantySweeteners;
    private boolean allowFreeShipping;
    private List<String> images;
    private List<String> tags;
    private Instant createdAt;

    public ProductPublicDto() {}

    public ProductPublicDto(String id, String sellerId, String title, String brand, String sku,
                            String description, String category, String condition, BigDecimal listPrice,
                            String currency, int availableInventory, int minOrderQuantity, int maxOrderQuantity,
                            BigDecimal shippingCost, int estimatedDeliveryDays, String warrantyInfo,
                            String returnPolicy, Map<String, String> specifications, boolean autoNegotiate,
                            int maxNegotiationRounds, boolean allowWarrantySweeteners, boolean allowFreeShipping,
                            List<String> images, List<String> tags, Instant createdAt) {
        this.id = id;
        this.sellerId = sellerId;
        this.title = title;
        this.brand = brand;
        this.sku = sku;
        this.description = description;
        this.category = category;
        this.condition = condition;
        this.listPrice = listPrice;
        this.currency = currency;
        this.availableInventory = availableInventory;
        this.minOrderQuantity = minOrderQuantity;
        this.maxOrderQuantity = maxOrderQuantity;
        this.shippingCost = shippingCost;
        this.estimatedDeliveryDays = estimatedDeliveryDays;
        this.warrantyInfo = warrantyInfo;
        this.returnPolicy = returnPolicy;
        this.specifications = specifications;
        this.autoNegotiate = autoNegotiate;
        this.maxNegotiationRounds = maxNegotiationRounds;
        this.allowWarrantySweeteners = allowWarrantySweeteners;
        this.allowFreeShipping = allowFreeShipping;
        this.images = images;
        this.tags = tags;
        this.createdAt = createdAt;
    }

    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private String id;
        private String sellerId;
        private String title;
        private String brand;
        private String sku;
        private String description;
        private String category;
        private String condition;
        private BigDecimal listPrice;
        private String currency;
        private int availableInventory;
        private int minOrderQuantity = 1;
        private int maxOrderQuantity = 10;
        private BigDecimal shippingCost = BigDecimal.ZERO;
        private int estimatedDeliveryDays = 3;
        private String warrantyInfo;
        private String returnPolicy;
        private Map<String, String> specifications;
        private boolean autoNegotiate;
        private int maxNegotiationRounds;
        private boolean allowWarrantySweeteners;
        private boolean allowFreeShipping;
        private List<String> images;
        private List<String> tags;
        private Instant createdAt;

        public Builder id(String id) { this.id = id; return this; }
        public Builder sellerId(String sellerId) { this.sellerId = sellerId; return this; }
        public Builder title(String title) { this.title = title; return this; }
        public Builder brand(String brand) { this.brand = brand; return this; }
        public Builder sku(String sku) { this.sku = sku; return this; }
        public Builder description(String description) { this.description = description; return this; }
        public Builder category(String category) { this.category = category; return this; }
        public Builder condition(String condition) { this.condition = condition; return this; }
        public Builder listPrice(BigDecimal listPrice) { this.listPrice = listPrice; return this; }
        public Builder currency(String currency) { this.currency = currency; return this; }
        public Builder availableInventory(int availableInventory) { this.availableInventory = availableInventory; return this; }
        public Builder minOrderQuantity(int minOrderQuantity) { this.minOrderQuantity = minOrderQuantity; return this; }
        public Builder maxOrderQuantity(int maxOrderQuantity) { this.maxOrderQuantity = maxOrderQuantity; return this; }
        public Builder shippingCost(BigDecimal shippingCost) { this.shippingCost = shippingCost; return this; }
        public Builder estimatedDeliveryDays(int estimatedDeliveryDays) { this.estimatedDeliveryDays = estimatedDeliveryDays; return this; }
        public Builder warrantyInfo(String warrantyInfo) { this.warrantyInfo = warrantyInfo; return this; }
        public Builder returnPolicy(String returnPolicy) { this.returnPolicy = returnPolicy; return this; }
        public Builder specifications(Map<String, String> specifications) { this.specifications = specifications; return this; }
        public Builder autoNegotiate(boolean autoNegotiate) { this.autoNegotiate = autoNegotiate; return this; }
        public Builder maxNegotiationRounds(int maxNegotiationRounds) { this.maxNegotiationRounds = maxNegotiationRounds; return this; }
        public Builder allowWarrantySweeteners(boolean allowWarrantySweeteners) { this.allowWarrantySweeteners = allowWarrantySweeteners; return this; }
        public Builder allowFreeShipping(boolean allowFreeShipping) { this.allowFreeShipping = allowFreeShipping; return this; }
        public Builder images(List<String> images) { this.images = images; return this; }
        public Builder tags(List<String> tags) { this.tags = tags; return this; }
        public Builder createdAt(Instant createdAt) { this.createdAt = createdAt; return this; }

        public ProductPublicDto build() {
            return new ProductPublicDto(id, sellerId, title, brand, sku, description, category, condition,
                    listPrice, currency, availableInventory, minOrderQuantity, maxOrderQuantity, shippingCost,
                    estimatedDeliveryDays, warrantyInfo, returnPolicy, specifications, autoNegotiate,
                    maxNegotiationRounds, allowWarrantySweeteners, allowFreeShipping, images, tags, createdAt);
        }
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getSellerId() { return sellerId; }
    public void setSellerId(String sellerId) { this.sellerId = sellerId; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getBrand() { return brand; }
    public void setBrand(String brand) { this.brand = brand; }
    public String getSku() { return sku; }
    public void setSku(String sku) { this.sku = sku; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public String getCondition() { return condition; }
    public void setCondition(String condition) { this.condition = condition; }
    public BigDecimal getListPrice() { return listPrice; }
    public void setListPrice(BigDecimal listPrice) { this.listPrice = listPrice; }
    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }
    public int getAvailableInventory() { return availableInventory; }
    public void setAvailableInventory(int availableInventory) { this.availableInventory = availableInventory; }
    public int getMinOrderQuantity() { return minOrderQuantity; }
    public void setMinOrderQuantity(int minOrderQuantity) { this.minOrderQuantity = minOrderQuantity; }
    public int getMaxOrderQuantity() { return maxOrderQuantity; }
    public void setMaxOrderQuantity(int maxOrderQuantity) { this.maxOrderQuantity = maxOrderQuantity; }
    public BigDecimal getShippingCost() { return shippingCost; }
    public void setShippingCost(BigDecimal shippingCost) { this.shippingCost = shippingCost; }
    public int getEstimatedDeliveryDays() { return estimatedDeliveryDays; }
    public void setEstimatedDeliveryDays(int estimatedDeliveryDays) { this.estimatedDeliveryDays = estimatedDeliveryDays; }
    public String getWarrantyInfo() { return warrantyInfo; }
    public void setWarrantyInfo(String warrantyInfo) { this.warrantyInfo = warrantyInfo; }
    public String getReturnPolicy() { return returnPolicy; }
    public void setReturnPolicy(String returnPolicy) { this.returnPolicy = returnPolicy; }
    public Map<String, String> getSpecifications() { return specifications; }
    public void setSpecifications(Map<String, String> specifications) { this.specifications = specifications; }
    public boolean isAutoNegotiate() { return autoNegotiate; }
    public void setAutoNegotiate(boolean autoNegotiate) { this.autoNegotiate = autoNegotiate; }
    public int getMaxNegotiationRounds() { return maxNegotiationRounds; }
    public void setMaxNegotiationRounds(int maxNegotiationRounds) { this.maxNegotiationRounds = maxNegotiationRounds; }
    public boolean isAllowWarrantySweeteners() { return allowWarrantySweeteners; }
    public void setAllowWarrantySweeteners(boolean allowWarrantySweeteners) { this.allowWarrantySweeteners = allowWarrantySweeteners; }
    public boolean isAllowFreeShipping() { return allowFreeShipping; }
    public void setAllowFreeShipping(boolean allowFreeShipping) { this.allowFreeShipping = allowFreeShipping; }
    public List<String> getImages() { return images; }
    public void setImages(List<String> images) { this.images = images; }
    public List<String> getTags() { return tags; }
    public void setTags(List<String> tags) { this.tags = tags; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
