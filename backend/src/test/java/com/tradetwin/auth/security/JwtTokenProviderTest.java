package com.tradetwin.auth.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class JwtTokenProviderTest {

    private JwtTokenProvider tokenProvider;
    private final String secret = "404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970";

    @BeforeEach
    void setUp() {
        tokenProvider = new JwtTokenProvider(secret, 3600000L);
    }

    @Test
    @DisplayName("Should generate and validate JWT token successfully")
    void testTokenGenerationAndValidation() {
        String token = tokenProvider.generateToken("user123", "alice@example.com", "ROLE_BUYER");
        assertNotNull(token);
        assertTrue(tokenProvider.validateToken(token));
        assertEquals("user123", tokenProvider.getUserIdFromToken(token));
        assertEquals("alice@example.com", tokenProvider.getEmailFromToken(token));
    }

    @Test
    @DisplayName("Should reject malformed or tampered token")
    void testInvalidToken() {
        assertFalse(tokenProvider.validateToken("invalid.token.structure"));
    }

    @Test
    @DisplayName("Should fail fast when JWT_SECRET is missing or shorter than 32 chars")
    void testMissingOrShortSecretThrows() {
        assertThrows(IllegalStateException.class, () -> new JwtTokenProvider("", 3600000L));
        assertThrows(IllegalStateException.class, () -> new JwtTokenProvider("short-secret", 3600000L));
    }
}
