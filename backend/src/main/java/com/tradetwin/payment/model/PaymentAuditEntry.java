package com.tradetwin.payment.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Document(collection = "payment_audit_logs")
public class PaymentAuditEntry {

    @Id
    private String id;

    @Indexed
    private String negotiationId;
    private String agreementId;

    @Indexed
    private String buyerId;
    private String sellerId;
    private String sellerStoreName;
    private String productId;
    private String productTitle;

    private String mandateId;
    private Map<String, Object> mandateSnapshot = new LinkedHashMap<>();

    private BigDecimal negotiatedAmount;
    private String currency = "USD";

    private boolean budgetCompliant = true;
    private String complianceSummary;
    private List<String> complianceViolations = new ArrayList<>();

    private String authorizationMethod;
    private String paypalOrderId;
    private String paypalCaptureId;
    private String finalStatus;
    private List<String> diagnosticDetails = new ArrayList<>();

    private Instant createdAt = Instant.now();
    private Instant updatedAt = Instant.now();

    public PaymentAuditEntry() {}

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getNegotiationId() { return negotiationId; }
    public void setNegotiationId(String negotiationId) { this.negotiationId = negotiationId; }
    public String getAgreementId() { return agreementId; }
    public void setAgreementId(String agreementId) { this.agreementId = agreementId; }
    public String getBuyerId() { return buyerId; }
    public void setBuyerId(String buyerId) { this.buyerId = buyerId; }
    public String getSellerId() { return sellerId; }
    public void setSellerId(String sellerId) { this.sellerId = sellerId; }
    public String getSellerStoreName() { return sellerStoreName; }
    public void setSellerStoreName(String sellerStoreName) { this.sellerStoreName = sellerStoreName; }
    public String getProductId() { return productId; }
    public void setProductId(String productId) { this.productId = productId; }
    public String getProductTitle() { return productTitle; }
    public void setProductTitle(String productTitle) { this.productTitle = productTitle; }
    public String getMandateId() { return mandateId; }
    public void setMandateId(String mandateId) { this.mandateId = mandateId; }
    public Map<String, Object> getMandateSnapshot() { return mandateSnapshot; }
    public void setMandateSnapshot(Map<String, Object> mandateSnapshot) { this.mandateSnapshot = mandateSnapshot != null ? mandateSnapshot : new LinkedHashMap<>(); }
    public BigDecimal getNegotiatedAmount() { return negotiatedAmount; }
    public void setNegotiatedAmount(BigDecimal negotiatedAmount) { this.negotiatedAmount = negotiatedAmount; }
    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }
    public boolean isBudgetCompliant() { return budgetCompliant; }
    public void setBudgetCompliant(boolean budgetCompliant) { this.budgetCompliant = budgetCompliant; }
    public String getComplianceSummary() { return complianceSummary; }
    public void setComplianceSummary(String complianceSummary) { this.complianceSummary = complianceSummary; }
    public List<String> getComplianceViolations() { return complianceViolations; }
    public void setComplianceViolations(List<String> complianceViolations) { this.complianceViolations = complianceViolations != null ? complianceViolations : new ArrayList<>(); }
    public String getAuthorizationMethod() { return authorizationMethod; }
    public void setAuthorizationMethod(String authorizationMethod) { this.authorizationMethod = authorizationMethod; }
    public String getPaypalOrderId() { return paypalOrderId; }
    public void setPaypalOrderId(String paypalOrderId) { this.paypalOrderId = paypalOrderId; }
    public String getPaypalCaptureId() { return paypalCaptureId; }
    public void setPaypalCaptureId(String paypalCaptureId) { this.paypalCaptureId = paypalCaptureId; }
    public String getFinalStatus() { return finalStatus; }
    public void setFinalStatus(String finalStatus) { this.finalStatus = finalStatus; }
    public List<String> getDiagnosticDetails() { return diagnosticDetails; }
    public void setDiagnosticDetails(List<String> diagnosticDetails) { this.diagnosticDetails = diagnosticDetails != null ? diagnosticDetails : new ArrayList<>(); }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
