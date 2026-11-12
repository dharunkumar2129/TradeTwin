package com.tradetwin.mandate.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record MandateValidationResult(
        boolean compliant,
        boolean hasMandate,
        String mandateId,
        String mandateName,
        PaymentApprovalPolicy paymentApprovalPolicy,
        BigDecimal maxAmountPerTransaction,
        BigDecimal dailySpendingLimit,
        BigDecimal dailySpentToday,
        BigDecimal projectedDailySpend,
        Instant expiresAt,
        List<String> violations
) {
}
