package com.tradetwin.payment.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tradetwin.deal.model.AgreementSeal;
import com.tradetwin.mandate.model.MandateStatus;
import com.tradetwin.mandate.model.PaymentApprovalPolicy;
import com.tradetwin.mandate.model.SpendMandate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.*;

/**
 * PayPal Vault v3 / Reference Transactions adapter for legitimate delegated payments.
 * Strictly requires official merchant Vault access + buyer Vault payment_token; never
 * substitutes ordinary OAuth client_credentials tokens and never simulates delegated capture.
 */
@Service
public class PayPalVaultDelegatedPaymentAdapter implements DelegatedPaymentAdapter {

    private static final Logger log = LoggerFactory.getLogger(PayPalVaultDelegatedPaymentAdapter.class);
    private static final String ADAPTER_NAME = "PAYPAL_VAULT_V3_REFERENCE_TRANSACTIONS";

    private final PayPalService payPalService;
    private final ObjectMapper objectMapper;
    private final RestTemplate restTemplate;

    @Value("${tradetwin.paypal.delegated-vault-enabled:false}")
    private boolean delegatedVaultEnabled = false;

    @Value("${tradetwin.paypal.base-url:https://api-m.sandbox.paypal.com}")
    private String baseUrl = "https://api-m.sandbox.paypal.com";

    @Autowired
    public PayPalVaultDelegatedPaymentAdapter(PayPalService payPalService, ObjectMapper objectMapper) {
        this(payPalService, objectMapper, payPalService != null ? payPalService.getRestTemplate() : new RestTemplate());
    }

    public PayPalVaultDelegatedPaymentAdapter(PayPalService payPalService,
                                              ObjectMapper objectMapper,
                                              RestTemplate restTemplate) {
        this.payPalService = payPalService;
        this.objectMapper = objectMapper;
        this.restTemplate = restTemplate != null ? restTemplate : new RestTemplate();
    }

