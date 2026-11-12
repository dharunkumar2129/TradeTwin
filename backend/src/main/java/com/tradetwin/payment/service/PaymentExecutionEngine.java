package com.tradetwin.payment.service;

import com.tradetwin.catalog.model.Product;
import com.tradetwin.catalog.service.ProductService;
import com.tradetwin.common.exception.BusinessRuleException;
import com.tradetwin.common.exception.ResourceNotFoundException;
import com.tradetwin.deal.model.AgreementSeal;
import com.tradetwin.mandate.model.MandateValidationResult;
import com.tradetwin.mandate.model.PaymentApprovalPolicy;
import com.tradetwin.mandate.model.SpendMandate;
import com.tradetwin.mandate.service.SpendMandateService;
import com.tradetwin.negotiation.model.Negotiation;
import com.tradetwin.negotiation.model.NegotiationStatus;
import com.tradetwin.negotiation.repository.NegotiationRepository;
import com.tradetwin.negotiation.service.AgreementPostProcessor;
import com.tradetwin.order.model.Order;
import com.tradetwin.order.repository.OrderRepository;
import com.tradetwin.order.service.OrderService;
import com.tradetwin.payment.model.PaymentAuditEntry;
import com.tradetwin.payment.model.PaymentRecord;
import com.tradetwin.payment.model.PaymentStatus;
import com.tradetwin.payment.repository.PaymentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.*;

/**
 * Deterministic Payment Execution Engine separating financial mandate validation and
 * payment execution strictly from LLM reasoning. Supports:
 * 1) SUPERVISED_PAYPAL_CHECKOUT — auto-prepares a PayPal Sandbox order upon agreement,
 *    requires human buyer PayPal approval, verifies capture, and locks the order.
 * 2) DELEGATED_AUTONOMOUS_PAYMENT — invokes an official PayPal Vault v3 adapter only
 *    when legitimate delegated credentials & merchant Vault access exist; never fakes
 *    autonomous execution or substitutes ordinary OAuth client tokens.
 */
@Service
public class PaymentExecutionEngine implements AgreementPostProcessor {

    private static final Logger log = LoggerFactory.getLogger(PaymentExecutionEngine.class);

    private final SpendMandateService mandateService;
    private final PayPalService payPalService;
    private final DelegatedPaymentAdapter delegatedPaymentAdapter;
    private final PaymentAuditService paymentAuditService;
    private final NegotiationRepository negotiationRepository;
    private final ProductService productService;
    private final PaymentRepository paymentRepository;
    private final OrderService orderService;
    private final OrderRepository orderRepository;

    public PaymentExecutionEngine(SpendMandateService mandateService,
                                  PayPalService payPalService,
                                  DelegatedPaymentAdapter delegatedPaymentAdapter,
                                  PaymentAuditService paymentAuditService,
                                  NegotiationRepository negotiationRepository,
                                  ProductService productService,
                                  PaymentRepository paymentRepository,
                                  OrderService orderService,
                                  OrderRepository orderRepository) {
        this.mandateService = mandateService;
        this.payPalService = payPalService;
        this.delegatedPaymentAdapter = delegatedPaymentAdapter;
        this.paymentAuditService = paymentAuditService;
        this.negotiationRepository = negotiationRepository;
        this.productService = productService;
        this.paymentRepository = paymentRepository;
        this.orderService = orderService;
        this.orderRepository = orderRepository;
    }

