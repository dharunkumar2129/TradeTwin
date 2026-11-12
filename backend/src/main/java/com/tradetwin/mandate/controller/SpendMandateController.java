package com.tradetwin.mandate.controller;

import com.tradetwin.common.api.ApiResponse;
import com.tradetwin.mandate.dto.SpendMandateDto;
import com.tradetwin.mandate.dto.SpendMandateRequest;
import com.tradetwin.mandate.service.SpendMandateService;
import com.tradetwin.user.model.User;
import com.tradetwin.user.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping({"/api/mandates", "/api/v1/mandates"})
public class SpendMandateController {

    private final SpendMandateService mandateService;
    private final UserService userService;

    public SpendMandateController(SpendMandateService mandateService, UserService userService) {
        this.mandateService = mandateService;
        this.userService = userService;
    }

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<List<SpendMandateDto>>> getMyMandates(@AuthenticationPrincipal UserDetails userDetails) {
        User buyer = userService.getUserByEmail(userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.ok("Buyer spend mandates retrieved", mandateService.getBuyerMandates(buyer.getId())));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<SpendMandateDto>> createMandate(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody SpendMandateRequest request) {
        User buyer = userService.getUserByEmail(userDetails.getUsername());
        SpendMandateDto created = mandateService.upsertMandate(buyer.getId(), null, request);
        return new ResponseEntity<>(ApiResponse.ok("Spend mandate saved cleanly", created), HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<SpendMandateDto>> updateMandate(
            @PathVariable String id,
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody SpendMandateRequest request) {
        User buyer = userService.getUserByEmail(userDetails.getUsername());
        SpendMandateDto updated = mandateService.upsertMandate(buyer.getId(), id, request);
        return ResponseEntity.ok(ApiResponse.ok("Spend mandate updated cleanly", updated));
    }

    @PostMapping("/{id}/revoke")
    public ResponseEntity<ApiResponse<SpendMandateDto>> revokeMandate(
            @PathVariable String id,
            @AuthenticationPrincipal UserDetails userDetails) {
        User buyer = userService.getUserByEmail(userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.ok("Spend mandate revoked", mandateService.revokeMandate(buyer.getId(), id)));
    }
}
