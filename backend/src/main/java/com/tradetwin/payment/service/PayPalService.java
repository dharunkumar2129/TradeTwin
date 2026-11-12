package com.tradetwin.payment.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tradetwin.catalog.model.Product;
import com.tradetwin.catalog.service.ProductService;
import com.tradetwin.common.exception.BusinessRuleException;
import com.tradetwin.common.exception.ResourceNotFoundException;
import com.tradetwin.deal.model.AgreementSeal;
import com.tradetwin.mandate.model.MandateValidationResult;
import com.tradetwin.mandate.service.SpendMandateService;
import com.tradetwin.negotiation.model.Negotiation;
import com.tradetwin.negotiation.model.NegotiationStatus;
import com.tradetwin.negotiation.repository.NegotiationRepository;
import com.tradetwin.order.model.Order;
import com.tradetwin.order.repository.OrderRepository;
import com.tradetwin.order.service.OrderService;
import com.tradetwin.payment.model.PaymentRecord;
import com.tradetwin.payment.model.PaymentStatus;
import com.tradetwin.payment.repository.PaymentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.*;

@Service
public class PayPalService {

    private static final Logger log = LoggerFactory.getLogger(PayPalService.class);
    private static final int PAYPAL_DESCRIPTION_MAX_LEN = 127;
    private static final int PAYPAL_ID_MAX_LEN = 127;

    private final NegotiationRepository negotiationRepository;
    private final OrderService orderService;
    private final OrderRepository orderRepository;
    private final ProductService productService;
    private final PaymentRepository paymentRepository;
    private final ObjectMapper objectMapper;
    private final RestTemplate restTemplate;
    private SpendMandateService spendMandateService;
    private PaymentAuditService paymentAuditService;

    @Value("${tradetwin.paypal.client-id:}")
    private String clientId = "";

    @Value("${tradetwin.paypal.client-secret:}")
    private String clientSecret = "";

    @Value("${tradetwin.paypal.base-url:https://api-m.sandbox.paypal.com}")
    private String baseUrl = "https://api-m.sandbox.paypal.com";

    @Value("${tradetwin.paypal.currency:USD}")
    private String currency = "USD";

    @Value("${tradetwin.paypal.webhook-id:}")
    private String webhookId = "";

    // In-memory cache for OAuth access token
    private String cachedAccessToken;
    private Instant tokenExpiresAt = Instant.MIN;

    @org.springframework.beans.factory.annotation.Autowired
    public PayPalService(NegotiationRepository negotiationRepository,
                         OrderService orderService,
                         OrderRepository orderRepository,
                         ProductService productService,
                         PaymentRepository paymentRepository,
                         ObjectMapper objectMapper) {
        this(negotiationRepository, orderService, orderRepository, productService, paymentRepository, objectMapper, new RestTemplate());
    }

    public PayPalService(NegotiationRepository negotiationRepository,
                         OrderService orderService,
                         OrderRepository orderRepository,
                         ProductService productService,
                         PaymentRepository paymentRepository,
                         ObjectMapper objectMapper,
                         RestTemplate restTemplate) {
        this.negotiationRepository = negotiationRepository;
        this.orderService = orderService;
        this.orderRepository = orderRepository;
        this.productService = productService;
        this.paymentRepository = paymentRepository;
        this.objectMapper = objectMapper;
        this.restTemplate = restTemplate != null ? restTemplate : new RestTemplate();
    }

    public RestTemplate getRestTemplate() {
        return restTemplate;
    }

    @org.springframework.beans.factory.annotation.Autowired(required = false)
    public void setSpendMandateService(SpendMandateService spendMandateService) {
        this.spendMandateService = spendMandateService;
    }

    @org.springframework.beans.factory.annotation.Autowired(required = false)
    public void setPaymentAuditService(PaymentAuditService paymentAuditService) {
        this.paymentAuditService = paymentAuditService;
    }

    public boolean isLiveCredentialsConfigured() {
        return hasLiveCredentials();
    }

    public CaptureVerification extractAndVerifyCaptureJson(JsonNode orderRoot, BigDecimal expectedAmount, String expectedCurrency) {
        CaptureExtraction c = extractAndVerifyCapture(orderRoot, expectedAmount, expectedCurrency);
        return new CaptureVerification(c.captureId(), c.payerId(), c.payerEmail());
    }

    public PayPalDiagnostic parsePayPalHttpDiagnostic(HttpStatusCodeException httpEx, String stage) {
        return parsePayPalError(httpEx, stage);
    }

