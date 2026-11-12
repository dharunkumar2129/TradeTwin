package com.tradetwin.auth.service;

import com.tradetwin.auth.dto.AuthResponse;
import com.tradetwin.auth.dto.LoginRequest;
import com.tradetwin.auth.dto.RegisterRequest;
import com.tradetwin.auth.dto.UserProfileDto;
import com.tradetwin.auth.security.JwtTokenProvider;
import com.tradetwin.common.exception.ApiException;
import com.tradetwin.common.exception.ResourceNotFoundException;
import com.tradetwin.common.exception.UnauthorizedException;
import com.tradetwin.user.model.ShoppingPreferences;
import com.tradetwin.user.model.User;
import com.tradetwin.user.model.UserRole;
import com.tradetwin.user.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider tokenProvider;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder, JwtTokenProvider tokenProvider) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.tokenProvider = tokenProvider;
    }

    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail().toLowerCase().trim())) {
            throw new ApiException("Email is already registered: " + request.getEmail(), HttpStatus.CONFLICT);
        }

        User user = User.builder()
                .email(request.getEmail().toLowerCase().trim())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .fullName(request.getFullName().trim())
                .role(request.getRole() != null ? request.getRole() : UserRole.ROLE_BUYER)
                .reputationScore(100.0)
                .defaultCurrency("USD")
                .shoppingPreferences(new ShoppingPreferences())
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        User savedUser = userRepository.save(user);
        String token = tokenProvider.generateToken(savedUser.getId(), savedUser.getEmail(), savedUser.getRole().name());

        return AuthResponse.builder()
                .token(token)
                .id(savedUser.getId())
                .email(savedUser.getEmail())
                .fullName(savedUser.getFullName())
                .role(savedUser.getRole())
                .reputationScore(savedUser.getReputationScore())
                .defaultCurrency(savedUser.getDefaultCurrency())
                .build();
    }

    public AuthResponse login(LoginRequest request) {
        String email = request.getEmail().toLowerCase().trim();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UnauthorizedException("Invalid email or password"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new UnauthorizedException("Invalid email or password");
        }

        String token = tokenProvider.generateToken(user.getId(), user.getEmail(), user.getRole().name());

        return AuthResponse.builder()
                .token(token)
                .id(user.getId())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .role(user.getRole())
                .reputationScore(user.getReputationScore())
                .defaultCurrency(user.getDefaultCurrency())
                .build();
    }

    public UserProfileDto getCurrentUser(String email) {
        User user = userRepository.findByEmail(email.toLowerCase().trim())
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", email));

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
