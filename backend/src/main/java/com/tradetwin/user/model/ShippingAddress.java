package com.tradetwin.user.model;

public class ShippingAddress {
    private String fullName;
    private String addressLine1;
    private String addressLine2;
    private String city;
    private String state;
    private String postalCode;
    private String country;

    public ShippingAddress() {}

    public ShippingAddress(String fullName, String addressLine1, String addressLine2, String city, String state, String postalCode, String country) {
        this.fullName = fullName;
        this.addressLine1 = addressLine1;
        this.addressLine2 = addressLine2;
        this.city = city;
        this.state = state;
        this.postalCode = postalCode;
        this.country = country != null ? country : "United States";
    }

    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private String fullName;
        private String addressLine1;
        private String addressLine2;
        private String city;
        private String state;
        private String postalCode;
        private String country = "United States";

        public Builder fullName(String fullName) { this.fullName = fullName; return this; }
        public Builder addressLine1(String addressLine1) { this.addressLine1 = addressLine1; return this; }
        public Builder addressLine2(String addressLine2) { this.addressLine2 = addressLine2; return this; }
        public Builder city(String city) { this.city = city; return this; }
        public Builder state(String state) { this.state = state; return this; }
        public Builder postalCode(String postalCode) { this.postalCode = postalCode; return this; }
        public Builder country(String country) { this.country = country; return this; }

        public ShippingAddress build() {
            return new ShippingAddress(fullName, addressLine1, addressLine2, city, state, postalCode, country);
        }
    }

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }
    public String getAddressLine1() { return addressLine1; }
    public void setAddressLine1(String addressLine1) { this.addressLine1 = addressLine1; }
    public String getAddressLine2() { return addressLine2; }
    public void setAddressLine2(String addressLine2) { this.addressLine2 = addressLine2; }
    public String getCity() { return city; }
    public void setCity(String city) { this.city = city; }
    public String getState() { return state; }
    public void setState(String state) { this.state = state; }
    public String getPostalCode() { return postalCode; }
    public void setPostalCode(String postalCode) { this.postalCode = postalCode; }
    public String getCountry() { return country; }
    public void setCountry(String country) { this.country = country; }
}
