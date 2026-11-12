package com.tradetwin.seller.controller;

import com.tradetwin.common.api.ApiResponse;
import com.tradetwin.seller.dto.SellerDashboardSummaryDto;
import com.tradetwin.seller.dto.SellerOnboardingRequest;
import com.tradetwin.seller.model.SellerProfile;
import com.tradetwin.seller.service.SellerService;
import com.tradetwin.user.model.User;
import com.tradetwin.user.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping({"/api/sellers", "/api/v1/sellers", "/api/v1/seller"})
public class SellerController {

    private final SellerService sellerService;
    private final UserService userService;

    public SellerController(SellerService sellerService, UserService userService) {
        this.sellerService = sellerService;
        this.userService = userService;
    }

    @PostMapping("/onboard")
    @PreAuthorize("hasRole('SELLER') or hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<SellerProfile>> onboard(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody SellerOnboardingRequest request) {
        User user = userService.getUserByEmail(userDetails.getUsername());
        SellerProfile profile = sellerService.onboardSeller(user.getId(), request);
        return new ResponseEntity<>(ApiResponse.ok("Seller onboarding completed successfully", profile), HttpStatus.CREATED);
    }

    @GetMapping("/me")
    @PreAuthorize("hasRole('SELLER') or hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<SellerProfile>> getMySellerProfile(@AuthenticationPrincipal UserDetails userDetails) {
        User user = userService.getUserByEmail(userDetails.getUsername());
        SellerProfile profile = sellerService.getProfileByUserId(user.getId());
        return ResponseEntity.ok(ApiResponse.ok(profile));
    }

    @GetMapping("/profile")
    @PreAuthorize("hasRole('SELLER') or hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<SellerProfile>> getProfile(@AuthenticationPrincipal UserDetails userDetails) {
        User user = userService.getUserByEmail(userDetails.getUsername());
        SellerProfile profile = sellerService.getProfileByUserId(user.getId());
        return ResponseEntity.ok(ApiResponse.ok(profile));
    }

    @PutMapping("/profile")
    @PreAuthorize("hasRole('SELLER') or hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<SellerProfile>> updateProfile(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestBody SellerProfile updateRequest) {
        User user = userService.getUserByEmail(userDetails.getUsername());
        SellerProfile profile = sellerService.updateProfile(user.getId(), updateRequest);
        return ResponseEntity.ok(ApiResponse.ok("Seller profile updated successfully", profile));
    }

    @GetMapping("/dashboard-summary")
    @PreAuthorize("hasRole('SELLER') or hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<SellerDashboardSummaryDto>> getDashboardSummary(
            @AuthenticationPrincipal UserDetails userDetails) {
        User user = userService.getUserByEmail(userDetails.getUsername());
        SellerDashboardSummaryDto summary = sellerService.getDashboardSummary(user.getId());
        return ResponseEntity.ok(ApiResponse.ok(summary));
    }
}
