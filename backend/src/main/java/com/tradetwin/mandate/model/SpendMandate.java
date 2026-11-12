package com.tradetwin.mandate.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Document(collection = "spend_mandates")
public class SpendMandate {

    @Id
    private String id;

    @Indexed
    private String buyerId;

    private String name;
    private BigDecimal maxAmountPerTransaction;
    private BigDecimal dailySpendingLimit;
    private String currency = "USD";
    private List<String> allowedCategories = new ArrayList<>();
    private List<String> preferredBrands = new ArrayList<>();
    private int maxQuantity = 5;
    private List<String> approvedSellerIds = new ArrayList<>();
    private Instant expiresAt;
    private PaymentApprovalPolicy paymentApprovalPolicy = PaymentApprovalPolicy.SUPERVISED_PAYPAL_CHECKOUT;

    /**
     * Optional PayPal Vault v3 / Reference Transaction payment_token ID supplied by an
     * authorized PayPal Vault integration. Ordinary PayPal OAuth access tokens must NEVER
     * be stored or accepted here.
     */
    private String delegatedVaultTokenId;
    private String delegatedMerchantId;
    private String delegatedMaxCurrency = "USD";

    private MandateStatus status = MandateStatus.ACTIVE;
    private Instant createdAt = Instant.now();
    private Instant updatedAt = Instant.now();

    public SpendMandate() {}

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getBuyerId() { return buyerId; }
    public void setBuyerId(String buyerId) { this.buyerId = buyerId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public BigDecimal getMaxAmountPerTransaction() { return maxAmountPerTransaction; }
    public void setMaxAmountPerTransaction(BigDecimal maxAmountPerTransaction) { this.maxAmountPerTransaction = maxAmountPerTransaction; }
    public BigDecimal getDailySpendingLimit() { return dailySpendingLimit; }
    public void setDailySpendingLimit(BigDecimal dailySpendingLimit) { this.dailySpendingLimit = dailySpendingLimit; }
    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }
    public List<String> getAllowedCategories() { return allowedCategories; }
    public void setAllowedCategories(List<String> allowedCategories) { this.allowedCategories = allowedCategories != null ? allowedCategories : new ArrayList<>(); }
    public List<String> getPreferredBrands() { return preferredBrands; }
    public void setPreferredBrands(List<String> preferredBrands) { this.preferredBrands = preferredBrands != null ? preferredBrands : new ArrayList<>(); }
    public int getMaxQuantity() { return maxQuantity; }
    public void setMaxQuantity(int maxQuantity) { this.maxQuantity = maxQuantity; }
    public List<String> getApprovedSellerIds() { return approvedSellerIds; }
    public void setApprovedSellerIds(List<String> approvedSellerIds) { this.approvedSellerIds = approvedSellerIds != null ? approvedSellerIds : new ArrayList<>(); }
    public Instant getExpiresAt() { return expiresAt; }
    public void setExpiresAt(Instant expiresAt) { this.expiresAt = expiresAt; }
    public PaymentApprovalPolicy getPaymentApprovalPolicy() { return paymentApprovalPolicy; }
    public void setPaymentApprovalPolicy(PaymentApprovalPolicy paymentApprovalPolicy) { this.paymentApprovalPolicy = paymentApprovalPolicy != null ? paymentApprovalPolicy : PaymentApprovalPolicy.SUPERVISED_PAYPAL_CHECKOUT; }
    public String getDelegatedVaultTokenId() { return delegatedVaultTokenId; }
    public void setDelegatedVaultTokenId(String delegatedVaultTokenId) { this.delegatedVaultTokenId = delegatedVaultTokenId; }
    public String getDelegatedMerchantId() { return delegatedMerchantId; }
    public void setDelegatedMerchantId(String delegatedMerchantId) { this.delegatedMerchantId = delegatedMerchantId; }
    public String getDelegatedMaxCurrency() { return delegatedMaxCurrency; }
    public void setDelegatedMaxCurrency(String delegatedMaxCurrency) { this.delegatedMaxCurrency = delegatedMaxCurrency; }
    public MandateStatus getStatus() { return status; }
    public void setStatus(MandateStatus status) { this.status = status; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
