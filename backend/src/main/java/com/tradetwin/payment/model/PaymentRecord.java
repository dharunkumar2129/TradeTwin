package com.tradetwin.payment.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.math.BigDecimal;
import java.time.Instant;

@Document(collection = "payments")
public class PaymentRecord {

    @Id
    private String id;

    @Indexed
    private String negotiationId;

    @Indexed
    private String orderId;

    @Indexed
    private String buyerId;

    @Indexed
    private String sellerId;

    private String productId;
    private String provider = "PAYPAL";

    @Indexed(unique = true, sparse = true)
    private String paypalOrderId;

    @Indexed(sparse = true)
    private String captureId;

    private BigDecimal amount;
    private String currency = "USD";
    private PaymentStatus status = PaymentStatus.PENDING;

    private String payerId;
    private String payerEmail;
    private String failureReason;

    // Sanitized PayPal diagnostics & agreement tracking
    private String agreementId;
    private String failureStage;
    private Integer paypalHttpStatus;
    private String paypalErrorName;
    private String paypalErrorMessage;
    private String paypalIssue;
    private String paypalDetailDescription;
    private String paypalDetailField;
    private String debugId;

    private Instant paidAt;
    private Instant createdAt = Instant.now();
    private Instant updatedAt = Instant.now();

    public PaymentRecord() {}

    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private String id;
        private String negotiationId;
        private String orderId;
        private String buyerId;
        private String sellerId;
        private String productId;
        private String provider = "PAYPAL";
        private String paypalOrderId;
        private String captureId;
        private BigDecimal amount;
        private String currency = "USD";
        private PaymentStatus status = PaymentStatus.PENDING;
        private String payerId;
        private String payerEmail;
        private String failureReason;
        private String agreementId;
        private String failureStage;
        private Integer paypalHttpStatus;
        private String paypalErrorName;
        private String paypalErrorMessage;
        private String paypalIssue;
        private String paypalDetailDescription;
        private String paypalDetailField;
        private String debugId;
        private Instant paidAt;
        private Instant createdAt = Instant.now();
        private Instant updatedAt = Instant.now();

        public Builder id(String id) { this.id = id; return this; }
        public Builder negotiationId(String negotiationId) { this.negotiationId = negotiationId; return this; }
        public Builder orderId(String orderId) { this.orderId = orderId; return this; }
        public Builder buyerId(String buyerId) { this.buyerId = buyerId; return this; }
        public Builder sellerId(String sellerId) { this.sellerId = sellerId; return this; }
        public Builder productId(String productId) { this.productId = productId; return this; }
        public Builder provider(String provider) { this.provider = provider; return this; }
        public Builder paypalOrderId(String paypalOrderId) { this.paypalOrderId = paypalOrderId; return this; }
        public Builder captureId(String captureId) { this.captureId = captureId; return this; }
        public Builder amount(BigDecimal amount) { this.amount = amount; return this; }
        public Builder currency(String currency) { this.currency = currency; return this; }
        public Builder status(PaymentStatus status) { this.status = status; return this; }
        public Builder payerId(String payerId) { this.payerId = payerId; return this; }
        public Builder payerEmail(String payerEmail) { this.payerEmail = payerEmail; return this; }
        public Builder failureReason(String failureReason) { this.failureReason = failureReason; return this; }
        public Builder agreementId(String agreementId) { this.agreementId = agreementId; return this; }
        public Builder failureStage(String failureStage) { this.failureStage = failureStage; return this; }
        public Builder paypalHttpStatus(Integer paypalHttpStatus) { this.paypalHttpStatus = paypalHttpStatus; return this; }
        public Builder paypalErrorName(String paypalErrorName) { this.paypalErrorName = paypalErrorName; return this; }
        public Builder paypalErrorMessage(String paypalErrorMessage) { this.paypalErrorMessage = paypalErrorMessage; return this; }
        public Builder paypalIssue(String paypalIssue) { this.paypalIssue = paypalIssue; return this; }
        public Builder paypalDetailDescription(String paypalDetailDescription) { this.paypalDetailDescription = paypalDetailDescription; return this; }
        public Builder paypalDetailField(String paypalDetailField) { this.paypalDetailField = paypalDetailField; return this; }
        public Builder debugId(String debugId) { this.debugId = debugId; return this; }
        public Builder paidAt(Instant paidAt) { this.paidAt = paidAt; return this; }
        public Builder createdAt(Instant createdAt) { this.createdAt = createdAt; return this; }
        public Builder updatedAt(Instant updatedAt) { this.updatedAt = updatedAt; return this; }

