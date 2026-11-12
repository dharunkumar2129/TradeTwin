package com.tradetwin.catalog.model;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Document(collection = "products")
public class Product {

    @Id
    private String id;

    @Indexed
    @NotBlank(message = "Seller ID is required")
    private String sellerId;

    @NotBlank(message = "Title is required")
    private String title;

    private String brand = "";

    private String sku;

    @NotBlank(message = "Description is required")
    private String description;

    @NotBlank(message = "Category is required")
    private String category;

    private String condition = "Brand New";

    @NotNull(message = "List price is required")
    @DecimalMin(value = "0.01", message = "List price must be greater than zero")
    private BigDecimal listPrice;

    private String currency = "USD";

    @Min(value = 0, message = "Inventory count cannot be negative")
    private int inventoryCount = 10;

    private int reservedCount = 0;

    @Min(value = 1, message = "Minimum order quantity must be at least 1")
    private int minOrderQuantity = 1;

    @Min(value = 1, message = "Maximum order quantity must be at least 1")
    private int maxOrderQuantity = 10;

    private BigDecimal shippingCost = BigDecimal.ZERO;

    private int estimatedDeliveryDays = 3;

    private String warrantyInfo = "12 Months Limited Warranty";

    private String returnPolicy = "30 Days Return Policy";

    private Map<String, String> specifications = new HashMap<>();

    @Valid
    @NotNull(message = "Negotiation policy is required")
    private NegotiationPolicy negotiationPolicy;

    private List<String> images = new ArrayList<>();
    private List<String> tags = new ArrayList<>();
    private boolean active = true;
    private Instant createdAt = Instant.now();
    private Instant updatedAt = Instant.now();

    public Product() {}

    public Product(String id, String sellerId, String title, String brand, String sku, String description,
                   String category, String condition, BigDecimal listPrice, String currency,
                   int inventoryCount, int reservedCount, int minOrderQuantity, int maxOrderQuantity,
                   BigDecimal shippingCost, int estimatedDeliveryDays, String warrantyInfo, String returnPolicy,
                   Map<String, String> specifications, NegotiationPolicy negotiationPolicy,
                   List<String> images, List<String> tags, boolean active, Instant createdAt, Instant updatedAt) {
        this.id = id;
        this.sellerId = sellerId;
        this.title = title;
        this.brand = brand != null ? brand : "";
        this.sku = sku;
        this.description = description;
        this.category = category;
        this.condition = condition != null ? condition : "Brand New";
        this.listPrice = listPrice;
        this.currency = currency != null ? currency : "USD";
        this.inventoryCount = inventoryCount;
        this.reservedCount = reservedCount;
        this.minOrderQuantity = minOrderQuantity > 0 ? minOrderQuantity : 1;
        this.maxOrderQuantity = maxOrderQuantity > 0 ? maxOrderQuantity : 10;
        this.shippingCost = shippingCost != null ? shippingCost : BigDecimal.ZERO;
        this.estimatedDeliveryDays = estimatedDeliveryDays > 0 ? estimatedDeliveryDays : 3;
        this.warrantyInfo = warrantyInfo != null ? warrantyInfo : "12 Months Limited Warranty";
        this.returnPolicy = returnPolicy != null ? returnPolicy : "30 Days Return Policy";
        this.specifications = specifications != null ? specifications : new HashMap<>();
        this.negotiationPolicy = negotiationPolicy;
        this.images = images != null ? images : new ArrayList<>();
        this.tags = tags != null ? tags : new ArrayList<>();
        this.active = active;
        this.createdAt = createdAt != null ? createdAt : Instant.now();
        this.updatedAt = updatedAt != null ? updatedAt : Instant.now();
    }

    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private String id;
        private String sellerId;
        private String title;
        private String brand = "";
        private String sku;
        private String description;
        private String category;
        private String condition = "Brand New";
        private BigDecimal listPrice;
        private String currency = "USD";
        private int inventoryCount = 10;
        private int reservedCount = 0;
        private int minOrderQuantity = 1;
        private int maxOrderQuantity = 10;
        private BigDecimal shippingCost = BigDecimal.ZERO;
        private int estimatedDeliveryDays = 3;
        private String warrantyInfo = "12 Months Limited Warranty";
        private String returnPolicy = "30 Days Return Policy";
        private Map<String, String> specifications = new HashMap<>();
        private NegotiationPolicy negotiationPolicy;
        private List<String> images = new ArrayList<>();
        private List<String> tags = new ArrayList<>();
        private boolean active = true;
        private Instant createdAt = Instant.now();
        private Instant updatedAt = Instant.now();

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
        public Builder inventoryCount(int inventoryCount) { this.inventoryCount = inventoryCount; return this; }
        public Builder reservedCount(int reservedCount) { this.reservedCount = reservedCount; return this; }
        public Builder minOrderQuantity(int minOrderQuantity) { this.minOrderQuantity = minOrderQuantity; return this; }
        public Builder maxOrderQuantity(int maxOrderQuantity) { this.maxOrderQuantity = maxOrderQuantity; return this; }
        public Builder shippingCost(BigDecimal shippingCost) { this.shippingCost = shippingCost; return this; }
        public Builder estimatedDeliveryDays(int estimatedDeliveryDays) { this.estimatedDeliveryDays = estimatedDeliveryDays; return this; }
        public Builder warrantyInfo(String warrantyInfo) { this.warrantyInfo = warrantyInfo; return this; }
        public Builder returnPolicy(String returnPolicy) { this.returnPolicy = returnPolicy; return this; }
        public Builder specifications(Map<String, String> specifications) { this.specifications = specifications; return this; }
        public Builder negotiationPolicy(NegotiationPolicy policy) { this.negotiationPolicy = policy; return this; }
        public Builder images(List<String> images) { this.images = images; return this; }
        public Builder tags(List<String> tags) { this.tags = tags; return this; }
        public Builder active(boolean active) { this.active = active; return this; }
        public Builder createdAt(Instant createdAt) { this.createdAt = createdAt; return this; }
        public Builder updatedAt(Instant updatedAt) { this.updatedAt = updatedAt; return this; }

        public Product build() {
            return new Product(id, sellerId, title, brand, sku, description, category, condition, listPrice, currency,
                    inventoryCount, reservedCount, minOrderQuantity, maxOrderQuantity, shippingCost,
                    estimatedDeliveryDays, warrantyInfo, returnPolicy, specifications, negotiationPolicy,
                    images, tags, active, createdAt, updatedAt);
        }
    }

    public int getAvailableInventory() {
        return Math.max(0, inventoryCount - reservedCount);
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
    public int getInventoryCount() { return inventoryCount; }
    public void setInventoryCount(int inventoryCount) { this.inventoryCount = inventoryCount; }
    public int getReservedCount() { return reservedCount; }
    public void setReservedCount(int reservedCount) { this.reservedCount = reservedCount; }
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
    public NegotiationPolicy getNegotiationPolicy() { return negotiationPolicy; }
    public void setNegotiationPolicy(NegotiationPolicy negotiationPolicy) { this.negotiationPolicy = negotiationPolicy; }
    public List<String> getImages() { return images; }
    public void setImages(List<String> images) { this.images = images; }
    public List<String> getTags() { return tags; }
    public void setTags(List<String> tags) { this.tags = tags; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
