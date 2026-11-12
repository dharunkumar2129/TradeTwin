package com.tradetwin.mandate.dto;

import com.tradetwin.mandate.model.MandateStatus;
import com.tradetwin.mandate.model.PaymentApprovalPolicy;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record SpendMandateDto(
        String id,
        String buyerId,
        String name,
        BigDecimal maxAmountPerTransaction,
        BigDecimal dailySpendingLimit,
        BigDecimal dailySpentToday,
        BigDecimal remainingDailyLimit,
        String currency,
        List<String> allowedCategories,
        List<String> preferredBrands,
        int maxQuantity,
        List<String> approvedSellerIds,
        Instant expiresAt,
        PaymentApprovalPolicy paymentApprovalPolicy,
        boolean delegatedVaultTokenConfigured,
        String delegatedVaultTokenMasked,
        String delegatedMerchantId,
        String delegatedMaxCurrency,
        MandateStatus status,
        Instant createdAt,
        Instant updatedAt
) {
}