    public void setDelegatedVaultEnabled(boolean delegatedVaultEnabled) {
        this.delegatedVaultEnabled = delegatedVaultEnabled;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    @Override
    public DelegatedCapabilityStatus inspectCapability(SpendMandate mandate,
                                                       BigDecimal amount,
                                                       String currency,
                                                       String sellerId) {
        boolean hasCredential = mandate != null
                && StringUtils.hasText(mandate.getDelegatedVaultTokenId())
                && !looksLikeOrdinaryOAuthToken(mandate.getDelegatedVaultTokenId());

        if (!delegatedVaultEnabled) {
            return new DelegatedCapabilityStatus(
                    true,
                    false,
                    hasCredential,
                    false,
                    ADAPTER_NAME,
                    "DELEGATED_ACCESS_UNAVAILABLE",
                    "This PayPal account does not have official PayPal Vault v3 / Reference Transactions delegated payment access enabled (tradetwin.paypal.delegated-vault-enabled=false). Use Supervised PayPal Checkout."
            );
        }

        if (payPalService == null || !payPalService.isLiveCredentialsConfigured()) {
            return new DelegatedCapabilityStatus(
                    true,
                    false,
                    hasCredential,
                    false,
                    ADAPTER_NAME,
                    "LIVE_PAYPAL_CREDENTIALS_REQUIRED",
                    "Delegated autonomous payment requires live PayPal REST credentials with Vault v3 / Reference Transactions enabled and cannot run in simulated dev mode."
            );
        }

        if (mandate == null || mandate.getPaymentApprovalPolicy() != PaymentApprovalPolicy.DELEGATED_AUTONOMOUS_PAYMENT) {
            return new DelegatedCapabilityStatus(
                    true,
                    true,
                    hasCredential,
                    false,
                    ADAPTER_NAME,
                    "POLICY_NOT_DELEGATED",
                    "Buyer Spend Mandate policy is set to Supervised PayPal Checkout."
            );
        }

        if (mandate.getStatus() != MandateStatus.ACTIVE
                || (mandate.getExpiresAt() != null && Instant.now().isAfter(mandate.getExpiresAt()))) {
            return new DelegatedCapabilityStatus(
                    true,
                    true,
                    hasCredential,
                    false,
                    ADAPTER_NAME,
                    "MANDATE_EXPIRED_OR_INACTIVE",
                    "Spend Mandate is expired or inactive; delegated authorization is blocked."
            );
        }

        if (!hasCredential) {
            return new DelegatedCapabilityStatus(
                    true,
                    true,
                    false,
                    false,
                    ADAPTER_NAME,
                    "DELEGATED_CREDENTIAL_MISSING",
                    "No valid PayPal Vault payment_token was supplied by an authorized Vault integration. Ordinary PayPal OAuth access tokens are not valid buyer payment authorizations."
            );
        }

        if (StringUtils.hasText(mandate.getDelegatedMerchantId())
                && StringUtils.hasText(sellerId)
                && !mandate.getDelegatedMerchantId().trim().equalsIgnoreCase(sellerId.trim())) {
            return new DelegatedCapabilityStatus(
                    true,
                    true,
                    true,
                    false,
                    ADAPTER_NAME,
                    "MERCHANT_RESTRICTION_VIOLATION",
                    "Delegated Vault token is restricted to merchant [" + mandate.getDelegatedMerchantId() + "] and cannot authorize seller [" + sellerId + "]."
            );
        }

        String normalizedTxCurr = StringUtils.hasText(currency) ? currency.trim().toUpperCase(Locale.ROOT) : "USD";
        if (StringUtils.hasText(mandate.getDelegatedMaxCurrency())
                && !mandate.getDelegatedMaxCurrency().trim().equalsIgnoreCase(normalizedTxCurr)) {
            return new DelegatedCapabilityStatus(
                    true,
                    true,
                    true,
                    false,
                    ADAPTER_NAME,
                    "CURRENCY_RESTRICTION_VIOLATION",
                    "Delegated Vault restriction permits currency [" + mandate.getDelegatedMaxCurrency() + "] only."
            );
        }

        if (mandate.getMaxAmountPerTransaction() != null && amount != null
                && amount.compareTo(mandate.getMaxAmountPerTransaction()) > 0) {
            return new DelegatedCapabilityStatus(
                    true,
                    true,
                    true,
                    false,
                    ADAPTER_NAME,
                    "AMOUNT_RESTRICTION_VIOLATION",
                    "Transaction amount ($" + amount.setScale(2, RoundingMode.HALF_UP) + ") exceeds delegated mandate limit ($" + mandate.getMaxAmountPerTransaction().setScale(2, RoundingMode.HALF_UP) + ")."
            );
        }

        return new DelegatedCapabilityStatus(
                true,
                true,
                true,
                true,
                ADAPTER_NAME,
                "ELIGIBLE",
                "Authorized PayPal Vault v3 payment token verified and compliant with merchant, currency, expiry, and amount constraints."
        );
    }

    @Override
    public DelegatedExecutionOutcome executeDelegatedPayment(String negotiationId,
                                                             AgreementSeal seal,
                                                             SpendMandate mandate,
                                                             BigDecimal totalAmount,
                                                             BigDecimal itemTotal,
                                                             BigDecimal shippingTotal,
                                                             String currency,
                                                             String sellerId,
                                                             String productTitle) {
        DelegatedCapabilityStatus cap = inspectCapability(mandate, totalAmount, currency, sellerId);
        if (!cap.eligible()) {
            return new DelegatedExecutionOutcome(
                    false,
                    null,
                    null,
                    null,
                    null,
                    cap.reasonCode(),
                    cap.message(),
                    List.of(
                            "adapter: " + ADAPTER_NAME,
                            "reasonCode: " + cap.reasonCode(),
                            "accountAccessEnabled: " + cap.accountAccessEnabled(),
                            "buyerCredentialLinked: " + cap.buyerCredentialLinked()
                    )
            );
        }

        try {
            String accessToken = payPalService.getAccessToken();
            String url = baseUrl + "/v2/checkout/orders";

            String normalizedCurrency = StringUtils.hasText(currency) ? currency.trim().toUpperCase(Locale.ROOT) : "USD";
            String totalStr = totalAmount.setScale(2, RoundingMode.HALF_UP).toPlainString();
            String itemStr = itemTotal.setScale(2, RoundingMode.HALF_UP).toPlainString();
            String shipStr = shippingTotal.setScale(2, RoundingMode.HALF_UP).toPlainString();
            String refId = truncate(StringUtils.hasText(seal.getAgreementId()) ? seal.getAgreementId() : negotiationId, 127);

            Map<String, Object> breakdown = new LinkedHashMap<>();
            breakdown.put("item_total", Map.of("currency_code", normalizedCurrency, "value", itemStr));
            if (shippingTotal.compareTo(BigDecimal.ZERO) > 0) {
                breakdown.put("shipping", Map.of("currency_code", normalizedCurrency, "value", shipStr));
            }

            Map<String, Object> amountMap = new LinkedHashMap<>();
            amountMap.put("currency_code", normalizedCurrency);
            amountMap.put("value", totalStr);
            amountMap.put("breakdown", breakdown);

            Map<String, Object> purchaseUnit = new LinkedHashMap<>();
            purchaseUnit.put("reference_id", refId);
            purchaseUnit.put("description", truncate("TradeTwin Delegated Order: " + (StringUtils.hasText(productTitle) ? productTitle : refId), 127));
            purchaseUnit.put("custom_id", refId);
            purchaseUnit.put("amount", amountMap);

            Map<String, Object> paypalPaymentSource = new LinkedHashMap<>();
            paypalPaymentSource.put("vault_id", mandate.getDelegatedVaultTokenId().trim());

            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("intent", "CAPTURE");
            payload.put("purchase_units", List.of(purchaseUnit));
            payload.put("payment_source", Map.of("paypal", paypalPaymentSource));

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(accessToken);
            headers.set("PayPal-Request-Id", "TT-DELEG-" + refId);

            ResponseEntity<String> response = restTemplate.postForEntity(url, new HttpEntity<>(payload, headers), String.class);
            JsonNode root = objectMapper.readTree(response.getBody());
            String orderId = root.path("id").asText("");
            String status = root.path("status").asText("");
            if (!"COMPLETED".equalsIgnoreCase(status)) {
                return new DelegatedExecutionOutcome(
                        false,
                        orderId,
                        null,
                        null,
                        null,
                        "DELEGATED_NOT_COMPLETED",
                        "PayPal Vault order returned status [" + status + "] instead of COMPLETED.",
                        List.of("adapter: " + ADAPTER_NAME, "paypalStatus: " + status, "paypalOrderId: " + orderId)
                );
            }

            PayPalService.CaptureVerification capture = payPalService.extractAndVerifyCaptureJson(root, totalAmount, normalizedCurrency);
            return new DelegatedExecutionOutcome(
                    true,
                    orderId,
                    capture.captureId(),
                    capture.payerId(),
                    capture.payerEmail(),
                    null,
                    "Delegated PayPal Vault v3 payment captured and verified.",
                    List.of("adapter: " + ADAPTER_NAME, "paypalOrderId: " + orderId, "captureId: " + capture.captureId())
            );
        } catch (HttpStatusCodeException httpEx) {
            PayPalService.PayPalDiagnostic diag = payPalService.parsePayPalHttpDiagnostic(httpEx, "DELEGATED_VAULT_CAPTURE");
            log.warn("Delegated Vault payment failed with PayPal HTTP {}: {}", diag.httpStatus(), diag.issue());
            return new DelegatedExecutionOutcome(
                    false,
                    null,
                    null,
                    null,
                    null,
                    StringUtils.hasText(diag.issue()) ? diag.issue() : "PAYPAL_DELEGATED_REJECTED",
                    diag.toUserMessage(),
                    diag.toDetailList()
            );
        } catch (Exception ex) {
            log.error("Delegated Vault payment execution failed: {}", ex.getMessage());
            return new DelegatedExecutionOutcome(
                    false,
                    null,
                    null,
                    null,
                    null,
                    "DELEGATED_EXECUTION_ERROR",
                    "Delegated Vault execution failed: " + ex.getMessage(),
                    List.of("adapter: " + ADAPTER_NAME, "error: " + ex.getMessage())
            );
        }
    }

    private boolean looksLikeOrdinaryOAuthToken(String token) {
        String t = token.trim();
        return t.startsWith("A21AA") || t.toLowerCase(Locale.ROOT).startsWith("bearer ");
    }

    private String truncate(String val, int maxLen) {
        if (val == null) return "";
        String cleaned = val.trim();
        return cleaned.length() <= maxLen ? cleaned : cleaned.substring(0, maxLen);
    }
}
