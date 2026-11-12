package com.tradetwin.seller.model;

public class BusinessContact {
    private String phone;
    private String supportEmail;
    private String supportHours;

    public BusinessContact() {}

    public BusinessContact(String phone, String supportEmail, String supportHours) {
        this.phone = phone;
        this.supportEmail = supportEmail;
        this.supportHours = supportHours;
    }

    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private String phone;
        private String supportEmail;
        private String supportHours;

        public Builder phone(String phone) { this.phone = phone; return this; }
        public Builder supportEmail(String supportEmail) { this.supportEmail = supportEmail; return this; }
        public Builder supportHours(String supportHours) { this.supportHours = supportHours; return this; }

        public BusinessContact build() {
            return new BusinessContact(phone, supportEmail, supportHours);
        }
    }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public String getSupportEmail() { return supportEmail; }
    public void setSupportEmail(String supportEmail) { this.supportEmail = supportEmail; }
    public String getSupportHours() { return supportHours; }
    public void setSupportHours(String supportHours) { this.supportHours = supportHours; }
}
