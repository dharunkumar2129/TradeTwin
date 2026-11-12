package com.tradetwin.seller.model;

import java.math.BigDecimal;

public class ShippingTerms {
    private int handlingTimeDays = 2;
    private BigDecimal freeShippingThreshold = new BigDecimal("75.00");
    private boolean allowExpressShipping = true;
    private int returnPolicyDays = 30;

    public ShippingTerms() {}

    public ShippingTerms(int handlingTimeDays, BigDecimal freeShippingThreshold, boolean allowExpressShipping, int returnPolicyDays) {
        this.handlingTimeDays = handlingTimeDays > 0 ? handlingTimeDays : 2;
        this.freeShippingThreshold = freeShippingThreshold != null ? freeShippingThreshold : new BigDecimal("75.00");
        this.allowExpressShipping = allowExpressShipping;
        this.returnPolicyDays = returnPolicyDays >= 0 ? returnPolicyDays : 30;
    }

    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private int handlingTimeDays = 2;
        private BigDecimal freeShippingThreshold = new BigDecimal("75.00");
        private boolean allowExpressShipping = true;
        private int returnPolicyDays = 30;

        public Builder handlingTimeDays(int handlingTimeDays) { this.handlingTimeDays = handlingTimeDays; return this; }
        public Builder freeShippingThreshold(BigDecimal freeShippingThreshold) { this.freeShippingThreshold = freeShippingThreshold; return this; }
        public Builder allowExpressShipping(boolean allowExpressShipping) { this.allowExpressShipping = allowExpressShipping; return this; }
        public Builder returnPolicyDays(int returnPolicyDays) { this.returnPolicyDays = returnPolicyDays; return this; }

        public ShippingTerms build() {
            return new ShippingTerms(handlingTimeDays, freeShippingThreshold, allowExpressShipping, returnPolicyDays);
        }
    }

    public int getHandlingTimeDays() { return handlingTimeDays; }
    public void setHandlingTimeDays(int handlingTimeDays) { this.handlingTimeDays = handlingTimeDays; }
    public BigDecimal getFreeShippingThreshold() { return freeShippingThreshold; }
    public void setFreeShippingThreshold(BigDecimal freeShippingThreshold) { this.freeShippingThreshold = freeShippingThreshold; }
    public boolean isAllowExpressShipping() { return allowExpressShipping; }
    public void setAllowExpressShipping(boolean allowExpressShipping) { this.allowExpressShipping = allowExpressShipping; }
    public int getReturnPolicyDays() { return returnPolicyDays; }
    public void setReturnPolicyDays(int returnPolicyDays) { this.returnPolicyDays = returnPolicyDays; }
}