    /**
     * Creates a PayPal order after strictly enforcing all backend payment rules:
     * 1. Retrieve the selected accepted deal from MongoDB.
     * 2. Confirm deal status (ACCEPTED/AGREED) and active TTL lock.
     * 3. Validate seller and product information.
     * 4. Validate & ensure inventory reservation.
     * 5. Recalculate & normalize final amount + breakdown on the backend.
     * 6. Confirm the amount does not exceed buyer authorization ceiling.
     * 7. Create PayPal order using server-side credentials (surfacing sanitized diagnostics on 4xx/422).
     * 8. Never accept the payment amount supplied by frontend as authoritative.
     */
    public Map<String, Object> createOrder(String negotiationId, String dealToken) {
        // 1. Retrieve selected accepted deal from MongoDB
        Negotiation neg = negotiationRepository.findById(negotiationId)
                .orElseThrow(() -> new ResourceNotFoundException("Negotiation", "id", negotiationId));

        // 2. Confirm its state
        if (neg.getStatus() == NegotiationStatus.COMPLETED) {
            throw new BusinessRuleException("Negotiation deal has already been paid and completed");
        }
        if (neg.getStatus() != NegotiationStatus.ACCEPTED && neg.getStatus() != NegotiationStatus.AGREED) {
            throw new BusinessRuleException("Cannot create payment: Negotiation deal is not in ACCEPTED state (Current: " + neg.getStatus() + ")");
        }

        AgreementSeal seal = neg.getAgreementSeal();
        if (seal == null || !seal.getDealToken().equals(dealToken)) {
            throw new BusinessRuleException("Invalid deal token for payment authorization");
        }

        int qty = Math.max(1, seal.getQuantity());
        if (seal.getExpiresAt() != null && Instant.now().isAfter(seal.getExpiresAt())) {
            releaseReservationSafely(neg, seal, qty);
            seal.setStatus("EXPIRED");
            neg.setStatus(NegotiationStatus.EXPIRED);
            negotiationRepository.save(neg);
            throw new BusinessRuleException("Deal agreement lock has expired. Please renegotiate.");
        }

        // 3. Validate seller and product information
        Product product = productService.getProductById(neg.getProductId());
        if (!product.isActive()) {
            throw new BusinessRuleException("Product listing is no longer active; cannot proceed with checkout");
        }
        if (!product.getSellerId().equals(neg.getSellerId())) {
            throw new BusinessRuleException("Seller mismatch on product listing");
        }

        // 4. Validate inventory (re-reserve safely if a prior failed/cancelled attempt released the lock)
        if (!seal.isStockReserved()) {
            productService.reserveStock(neg.getProductId(), qty);
            seal.setStockReserved(true);
            negotiationRepository.save(neg);
        } else if (product.getInventoryCount() < qty || (product.getAvailableInventory() <= 0 && product.getReservedCount() <= 0)) {
            throw new BusinessRuleException("Product inventory is depleted; cannot proceed with checkout");
        }

        // 5. Recalculate & normalize final amount on the backend (never trust client amount)
        BigDecimal rawFinalAmount = seal.getFinalPrice();
        if (rawFinalAmount == null || rawFinalAmount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessRuleException("Invalid negotiated price on deal seal");
        }

        String dealCurrency = StringUtils.hasText(seal.getCurrency())
                ? normalizeCurrency(seal.getCurrency())
                : normalizeCurrency(this.currency);
        String gatewayCurrency = normalizeCurrency(this.currency);
        if (!dealCurrency.equals(gatewayCurrency)) {
            throw new BusinessRuleException(
                    "PayPal currency mismatch: agreement currency (" + dealCurrency + ") does not match configured PayPal currency (" + gatewayCurrency + ").",
                    List.of("stage: CREATE_ORDER", "issue: CURRENCY_MISMATCH", "field: purchase_units[0].amount.currency_code")
            );
        }
        if (StringUtils.hasText(product.getCurrency()) && !product.getCurrency().trim().equalsIgnoreCase(dealCurrency)) {
            throw new BusinessRuleException(
                    "Listing currency (" + product.getCurrency() + ") conflicts with agreement currency (" + dealCurrency + ").",
                    List.of("stage: CREATE_ORDER", "issue: CURRENCY_MISMATCH", "field: product.currency")
            );
        }

        BigDecimal finalAmount = rawFinalAmount.setScale(2, RoundingMode.HALF_UP);
        BigDecimal shippingAmount = (seal.getShippingCost() != null && seal.getShippingCost().compareTo(BigDecimal.ZERO) > 0)
                ? seal.getShippingCost().setScale(2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        if (shippingAmount.compareTo(finalAmount) > 0) {
            shippingAmount = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        BigDecimal itemTotalAmount = finalAmount.subtract(shippingAmount).setScale(2, RoundingMode.HALF_UP);

        // 6. Confirm amount does not exceed buyer authorization ceiling
        if (neg.getBuyerPreferences() != null && neg.getBuyerPreferences().getMaxCeilingPrice() != null) {
            if (finalAmount.compareTo(neg.getBuyerPreferences().getMaxCeilingPrice()) > 0) {
                throw new BusinessRuleException("Final payable amount exceeds buyer authorization ceiling");
            }
        }

        // 6b. Deterministic Spend Mandate check (strictly isolated from LLM reasoning)
        MandateValidationResult mandateValidation = validateMandateIfConfigured(neg, product, finalAmount, qty, dealCurrency);
        if (mandateValidation != null && !mandateValidation.compliant()) {
            recordAuditSafely(neg, mandateValidation, "BLOCKED_MANDATE_PRECHECK", null, null,
                    "BLOCKED_MANDATE_VIOLATION", mandateValidation.violations());
            releaseReservationSafely(neg, seal, qty);
            throw new BusinessRuleException(
                    "Spend Mandate violation: " + String.join("; ", mandateValidation.violations()),
                    mandateValidation.violations()
            );
        }

        // 6c. Reuse pre-created active PENDING PayPal order for this agreement when already prepared
        Optional<PaymentRecord> existingPending = paymentRepository.findByNegotiationId(negotiationId);
        if (existingPending.isPresent()) {
            PaymentRecord p = existingPending.get();
            boolean sameAgreement = !StringUtils.hasText(seal.getAgreementId())
                    || !StringUtils.hasText(p.getAgreementId())
                    || seal.getAgreementId().equals(p.getAgreementId());
            boolean sameAmount = p.getAmount() != null && p.getAmount().setScale(2, RoundingMode.HALF_UP).compareTo(finalAmount) == 0;
            boolean reusableMode = !hasLiveCredentials() || (p.getPaypalOrderId() != null && !p.getPaypalOrderId().startsWith("SANDBOX-"));
            if (p.getStatus() == PaymentStatus.PENDING && sameAgreement && sameAmount && reusableMode && StringUtils.hasText(p.getPaypalOrderId())) {
                recordAuditSafely(neg, mandateValidation,
                        p.getPaypalOrderId().startsWith("SANDBOX-") ? "DEV_SANDBOX_SIMULATION" : "SUPERVISED_PAYPAL_CHECKOUT",
                        p.getPaypalOrderId(), null, "PENDING_SUPERVISED_APPROVAL", List.of("reusedPreparedOrder: true"));
                Map<String, Object> reused = new LinkedHashMap<>();
                reused.put("paypalOrderId", p.getPaypalOrderId());
                reused.put("amount", finalAmount);
                reused.put("currency", dealCurrency);
                reused.put("status", "PENDING");
                reused.put("simulated", p.getPaypalOrderId().startsWith("SANDBOX-"));
                if (StringUtils.hasText(seal.getAgreementId())) {
                    reused.put("agreementId", seal.getAgreementId());
                }
                return reused;
            }
        }

        // 7. Create PayPal order via server-side credentials
        String paypalOrderId;
        boolean simulated = false;

        if (hasLiveCredentials()) {
            try {
                String accessToken = getAccessToken();
                String url = baseUrl + "/v2/checkout/orders";

                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.APPLICATION_JSON);
                headers.setBearerAuth(accessToken);
                headers.set("PayPal-Request-Id", truncate("TT-CREATE-" + negotiationId + "-" + UUID.randomUUID().toString().substring(0, 8), PAYPAL_ID_MAX_LEN));
                headers.set("Prefer", "return=representation");

                String safeDescription = truncate("TradeTwin Order: " + (product.getTitle() != null ? product.getTitle() : "Negotiated Item"), PAYPAL_DESCRIPTION_MAX_LEN);
                String safeRefId = truncate(negotiationId, PAYPAL_ID_MAX_LEN);
                String safeCustomId = truncate(StringUtils.hasText(seal.getAgreementId()) ? seal.getAgreementId() : negotiationId, PAYPAL_ID_MAX_LEN);

                Map<String, Object> breakdown = new LinkedHashMap<>();
                breakdown.put("item_total", Map.of("currency_code", dealCurrency, "value", itemTotalAmount.toPlainString()));
                breakdown.put("shipping", Map.of("currency_code", dealCurrency, "value", shippingAmount.toPlainString()));

                Map<String, Object> amountMap = new LinkedHashMap<>();
                amountMap.put("currency_code", dealCurrency);
                amountMap.put("value", finalAmount.toPlainString());
                amountMap.put("breakdown", breakdown);

                Map<String, Object> unit = new LinkedHashMap<>();
                unit.put("reference_id", safeRefId);
                unit.put("custom_id", safeCustomId);
                unit.put("description", safeDescription);
                unit.put("amount", amountMap);

                Map<String, Object> body = new LinkedHashMap<>();
                body.put("intent", "CAPTURE");
                body.put("purchase_units", List.of(unit));

                HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);
                ResponseEntity<String> response = restTemplate.postForEntity(url, request, String.class);

                JsonNode root = objectMapper.readTree(response.getBody());
                paypalOrderId = root.path("id").asText();
                if (!StringUtils.hasText(paypalOrderId)) {
                    throw new BusinessRuleException(
                            "PayPal did not return an order ID when initializing checkout.",
                            List.of("stage: CREATE_ORDER", "issue: MISSING_PAYPAL_ORDER_ID")
                    );
                }
            } catch (HttpStatusCodeException httpEx) {
                PayPalDiagnostic diag = parsePayPalError(httpEx, "CREATE_ORDER");
                log.warn("PayPal create-order rejected [httpStatus={}, name={}, issue={}, debugId={}]",
                        diag.httpStatus, diag.name, diag.issue, diag.debugId);
                saveDiagnosticFailureRecord(null, negotiationId, neg, seal, finalAmount, dealCurrency, diag, PaymentStatus.FAILED);
                releaseReservationSafely(neg, seal, qty);
                throw new BusinessRuleException(diag.toUserMessage(), diag.toDetailList());
            } catch (BusinessRuleException bre) {
                releaseReservationSafely(neg, seal, qty);
                throw bre;
            } catch (Exception ex) {
                log.error("PayPal create-order communication error: {}", ex.getMessage());
                PayPalDiagnostic diag = PayPalDiagnostic.communicationError("CREATE_ORDER", ex.getMessage());
                saveDiagnosticFailureRecord(null, negotiationId, neg, seal, finalAmount, dealCurrency, diag, PaymentStatus.FAILED);
                releaseReservationSafely(neg, seal, qty);
                throw new BusinessRuleException(diag.toUserMessage(), diag.toDetailList());
            }
        } else {
            paypalOrderId = "SANDBOX-ORDER-" + UUID.randomUUID().toString().substring(0, 12).toUpperCase();
            simulated = true;
        }

        // 8. Store initial PENDING payment record
        PaymentRecord paymentRecord = PaymentRecord.builder()
                .negotiationId(negotiationId)
                .agreementId(seal.getAgreementId())
                .buyerId(neg.getBuyerId())
                .sellerId(neg.getSellerId())
                .productId(neg.getProductId())
                .provider("PAYPAL")
                .paypalOrderId(paypalOrderId)
                .amount(finalAmount)
                .currency(dealCurrency)
                .status(PaymentStatus.PENDING)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        paymentRepository.save(paymentRecord);
        recordAuditSafely(
                neg,
                mandateValidation,
                simulated ? "DEV_SANDBOX_SIMULATION" : "SUPERVISED_PAYPAL_CHECKOUT",
                paypalOrderId,
                null,
                "PENDING_SUPERVISED_APPROVAL",
                simulated ? List.of("mode: DEV_SANDBOX_SIMULATION") : List.of("mode: SUPERVISED_PAYPAL_CHECKOUT")
        );

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("paypalOrderId", paypalOrderId);
        result.put("amount", finalAmount);
        result.put("currency", dealCurrency);
        result.put("status", "PENDING");
        result.put("simulated", simulated);
        if (StringUtils.hasText(seal.getAgreementId())) {
            result.put("agreementId", seal.getAgreementId());
        }
        return result;
    }

