package com.tradetwin.mandate.dto;

import com.tradetwin.mandate.model.PaymentApprovalPolicy;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record SpendMandateRequest(
        @NotBlank(message = "Mandate name is required")
        String name,

        @NotNull(message = "Maximum amount per transaction is required")
        @DecimalMin(value = "0.01", message = "Maximum amount per transaction must be greater than zero")
        BigDecimal maxAmountPerTransaction,

        @NotNull(message = "Daily spending limit is required")
        @DecimalMin(value = "0.01", message = "Daily spending limit must be greater than zero")
        BigDecimal dailySpendingLimit,

        String currency,
        List<String> allowedCategories,
        List<String> preferredBrands,

        @Min(value = 1, message = "Maximum quantity must be at least 1")
        int maxQuantity,

        List<String> approvedSellerIds,

        @NotNull(message = "Mandate expiration timestamp is required")
        Instant expiresAt,

        @NotNull(message = "Payment approval policy is required")
        PaymentApprovalPolicy paymentApprovalPolicy,

        String delegatedVaultTokenId,
        String delegatedMerchantId,
        String delegatedMaxCurrency
) {
}
