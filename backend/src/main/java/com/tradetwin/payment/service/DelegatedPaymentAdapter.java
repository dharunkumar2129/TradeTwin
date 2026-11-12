package com.tradetwin.payment.service;

import com.tradetwin.deal.model.AgreementSeal;
import com.tradetwin.mandate.model.SpendMandate;

import java.math.BigDecimal;
import java.util.List;

/**
 * Integration adapter abstraction for official delegated payment authorization mechanisms
 * (e.g., PayPal Vault v3 / Reference Transactions or Braintree Vault payment tokens).
 */
public interface DelegatedPaymentAdapter {

    record DelegatedCapabilityStatus(
            boolean adapterSupported,
            boolean accountAccessEnabled,
            boolean buyerCredentialLinked,
            boolean eligible,
            String adapterName,
            String reasonCode,
            String message
    ) {}

    record DelegatedExecutionOutcome(
            boolean settled,
            String paypalOrderId,
            String captureId,
            String payerId,
            String payerEmail,
            String failureCode,
            String message,
            List<String> diagnostics
    ) {}

    DelegatedCapabilityStatus inspectCapability(SpendMandate mandate,
                                                BigDecimal amount,
                                                String currency,
                                                String sellerId);

    DelegatedExecutionOutcome executeDelegatedPayment(String negotiationId,
                                                      AgreementSeal seal,
                                                      SpendMandate mandate,
                                                      BigDecimal totalAmount,
                                                      BigDecimal itemTotal,
                                                      BigDecimal shippingTotal,
                                                      String currency,
                                                      String sellerId,
                                                      String productTitle);
}