    /**
     * Captures an approved PayPal order, handles duplicate capture requests,
     * updates payment records, and completes order settlement.
     */
    public Order captureOrder(
            String paypalOrderId,
            String negotiationId,
            String dealToken,
            Map<String, String> shippingDetails) {

        // 1. Idempotency Check: Handle duplicate capture requests
        Optional<PaymentRecord> existingPaymentOpt = paymentRepository.findByPaypalOrderId(paypalOrderId);
        if (existingPaymentOpt.isPresent() && existingPaymentOpt.get().getStatus() == PaymentStatus.COMPLETED) {
            log.info("Duplicate capture request detected for paypalOrderId [{}]. Returning existing completed order.", paypalOrderId);
            return orderRepository.findByNegotiationId(negotiationId)
                    .orElseThrow(() -> new BusinessRuleException("Payment already completed but order record not found"));
        }

        Negotiation neg = negotiationRepository.findById(negotiationId)
                .orElseThrow(() -> new ResourceNotFoundException("Negotiation", "id", negotiationId));

        if (neg.getStatus() == NegotiationStatus.COMPLETED) {
            log.info("Duplicate capture request detected for completed negotiation [{}]. Returning existing order.", negotiationId);
            return orderRepository.findByNegotiationId(negotiationId)
                    .orElseThrow(() -> new BusinessRuleException("Negotiation already completed but order record not found"));
        }

        AgreementSeal seal = neg.getAgreementSeal();
        if (seal == null || !seal.getDealToken().equals(dealToken)) {
            throw new BusinessRuleException("Invalid agreement deal token provided for capture authorization");
        }

        int qty = Math.max(1, seal.getQuantity());
        if (seal.getExpiresAt() != null && Instant.now().isAfter(seal.getExpiresAt())) {
            releaseReservationSafely(neg, seal, qty);
            seal.setStatus("EXPIRED");
            neg.setStatus(NegotiationStatus.EXPIRED);
            negotiationRepository.save(neg);
            throw new BusinessRuleException("Deal agreement lock has expired. Please renegotiate.");
        }

        BigDecimal amount = seal.getFinalPrice().setScale(2, RoundingMode.HALF_UP);
        String expectedCurrency = StringUtils.hasText(seal.getCurrency())
                ? normalizeCurrency(seal.getCurrency())
                : normalizeCurrency(this.currency);

        // Enforce Spend Mandate deterministically before capturing (checks expiration, daily limit, per-tx limit)
        Product mandateProduct = null;
        try {
            mandateProduct = productService.getProductById(neg.getProductId());
        } catch (Exception ignored) {}
        MandateValidationResult mandateValidation = validateMandateIfConfigured(neg, mandateProduct, amount, qty, expectedCurrency);
        if (mandateValidation != null && !mandateValidation.compliant()) {
            recordAuditSafely(neg, mandateValidation, "BLOCKED_MANDATE_CAPTURE", paypalOrderId, null,
                    "BLOCKED_MANDATE_VIOLATION", mandateValidation.violations());
            releaseReservationSafely(neg, seal, qty);
            throw new BusinessRuleException(
                    "Spend Mandate violation prevents capture: " + String.join("; ", mandateValidation.violations()),
                    mandateValidation.violations()
            );
        }

        String captureId = "CAP-" + UUID.randomUUID().toString().substring(0, 10).toUpperCase();
        String payerId = null;
        String payerEmail = null;

        // 2. Perform live PayPal order inspection + capture if live credentials provided and order is not simulated
        if (hasLiveCredentials() && !paypalOrderId.startsWith("SANDBOX-")) {
            try {
                String accessToken = getAccessToken();

                // 2a. Pre-capture order inspection via PayPal Orders v2 GET
                JsonNode inspectedOrder = fetchPayPalOrder(paypalOrderId, accessToken);
                String orderStatus = inspectedOrder != null ? inspectedOrder.path("status").asText("") : "";

                if ("COMPLETED".equalsIgnoreCase(orderStatus)) {
                    CaptureExtraction extracted = extractAndVerifyCapture(inspectedOrder, amount, expectedCurrency);
                    captureId = extracted.captureId;
                    payerId = extracted.payerId;
                    payerEmail = extracted.payerEmail;
                } else if ("CREATED".equalsIgnoreCase(orderStatus) || "PAYER_ACTION_REQUIRED".equalsIgnoreCase(orderStatus)) {
                    PayPalDiagnostic diag = new PayPalDiagnostic(
                            422,
                            "UNPROCESSABLE_ENTITY",
                            "Order is in " + orderStatus + " status and has not been approved by the buyer yet.",
                            "ORDER_NOT_APPROVED",
                            "Complete PayPal Sandbox buyer approval inside the PayPal window before capturing payment.",
                            "status",
                            null,
                            "CAPTURE_ORDER"
                    );
                    saveDiagnosticFailureRecord(paypalOrderId, negotiationId, neg, seal, amount, expectedCurrency, diag, PaymentStatus.FAILED);
                    releaseReservationSafely(neg, seal, qty);
                    throw new BusinessRuleException(diag.toUserMessage(), diag.toDetailList());
                } else if ("VOIDED".equalsIgnoreCase(orderStatus)) {
                    PayPalDiagnostic diag = new PayPalDiagnostic(
                            422,
                            "UNPROCESSABLE_ENTITY",
                            "PayPal order has expired or been voided.",
                            "ORDER_VOIDED",
                            "Please click 'Continue securely with PayPal' to start a fresh checkout session.",
                            "status",
                            null,
                            "CAPTURE_ORDER"
                    );
                    saveDiagnosticFailureRecord(paypalOrderId, negotiationId, neg, seal, amount, expectedCurrency, diag, PaymentStatus.CANCELLED);
                    releaseReservationSafely(neg, seal, qty);
                    throw new BusinessRuleException(diag.toUserMessage(), diag.toDetailList());
                } else {
                    // 2b. Capture APPROVED PayPal order with idempotency header
                    String url = baseUrl + "/v2/checkout/orders/" + paypalOrderId + "/capture";

                    HttpHeaders headers = new HttpHeaders();
                    headers.setContentType(MediaType.APPLICATION_JSON);
                    headers.setBearerAuth(accessToken);
                    headers.set("PayPal-Request-Id", truncate("TT-CAP-" + paypalOrderId, PAYPAL_ID_MAX_LEN));
                    headers.set("Prefer", "return=representation");

                    HttpEntity<String> request = new HttpEntity<>("{}", headers);
                    ResponseEntity<String> response = restTemplate.postForEntity(url, request, String.class);

                    JsonNode root = objectMapper.readTree(response.getBody());
                    String status = root.path("status").asText();

                    if ("COMPLETED".equalsIgnoreCase(status)) {
                        CaptureExtraction extracted = extractAndVerifyCapture(root, amount, expectedCurrency);
                        captureId = extracted.captureId;
                        payerId = extracted.payerId;
                        payerEmail = extracted.payerEmail;
                    } else {
                        PayPalDiagnostic diag = new PayPalDiagnostic(
                                422,
                                "CAPTURE_NOT_COMPLETED",
                                "PayPal capture returned status: " + status,
                                status,
                                "Payment authorization was not completed by the funding source.",
                                "status",
                                null,
                                "CAPTURE_ORDER"
                        );
                        saveDiagnosticFailureRecord(paypalOrderId, negotiationId, neg, seal, amount, expectedCurrency, diag, PaymentStatus.DECLINED);
                        releaseReservationSafely(neg, seal, qty);
                        throw new BusinessRuleException(diag.toUserMessage(), diag.toDetailList());
                    }
                }
            } catch (BusinessRuleException bre) {
                throw bre;
            } catch (HttpStatusCodeException httpEx) {
                PayPalDiagnostic diag = parsePayPalError(httpEx, "CAPTURE_ORDER");
                if ("ORDER_ALREADY_CAPTURED".equalsIgnoreCase(diag.issue)) {
                    try {
                        JsonNode existingOrder = fetchPayPalOrder(paypalOrderId, getAccessToken());
                        if (existingOrder != null && "COMPLETED".equalsIgnoreCase(existingOrder.path("status").asText())) {
                            CaptureExtraction extracted = extractAndVerifyCapture(existingOrder, amount, expectedCurrency);
                            captureId = extracted.captureId;
                            payerId = extracted.payerId;
                            payerEmail = extracted.payerEmail;
                            log.info("Recovered already-captured PayPal order [{}]", paypalOrderId);
                        } else {
                            saveDiagnosticFailureRecord(paypalOrderId, negotiationId, neg, seal, amount, expectedCurrency, diag, PaymentStatus.DECLINED);
                            releaseReservationSafely(neg, seal, qty);
                            throw new BusinessRuleException(diag.toUserMessage(), diag.toDetailList());
                        }
                    } catch (BusinessRuleException innerBre) {
                        throw innerBre;
                    } catch (Exception recoveryEx) {
                        saveDiagnosticFailureRecord(paypalOrderId, negotiationId, neg, seal, amount, expectedCurrency, diag, PaymentStatus.DECLINED);
                        releaseReservationSafely(neg, seal, qty);
                        throw new BusinessRuleException(diag.toUserMessage(), diag.toDetailList());
                    }
                } else {
                    log.warn("PayPal capture rejected [httpStatus={}, name={}, issue={}, debugId={}]",
                            diag.httpStatus, diag.name, diag.issue, diag.debugId);
                    saveDiagnosticFailureRecord(paypalOrderId, negotiationId, neg, seal, amount, expectedCurrency, diag, PaymentStatus.DECLINED);
                    releaseReservationSafely(neg, seal, qty);
                    throw new BusinessRuleException(diag.toUserMessage(), diag.toDetailList());
                }
            } catch (Exception ex) {
                log.error("Unexpected PayPal live capture communication error: {}", ex.getMessage());
                PayPalDiagnostic diag = PayPalDiagnostic.communicationError("CAPTURE_ORDER", ex.getMessage());
                saveDiagnosticFailureRecord(paypalOrderId, negotiationId, neg, seal, amount, expectedCurrency, diag, PaymentStatus.FAILED);
                releaseReservationSafely(neg, seal, qty);
                throw new BusinessRuleException(diag.toUserMessage(), diag.toDetailList());
            }
        }

        // 3. Update or create durable PaymentRecord in MongoDB
        PaymentRecord paymentRecord = existingPaymentOpt.orElseGet(() ->
                PaymentRecord.builder()
                        .negotiationId(negotiationId)
                        .agreementId(seal.getAgreementId())
                        .buyerId(neg.getBuyerId())
                        .sellerId(neg.getSellerId())
                        .productId(neg.getProductId())
                        .provider("PAYPAL")
                        .paypalOrderId(paypalOrderId)
                        .amount(amount)
                        .currency(expectedCurrency)
                        .build()
        );

        paymentRecord.setStatus(PaymentStatus.COMPLETED);
        paymentRecord.setAgreementId(seal.getAgreementId());
        paymentRecord.setCaptureId(captureId);
        paymentRecord.setPayerId(payerId);
        paymentRecord.setPayerEmail(payerEmail);
        paymentRecord.setFailureReason(null);
        paymentRecord.setFailureStage(null);
        paymentRecord.setPaypalIssue(null);
        paymentRecord.setPaypalDetailDescription(null);
        paymentRecord.setPaidAt(Instant.now());
        paymentRecord.setUpdatedAt(Instant.now());

        paymentRepository.save(paymentRecord);
        recordAuditSafely(
                neg,
                mandateValidation,
                paypalOrderId.startsWith("SANDBOX-") ? "DEV_SANDBOX_SIMULATION" : "SUPERVISED_PAYPAL_CHECKOUT",
                paypalOrderId,
                captureId,
                "COMPLETED",
                List.of("captureStatus: COMPLETED")
        );

        // 4. Complete order via OrderService (deducts inventory, records COMPLETED state)
        return orderService.createOrderFromAgreement(negotiationId, dealToken, paymentRecord, shippingDetails);
    }

