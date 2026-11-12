package com.tradetwin.payment.service;

import com.tradetwin.deal.model.AgreementSeal;
import com.tradetwin.mandate.model.MandateValidationResult;
import com.tradetwin.negotiation.model.Negotiation;
import com.tradetwin.payment.model.PaymentAuditEntry;
import com.tradetwin.payment.repository.PaymentAuditEntryRepository;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class PaymentAuditService {

    private final PaymentAuditEntryRepository auditRepository;

    public PaymentAuditService(PaymentAuditEntryRepository auditRepository) {
        this.auditRepository = auditRepository;
    }

    public List<PaymentAuditEntry> getAuditHistoryForBuyer(String buyerId) {
        return auditRepository.findByBuyerIdOrderByUpdatedAtDesc(buyerId);
    }

    public PaymentAuditEntry recordEvent(Negotiation neg,
                                         MandateValidationResult validation,
                                         String authorizationMethod,
                                         String paypalOrderId,
                                         String paypalCaptureId,
                                         String finalStatus,
                                         List<String> diagnostics) {
        if (neg == null || !StringUtils.hasText(neg.getId())) {
            return null;
        }

        AgreementSeal seal = neg.getAgreementSeal();
        PaymentAuditEntry entry = auditRepository.findByNegotiationId(neg.getId())
                .orElseGet(PaymentAuditEntry::new);

        if (entry.getCreatedAt() == null) {
            entry.setCreatedAt(Instant.now());
        }
        entry.setNegotiationId(neg.getId());
        entry.setAgreementId(seal != null ? seal.getAgreementId() : entry.getAgreementId());
        entry.setBuyerId(neg.getBuyerId());
        entry.setSellerId(neg.getSellerId());
        entry.setSellerStoreName(StringUtils.hasText(neg.getCurrentSellerStoreName())
                ? neg.getCurrentSellerStoreName()
                : neg.getSellerId());
        entry.setProductId(neg.getProductId());
        entry.setProductTitle(neg.getProductTitle());

        BigDecimal negotiatedAmt = seal != null && seal.getFinalPrice() != null
                ? seal.getFinalPrice()
                : entry.getNegotiatedAmount();
        entry.setNegotiatedAmount(negotiatedAmt);
        entry.setCurrency(seal != null && StringUtils.hasText(seal.getCurrency()) ? seal.getCurrency() : "USD");

        if (validation != null) {
            entry.setMandateId(validation.mandateId());
            entry.setBudgetCompliant(validation.compliant());
            entry.setComplianceViolations(validation.violations());
            entry.setComplianceSummary(validation.compliant()
                    ? (validation.hasMandate() ? "Within Spend Mandate limits" : "Standard budget guardrail compliant")
                    : String.join("; ", validation.violations()));

            Map<String, Object> snapshot = new LinkedHashMap<>();
            snapshot.put("hasMandate", validation.hasMandate());
            snapshot.put("mandateName", validation.mandateName());
            snapshot.put("paymentApprovalPolicy", validation.paymentApprovalPolicy() != null
                    ? validation.paymentApprovalPolicy().name()
                    : "SUPERVISED_PAYPAL_CHECKOUT");
            snapshot.put("maxAmountPerTransaction", validation.maxAmountPerTransaction());
            snapshot.put("dailySpendingLimit", validation.dailySpendingLimit());
            snapshot.put("dailySpentToday", validation.dailySpentToday());
            snapshot.put("projectedDailySpend", validation.projectedDailySpend());
            snapshot.put("expiresAt", validation.expiresAt() != null ? validation.expiresAt().toString() : null);
            entry.setMandateSnapshot(snapshot);
        }

        if (StringUtils.hasText(authorizationMethod)) {
            entry.setAuthorizationMethod(authorizationMethod);
        }
        if (StringUtils.hasText(paypalOrderId)) {
            entry.setPaypalOrderId(paypalOrderId);
        }
        if (StringUtils.hasText(paypalCaptureId)) {
            entry.setPaypalCaptureId(paypalCaptureId);
        }
        if (StringUtils.hasText(finalStatus)) {
            entry.setFinalStatus(finalStatus);
        }
        if (diagnostics != null) {
            entry.setDiagnosticDetails(diagnostics);
        }
        entry.setUpdatedAt(Instant.now());

        return auditRepository.save(entry);
    }
}
