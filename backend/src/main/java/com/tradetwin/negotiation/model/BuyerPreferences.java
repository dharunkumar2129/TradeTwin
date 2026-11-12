package com.tradetwin.negotiation.model;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public class BuyerPreferences {

    @NotNull(message = "Target price is required")
    @DecimalMin(value = "0.01", message = "Target price must be positive")
    private BigDecimal targetPrice;

    @NotNull(message = "Max ceiling price is required")
    @DecimalMin(value = "0.01", message = "Ceiling price must be positive")
    private BigDecimal maxCeilingPrice;

    private String urgency = "MEDIUM";
    private int desiredDeliveryDays = 3;
    private boolean requireExtendedWarranty = true;
    private int quantity = 1;
    private int minWarrantyMonths = 12;
    private String requiredBrand;
    private String preferredBrand;
    private String buyerPrompt;

    public BuyerPreferences() {}

    public BuyerPreferences(BigDecimal targetPrice, BigDecimal maxCeilingPrice, String urgency, int desiredDeliveryDays, boolean requireExtendedWarranty) {
        this.targetPrice = targetPrice;
        this.maxCeilingPrice = maxCeilingPrice;
        this.urgency = urgency != null ? urgency : "MEDIUM";
        this.desiredDeliveryDays = desiredDeliveryDays;
        this.requireExtendedWarranty = requireExtendedWarranty;
        this.minWarrantyMonths = requireExtendedWarranty ? 12 : 0;
    }

    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private BigDecimal targetPrice;
        private BigDecimal maxCeilingPrice;
        private String urgency = "MEDIUM";
        private int desiredDeliveryDays = 3;
        private boolean requireExtendedWarranty = true;
        private int quantity = 1;
        private int minWarrantyMonths = 12;
        private String requiredBrand;
        private String preferredBrand;
        private String buyerPrompt;

        public Builder targetPrice(BigDecimal targetPrice) { this.targetPrice = targetPrice; return this; }
        public Builder maxCeilingPrice(BigDecimal maxCeilingPrice) { this.maxCeilingPrice = maxCeilingPrice; return this; }
        public Builder urgency(String urgency) { this.urgency = urgency; return this; }
        public Builder desiredDeliveryDays(int desiredDeliveryDays) { this.desiredDeliveryDays = desiredDeliveryDays; return this; }
        public Builder requireExtendedWarranty(boolean requireExtendedWarranty) { this.requireExtendedWarranty = requireExtendedWarranty; return this; }
        public Builder quantity(int quantity) { this.quantity = quantity > 0 ? quantity : 1; return this; }
        public Builder minWarrantyMonths(int minWarrantyMonths) { this.minWarrantyMonths = Math.max(0, minWarrantyMonths); return this; }
        public Builder requiredBrand(String requiredBrand) { this.requiredBrand = requiredBrand; return this; }
        public Builder preferredBrand(String preferredBrand) { this.preferredBrand = preferredBrand; return this; }
        public Builder buyerPrompt(String buyerPrompt) { this.buyerPrompt = buyerPrompt; return this; }

        public BuyerPreferences build() {
            BuyerPreferences bp = new BuyerPreferences(targetPrice, maxCeilingPrice, urgency, desiredDeliveryDays, requireExtendedWarranty);
            bp.setQuantity(quantity > 0 ? quantity : 1);
            bp.setMinWarrantyMonths(minWarrantyMonths);
            bp.setRequiredBrand(requiredBrand);
            bp.setPreferredBrand(preferredBrand);
            bp.setBuyerPrompt(buyerPrompt);
            return bp;
        }
    }

    public BigDecimal getTargetPrice() { return targetPrice; }
    public void setTargetPrice(BigDecimal targetPrice) { this.targetPrice = targetPrice; }
    public BigDecimal getMaxCeilingPrice() { return maxCeilingPrice; }
    public void setMaxCeilingPrice(BigDecimal maxCeilingPrice) { this.maxCeilingPrice = maxCeilingPrice; }
    public String getUrgency() { return urgency; }
    public void setUrgency(String urgency) { this.urgency = urgency; }
    public int getDesiredDeliveryDays() { return desiredDeliveryDays; }
    public void setDesiredDeliveryDays(int desiredDeliveryDays) { this.desiredDeliveryDays = desiredDeliveryDays; }
    public boolean isRequireExtendedWarranty() { return requireExtendedWarranty; }
    public void setRequireExtendedWarranty(boolean requireExtendedWarranty) { this.requireExtendedWarranty = requireExtendedWarranty; }
    public int getQuantity() { return quantity > 0 ? quantity : 1; }
    public void setQuantity(int quantity) { this.quantity = quantity > 0 ? quantity : 1; }
    public int getMinWarrantyMonths() { return minWarrantyMonths; }
    public void setMinWarrantyMonths(int minWarrantyMonths) { this.minWarrantyMonths = minWarrantyMonths; }
    public String getRequiredBrand() { return requiredBrand; }
    public void setRequiredBrand(String requiredBrand) { this.requiredBrand = requiredBrand; }
    public String getPreferredBrand() { return preferredBrand; }
    public void setPreferredBrand(String preferredBrand) { this.preferredBrand = preferredBrand; }
    public String getBuyerPrompt() { return buyerPrompt; }
    public void setBuyerPrompt(String buyerPrompt) { this.buyerPrompt = buyerPrompt; }
}
