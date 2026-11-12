package com.tradetwin.catalog.model;

import com.tradetwin.seller.model.NegotiationStrategy;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class NegotiationPolicy {

    private boolean autoNegotiate = true;

    @NotNull(message = "Floor price is required")
    @DecimalMin(value = "0.01", message = "Floor price must be greater than zero")
    private BigDecimal floorPrice;

    @NotNull(message = "Target price is required")
    @DecimalMin(value = "0.01", message = "Target price must be greater than zero")
    private BigDecimal targetPrice;

    private BigDecimal maxDiscountPct = new BigDecimal("15.00");

    private BigDecimal autoAcceptPriceThreshold;

    private NegotiationStrategy strategy = NegotiationStrategy.BALANCED;

    private int maxRounds = 5;

    private List<QuantityDiscountRule> quantityDiscounts = new ArrayList<>();

    private boolean allowWarrantySweeteners = true;

    private int maxWarrantyMonthsSweetener = 12;

    private boolean allowFreeShipping = true;

    private int maxDeliveryDaysCompromise = 2;

    private double minimumBuyerReputation = 70.0;

    public NegotiationPolicy() {}

    public NegotiationPolicy(boolean autoNegotiate, BigDecimal floorPrice, BigDecimal targetPrice,
                              BigDecimal maxDiscountPct, BigDecimal autoAcceptPriceThreshold,
                              NegotiationStrategy strategy, int maxRounds,
                              List<QuantityDiscountRule> quantityDiscounts,
                              boolean allowWarrantySweeteners, int maxWarrantyMonthsSweetener,
                              boolean allowFreeShipping, int maxDeliveryDaysCompromise,
                              double minimumBuyerReputation) {
        this.autoNegotiate = autoNegotiate;
        this.floorPrice = floorPrice;
        this.targetPrice = targetPrice;
        this.maxDiscountPct = maxDiscountPct != null ? maxDiscountPct : new BigDecimal("15.00");
        this.autoAcceptPriceThreshold = autoAcceptPriceThreshold != null ? autoAcceptPriceThreshold : targetPrice;
        this.strategy = strategy != null ? strategy : NegotiationStrategy.BALANCED;
        this.maxRounds = maxRounds > 0 ? maxRounds : 5;
        this.quantityDiscounts = quantityDiscounts != null ? quantityDiscounts : new ArrayList<>();
        this.allowWarrantySweeteners = allowWarrantySweeteners;
        this.maxWarrantyMonthsSweetener = maxWarrantyMonthsSweetener >= 0 ? maxWarrantyMonthsSweetener : 12;
        this.allowFreeShipping = allowFreeShipping;
        this.maxDeliveryDaysCompromise = maxDeliveryDaysCompromise >= 0 ? maxDeliveryDaysCompromise : 2;
        this.minimumBuyerReputation = minimumBuyerReputation >= 0 ? minimumBuyerReputation : 70.0;
    }

    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private boolean autoNegotiate = true;
        private BigDecimal floorPrice;
        private BigDecimal targetPrice;
        private BigDecimal maxDiscountPct = new BigDecimal("15.00");
        private BigDecimal autoAcceptPriceThreshold;
        private NegotiationStrategy strategy = NegotiationStrategy.BALANCED;
        private int maxRounds = 5;
        private List<QuantityDiscountRule> quantityDiscounts = new ArrayList<>();
        private boolean allowWarrantySweeteners = true;
        private int maxWarrantyMonthsSweetener = 12;
        private boolean allowFreeShipping = true;
        private int maxDeliveryDaysCompromise = 2;
        private double minimumBuyerReputation = 70.0;

        public Builder autoNegotiate(boolean autoNegotiate) { this.autoNegotiate = autoNegotiate; return this; }
        public Builder floorPrice(BigDecimal floorPrice) { this.floorPrice = floorPrice; return this; }
        public Builder targetPrice(BigDecimal targetPrice) { this.targetPrice = targetPrice; return this; }
        public Builder maxDiscountPct(BigDecimal maxDiscountPct) { this.maxDiscountPct = maxDiscountPct; return this; }
        public Builder autoAcceptPriceThreshold(BigDecimal threshold) { this.autoAcceptPriceThreshold = threshold; return this; }
        public Builder strategy(NegotiationStrategy strategy) { this.strategy = strategy; return this; }
        public Builder maxRounds(int maxRounds) { this.maxRounds = maxRounds; return this; }
        public Builder quantityDiscounts(List<QuantityDiscountRule> quantityDiscounts) { this.quantityDiscounts = quantityDiscounts; return this; }
        public Builder allowWarrantySweeteners(boolean allowWarrantySweeteners) { this.allowWarrantySweeteners = allowWarrantySweeteners; return this; }
        public Builder maxWarrantyMonthsSweetener(int maxWarrantyMonthsSweetener) { this.maxWarrantyMonthsSweetener = maxWarrantyMonthsSweetener; return this; }
        public Builder allowFreeShipping(boolean allowFreeShipping) { this.allowFreeShipping = allowFreeShipping; return this; }
        public Builder maxDeliveryDaysCompromise(int maxDeliveryDaysCompromise) { this.maxDeliveryDaysCompromise = maxDeliveryDaysCompromise; return this; }
        public Builder minimumBuyerReputation(double minimumBuyerReputation) { this.minimumBuyerReputation = minimumBuyerReputation; return this; }

        public NegotiationPolicy build() {
            return new NegotiationPolicy(autoNegotiate, floorPrice, targetPrice, maxDiscountPct, autoAcceptPriceThreshold, strategy, maxRounds, quantityDiscounts, allowWarrantySweeteners, maxWarrantyMonthsSweetener, allowFreeShipping, maxDeliveryDaysCompromise, minimumBuyerReputation);
        }
    }

    public boolean isAutoNegotiate() { return autoNegotiate; }
    public void setAutoNegotiate(boolean autoNegotiate) { this.autoNegotiate = autoNegotiate; }
    public BigDecimal getFloorPrice() { return floorPrice; }
    public void setFloorPrice(BigDecimal floorPrice) { this.floorPrice = floorPrice; }
    public BigDecimal getTargetPrice() { return targetPrice; }
    public void setTargetPrice(BigDecimal targetPrice) { this.targetPrice = targetPrice; }
    public BigDecimal getMaxDiscountPct() { return maxDiscountPct; }
    public void setMaxDiscountPct(BigDecimal maxDiscountPct) { this.maxDiscountPct = maxDiscountPct; }
    public BigDecimal getAutoAcceptPriceThreshold() { return autoAcceptPriceThreshold; }
    public void setAutoAcceptPriceThreshold(BigDecimal autoAcceptPriceThreshold) { this.autoAcceptPriceThreshold = autoAcceptPriceThreshold; }
    public NegotiationStrategy getStrategy() { return strategy; }
    public void setStrategy(NegotiationStrategy strategy) { this.strategy = strategy; }
    public int getMaxRounds() { return maxRounds; }
    public void setMaxRounds(int maxRounds) { this.maxRounds = maxRounds; }
    public List<QuantityDiscountRule> getQuantityDiscounts() { return quantityDiscounts; }
    public void setQuantityDiscounts(List<QuantityDiscountRule> quantityDiscounts) { this.quantityDiscounts = quantityDiscounts; }
    public boolean isAllowWarrantySweeteners() { return allowWarrantySweeteners; }
    public void setAllowWarrantySweeteners(boolean allowWarrantySweeteners) { this.allowWarrantySweeteners = allowWarrantySweeteners; }
    public int getMaxWarrantyMonthsSweetener() { return maxWarrantyMonthsSweetener; }
    public void setMaxWarrantyMonthsSweetener(int maxWarrantyMonthsSweetener) { this.maxWarrantyMonthsSweetener = maxWarrantyMonthsSweetener; }
    public boolean isAllowFreeShipping() { return allowFreeShipping; }
    public void setAllowFreeShipping(boolean allowFreeShipping) { this.allowFreeShipping = allowFreeShipping; }
    public int getMaxDeliveryDaysCompromise() { return maxDeliveryDaysCompromise; }
    public void setMaxDeliveryDaysCompromise(int maxDeliveryDaysCompromise) { this.maxDeliveryDaysCompromise = maxDeliveryDaysCompromise; }
    public double getMinimumBuyerReputation() { return minimumBuyerReputation; }
    public void setMinimumBuyerReputation(double minimumBuyerReputation) { this.minimumBuyerReputation = minimumBuyerReputation; }
}
