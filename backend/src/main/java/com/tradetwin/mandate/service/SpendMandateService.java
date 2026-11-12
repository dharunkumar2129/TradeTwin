package com.tradetwin.mandate.service;

import com.tradetwin.catalog.model.Product;
import com.tradetwin.common.exception.BusinessRuleException;
import com.tradetwin.common.exception.ResourceNotFoundException;
import com.tradetwin.mandate.dto.SpendMandateDto;
import com.tradetwin.mandate.dto.SpendMandateRequest;
import com.tradetwin.mandate.model.MandateStatus;
import com.tradetwin.mandate.model.MandateValidationResult;
import com.tradetwin.mandate.model.PaymentApprovalPolicy;
import com.tradetwin.mandate.model.SpendMandate;
import com.tradetwin.mandate.repository.SpendMandateRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class SpendMandateService {

    private final SpendMandateRepository mandateRepository;
    private final DailySpendLookup dailySpendLookup;

    public SpendMandateService(SpendMandateRepository mandateRepository) {
        this(mandateRepository, (buyerId, currency) -> BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP));
    }

    @Autowired
    public SpendMandateService(SpendMandateRepository mandateRepository,
                               @Autowired(required = false) DailySpendLookup dailySpendLookup) {
        this.mandateRepository = mandateRepository;
        this.dailySpendLookup = dailySpendLookup != null
                ? dailySpendLookup
                : (buyerId, currency) -> BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
    }

    public SpendMandateDto upsertMandate(String buyerId, String mandateId, SpendMandateRequest req) {
        if (req.dailySpendingLimit().compareTo(req.maxAmountPerTransaction()) < 0) {
            throw new BusinessRuleException("Daily spending limit cannot be lower than the per-transaction maximum.");
        }
        if (req.expiresAt().isBefore(Instant.now())) {
            throw new BusinessRuleException("Mandate expiration timestamp must be in the future.");
        }

        // Guard against misusing an ordinary PayPal OAuth access token as a delegated vault credential
        String sanitizedVaultToken = sanitizeDelegatedVaultToken(req.delegatedVaultTokenId());

        SpendMandate mandate;
        if (StringUtils.hasText(mandateId)) {
            mandate = mandateRepository.findById(mandateId)
                    .orElseThrow(() -> new ResourceNotFoundException("SpendMandate", "id", mandateId));
            if (!mandate.getBuyerId().equals(buyerId)) {
                throw new BusinessRuleException("You do not have permission to modify this Spend Mandate.");
            }
        } else {
            // Supersede any previous active mandate for cleanly keeping one primary active rule
            mandateRepository.findFirstByBuyerIdAndStatusOrderByUpdatedAtDesc(buyerId, MandateStatus.ACTIVE)
                    .ifPresent(existing -> {
                        existing.setStatus(MandateStatus.REVOKED);
                        existing.setUpdatedAt(Instant.now());
                        mandateRepository.save(existing);
                    });
            mandate = new SpendMandate();
            mandate.setBuyerId(buyerId);
            mandate.setCreatedAt(Instant.now());
        }

        mandate.setName(req.name().trim());
        mandate.setMaxAmountPerTransaction(req.maxAmountPerTransaction().setScale(2, RoundingMode.HALF_UP));
        mandate.setDailySpendingLimit(req.dailySpendingLimit().setScale(2, RoundingMode.HALF_UP));
        mandate.setCurrency(StringUtils.hasText(req.currency()) ? req.currency().trim().toUpperCase() : "USD");
        mandate.setAllowedCategories(normalizeList(req.allowedCategories()));
        mandate.setPreferredBrands(normalizeList(req.preferredBrands()));
        mandate.setMaxQuantity(Math.max(1, req.maxQuantity()));
        mandate.setApprovedSellerIds(normalizeList(req.approvedSellerIds()));
        mandate.setExpiresAt(req.expiresAt());
        mandate.setPaymentApprovalPolicy(req.paymentApprovalPolicy());
        mandate.setDelegatedVaultTokenId(sanitizedVaultToken);
        mandate.setDelegatedMerchantId(StringUtils.hasText(req.delegatedMerchantId()) ? req.delegatedMerchantId().trim() : null);
        mandate.setDelegatedMaxCurrency(StringUtils.hasText(req.delegatedMaxCurrency())
                ? req.delegatedMaxCurrency().trim().toUpperCase()
                : mandate.getCurrency());
        mandate.setStatus(MandateStatus.ACTIVE);
        mandate.setUpdatedAt(Instant.now());

        SpendMandate saved = mandateRepository.save(mandate);
        return toDto(saved);
    }

    public List<SpendMandateDto> getBuyerMandates(String buyerId) {
        List<SpendMandate> mandates = mandateRepository.findByBuyerIdOrderByUpdatedAtDesc(buyerId);
        List<SpendMandateDto> result = new ArrayList<>();
        for (SpendMandate m : mandates) {
            refreshExpirationStateIfNeeded(m);
            result.add(toDto(m));
        }
        return result;
    }

    public SpendMandateDto revokeMandate(String buyerId, String mandateId) {
        SpendMandate mandate = mandateRepository.findById(mandateId)
                .orElseThrow(() -> new ResourceNotFoundException("SpendMandate", "id", mandateId));
        if (!mandate.getBuyerId().equals(buyerId)) {
            throw new BusinessRuleException("You do not have permission to revoke this Spend Mandate.");
        }
        mandate.setStatus(MandateStatus.REVOKED);
        mandate.setUpdatedAt(Instant.now());
        return toDto(mandateRepository.save(mandate));
    }

    public Optional<SpendMandate> resolveEffectiveMandateForBuyer(String buyerId) {
        if (!StringUtils.hasText(buyerId)) {
            return Optional.empty();
        }
        List<SpendMandate> mandates = mandateRepository.findByBuyerIdOrderByUpdatedAtDesc(buyerId);
        if (mandates == null || mandates.isEmpty()) {
            return Optional.empty();
        }
        for (SpendMandate m : mandates) {
            refreshExpirationStateIfNeeded(m);
            if (m.getStatus() == MandateStatus.ACTIVE) {
                return Optional.of(m);
            }
        }
        // If buyer configured mandates previously and all are EXPIRED or REVOKED, return the latest so validation enforces expiry/revocation.
        return Optional.of(mandates.get(0));
    }

    /**
     * Deterministically validates a proposed deal against the buyer's Spend Mandate.
     * Completely isolated from LLM reasoning.
     */
    public MandateValidationResult validateDealForBuyer(String buyerId,
                                                        Product product,
                                                        BigDecimal totalAmount,
                                                        int quantity,
                                                        String currency) {
        Optional<SpendMandate> mandateOpt = resolveEffectiveMandateForBuyer(buyerId);
        BigDecimal normalizedAmount = totalAmount != null
                ? totalAmount.setScale(2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        String txCurrency = StringUtils.hasText(currency) ? currency.trim().toUpperCase() : "USD";
        BigDecimal spentToday = dailySpendLookup.getCompletedSpendToday(buyerId, txCurrency);
        if (spentToday == null) {
            spentToday = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        } else {
            spentToday = spentToday.setScale(2, RoundingMode.HALF_UP);
        }

        if (mandateOpt.isEmpty()) {
            return new MandateValidationResult(
                    true, false, null, "Standard Supervised Checkout",
                    PaymentApprovalPolicy.SUPERVISED_PAYPAL_CHECKOUT,
                    null, null, spentToday, spentToday.add(normalizedAmount), null, List.of()
            );
        }

        SpendMandate mandate = mandateOpt.get();
        refreshExpirationStateIfNeeded(mandate);

        List<String> violations = new ArrayList<>();

        if (mandate.getStatus() == MandateStatus.EXPIRED
                || (mandate.getExpiresAt() != null && Instant.now().isAfter(mandate.getExpiresAt()))) {
            violations.add("Spend mandate has expired (" + mandate.getExpiresAt() + "). Update or renew your mandate before settlement.");
        } else if (mandate.getStatus() == MandateStatus.REVOKED) {
            violations.add("Spend mandate [" + mandate.getName() + "] has been revoked.");
        }

        String mandateCurrency = StringUtils.hasText(mandate.getCurrency()) ? mandate.getCurrency().trim().toUpperCase() : "USD";
        if (!mandateCurrency.equals(txCurrency)) {
            violations.add("Transaction currency (" + txCurrency + ") does not match mandate currency (" + mandateCurrency + ").");
        }

        if (mandate.getMaxAmountPerTransaction() != null
                && normalizedAmount.compareTo(mandate.getMaxAmountPerTransaction()) > 0) {
            violations.add("Negotiated total ($" + normalizedAmount + ") exceeds per-transaction mandate cap ($"
                    + mandate.getMaxAmountPerTransaction().setScale(2, RoundingMode.HALF_UP) + ").");
        }

        BigDecimal projectedDaily = spentToday.add(normalizedAmount).setScale(2, RoundingMode.HALF_UP);
        if (mandate.getDailySpendingLimit() != null
                && projectedDaily.compareTo(mandate.getDailySpendingLimit()) > 0) {
            violations.add("Projected daily spend ($" + projectedDaily + ") exceeds daily spending limit ($"
                    + mandate.getDailySpendingLimit().setScale(2, RoundingMode.HALF_UP) + "; already spent today: $" + spentToday + ").");
        }

        if (mandate.getMaxQuantity() > 0 && quantity > mandate.getMaxQuantity()) {
            violations.add("Order quantity (" + quantity + ") exceeds mandate limit (" + mandate.getMaxQuantity() + ").");
        }

        if (product != null) {
            if (mandate.getAllowedCategories() != null && !mandate.getAllowedCategories().isEmpty()) {
                boolean catMatched = StringUtils.hasText(product.getCategory()) && mandate.getAllowedCategories().stream()
                        .anyMatch(c -> c.equalsIgnoreCase(product.getCategory().trim())
                                || product.getCategory().toLowerCase().contains(c.toLowerCase()));
                if (!catMatched) {
                    violations.add("Product category '" + product.getCategory() + "' is outside allowed mandate categories "
                            + mandate.getAllowedCategories() + ".");
                }
            }

            if (mandate.getPreferredBrands() != null && !mandate.getPreferredBrands().isEmpty()) {
                boolean brandMatched = StringUtils.hasText(product.getBrand()) && mandate.getPreferredBrands().stream()
                        .anyMatch(b -> b.equalsIgnoreCase(product.getBrand().trim()));
                if (!brandMatched) {
                    violations.add("Product brand '" + product.getBrand() + "' does not match mandate preferred brands "
                            + mandate.getPreferredBrands() + ".");
                }
            }

            if (mandate.getApprovedSellerIds() != null && !mandate.getApprovedSellerIds().isEmpty()) {
                boolean sellerMatched = StringUtils.hasText(product.getSellerId()) && mandate.getApprovedSellerIds().stream()
                        .anyMatch(s -> s.equalsIgnoreCase(product.getSellerId().trim()));
                if (!sellerMatched) {
                    violations.add("Seller [" + product.getSellerId() + "] is not in the approved sellers list.");
                }
            }
        }

        return new MandateValidationResult(
                violations.isEmpty(),
                true,
                mandate.getId(),
                mandate.getName(),
                mandate.getPaymentApprovalPolicy(),
                mandate.getMaxAmountPerTransaction(),
                mandate.getDailySpendingLimit(),
                spentToday,
                projectedDaily,
                mandate.getExpiresAt(),
                violations
        );
    }

    public SpendMandateDto toDto(SpendMandate mandate) {
        String currency = StringUtils.hasText(mandate.getCurrency()) ? mandate.getCurrency() : "USD";
        BigDecimal spentToday = dailySpendLookup.getCompletedSpendToday(mandate.getBuyerId(), currency);
        if (spentToday == null) spentToday = BigDecimal.ZERO;
        spentToday = spentToday.setScale(2, RoundingMode.HALF_UP);
        BigDecimal dailyLimit = mandate.getDailySpendingLimit() != null
                ? mandate.getDailySpendingLimit().setScale(2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        BigDecimal remaining = dailyLimit.subtract(spentToday).max(BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP);

        return new SpendMandateDto(
                mandate.getId(),
                mandate.getBuyerId(),
                mandate.getName(),
                mandate.getMaxAmountPerTransaction(),
                dailyLimit,
                spentToday,
                remaining,
                currency,
                mandate.getAllowedCategories(),
                mandate.getPreferredBrands(),
                mandate.getMaxQuantity(),
                mandate.getApprovedSellerIds(),
                mandate.getExpiresAt(),
                mandate.getPaymentApprovalPolicy(),
                StringUtils.hasText(mandate.getDelegatedVaultTokenId()),
                maskSecret(mandate.getDelegatedVaultTokenId()),
                mandate.getDelegatedMerchantId(),
                mandate.getDelegatedMaxCurrency(),
                mandate.getStatus(),
                mandate.getCreatedAt(),
                mandate.getUpdatedAt()
        );
    }

    private void refreshExpirationStateIfNeeded(SpendMandate mandate) {
        if (mandate.getStatus() == MandateStatus.ACTIVE
                && mandate.getExpiresAt() != null
                && Instant.now().isAfter(mandate.getExpiresAt())) {
            mandate.setStatus(MandateStatus.EXPIRED);
            mandate.setUpdatedAt(Instant.now());
            mandateRepository.save(mandate);
        }
    }

    private String sanitizeDelegatedVaultToken(String token) {
        if (!StringUtils.hasText(token)) {
            return null;
        }
        String trimmed = token.trim();
        // Explicit security check: refuse ordinary PayPal OAuth bearer/client_credentials access tokens
        if (trimmed.startsWith("A21AA") || trimmed.toLowerCase().startsWith("bearer ")) {
            throw new BusinessRuleException(
                    "Invalid delegated payment credential: ordinary PayPal OAuth access tokens cannot substitute for buyer delegated Vault payment authorization."
            );
        }
        return trimmed;
    }

    private List<String> normalizeList(List<String> raw) {
        if (raw == null || raw.isEmpty()) {
            return new ArrayList<>();
        }
        List<String> cleaned = new ArrayList<>();
        for (String item : raw) {
            if (StringUtils.hasText(item)) {
                cleaned.add(item.trim());
            }
        }
        return cleaned;
    }

    private String maskSecret(String secret) {
        if (!StringUtils.hasText(secret)) return null;
        String trimmed = secret.trim();
        if (trimmed.length() <= 6) return "***" + trimmed.charAt(trimmed.length() - 1);
        return trimmed.substring(0, 3) + "..." + trimmed.substring(trimmed.length() - 4);
    }
}
