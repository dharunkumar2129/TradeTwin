package com.tradetwin.catalog.model;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public class QuantityDiscountRule {

    @Min(value = 2, message = "Minimum quantity must be at least 2")
    private int minQuantity;

    @NotNull(message = "Discount percentage is required")
    @DecimalMin(value = "0.01", message = "Discount percentage must be positive")
    private BigDecimal discountPct;

    public QuantityDiscountRule() {}

    public QuantityDiscountRule(int minQuantity, BigDecimal discountPct) {
        this.minQuantity = minQuantity;
        this.discountPct = discountPct != null ? discountPct : BigDecimal.ZERO;
    }

    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private int minQuantity = 2;
        private BigDecimal discountPct = new BigDecimal("5.00");

        public Builder minQuantity(int minQuantity) { this.minQuantity = minQuantity; return this; }
        public Builder discountPct(BigDecimal discountPct) { this.discountPct = discountPct; return this; }

        public QuantityDiscountRule build() {
            return new QuantityDiscountRule(minQuantity, discountPct);
        }
    }

    public int getMinQuantity() { return minQuantity; }
    public void setMinQuantity(int minQuantity) { this.minQuantity = minQuantity; }
    public BigDecimal getDiscountPct() { return discountPct; }
    public void setDiscountPct(BigDecimal discountPct) { this.discountPct = discountPct; }
}