        public PaymentRecord build() {
            PaymentRecord r = new PaymentRecord();
            r.id = this.id;
            r.negotiationId = this.negotiationId;
            r.orderId = this.orderId;
            r.buyerId = this.buyerId;
            r.sellerId = this.sellerId;
            r.productId = this.productId;
            r.provider = this.provider;
            r.paypalOrderId = this.paypalOrderId;
            r.captureId = this.captureId;
            r.amount = this.amount;
            r.currency = this.currency != null ? this.currency : "USD";
            r.status = this.status != null ? this.status : PaymentStatus.PENDING;
            r.payerId = this.payerId;
            r.payerEmail = this.payerEmail;
            r.failureReason = this.failureReason;
            r.agreementId = this.agreementId;
            r.failureStage = this.failureStage;
            r.paypalHttpStatus = this.paypalHttpStatus;
            r.paypalErrorName = this.paypalErrorName;
            r.paypalErrorMessage = this.paypalErrorMessage;
            r.paypalIssue = this.paypalIssue;
            r.paypalDetailDescription = this.paypalDetailDescription;
            r.paypalDetailField = this.paypalDetailField;
            r.debugId = this.debugId;
            r.paidAt = this.paidAt;
            r.createdAt = this.createdAt != null ? this.createdAt : Instant.now();
            r.updatedAt = this.updatedAt != null ? this.updatedAt : Instant.now();
            return r;
        }
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getNegotiationId() { return negotiationId; }
    public void setNegotiationId(String negotiationId) { this.negotiationId = negotiationId; }
    public String getOrderId() { return orderId; }
    public void setOrderId(String orderId) { this.orderId = orderId; }
    public String getBuyerId() { return buyerId; }
    public void setBuyerId(String buyerId) { this.buyerId = buyerId; }
    public String getSellerId() { return sellerId; }
    public void setSellerId(String sellerId) { this.sellerId = sellerId; }
    public String getProductId() { return productId; }
    public void setProductId(String productId) { this.productId = productId; }
    public String getProvider() { return provider; }
    public void setProvider(String provider) { this.provider = provider; }
    public String getPaypalOrderId() { return paypalOrderId; }
    public void setPaypalOrderId(String paypalOrderId) { this.paypalOrderId = paypalOrderId; }
    public String getCaptureId() { return captureId; }
    public void setCaptureId(String captureId) { this.captureId = captureId; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }
    public PaymentStatus getStatus() { return status; }
    public void setStatus(PaymentStatus status) { this.status = status; }
    public String getPayerId() { return payerId; }
    public void setPayerId(String payerId) { this.payerId = payerId; }
    public String getPayerEmail() { return payerEmail; }
    public void setPayerEmail(String payerEmail) { this.payerEmail = payerEmail; }
    public String getFailureReason() { return failureReason; }
    public void setFailureReason(String failureReason) { this.failureReason = failureReason; }
    public String getAgreementId() { return agreementId; }
    public void setAgreementId(String agreementId) { this.agreementId = agreementId; }
    public String getFailureStage() { return failureStage; }
    public void setFailureStage(String failureStage) { this.failureStage = failureStage; }
    public Integer getPaypalHttpStatus() { return paypalHttpStatus; }
    public void setPaypalHttpStatus(Integer paypalHttpStatus) { this.paypalHttpStatus = paypalHttpStatus; }
    public String getPaypalErrorName() { return paypalErrorName; }
    public void setPaypalErrorName(String paypalErrorName) { this.paypalErrorName = paypalErrorName; }
    public String getPaypalErrorMessage() { return paypalErrorMessage; }
    public void setPaypalErrorMessage(String paypalErrorMessage) { this.paypalErrorMessage = paypalErrorMessage; }
    public String getPaypalIssue() { return paypalIssue; }
    public void setPaypalIssue(String paypalIssue) { this.paypalIssue = paypalIssue; }
    public String getPaypalDetailDescription() { return paypalDetailDescription; }
    public void setPaypalDetailDescription(String paypalDetailDescription) { this.paypalDetailDescription = paypalDetailDescription; }
    public String getPaypalDetailField() { return paypalDetailField; }
    public void setPaypalDetailField(String paypalDetailField) { this.paypalDetailField = paypalDetailField; }
    public String getDebugId() { return debugId; }
    public void setDebugId(String debugId) { this.debugId = debugId; }
    public Instant getPaidAt() { return paidAt; }
    public void setPaidAt(Instant paidAt) { this.paidAt = paidAt; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
