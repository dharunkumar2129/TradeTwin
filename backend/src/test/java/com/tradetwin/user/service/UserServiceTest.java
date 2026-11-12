package com.tradetwin.user.service;

import com.tradetwin.auth.dto.UserProfileDto;
import com.tradetwin.user.dto.UpdateProfileRequest;
import com.tradetwin.user.model.ShippingAddress;
import com.tradetwin.user.model.ShoppingPreferences;
import com.tradetwin.user.model.User;
import com.tradetwin.user.model.UserRole;
import com.tradetwin.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserService userService;

    private User sampleUser;

    @BeforeEach
    void setUp() {
        sampleUser = User.builder()
                .id("u1")
                .email("buyer@test.com")
                .fullName("Original Name")
                .role(UserRole.ROLE_BUYER)
                .defaultCurrency("USD")
                .build();
    }

    @Test
    @DisplayName("Update user profile updates address, currency, and preferences")
    void testUpdateProfile() {
        ShippingAddress newAddress = ShippingAddress.builder()
                .fullName("Updated Name")
                .addressLine1("123 Tech Way")
                .city("San Francisco")
                .state("CA")
                .postalCode("94107")
                .country("United States")
                .build();

        ShoppingPreferences newPrefs = ShoppingPreferences.builder()
                .defaultCurrency("EUR")
                .categoriesOfInterest(List.of("Electronics"))
                .priceSensitivity("DISCOUNT_SEEKER")
                .preferredWarrantyMonths(24)
                .autoNegotiateDefault(true)
                .build();

        UpdateProfileRequest request = UpdateProfileRequest.builder()
                .fullName("Updated Name")
                .defaultCurrency("EUR")
                .shippingAddress(newAddress)
                .shoppingPreferences(newPrefs)
                .build();

        when(userRepository.findByEmail("buyer@test.com")).thenReturn(Optional.of(sampleUser));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UserProfileDto result = userService.updateProfile("buyer@test.com", request);

        assertNotNull(result);
        assertEquals("Updated Name", result.getFullName());
        assertEquals("EUR", result.getDefaultCurrency());
        assertNotNull(result.getShippingAddress());
        assertEquals("123 Tech Way", result.getShippingAddress().getAddressLine1());
        assertEquals("DISCOUNT_SEEKER", result.getShoppingPreferences().getPriceSensitivity());
        verify(userRepository, times(1)).save(sampleUser);
    }
}