    /**
     * Retrieves PayPal order details from PayPal REST API.
     */
    public Map<String, Object> retrieveOrderDetails(String paypalOrderId) {
        if (hasLiveCredentials() && !paypalOrderId.startsWith("SANDBOX-")) {
            try {
                String accessToken = getAccessToken();
                String url = baseUrl + "/v2/checkout/orders/" + paypalOrderId;

                HttpHeaders headers = new HttpHeaders();
                headers.setBearerAuth(accessToken);

                HttpEntity<Void> request = new HttpEntity<>(headers);
                ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, request, String.class);

                JsonNode root = objectMapper.readTree(response.getBody());
                return objectMapper.convertValue(root, Map.class);
            } catch (Exception ex) {
                log.warn("Failed to retrieve live PayPal order details for {}: {}", paypalOrderId, ex.getMessage());
            }
        }

        Optional<PaymentRecord> recordOpt = paymentRepository.findByPaypalOrderId(paypalOrderId);
        if (recordOpt.isPresent()) {
            PaymentRecord r = recordOpt.get();
            return Map.of(
                    "id", r.getPaypalOrderId(),
                    "status", r.getStatus().name(),
                    "amount", Map.of("currency_code", r.getCurrency(), "value", r.getAmount().toPlainString()),
                    "provider", r.getProvider()
            );
        }