    /**
     * Automatically invoked when buyer and seller agents seal an agreement.
     * Validates the buyer's Spend Mandate and, for Supervised PayPal Checkout,
     * pre-creates the PayPal Sandbox order ready for buyer authorization on CheckoutPage.
     */
    @Override
    public void onAgreementSealed(Negotiation neg) {
        if (neg == null || neg.getAgreementSeal() == null) {
            return;
        }
        AgreementSeal seal = neg.getAgreementSeal();
        Product product = productService.getProductById(neg.getProductId());
        int qty = Math.max(1, seal.getQuantity());
        BigDecimal total = seal.getFinalPrice() != null
                ? seal.getFinalPrice().setScale(2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;
        String currency = StringUtils.hasText(seal.getCurrency()) ? seal.getCurrency() : "USD";

        MandateValidationResult validation = mandateService.validateDealForBuyer(
                neg.getBuyerId(), product, total, qty, currency
        );
        seal.setMandateId(validation.mandateId());
        seal.setPaymentExecutionMode(validation.paymentApprovalPolicy().name());

        if (!validation.compliant()) {
            seal.setPaymentExecutionMode("MANDATE_VIOLATION_BLOCKED");
            negotiationRepository.save(neg);
            paymentAuditService.recordEvent(
                    neg, validation, "BLOCKED_MANDATE_PRECHECK",
                    null, null, "BLOCKED_MANDATE_VIOLATION", validation.violations()
            );
            log.warn("Agreement [{}] failed Spend Mandate validation for buyer [{}]: {}",
                    seal.getAgreementId(), neg.getBuyerId(), validation.violations());
            return;
        }

        if (validation.paymentApprovalPolicy() == PaymentApprovalPolicy.SUPERVISED_PAYPAL_CHECKOUT) {
            try {
                Map<String, Object> orderInit = payPalService.createOrder(neg.getId(), seal.getDealToken());
                String paypalOrderId = orderInit != null ? String.valueOf(orderInit.getOrDefault("paypalOrderId", "")) : "";
                if (StringUtils.hasText(paypalOrderId)) {
                    seal.setPreparedPaypalOrderId(paypalOrderId);
                    negotiationRepository.save(neg);
                }
            } catch (Exception ex) {
                // Keep agreement sealed safely; CheckoutPage will display exact diagnostics on retry
                log.warn("Supervised automatic PayPal order pre-creation deferred for [{}]: {}", neg.getId(), ex.getMessage());
            }
        } else {
            SpendMandate mandate = mandateService.resolveEffectiveMandateForBuyer(neg.getBuyerId()).orElse(null);
            DelegatedPaymentAdapter.DelegatedCapabilityStatus cap = delegatedPaymentAdapter.inspectCapability(
                    mandate, total, currency, neg.getSellerId()
            );
            paymentAuditService.recordEvent(
                    neg, validation, "DELEGATED_PAYPAL_VAULT_V3",
                    null, null,
                    cap.eligible() ? "AWAITING_DELEGATED_EXECUTION" : cap.reasonCode(),
                    List.of(cap.message())
            );
        }
    }

    public Map<String, Object> getExecutionStatus(String negotiationId, String buyerId) {
        Negotiation neg = negotiationRepository.findById(negotiationId)
                .orElseThrow(() -> new ResourceNotFoundException("Negotiation", "id", negotiationId));
        if (StringUtils.hasText(buyerId) && !buyerId.equals(neg.getBuyerId())) {
            throw new BusinessRuleException("Access denied: you can only inspect payment execution status for your own negotiation.");
        }

        AgreementSeal seal = neg.getAgreementSeal();
        Product product = null;
        try {
            product = productService.getProductById(neg.getProductId());
        } catch (Exception ignored) {}

        int qty = seal != null ? Math.max(1, seal.getQuantity()) : 1;
        BigDecimal total = seal != null && seal.getFinalPrice() != null
                ? seal.getFinalPrice().setScale(2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;
        String currency = seal != null && StringUtils.hasText(seal.getCurrency()) ? seal.getCurrency() : "USD";

        MandateValidationResult validation = mandateService.validateDealForBuyer(
                neg.getBuyerId(), product, total, qty, currency
        );
        SpendMandate mandate = mandateService.resolveEffectiveMandateForBuyer(neg.getBuyerId()).orElse(null);
        DelegatedPaymentAdapter.DelegatedCapabilityStatus delegatedCap =
                delegatedPaymentAdapter.inspectCapability(mandate, total, currency, neg.getSellerId());

        String preparedOrderId = seal != null ? seal.getPreparedPaypalOrderId() : null;
        Optional<PaymentRecord> latestPayment = paymentRepository.findByNegotiationId(negotiationId);
        if (!StringUtils.hasText(preparedOrderId) && latestPayment.isPresent()
                && latestPayment.get().getStatus() == PaymentStatus.PENDING) {
            preparedOrderId = latestPayment.get().getPaypalOrderId();
        }

        PaymentAuditEntry latestAudit = paymentAuditService.getAuditHistoryForBuyer(neg.getBuyerId()).stream()
                .filter(a -> negotiationId.equals(a.getNegotiationId()))
                .findFirst()
                .orElse(null);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("negotiationId", negotiationId);
        result.put("paymentExecutionMode", validation.paymentApprovalPolicy().name());
        result.put("preparedPaypalOrderId", preparedOrderId);
        result.put("mandateValidation", validation);
        result.put("delegatedCapability", delegatedCap);
        result.put("latestAudit", latestAudit);
        return result;
    }

    /**
     * Executes payment via official Delegated Vault Adapter ONLY when legitimate Vault
     * credentials and PayPal account Vault access are present AND all Spend Mandate limits pass.
     * Never simulates a delegated payment when access or credentials are unavailable.
     */
    public Order executeDelegatedPayment(String negotiationId,
                                         String dealToken,
                                         String buyerId,
                                         Map<String, String> shippingDetails) {
        Negotiation neg = negotiationRepository.findById(negotiationId)
                .orElseThrow(() -> new ResourceNotFoundException("Negotiation", "id", negotiationId));

        if (StringUtils.hasText(buyerId) && !buyerId.equals(neg.getBuyerId())) {
            throw new BusinessRuleException("Only the owning buyer can initiate delegated settlement.");
        }

        if (neg.getStatus() == NegotiationStatus.COMPLETED) {
            return orderRepository.findByNegotiationId(negotiationId)
                    .orElseThrow(() -> new BusinessRuleException("Deal already settled cleanly."));
        }

        if (neg.getStatus() != NegotiationStatus.ACCEPTED && neg.getStatus() != NegotiationStatus.AGREED) {
            throw new BusinessRuleException("Deal must be in ACCEPTED state before payment execution.");
        }

        AgreementSeal seal = neg.getAgreementSeal();
        if (seal == null || !seal.getDealToken().equals(dealToken)) {
            throw new BusinessRuleException("Invalid agreement deal token for delegated execution.");
        }

        int qty = Math.max(1, seal.getQuantity());
        if (seal.getExpiresAt() != null && Instant.now().isAfter(seal.getExpiresAt())) {
            releaseReservationSafely(neg, seal, qty);
            seal.setStatus("EXPIRED");
            neg.setStatus(NegotiationStatus.EXPIRED);
            negotiationRepository.save(neg);
            throw new BusinessRuleException("Deal agreement lock has expired.");
        }

        Product product = productService.getProductById(neg.getProductId());
        if (!product.isActive()) {
            throw new BusinessRuleException("Listing is inactive and cannot be settled.");
        }

        BigDecimal totalAmount = seal.getFinalPrice().setScale(2, RoundingMode.HALF_UP);
        BigDecimal shippingAmount = (seal.getShippingCost() != null && seal.getShippingCost().compareTo(BigDecimal.ZERO) > 0)
                ? seal.getShippingCost().setScale(2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        if (shippingAmount.compareTo(totalAmount) > 0) {
            shippingAmount = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        BigDecimal itemTotal = totalAmount.subtract(shippingAmount).setScale(2, RoundingMode.HALF_UP);
        String currency = StringUtils.hasText(seal.getCurrency()) ? seal.getCurrency().trim().toUpperCase(Locale.ROOT) : "USD";

        // Strict deterministic Spend Mandate enforcement (separate from LLM reasoning)
        MandateValidationResult validation = mandateService.validateDealForBuyer(
                neg.getBuyerId(), product, totalAmount, qty, currency
        );
        if (!validation.compliant()) {
            releaseReservationSafely(neg, seal, qty);
            paymentAuditService.recordEvent(
                    neg, validation, "DELEGATED_PAYPAL_VAULT_V3",
                    null, null, "BLOCKED_MANDATE_VIOLATION", validation.violations()
            );
            throw new BusinessRuleException(
                    "Spend Mandate violation blocked delegated execution: " + String.join("; ", validation.violations()),
                    validation.violations()
            );
        }

        if (validation.paymentApprovalPolicy() != PaymentApprovalPolicy.DELEGATED_AUTONOMOUS_PAYMENT) {
            throw new BusinessRuleException(
                    "Buyer Spend Mandate policy is set to SUPERVISED_PAYPAL_CHECKOUT. Complete approval through Supervised PayPal Checkout.",
                    List.of("policy: SUPERVISED_PAYPAL_CHECKOUT")
            );
        }

        SpendMandate mandate = mandateService.resolveEffectiveMandateForBuyer(neg.getBuyerId())
                .orElseThrow(() -> new BusinessRuleException("No active Spend Mandate found for delegated execution."));

        DelegatedPaymentAdapter.DelegatedCapabilityStatus cap = delegatedPaymentAdapter.inspectCapability(
                mandate, totalAmount, currency, neg.getSellerId()
        );
        if (!cap.eligible()) {
            releaseReservationSafely(neg, seal, qty);
            List<String> diagList = List.of(
                    "adapter: " + cap.adapterName(),
                    "reasonCode: " + cap.reasonCode(),
                    "accountAccessEnabled: " + cap.accountAccessEnabled(),
                    "buyerCredentialLinked: " + cap.buyerCredentialLinked()
            );
            paymentAuditService.recordEvent(
                    neg, validation, "DELEGATED_PAYPAL_VAULT_V3",
                    null, null, cap.reasonCode(), diagList
            );
            throw new BusinessRuleException(cap.message(), diagList);
        }

        if (!seal.isStockReserved()) {
            productService.reserveStock(neg.getProductId(), qty);
            seal.setStockReserved(true);
            negotiationRepository.save(neg);
        }

        DelegatedPaymentAdapter.DelegatedExecutionOutcome outcome = delegatedPaymentAdapter.executeDelegatedPayment(
                negotiationId, seal, mandate, totalAmount, itemTotal, shippingAmount, currency, neg.getSellerId(), product.getTitle()
        );
        if (!outcome.settled()) {
            releaseReservationSafely(neg, seal, qty);
            PaymentRecord failed = PaymentRecord.builder()
                    .negotiationId(negotiationId)
                    .agreementId(seal.getAgreementId())
                    .buyerId(neg.getBuyerId())
                    .sellerId(neg.getSellerId())
                    .productId(neg.getProductId())
                    .provider("PAYPAL_VAULT")
                    .paypalOrderId(StringUtils.hasText(outcome.paypalOrderId()) ? outcome.paypalOrderId() : "DELEGATED-FAILED")
                    .amount(totalAmount)
                    .currency(currency)
                    .status(PaymentStatus.FAILED)
                    .failureStage("DELEGATED_VAULT_CAPTURE")
                    .paypalIssue(outcome.failureCode())
                    .failureReason(outcome.message())
                    .createdAt(Instant.now())
                    .updatedAt(Instant.now())
                    .build();
            paymentRepository.save(failed);

            paymentAuditService.recordEvent(
                    neg, validation, "DELEGATED_PAYPAL_VAULT_V3",
                    outcome.paypalOrderId(), null, outcome.failureCode(), outcome.diagnostics()
            );
            throw new BusinessRuleException(outcome.message(), outcome.diagnostics());
        }

        PaymentRecord record = paymentRepository.findByNegotiationId(negotiationId).orElseGet(() ->
                PaymentRecord.builder()
                        .negotiationId(negotiationId)
                        .agreementId(seal.getAgreementId())
                        .buyerId(neg.getBuyerId())
                        .sellerId(neg.getSellerId())
                        .productId(neg.getProductId())
                        .provider("PAYPAL_VAULT")
                        .amount(totalAmount)
                        .currency(currency)
                        .createdAt(Instant.now())
                        .build()
        );
        record.setPaypalOrderId(outcome.paypalOrderId());
        record.setCaptureId(outcome.captureId());
        record.setPayerId(outcome.payerId());
        record.setPayerEmail(outcome.payerEmail());
        record.setStatus(PaymentStatus.COMPLETED);
        record.setPaidAt(Instant.now());
        record.setUpdatedAt(Instant.now());
        paymentRepository.save(record);

        paymentAuditService.recordEvent(
                neg, validation, "DELEGATED_PAYPAL_VAULT_V3",
                outcome.paypalOrderId(), outcome.captureId(), "COMPLETED", outcome.diagnostics()
        );

        return orderService.createOrderFromAgreement(negotiationId, dealToken, record, shippingDetails);
    }

    private void releaseReservationSafely(Negotiation neg, AgreementSeal seal, int qty) {
        if (neg == null || seal == null || !seal.isStockReserved()) return;
        try {
            productService.releaseStock(neg.getProductId(), qty);
        } catch (Exception ignored) {}
        seal.setStockReserved(false);
        negotiationRepository.save(neg);
    }
}
