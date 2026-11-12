package com.tradetwin.user.dto;

import com.tradetwin.user.model.ShippingAddress;
import com.tradetwin.user.model.ShoppingPreferences;
import jakarta.validation.constraints.Size;

public class UpdateProfileRequest {

    @Size(max = 100, message = "Full name must be at most 100 characters")
    private String fullName;

    @Size(min = 3, max = 5, message = "Currency code must be 3 to 5 characters")
    private String defaultCurrency;

    private ShippingAddress shippingAddress;

    private ShoppingPreferences shoppingPreferences;

    public UpdateProfileRequest() {}

    public UpdateProfileRequest(String fullName, String defaultCurrency, ShippingAddress shippingAddress, ShoppingPreferences shoppingPreferences) {
        this.fullName = fullName;
        this.defaultCurrency = defaultCurrency;
        this.shippingAddress = shippingAddress;
        this.shoppingPreferences = shoppingPreferences;
    }

    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private String fullName;
        private String defaultCurrency;
        private ShippingAddress shippingAddress;
        private ShoppingPreferences shoppingPreferences;

        public Builder fullName(String fullName) { this.fullName = fullName; return this; }
        public Builder defaultCurrency(String defaultCurrency) { this.defaultCurrency = defaultCurrency; return this; }
        public Builder shippingAddress(ShippingAddress shippingAddress) { this.shippingAddress = shippingAddress; return this; }
        public Builder shoppingPreferences(ShoppingPreferences shoppingPreferences) { this.shoppingPreferences = shoppingPreferences; return this; }

        public UpdateProfileRequest build() {
            return new UpdateProfileRequest(fullName, defaultCurrency, shippingAddress, shoppingPreferences);
        }
    }

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }
    public String getDefaultCurrency() { return defaultCurrency; }
    public void setDefaultCurrency(String defaultCurrency) { this.defaultCurrency = defaultCurrency; }
    public ShippingAddress getShippingAddress() { return shippingAddress; }
    public void setShippingAddress(ShippingAddress shippingAddress) { this.shippingAddress = shippingAddress; }
    public ShoppingPreferences getShoppingPreferences() { return shoppingPreferences; }
    public void setShoppingPreferences(ShoppingPreferences shoppingPreferences) { this.shoppingPreferences = shoppingPreferences; }
}