        throw new ResourceNotFoundException("PayPalOrder", "id", paypalOrderId);
    }

    /**
     * Records buyer-initiated cancellation of a pending PayPal checkout without altering the agreement lock.
     */
    public PaymentRecord cancelOrder(String paypalOrderId, String negotiationId, String reason) {
        return updateNonCompletedPaymentStatus(
                paypalOrderId,
                negotiationId,
                PaymentStatus.CANCELLED,
                reason != null && !reason.isBlank() ? reason : "Buyer cancelled checkout in PayPal window"
        );
    }

    /**
     * Records a declined or failed PayPal checkout attempt cleanly without marking any order as PAID.
     */
    public PaymentRecord recordPaymentFailure(String paypalOrderId, String negotiationId, String reason) {
        PaymentStatus status = (reason != null && reason.toUpperCase().contains("DECLIN"))
                ? PaymentStatus.DECLINED
                : PaymentStatus.FAILED;
        return updateNonCompletedPaymentStatus(
                paypalOrderId,
                negotiationId,
                status,
                reason != null && !reason.isBlank() ? reason : "PayPal transaction declined or failed"
        );
    }

    private PaymentRecord updateNonCompletedPaymentStatus(
            String paypalOrderId,
            String negotiationId,
            PaymentStatus targetStatus,
            String reason) {

        Optional<PaymentRecord> existingOpt = StringUtils.hasText(paypalOrderId)
                ? paymentRepository.findByPaypalOrderId(paypalOrderId)
                : Optional.empty();

        if (existingOpt.isEmpty() && StringUtils.hasText(negotiationId)) {
            existingOpt = paymentRepository.findByNegotiationId(negotiationId);
        }

        if (existingOpt.isPresent()) {
            PaymentRecord existing = existingOpt.get();
            if (existing.getStatus() == PaymentStatus.COMPLETED) {
                throw new BusinessRuleException("Cannot cancel or mark failed: payment has already been completed");
            }
            existing.setStatus(targetStatus);
            existing.setFailureReason(reason);
            existing.setUpdatedAt(Instant.now());
            String negId = StringUtils.hasText(negotiationId) ? negotiationId : existing.getNegotiationId();
            if (StringUtils.hasText(negId)) {
                negotiationRepository.findById(negId).ifPresent(neg -> {
                    AgreementSeal seal = neg.getAgreementSeal();
                    if (seal != null && seal.isStockReserved()) {
                        releaseReservationSafely(neg, seal, Math.max(1, seal.getQuantity()));
                    }
                    recordAuditSafely(neg, validateMandateIfConfigured(neg, null, existing.getAmount(),
                                    seal != null ? Math.max(1, seal.getQuantity()) : 1, existing.getCurrency()),
                            "SUPERVISED_PAYPAL_CHECKOUT", existing.getPaypalOrderId(), null,
                            targetStatus.name(), List.of("reason: " + reason));
                });
            }
            return paymentRepository.save(existing);
        }

        if (!StringUtils.hasText(negotiationId)) {
            throw new ResourceNotFoundException("PaymentRecord", "paypalOrderId", paypalOrderId);
        }

        Negotiation neg = negotiationRepository.findById(negotiationId)
                .orElseThrow(() -> new ResourceNotFoundException("Negotiation", "id", negotiationId));
        if (neg.getStatus() == NegotiationStatus.COMPLETED) {
            throw new BusinessRuleException("Cannot modify payment state for an already completed negotiation");
        }

        AgreementSeal seal = neg.getAgreementSeal();
        if (seal != null && seal.isStockReserved()) {
            releaseReservationSafely(neg, seal, Math.max(1, seal.getQuantity()));
        }

        BigDecimal dealAmount = seal != null && seal.getFinalPrice() != null
                ? seal.getFinalPrice()
                : BigDecimal.ZERO;

        PaymentRecord auditRecord = PaymentRecord.builder()
                .negotiationId(negotiationId)
                .agreementId(seal != null ? seal.getAgreementId() : null)
                .buyerId(neg.getBuyerId())
                .sellerId(neg.getSellerId())
                .productId(neg.getProductId())
                .provider("PAYPAL")
                .paypalOrderId(StringUtils.hasText(paypalOrderId) ? paypalOrderId : "CANCELLED-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase())
                .amount(dealAmount)
                .currency(seal != null && seal.getCurrency() != null ? seal.getCurrency() : currency)
                .status(targetStatus)
                .failureReason(reason)
                .failureStage("CLIENT")
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        return paymentRepository.save(auditRecord);
    }

    /**
     * Retrieves stored PaymentRecord by record ID, paypalOrderId, or negotiationId.
     */
    public PaymentRecord getPaymentById(String identifier) {
        return paymentRepository.findById(identifier)
                .or(() -> paymentRepository.findByPaypalOrderId(identifier))
                .or(() -> paymentRepository.findByNegotiationId(identifier))
                .orElseThrow(() -> new ResourceNotFoundException("PaymentRecord", "identifier", identifier));
    }

    /**
     * Verifies PayPal webhook signature.
     */
    public boolean verifyWebhookSignature(Map<String, String> headers, String body) {
        if (!hasLiveCredentials() || !StringUtils.hasText(webhookId)) {
            // In dev / test sandbox without live webhook configuration, verify presence of standard payload
            return body != null && !body.isBlank();
        }

        try {
            String accessToken = getAccessToken();
            String url = baseUrl + "/v1/notifications/verify-webhook-signature";

            HttpHeaders httpHeaders = new HttpHeaders();
            httpHeaders.setContentType(MediaType.APPLICATION_JSON);
            httpHeaders.setBearerAuth(accessToken);

            Map<String, Object> payload = Map.of(
                    "auth_algo", headers.getOrDefault("paypal-auth-algo", ""),
                    "cert_url", headers.getOrDefault("paypal-cert-url", ""),
                    "transmission_id", headers.getOrDefault("paypal-transmission-id", ""),
                    "transmission_sig", headers.getOrDefault("paypal-transmission-sig", ""),
                    "transmission_time", headers.getOrDefault("paypal-transmission-time", ""),
                    "webhook_id", webhookId,
                    "webhook_event", objectMapper.readTree(body)
            );

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(payload, httpHeaders);
            ResponseEntity<String> response = restTemplate.postForEntity(url, request, String.class);

            JsonNode root = objectMapper.readTree(response.getBody());
            String status = root.path("verification_status").asText();
            return "SUCCESS".equalsIgnoreCase(status);
        } catch (Exception ex) {
            log.error("Webhook signature verification exception: {}", ex.getMessage());
            return false;
        }
    }

    /**
     * Retrieves OAuth 2.0 access token with in-memory caching.
     */
    public synchronized String getAccessToken() throws Exception {
        if (cachedAccessToken != null && Instant.now().isBefore(tokenExpiresAt)) {
            return cachedAccessToken;
        }

        String authUrl = baseUrl + "/v1/oauth2/token";
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        String auth = clientId + ":" + clientSecret;
        byte[] encodedAuth = Base64.getEncoder().encode(auth.getBytes(StandardCharsets.UTF_8));
        headers.set("Authorization", "Basic " + new String(encodedAuth));

        HttpEntity<String> request = new HttpEntity<>("grant_type=client_credentials", headers);
        ResponseEntity<String> response = restTemplate.postForEntity(authUrl, request, String.class);

        JsonNode node = objectMapper.readTree(response.getBody());
        cachedAccessToken = node.path("access_token").asText();
        int expiresIn = node.path("expires_in").asInt(3600);
        tokenExpiresAt = Instant.now().plusSeconds(expiresIn - 60);

        return cachedAccessToken;
    }

    private boolean hasLiveCredentials() {
        return StringUtils.hasText(clientId)
                && !clientId.startsWith("your_")
                && !"sb".equalsIgnoreCase(clientId.trim())
                && StringUtils.hasText(clientSecret)
                && !clientSecret.startsWith("your_");
    }

    private String normalizeCurrency(String raw) {
        return StringUtils.hasText(raw) ? raw.trim().toUpperCase(Locale.ROOT) : "USD";
    }

    private String truncate(String val, int maxLen) {
        if (val == null) return "";
        String cleaned = val.trim();
        return cleaned.length() <= maxLen ? cleaned : cleaned.substring(0, maxLen);
    }

    private void releaseReservationSafely(Negotiation neg, AgreementSeal seal, int qty) {
        if (neg == null || seal == null || !seal.isStockReserved()) {
            return;
        }
        try {
            productService.releaseStock(neg.getProductId(), qty);
        } catch (Exception ex) {
            log.warn("Could not release stock reservation for product [{}]: {}", neg.getProductId(), ex.getMessage());
        }
        seal.setStockReserved(false);
        negotiationRepository.save(neg);
    }

    private JsonNode fetchPayPalOrder(String paypalOrderId, String accessToken) {
        try {
            String url = baseUrl + "/v2/checkout/orders/" + paypalOrderId;
            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(accessToken);
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, new HttpEntity<>(headers), String.class);
            if (!StringUtils.hasText(response.getBody())) {
                return null;
            }
            return objectMapper.readTree(response.getBody());
        } catch (HttpStatusCodeException ex) {
            log.debug("Pre-capture order inspection returned status {} for [{}]", ex.getStatusCode(), paypalOrderId);
            return null;
        } catch (Exception ex) {
            log.debug("Pre-capture order inspection error for [{}]: {}", paypalOrderId, ex.getMessage());
            return null;
        }
    }

    private CaptureExtraction extractAndVerifyCapture(JsonNode orderRoot, BigDecimal expectedAmount, String expectedCurrency) {
        JsonNode purchaseUnits = orderRoot.path("purchase_units");
        if (!purchaseUnits.isArray() || purchaseUnits.isEmpty()) {
            throw new BusinessRuleException(
                    "PayPal capture response was missing purchase unit verification details.",
                    List.of("stage: CAPTURE_ORDER", "issue: MISSING_PURCHASE_UNIT")
            );
        }
        JsonNode captures = purchaseUnits.get(0).path("payments").path("captures");
        if (!captures.isArray() || captures.isEmpty()) {
            throw new BusinessRuleException(
                    "PayPal order does not contain a completed capture transaction.",
                    List.of("stage: CAPTURE_ORDER", "issue: MISSING_CAPTURE_RECORD")
            );
        }
        JsonNode captureNode = captures.get(0);
        String captureStatus = captureNode.path("status").asText("");
        if (StringUtils.hasText(captureStatus) && !"COMPLETED".equalsIgnoreCase(captureStatus)) {
            throw new BusinessRuleException(
                    "PayPal capture transaction status is " + captureStatus + " instead of COMPLETED.",
                    List.of("stage: CAPTURE_ORDER", "issue: CAPTURE_" + captureStatus.toUpperCase(Locale.ROOT))
            );
        }

        JsonNode amountNode = captureNode.path("amount");
        String capturedCurrency = amountNode.path("currency_code").asText("");
        String capturedValueRaw = amountNode.path("value").asText("");
        if (StringUtils.hasText(capturedCurrency) && !capturedCurrency.equalsIgnoreCase(expectedCurrency)) {
            throw new BusinessRuleException(
                    "Captured currency (" + capturedCurrency + ") does not match agreed currency (" + expectedCurrency + ").",
                    List.of("stage: CAPTURE_ORDER", "issue: CURRENCY_MISMATCH")
            );
        }
        if (StringUtils.hasText(capturedValueRaw)) {
            BigDecimal capturedValue = new BigDecimal(capturedValueRaw).setScale(2, RoundingMode.HALF_UP);
            if (capturedValue.compareTo(expectedAmount) != 0) {
                throw new BusinessRuleException(
                        "Captured amount (" + capturedValue.toPlainString() + ") does not match agreed total (" + expectedAmount.toPlainString() + ").",
                        List.of("stage: CAPTURE_ORDER", "issue: AMOUNT_MISMATCH")
                );
            }
        }

        String captureId = captureNode.path("id").asText("CAP-" + UUID.randomUUID().toString().substring(0, 10).toUpperCase());
        String payerId = orderRoot.path("payer").path("payer_id").asText(null);
        String payerEmail = orderRoot.path("payer").path("email_address").asText(null);
        return new CaptureExtraction(captureId, payerId, payerEmail);
    }

    private PayPalDiagnostic parsePayPalError(HttpStatusCodeException httpEx, String stage) {
        int statusCode = httpEx.getStatusCode().value();
        String name = httpEx.getStatusCode().toString();
        String message = "PayPal rejected the request.";
        String issue = null;
        String detailDescription = null;
        String detailField = null;
        String debugId = null;

        String rawBody = httpEx.getResponseBodyAsString();
        if (StringUtils.hasText(rawBody)) {
            try {
                JsonNode root = objectMapper.readTree(rawBody);
                if (StringUtils.hasText(root.path("name").asText())) {
                    name = root.path("name").asText();
                }
                if (StringUtils.hasText(root.path("message").asText())) {
                    message = root.path("message").asText();
                }
                if (StringUtils.hasText(root.path("debug_id").asText())) {
                    debugId = root.path("debug_id").asText();
                }
                JsonNode details = root.path("details");
                if (details.isArray() && !details.isEmpty()) {
                    JsonNode first = details.get(0);
                    issue = first.path("issue").asText(null);
                    detailDescription = first.path("description").asText(null);
                    detailField = first.path("field").asText(null);
                }
            } catch (Exception ignore) {
                // Non-JSON response body; keep safe generic message without exposing raw payloads
            }
        }

        return new PayPalDiagnostic(statusCode, name, message, issue, detailDescription, detailField, debugId, stage);
    }

    private void saveDiagnosticFailureRecord(
            String paypalOrderId,
            String negotiationId,
            Negotiation neg,
            AgreementSeal seal,
            BigDecimal amount,
            String dealCurrency,
            PayPalDiagnostic diag,
            PaymentStatus targetStatus) {

        Optional<PaymentRecord> existingOpt = StringUtils.hasText(paypalOrderId)
                ? paymentRepository.findByPaypalOrderId(paypalOrderId)
                : Optional.empty();

        PaymentRecord record = existingOpt.orElseGet(() -> PaymentRecord.builder()
                .negotiationId(negotiationId)
                .agreementId(seal != null ? seal.getAgreementId() : null)
                .buyerId(neg.getBuyerId())
                .sellerId(neg.getSellerId())
                .productId(neg.getProductId())
                .provider("PAYPAL")
                .paypalOrderId(StringUtils.hasText(paypalOrderId) ? paypalOrderId : "FAILED-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase())
                .amount(amount)
                .currency(dealCurrency)
                .createdAt(Instant.now())
                .build());

        record.setStatus(targetStatus);
        record.setFailureStage(diag.stage);
        record.setPaypalHttpStatus(diag.httpStatus);
        record.setPaypalErrorName(diag.name);
        record.setPaypalErrorMessage(diag.message);
        record.setPaypalIssue(diag.issue);
        record.setPaypalDetailDescription(diag.detailDescription);
        record.setPaypalDetailField(diag.detailField);
        record.setDebugId(diag.debugId);
        record.setFailureReason(diag.toUserMessage());
        record.setUpdatedAt(Instant.now());
        paymentRepository.save(record);
        recordAuditSafely(
                neg,
                validateMandateIfConfigured(neg, null, amount, seal != null ? Math.max(1, seal.getQuantity()) : 1, dealCurrency),
                "SUPERVISED_PAYPAL_CHECKOUT",
                record.getPaypalOrderId(),
                null,
                targetStatus.name(),
                diag.toDetailList()
        );
    }

    private MandateValidationResult validateMandateIfConfigured(Negotiation neg,
                                                                Product product,
                                                                BigDecimal amount,
                                                                int qty,
                                                                String curr) {
        if (spendMandateService == null || neg == null || !StringUtils.hasText(neg.getBuyerId())) {
            return null;
        }
        return spendMandateService.validateDealForBuyer(neg.getBuyerId(), product, amount, qty, curr);
    }

    private void recordAuditSafely(Negotiation neg,
                                   MandateValidationResult validation,
                                   String authMethod,
                                   String paypalOrderId,
                                   String captureId,
                                   String finalStatus,
                                   List<String> diagnostics) {
        if (paymentAuditService == null || neg == null) {
            return;
        }
        try {
            paymentAuditService.recordEvent(neg, validation, authMethod, paypalOrderId, captureId, finalStatus, diagnostics);
        } catch (Exception ex) {
            log.warn("Non-blocking audit logging failure for negotiation [{}]: {}", neg.getId(), ex.getMessage());
        }
    }

    public record CaptureVerification(String captureId, String payerId, String payerEmail) {}

    private record CaptureExtraction(String captureId, String payerId, String payerEmail) {}

    public record PayPalDiagnostic(
            int httpStatus,
            String name,
            String message,
            String issue,
            String detailDescription,
            String detailField,
            String debugId,
            String stage) {

        public static PayPalDiagnostic communicationError(String stage, String message) {
            return new PayPalDiagnostic(
                    502,
                    "PAYPAL_GATEWAY_COMMUNICATION_ERROR",
                    StringUtils.hasText(message) ? message : "Unable to reach PayPal Sandbox API",
                    "GATEWAY_UNREACHABLE",
                    "Check Sandbox network connectivity and server PayPal configuration.",
                    null,
                    null,
                    stage
            );
        }

        public String toUserMessage() {
            StringBuilder sb = new StringBuilder("PayPal ");
            sb.append("CREATE_ORDER".equals(stage) ? "order creation " : "payment capture ");
            sb.append("failed (").append(httpStatus).append(" ").append(name).append(")");
            if (StringUtils.hasText(issue)) {
                sb.append(" [").append(issue).append("]");
            }
            if (StringUtils.hasText(detailDescription)) {
                sb.append(": ").append(detailDescription);
            } else if (StringUtils.hasText(message)) {
                sb.append(": ").append(message);
            }
            return sb.toString();
        }

        public List<String> toDetailList() {
            List<String> list = new ArrayList<>();
            list.add("stage: " + stage);
            list.add("httpStatus: " + httpStatus);
            if (StringUtils.hasText(name)) list.add("name: " + name);
            if (StringUtils.hasText(issue)) list.add("issue: " + issue);
            if (StringUtils.hasText(detailDescription)) list.add("description: " + detailDescription);
            if (StringUtils.hasText(detailField)) list.add("field: " + detailField);
            if (StringUtils.hasText(debugId)) list.add("debug_id: " + debugId);
            return list;
        }
    }
}
