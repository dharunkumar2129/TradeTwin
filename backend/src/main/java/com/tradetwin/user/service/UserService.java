package com.tradetwin.user.service;

import com.tradetwin.auth.dto.UserProfileDto;
import com.tradetwin.common.exception.ResourceNotFoundException;
import com.tradetwin.user.dto.UpdateProfileRequest;
import com.tradetwin.user.model.User;
import com.tradetwin.user.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
public class UserService {

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public User getUserById(String id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", id));
    }

    public User getUserByEmail(String email) {
        return userRepository.findByEmail(email.toLowerCase().trim())
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", email));
    }

    public UserProfileDto getUserProfile(String email) {
        User user = getUserByEmail(email);
        return toDto(user);
    }

    public UserProfileDto updateProfile(String email, UpdateProfileRequest request) {
        User user = getUserByEmail(email);

        if (request.getFullName() != null && !request.getFullName().trim().isEmpty()) {
            user.setFullName(request.getFullName().trim());
        }

        if (request.getDefaultCurrency() != null && !request.getDefaultCurrency().trim().isEmpty()) {
            user.setDefaultCurrency(request.getDefaultCurrency().trim().toUpperCase());
        }

        if (request.getShippingAddress() != null) {
            user.setShippingAddress(request.getShippingAddress());
        }

        if (request.getShoppingPreferences() != null) {
            user.setShoppingPreferences(request.getShoppingPreferences());
        }

        user.setUpdatedAt(Instant.now());
        User saved = userRepository.save(user);
        return toDto(saved);
    }

    public User updateReputationScore(String userId, double delta) {
        User user = getUserById(userId);
        double newScore = Math.max(0.0, Math.min(100.0, user.getReputationScore() + delta));
        user.setReputationScore(newScore);
        user.setUpdatedAt(Instant.now());
        return userRepository.save(user);
    }

    public UserProfileDto toDto(User user) {
        return UserProfileDto.builder()
                .id(user.getId())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .role(user.getRole())
                .reputationScore(user.getReputationScore())
                .defaultCurrency(user.getDefaultCurrency())
                .shippingAddress(user.getShippingAddress())
                .shoppingPreferences(user.getShoppingPreferences())
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .build();
    }
}
