package com.tradetwin.intent.controller;

import com.tradetwin.common.api.ApiResponse;
import com.tradetwin.intent.model.BuyerIntent;
import com.tradetwin.intent.service.IntentParsingService;
import com.tradetwin.user.model.User;
import com.tradetwin.user.service.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping({"/api/intents", "/api/v1/intents", "/api/intent", "/api/v1/intent"})
public class IntentController {

    private final IntentParsingService intentParsingService;
    private final UserService userService;

    public IntentController(IntentParsingService intentParsingService, UserService userService) {
        this.intentParsingService = intentParsingService;
        this.userService = userService;
    }

    @PostMapping("/parse")
    public ResponseEntity<ApiResponse<Map<String, Object>>> parseIntent(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestBody Map<String, String> request) {
        String query = request.getOrDefault("query", "");
        String buyerId = resolveBuyerId(userDetails);
        Map<String, Object> result = intentParsingService.parseAndMatch(query, buyerId);
        return ResponseEntity.ok(ApiResponse.ok("Intent parsed successfully", result));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<BuyerIntent>> confirmIntent(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestBody BuyerIntent intent) {
        String buyerId = resolveBuyerId(userDetails);
        BuyerIntent confirmed = intentParsingService.confirmIntent(intent, buyerId);
        return new ResponseEntity<>(ApiResponse.ok("Intent confirmed successfully", confirmed), HttpStatus.CREATED);
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<BuyerIntent>>> getMyIntents(
            @AuthenticationPrincipal UserDetails userDetails) {
        String buyerId = resolveBuyerId(userDetails);
        if (buyerId == null) {
            return ResponseEntity.ok(ApiResponse.ok(List.of()));
        }
        List<BuyerIntent> intents = intentParsingService.getIntentsForBuyer(buyerId);
        return ResponseEntity.ok(ApiResponse.ok(intents));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<BuyerIntent>> getIntentById(
            @PathVariable String id,
            @AuthenticationPrincipal UserDetails userDetails) {
        BuyerIntent intent = intentParsingService.getIntentById(id);
        if (intent != null && intent.getBuyerId() != null) {
            String callerId = resolveBuyerId(userDetails);
            if (!intent.getBuyerId().equals(callerId)) {
                BuyerIntent masked = BuyerIntent.builder()
                        .id(intent.getId())
                        .buyerId(intent.getBuyerId())
                        .rawQuery(intent.getRawQuery())
                        .productType(intent.getProductType())
                        .brandPreferences(intent.getBrandPreferences())
                        .requiredSpecifications(intent.getRequiredSpecifications())
                        .optionalPreferences(intent.getOptionalPreferences())
                        .maxBudget(null)
                        .targetBudget(null)
                        .quantity(intent.getQuantity())
                        .minWarrantyMonths(intent.getMinWarrantyMonths())
                        .maxDeliveryDays(intent.getMaxDeliveryDays())
                        .preferredConditions(intent.getPreferredConditions())
                        .shippingLocation(intent.getShippingLocation())
                        .currency(intent.getCurrency())
                        .ambiguous(intent.isAmbiguous())
                        .followUpQuestion(intent.getFollowUpQuestion())
                        .status(intent.getStatus())
                        .category(intent.getCategory())
                        .keywords(intent.getKeywords())
                        .urgency(intent.getUrgency())
                        .keyRequirements(intent.getKeyRequirements())
                        .createdAt(intent.getCreatedAt())
                        .updatedAt(intent.getUpdatedAt())
                        .build();
                return ResponseEntity.ok(ApiResponse.ok(masked));
            }
        }
        return ResponseEntity.ok(ApiResponse.ok(intent));
    }

    private String resolveBuyerId(UserDetails userDetails) {
        if (userDetails != null && userDetails.getUsername() != null) {
            try {
                User user = userService.getUserByEmail(userDetails.getUsername());
                return user.getId();
            } catch (Exception ignored) {}
        }
        return null;
    }
}
