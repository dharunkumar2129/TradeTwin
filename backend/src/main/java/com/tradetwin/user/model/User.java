package com.tradetwin.user.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Document(collection = "users")
public class User {

    @Id
    private String id;

    @Indexed(unique = true)
    private String email;

    private String passwordHash;

    private String fullName;

    private UserRole role = UserRole.ROLE_BUYER;

    private Double reputationScore = 100.0;

    private String defaultCurrency = "USD";

    private ShippingAddress shippingAddress;

    private ShoppingPreferences shoppingPreferences;

    private Instant createdAt = Instant.now();

    private Instant updatedAt = Instant.now();

    public User() {}

    public User(String id, String email, String passwordHash, String fullName, UserRole role,
                Double reputationScore, String defaultCurrency, ShippingAddress shippingAddress,
                ShoppingPreferences shoppingPreferences, Instant createdAt, Instant updatedAt) {
        this.id = id;
        this.email = email;
        this.passwordHash = passwordHash;
        this.fullName = fullName;
        this.role = role != null ? role : UserRole.ROLE_BUYER;
        this.reputationScore = reputationScore != null ? reputationScore : 100.0;
        this.defaultCurrency = defaultCurrency != null ? defaultCurrency : "USD";
        this.shippingAddress = shippingAddress;
        this.shoppingPreferences = shoppingPreferences != null ? shoppingPreferences : new ShoppingPreferences();
        this.createdAt = createdAt != null ? createdAt : Instant.now();
        this.updatedAt = updatedAt != null ? updatedAt : Instant.now();
    }

    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private String id;
        private String email;
        private String passwordHash;
        private String fullName;
        private UserRole role = UserRole.ROLE_BUYER;
        private Double reputationScore = 100.0;
        private String defaultCurrency = "USD";
        private ShippingAddress shippingAddress;
        private ShoppingPreferences shoppingPreferences = new ShoppingPreferences();
        private Instant createdAt = Instant.now();
        private Instant updatedAt = Instant.now();

        public Builder id(String id) { this.id = id; return this; }
        public Builder email(String email) { this.email = email; return this; }
        public Builder passwordHash(String passwordHash) { this.passwordHash = passwordHash; return this; }
        public Builder fullName(String fullName) { this.fullName = fullName; return this; }
        public Builder role(UserRole role) { this.role = role; return this; }
        public Builder reputationScore(Double reputationScore) { this.reputationScore = reputationScore; return this; }
        public Builder defaultCurrency(String defaultCurrency) { this.defaultCurrency = defaultCurrency; return this; }
        public Builder shippingAddress(ShippingAddress shippingAddress) { this.shippingAddress = shippingAddress; return this; }
        public Builder shoppingPreferences(ShoppingPreferences shoppingPreferences) { this.shoppingPreferences = shoppingPreferences; return this; }
        public Builder createdAt(Instant createdAt) { this.createdAt = createdAt; return this; }
        public Builder updatedAt(Instant updatedAt) { this.updatedAt = updatedAt; return this; }

        public User build() {
            return new User(id, email, passwordHash, fullName, role, reputationScore, defaultCurrency, shippingAddress, shoppingPreferences, createdAt, updatedAt);
        }
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }
    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }
    public UserRole getRole() { return role; }
    public void setRole(UserRole role) { this.role = role; }
    public Double getReputationScore() { return reputationScore; }
    public void setReputationScore(Double reputationScore) { this.reputationScore = reputationScore; }
    public String getDefaultCurrency() { return defaultCurrency; }
    public void setDefaultCurrency(String defaultCurrency) { this.defaultCurrency = defaultCurrency; }
    public ShippingAddress getShippingAddress() { return shippingAddress; }
    public void setShippingAddress(ShippingAddress shippingAddress) { this.shippingAddress = shippingAddress; }
    public ShoppingPreferences getShoppingPreferences() { return shoppingPreferences; }
    public void setShoppingPreferences(ShoppingPreferences shoppingPreferences) { this.shoppingPreferences = shoppingPreferences; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
