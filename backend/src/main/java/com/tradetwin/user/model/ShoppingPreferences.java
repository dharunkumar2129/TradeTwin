package com.tradetwin.user.model;

import java.util.ArrayList;
import java.util.List;

public class ShoppingPreferences {
    private String defaultCurrency = "USD";
    private List<String> categoriesOfInterest = new ArrayList<>();
    private String priceSensitivity = "BALANCED"; // BALANCED, DISCOUNT_SEEKER, PREMIUM
    private int preferredWarrantyMonths = 12;
    private boolean autoNegotiateDefault = true;

    public ShoppingPreferences() {}

    public ShoppingPreferences(String defaultCurrency, List<String> categoriesOfInterest, String priceSensitivity, int preferredWarrantyMonths, boolean autoNegotiateDefault) {
        this.defaultCurrency = defaultCurrency != null ? defaultCurrency : "USD";
        this.categoriesOfInterest = categoriesOfInterest != null ? categoriesOfInterest : new ArrayList<>();
        this.priceSensitivity = priceSensitivity != null ? priceSensitivity : "BALANCED";
        this.preferredWarrantyMonths = preferredWarrantyMonths > 0 ? preferredWarrantyMonths : 12;
        this.autoNegotiateDefault = autoNegotiateDefault;
    }

    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private String defaultCurrency = "USD";
        private List<String> categoriesOfInterest = new ArrayList<>();
        private String priceSensitivity = "BALANCED";
        private int preferredWarrantyMonths = 12;
        private boolean autoNegotiateDefault = true;

        public Builder defaultCurrency(String defaultCurrency) { this.defaultCurrency = defaultCurrency; return this; }
        public Builder categoriesOfInterest(List<String> categories) { this.categoriesOfInterest = categories; return this; }
        public Builder priceSensitivity(String priceSensitivity) { this.priceSensitivity = priceSensitivity; return this; }
        public Builder preferredWarrantyMonths(int months) { this.preferredWarrantyMonths = months; return this; }
        public Builder autoNegotiateDefault(boolean autoNegotiateDefault) { this.autoNegotiateDefault = autoNegotiateDefault; return this; }

        public ShoppingPreferences build() {
            return new ShoppingPreferences(defaultCurrency, categoriesOfInterest, priceSensitivity, preferredWarrantyMonths, autoNegotiateDefault);
        }
    }

    public String getDefaultCurrency() { return defaultCurrency; }
    public void setDefaultCurrency(String defaultCurrency) { this.defaultCurrency = defaultCurrency; }
    public List<String> getCategoriesOfInterest() { return categoriesOfInterest; }
    public void setCategoriesOfInterest(List<String> categoriesOfInterest) { this.categoriesOfInterest = categoriesOfInterest; }
    public String getPriceSensitivity() { return priceSensitivity; }
    public void setPriceSensitivity(String priceSensitivity) { this.priceSensitivity = priceSensitivity; }
    public int getPreferredWarrantyMonths() { return preferredWarrantyMonths; }
    public void setPreferredWarrantyMonths(int preferredWarrantyMonths) { this.preferredWarrantyMonths = preferredWarrantyMonths; }
    public boolean isAutoNegotiateDefault() { return autoNegotiateDefault; }
    public void setAutoNegotiateDefault(boolean autoNegotiateDefault) { this.autoNegotiateDefault = autoNegotiateDefault; }
}
