package com.tradetwin.negotiation.controller;

import com.tradetwin.common.api.ApiResponse;
import com.tradetwin.deal.model.FairDealComparisonResult;
import com.tradetwin.event.model.NegotiationEventDocument;
import com.tradetwin.event.service.SseStreamingHub;
import com.tradetwin.negotiation.dto.SelectDealRequest;
import com.tradetwin.negotiation.dto.StartNegotiationRequest;
import com.tradetwin.negotiation.model.BuyerPreferences;
import com.tradetwin.negotiation.model.Negotiation;
import com.tradetwin.negotiation.service.NegotiationOrchestrator;
import com.tradetwin.user.model.User;
import com.tradetwin.user.model.UserRole;
import com.tradetwin.user.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;

@RestController
@RequestMapping({"/api/negotiations", "/api/v1/negotiations"})
public class NegotiationController {

    private final NegotiationOrchestrator negotiationOrchestrator;
    private final UserService userService;
    private final SseStreamingHub sseStreamingHub;

    public NegotiationController(NegotiationOrchestrator negotiationOrchestrator, UserService userService, SseStreamingHub sseStreamingHub) {
        this.negotiationOrchestrator = negotiationOrchestrator;
        this.userService = userService;
        this.sseStreamingHub = sseStreamingHub;
    }

    @PostMapping("/start")
    public ResponseEntity<ApiResponse<Negotiation>> startNegotiation(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody StartNegotiationRequest request) {
        User buyer = userService.getUserByEmail(userDetails.getUsername());
        Negotiation negotiation = negotiationOrchestrator.startNegotiation(buyer.getId(), request);
        return new ResponseEntity<>(ApiResponse.ok("Negotiation initiated", negotiation), HttpStatus.CREATED);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<Negotiation>> getNegotiationById(
            @PathVariable String id,
            @AuthenticationPrincipal UserDetails userDetails) {
        Negotiation negotiation = negotiationOrchestrator.getNegotiationById(id);
        return ResponseEntity.ok(ApiResponse.ok(sanitizeNegotiationForCaller(negotiation, userDetails)));
    }

    private Negotiation sanitizeNegotiationForCaller(Negotiation neg, UserDetails userDetails) {
        if (neg == null || neg.getBuyerPreferences() == null) {
            return neg;
        }
        boolean authorizedOwner = false;
        if (userDetails != null && userDetails.getUsername() != null) {
            try {
                User caller = userService.getUserByEmail(userDetails.getUsername());
                if (caller != null && (caller.getRole() == UserRole.ROLE_ADMIN
                        || (neg.getBuyerId() != null && neg.getBuyerId().equals(caller.getId())))) {
                    authorizedOwner = true;
                }
            } catch (Exception ignored) {}
        }
        if (authorizedOwner) {
            return neg;
        }
        // Mask buyer's private reserve ceiling for unauthenticated or non-owner callers
        BuyerPreferences pref = neg.getBuyerPreferences();
        BuyerPreferences maskedPref = BuyerPreferences.builder()
                .targetPrice(pref.getTargetPrice())
                .maxCeilingPrice(null)
                .urgency(pref.getUrgency())
                .desiredDeliveryDays(pref.getDesiredDeliveryDays())
                .requireExtendedWarranty(pref.isRequireExtendedWarranty())
                .quantity(pref.getQuantity())
                .minWarrantyMonths(pref.getMinWarrantyMonths())
                .requiredBrand(pref.getRequiredBrand())
                .preferredBrand(pref.getPreferredBrand())
                .buyerPrompt(pref.getBuyerPrompt())
                .build();
        Negotiation copy = Negotiation.builder()
                .id(neg.getId())
                .productId(neg.getProductId())
                .buyerId(neg.getBuyerId())
                .sellerId(neg.getSellerId())
                .status(neg.getStatus())
                .roundCount(neg.getRoundCount())
                .maxRounds(neg.getMaxRounds())
                .buyerPreferences(maskedPref)
                .currentOffer(neg.getCurrentOffer())
                .turns(neg.getTurns())
                .offers(neg.getOffers())
                .participatingSellerIds(neg.getParticipatingSellerIds())
                .participatingProductIds(neg.getParticipatingProductIds())
                .fairDealMetrics(neg.getFairDealMetrics())
                .agreementSeal(neg.getAgreementSeal())
                .createdAt(neg.getCreatedAt())
                .updatedAt(neg.getUpdatedAt())
                .build();
        copy.setBuyerPrompt(neg.getBuyerPrompt());
        copy.setProductTitle(neg.getProductTitle());
        copy.setProductImage(neg.getProductImage());
        copy.setCurrentSellerStoreName(neg.getCurrentSellerStoreName());
        copy.setCurrentSellerIndex(neg.getCurrentSellerIndex());
        copy.setSellerQueue(neg.getSellerQueue());
        copy.setFailedSellerAttempts(neg.getFailedSellerAttempts());
        copy.setFallbackNotice(neg.getFallbackNotice());
        copy.setNoDealSummary(neg.getNoDealSummary());
        copy.setAlternatives(neg.getAlternatives());
        return copy;
    }

    @GetMapping(value = "/{id}/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamNegotiation(@PathVariable String id) {
        return sseStreamingHub.subscribe(id);
    }

    @GetMapping("/{id}/events")
    public ResponseEntity<ApiResponse<List<NegotiationEventDocument>>> getPersistedEvents(@PathVariable String id) {
        List<NegotiationEventDocument> events = sseStreamingHub.getPersistedEvents(id);
        return ResponseEntity.ok(ApiResponse.ok("Persisted events retrieved", events));
    }

    @GetMapping("/{id}/compare-offers")
    public ResponseEntity<ApiResponse<FairDealComparisonResult>> compareOffers(@PathVariable String id) {
        FairDealComparisonResult result = negotiationOrchestrator.compareOffersForNegotiation(id);
        return ResponseEntity.ok(ApiResponse.ok("FairDeal comparison computed", result));
    }

    @PostMapping("/{id}/select-deal")
    public ResponseEntity<ApiResponse<Negotiation>> selectDeal(
            @PathVariable String id,
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody SelectDealRequest request) {
        User buyer = userService.getUserByEmail(userDetails.getUsername());
        Negotiation negotiation = negotiationOrchestrator.selectAndApproveDeal(id, request.offerId(), buyer.getId());
        return ResponseEntity.ok(ApiResponse.ok("Deal selected and approved", negotiation));
    }

    @GetMapping("/buyer/my-negotiations")
    public ResponseEntity<ApiResponse<List<Negotiation>>> getMyNegotiations(@AuthenticationPrincipal UserDetails userDetails) {
        User buyer = userService.getUserByEmail(userDetails.getUsername());
        List<Negotiation> list = negotiationOrchestrator.getNegotiationsForBuyer(buyer.getId());
        return ResponseEntity.ok(ApiResponse.ok(list));
    }
}
