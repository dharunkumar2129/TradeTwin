package com.tradetwin.deal.model;

import java.math.BigDecimal;
import java.time.Instant;

public class AgreementSeal {
    private String agreementId;
    private String dealToken;
    private Instant sealedAt;
    private Instant expiresAt;
    private Instant reservationExpiresAt;
    private BigDecimal finalPrice;
    private BigDecimal unitPrice;
    private BigDecimal shippingCost = BigDecimal.ZERO;
    private int quantity = 1;
    private String currency;
    private String status = "AWAITING_PAYMENT";
    private boolean stockReserved = true;
    private String preparedPaypalOrderId;
    private String paymentExecutionMode = "SUPERVISED_PAYPAL_CHECKOUT";
    private String mandateId;

    public AgreementSeal() {}

    public AgreementSeal(String dealToken, Instant sealedAt, Instant expiresAt, BigDecimal finalPrice, String currency, String status) {
        this.dealToken = dealToken;
        this.sealedAt = sealedAt;
        this.expiresAt = expiresAt;
        this.finalPrice = finalPrice;
        this.currency = currency;
        this.status = status != null ? status : "AWAITING_PAYMENT";
    }

    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private String agreementId;
        private String dealToken;
        private Instant sealedAt;
        private Instant expiresAt;
        private Instant reservationExpiresAt;
        private BigDecimal finalPrice;
        private BigDecimal unitPrice;
        private BigDecimal shippingCost = BigDecimal.ZERO;
        private int quantity = 1;
        private String currency;
        private String status = "AWAITING_PAYMENT";
        private boolean stockReserved = true;

        public Builder agreementId(String agreementId) { this.agreementId = agreementId; return this; }
        public Builder dealToken(String dealToken) { this.dealToken = dealToken; return this; }
        public Builder sealedAt(Instant sealedAt) { this.sealedAt = sealedAt; return this; }
        public Builder expiresAt(Instant expiresAt) { this.expiresAt = expiresAt; return this; }
        public Builder reservationExpiresAt(Instant reservationExpiresAt) { this.reservationExpiresAt = reservationExpiresAt; return this; }
        public Builder finalPrice(BigDecimal finalPrice) { this.finalPrice = finalPrice; return this; }
        public Builder unitPrice(BigDecimal unitPrice) { this.unitPrice = unitPrice; return this; }
        public Builder shippingCost(BigDecimal shippingCost) { this.shippingCost = shippingCost; return this; }
        public Builder quantity(int quantity) { this.quantity = quantity > 0 ? quantity : 1; return this; }
        public Builder currency(String currency) { this.currency = currency; return this; }
        public Builder status(String status) { this.status = status; return this; }
        public Builder stockReserved(boolean stockReserved) { this.stockReserved = stockReserved; return this; }

        public AgreementSeal build() {
            AgreementSeal seal = new AgreementSeal(dealToken, sealedAt, expiresAt, finalPrice, currency, status);
            seal.setAgreementId(agreementId);
            seal.setReservationExpiresAt(reservationExpiresAt != null ? reservationExpiresAt : expiresAt);
            seal.setUnitPrice(unitPrice);
            seal.setShippingCost(shippingCost != null ? shippingCost : BigDecimal.ZERO);
            seal.setQuantity(quantity > 0 ? quantity : 1);
            seal.setStockReserved(stockReserved);
            return seal;
        }
    }

    public String getAgreementId() { return agreementId; }
    public void setAgreementId(String agreementId) { this.agreementId = agreementId; }
    public String getDealToken() { return dealToken; }
    public void setDealToken(String dealToken) { this.dealToken = dealToken; }
    public Instant getSealedAt() { return sealedAt; }
    public void setSealedAt(Instant sealedAt) { this.sealedAt = sealedAt; }
    public Instant getExpiresAt() { return expiresAt; }
    public void setExpiresAt(Instant expiresAt) { this.expiresAt = expiresAt; }
    public Instant getReservationExpiresAt() { return reservationExpiresAt; }
    public void setReservationExpiresAt(Instant reservationExpiresAt) { this.reservationExpiresAt = reservationExpiresAt; }
    public BigDecimal getFinalPrice() { return finalPrice; }
    public void setFinalPrice(BigDecimal finalPrice) { this.finalPrice = finalPrice; }
    public BigDecimal getUnitPrice() { return unitPrice; }
    public void setUnitPrice(BigDecimal unitPrice) { this.unitPrice = unitPrice; }
    public BigDecimal getShippingCost() { return shippingCost; }
    public void setShippingCost(BigDecimal shippingCost) { this.shippingCost = shippingCost; }
    public int getQuantity() { return quantity > 0 ? quantity : 1; }
    public void setQuantity(int quantity) { this.quantity = quantity > 0 ? quantity : 1; }
    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public boolean isStockReserved() { return stockReserved; }
    public void setStockReserved(boolean stockReserved) { this.stockReserved = stockReserved; }
    public String getPreparedPaypalOrderId() { return preparedPaypalOrderId; }
    public void setPreparedPaypalOrderId(String preparedPaypalOrderId) { this.preparedPaypalOrderId = preparedPaypalOrderId; }
    public String getPaymentExecutionMode() { return paymentExecutionMode; }
    public void setPaymentExecutionMode(String paymentExecutionMode) { this.paymentExecutionMode = paymentExecutionMode; }
    public String getMandateId() { return mandateId; }
    public void setMandateId(String mandateId) { this.mandateId = mandateId; }
}
