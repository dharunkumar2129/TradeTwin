package com.tradetwin.negotiation.model;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class OfferProposal {
    private BigDecimal price;
    private BigDecimal unitPrice;
    private BigDecimal shippingCost = BigDecimal.ZERO;
    private BigDecimal totalPayable;
    private int quantity = 1;
    private String currency = "USD";
    private int deliveryDays = 3;
    private int warrantyMonths = 12;
    private String offerStatus = "OFFERED";
    private int roundNumber = 1;
    private List<String> bonusPerks = new ArrayList<>();

    public OfferProposal() {}

    public OfferProposal(BigDecimal price, String currency, int deliveryDays, int warrantyMonths, List<String> bonusPerks) {
        this.price = price;
        this.unitPrice = price;
        this.shippingCost = BigDecimal.ZERO;
        this.totalPayable = price;
        this.quantity = 1;
        this.currency = currency != null ? currency : "USD";
        this.deliveryDays = deliveryDays;
        this.warrantyMonths = warrantyMonths;
        this.bonusPerks = bonusPerks != null ? bonusPerks : new ArrayList<>();
    }

    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private BigDecimal price;
        private BigDecimal unitPrice;
        private BigDecimal shippingCost = BigDecimal.ZERO;
        private BigDecimal totalPayable;
        private int quantity = 1;
        private String currency = "USD";
        private int deliveryDays = 3;
        private int warrantyMonths = 12;
        private String offerStatus = "OFFERED";
        private int roundNumber = 1;
        private List<String> bonusPerks = new ArrayList<>();

        public Builder price(BigDecimal price) { this.price = price; return this; }
        public Builder unitPrice(BigDecimal unitPrice) { this.unitPrice = unitPrice; return this; }
        public Builder shippingCost(BigDecimal shippingCost) { this.shippingCost = shippingCost; return this; }
        public Builder totalPayable(BigDecimal totalPayable) { this.totalPayable = totalPayable; return this; }
        public Builder quantity(int quantity) { this.quantity = quantity > 0 ? quantity : 1; return this; }
        public Builder currency(String currency) { this.currency = currency; return this; }
        public Builder deliveryDays(int deliveryDays) { this.deliveryDays = deliveryDays; return this; }
        public Builder warrantyMonths(int warrantyMonths) { this.warrantyMonths = warrantyMonths; return this; }
        public Builder offerStatus(String offerStatus) { this.offerStatus = offerStatus; return this; }
        public Builder roundNumber(int roundNumber) { this.roundNumber = roundNumber; return this; }
        public Builder bonusPerks(List<String> bonusPerks) { this.bonusPerks = bonusPerks; return this; }

        public OfferProposal build() {
            BigDecimal effectiveUnit = unitPrice != null ? unitPrice : price;
            BigDecimal effectiveShipping = shippingCost != null ? shippingCost : BigDecimal.ZERO;
            int qty = quantity > 0 ? quantity : 1;
            BigDecimal effectiveTotal = totalPayable != null
                    ? totalPayable
                    : (effectiveUnit != null ? effectiveUnit.multiply(BigDecimal.valueOf(qty)).add(effectiveShipping) : null);
            OfferProposal p = new OfferProposal(price != null ? price : effectiveUnit, currency, deliveryDays, warrantyMonths, bonusPerks);
            p.setUnitPrice(effectiveUnit);
            p.setShippingCost(effectiveShipping);
            p.setTotalPayable(effectiveTotal);
            p.setQuantity(qty);
            p.setOfferStatus(offerStatus != null ? offerStatus : "OFFERED");
            p.setRoundNumber(roundNumber > 0 ? roundNumber : 1);
            return p;
        }
    }

    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal price) { this.price = price; }
    public BigDecimal getUnitPrice() { return unitPrice != null ? unitPrice : price; }
    public void setUnitPrice(BigDecimal unitPrice) { this.unitPrice = unitPrice; }
    public BigDecimal getShippingCost() { return shippingCost; }
    public void setShippingCost(BigDecimal shippingCost) { this.shippingCost = shippingCost; }
    public BigDecimal getTotalPayable() { return totalPayable != null ? totalPayable : price; }
    public void setTotalPayable(BigDecimal totalPayable) { this.totalPayable = totalPayable; }
    public int getQuantity() { return quantity > 0 ? quantity : 1; }
    public void setQuantity(int quantity) { this.quantity = quantity > 0 ? quantity : 1; }
    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }
    public int getDeliveryDays() { return deliveryDays; }
    public void setDeliveryDays(int deliveryDays) { this.deliveryDays = deliveryDays; }
    public int getWarrantyMonths() { return warrantyMonths; }
    public void setWarrantyMonths(int warrantyMonths) { this.warrantyMonths = warrantyMonths; }
    public String getOfferStatus() { return offerStatus; }
    public void setOfferStatus(String offerStatus) { this.offerStatus = offerStatus; }
    public int getRoundNumber() { return roundNumber; }
    public void setRoundNumber(int roundNumber) { this.roundNumber = roundNumber; }
    public List<String> getBonusPerks() { return bonusPerks; }
    public void setBonusPerks(List<String> bonusPerks) { this.bonusPerks = bonusPerks; }
}
