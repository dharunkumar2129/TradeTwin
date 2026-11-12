package com.tradetwin.user.controller;

import com.tradetwin.auth.dto.UserProfileDto;
import com.tradetwin.common.api.ApiResponse;
import com.tradetwin.user.dto.UpdateProfileRequest;
import com.tradetwin.user.model.User;
import com.tradetwin.user.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping({"/api/users", "/api/v1/users"})
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/profile")
    public ResponseEntity<ApiResponse<UserProfileDto>> getProfile(@AuthenticationPrincipal UserDetails userDetails) {
        UserProfileDto profile = userService.getUserProfile(userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.ok(profile));
    }

    @PutMapping("/profile")
    public ResponseEntity<ApiResponse<UserProfileDto>> updateProfile(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody UpdateProfileRequest request) {
        UserProfileDto updatedProfile = userService.updateProfile(userDetails.getUsername(), request);
        return ResponseEntity.ok(ApiResponse.ok("Profile updated successfully", updatedProfile));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or authentication.name == @userService.getUserById(#id).email")
    public ResponseEntity<ApiResponse<User>> getUserById(@PathVariable String id) {
        User user = userService.getUserById(id);
        user.setPasswordHash(null);
        return ResponseEntity.ok(ApiResponse.ok(user));
    }
}
