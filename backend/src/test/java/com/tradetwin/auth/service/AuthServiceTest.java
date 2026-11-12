package com.tradetwin.auth.service;

import com.tradetwin.auth.dto.AuthResponse;
import com.tradetwin.auth.dto.LoginRequest;
import com.tradetwin.auth.dto.RegisterRequest;
import com.tradetwin.auth.dto.UserProfileDto;
import com.tradetwin.auth.security.JwtTokenProvider;
import com.tradetwin.common.exception.ApiException;
import com.tradetwin.common.exception.UnauthorizedException;
import com.tradetwin.user.model.User;
import com.tradetwin.user.model.UserRole;
import com.tradetwin.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    private JwtTokenProvider tokenProvider;
    private AuthService authService;

    private User sampleUser;

    @BeforeEach
    void setUp() {
        tokenProvider = new JwtTokenProvider(
                "404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970",
                86400000L
        );
        authService = new AuthService(userRepository, passwordEncoder, tokenProvider);

        sampleUser = User.builder()
                .id("user-123")
                .email("test@example.com")
                .passwordHash("hashed_secret")
                .fullName("Test User")
                .role(UserRole.ROLE_BUYER)
                .reputationScore(100.0)
                .defaultCurrency("USD")
                .build();
    }

    @Test
    @DisplayName("Register new buyer user successfully")
    void testRegisterBuyerSuccess() {
        RegisterRequest req = RegisterRequest.builder()
                .email("newbuyer@example.com")
                .password("password123")
                .fullName("New Buyer")
                .role(UserRole.ROLE_BUYER)
                .build();

        when(userRepository.existsByEmail("newbuyer@example.com")).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("encoded_pass");
        when(userRepository.save(any(User.class))).thenReturn(sampleUser);

        AuthResponse resp = authService.register(req);

        assertNotNull(resp);
        assertNotNull(resp.getToken());
        assertTrue(tokenProvider.validateToken(resp.getToken()));
        assertEquals("test@example.com", resp.getEmail());
        verify(userRepository, times(1)).save(any(User.class));
    }

    @Test
    @DisplayName("Register fails when email already exists")
    void testRegisterDuplicateEmail() {
        RegisterRequest req = RegisterRequest.builder()
                .email("test@example.com")
                .password("password123")
                .fullName("Test User")
                .role(UserRole.ROLE_BUYER)
                .build();

        when(userRepository.existsByEmail("test@example.com")).thenReturn(true);

        assertThrows(ApiException.class, () -> authService.register(req));
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("Login succeeds with valid credentials")
    void testLoginSuccess() {
        LoginRequest req = LoginRequest.builder()
                .email("test@example.com")
                .password("correct_pass")
                .build();

        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(sampleUser));
        when(passwordEncoder.matches("correct_pass", "hashed_secret")).thenReturn(true);

        AuthResponse resp = authService.login(req);

        assertNotNull(resp);
        assertNotNull(resp.getToken());
        assertTrue(tokenProvider.validateToken(resp.getToken()));
        assertEquals(UserRole.ROLE_BUYER, resp.getRole());
    }

    @Test
    @DisplayName("Login fails with invalid password")
    void testLoginInvalidPassword() {
        LoginRequest req = LoginRequest.builder()
                .email("test@example.com")
                .password("wrong_pass")
                .build();

        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(sampleUser));
        when(passwordEncoder.matches("wrong_pass", "hashed_secret")).thenReturn(false);

        assertThrows(UnauthorizedException.class, () -> authService.login(req));
    }

    @Test
    @DisplayName("GetCurrentUser returns complete user profile DTO")
    void testGetCurrentUser() {
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(sampleUser));

        UserProfileDto profile = authService.getCurrentUser("test@example.com");

        assertNotNull(profile);
        assertEquals("user-123", profile.getId());
        assertEquals("test@example.com", profile.getEmail());
        assertEquals("USD", profile.getDefaultCurrency());
        assertEquals(UserRole.ROLE_BUYER, profile.getRole());
    }
}
