package com.tradetwin.auth.dto;

import com.tradetwin.user.model.ShippingAddress;
import com.tradetwin.user.model.ShoppingPreferences;
import com.tradetwin.user.model.UserRole;

import java.time.Instant;

public class UserProfileDto {
    private String id;
    private String email;
    private String fullName;
    private UserRole role;
    private Double reputationScore;
    private String defaultCurrency;
    private ShippingAddress shippingAddress;
    private ShoppingPreferences shoppingPreferences;
    private Instant createdAt;
    private Instant updatedAt;

    public UserProfileDto() {}

    public UserProfileDto(String id, String email, String fullName, UserRole role, Double reputationScore,
                          String defaultCurrency, ShippingAddress shippingAddress,
                          ShoppingPreferences shoppingPreferences, Instant createdAt, Instant updatedAt) {
        this.id = id;
        this.email = email;
        this.fullName = fullName;
        this.role = role;
        this.reputationScore = reputationScore;
        this.defaultCurrency = defaultCurrency;
        this.shippingAddress = shippingAddress;
        this.shoppingPreferences = shoppingPreferences;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private String id;
        private String email;
        private String fullName;
        private UserRole role;
        private Double reputationScore;
        private String defaultCurrency;
        private ShippingAddress shippingAddress;
        private ShoppingPreferences shoppingPreferences;
        private Instant createdAt;
        private Instant updatedAt;

        public Builder id(String id) { this.id = id; return this; }
        public Builder email(String email) { this.email = email; return this; }
        public Builder fullName(String fullName) { this.fullName = fullName; return this; }
        public Builder role(UserRole role) { this.role = role; return this; }
        public Builder reputationScore(Double reputationScore) { this.reputationScore = reputationScore; return this; }
        public Builder defaultCurrency(String defaultCurrency) { this.defaultCurrency = defaultCurrency; return this; }
        public Builder shippingAddress(ShippingAddress shippingAddress) { this.shippingAddress = shippingAddress; return this; }
        public Builder shoppingPreferences(ShoppingPreferences shoppingPreferences) { this.shoppingPreferences = shoppingPreferences; return this; }
        public Builder createdAt(Instant createdAt) { this.createdAt = createdAt; return this; }
        public Builder updatedAt(Instant updatedAt) { this.updatedAt = updatedAt; return this; }

        public UserProfileDto build() {
            return new UserProfileDto(id, email, fullName, role, reputationScore, defaultCurrency, shippingAddress, shoppingPreferences, createdAt, updatedAt);
        }
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
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
