package com.tradetwin.seller.service;

import com.tradetwin.catalog.model.Product;
import com.tradetwin.catalog.repository.ProductRepository;
import com.tradetwin.negotiation.model.Negotiation;
import com.tradetwin.negotiation.model.NegotiationStatus;
import com.tradetwin.negotiation.repository.NegotiationRepository;
import com.tradetwin.order.model.Order;
import com.tradetwin.order.repository.OrderRepository;
import com.tradetwin.seller.dto.SellerDashboardSummaryDto;
import com.tradetwin.seller.dto.SellerOnboardingRequest;
import com.tradetwin.seller.model.BusinessContact;
import com.tradetwin.seller.model.NegotiationStrategy;
import com.tradetwin.seller.model.SellerProfile;
import com.tradetwin.seller.model.ShippingTerms;
import com.tradetwin.seller.repository.SellerProfileRepository;
import com.tradetwin.user.model.User;
import com.tradetwin.user.model.UserRole;
import com.tradetwin.user.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SellerServiceTest {

    @Mock
    private SellerProfileRepository sellerProfileRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private NegotiationRepository negotiationRepository;

    @Mock
    private OrderRepository orderRepository;

    @InjectMocks
    private SellerService sellerService;

    @Test
    @DisplayName("Onboard seller sets up complete merchant profile and elevates buyer role if needed")
    void testOnboardSeller() {
        String userId = "seller-usr-1";
        User user = User.builder().id(userId).email("seller@test.com").role(UserRole.ROLE_BUYER).build();

        BusinessContact contact = BusinessContact.builder()
                .phone("+1 800 555 1234")
                .supportEmail("support@acme.com")
                .supportHours("9AM - 5PM PST")
                .build();

        ShippingTerms terms = ShippingTerms.builder()
                .handlingTimeDays(2)
                .freeShippingThreshold(new BigDecimal("150.00"))
                .allowExpressShipping(true)
                .returnPolicyDays(30)
                .build();

        SellerOnboardingRequest request = SellerOnboardingRequest.builder()
                .storeName("Acme SuperStore")
                .description("Global retailer of gadgets")
                .logoUrl("https://example.com/logo.png")
                .businessContact(contact)
                .shippingRegions(List.of("United States", "Canada"))
                .defaultShippingTerms(terms)
                .defaultStrategy(NegotiationStrategy.BALANCED)
                .autoNegotiateEnabled(true)
                .minimumReputationRequired(75.0)
                .onboardingCompleted(true)
                .onboardingStep(4)
                .build();

        when(sellerProfileRepository.findByUserId(userId)).thenReturn(Optional.empty());
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(sellerProfileRepository.save(any(SellerProfile.class))).thenAnswer(inv -> inv.getArgument(0));

        SellerProfile profile = sellerService.onboardSeller(userId, request);

        assertNotNull(profile);
        assertEquals("Acme SuperStore", profile.getStoreName());
        assertEquals("https://example.com/logo.png", profile.getLogoUrl());
        assertTrue(profile.isOnboardingCompleted());
        assertEquals(4, profile.getOnboardingStep());
        assertEquals(new BigDecimal("150.00"), profile.getDefaultShippingTerms().getFreeShippingThreshold());

        // Role should be updated to ROLE_SELLER
        assertEquals(UserRole.ROLE_SELLER, user.getRole());
        verify(userRepository, times(1)).save(user);
        verify(sellerProfileRepository, times(1)).save(any(SellerProfile.class));
    }

    @Test
    @DisplayName("GetDashboardSummary calculates correct aggregates for seller products, negotiations, and orders")
    void testGetDashboardSummary() {
        String sellerId = "seller-123";

        Product p1 = Product.builder().id("p1").sellerId(sellerId).title("Product 1").inventoryCount(10).reservedCount(2).active(true).build();
        Product p2 = Product.builder().id("p2").sellerId(sellerId).title("Product 2").inventoryCount(4).reservedCount(0).active(false).build();

        Negotiation n1 = Negotiation.builder().id("n1").sellerId(sellerId).status(NegotiationStatus.AGREED).roundCount(3).updatedAt(Instant.now()).build();
        Negotiation n2 = Negotiation.builder().id("n2").sellerId(sellerId).status(NegotiationStatus.IN_PROGRESS).roundCount(1).updatedAt(Instant.now()).build();

        Order o1 = Order.builder().id("o1").sellerId(sellerId).orderNumber("ORD-101").finalAmount(new BigDecimal("250.00")).currency("USD").createdAt(Instant.now()).build();

        when(productRepository.findBySellerId(sellerId)).thenReturn(List.of(p1, p2));
        when(negotiationRepository.findBySellerIdOrderByCreatedAtDesc(sellerId)).thenReturn(List.of(n1, n2));
        when(orderRepository.findBySellerIdOrderByCreatedAtDesc(sellerId)).thenReturn(List.of(o1));

        SellerDashboardSummaryDto summary = sellerService.getDashboardSummary(sellerId);

        assertNotNull(summary);
        assertEquals(2, summary.getTotalProducts());
        assertEquals(1, summary.getActiveListings());
        assertEquals(12, summary.getTotalInventory()); // (10-2) + 4 = 12
        assertEquals(2, summary.getReservedInventory());
        assertEquals(1, summary.getLowStockCount()); // p2 has 4 <= 5
        assertEquals(2, summary.getNegotiationsReceived());
        assertEquals(1, summary.getAcceptedDeals());
        assertEquals(1, summary.getTotalOrders());
        assertEquals(new BigDecimal("250.00"), summary.getTotalSalesVolume());
        assertFalse(summary.getRecentActivity().isEmpty());
    }
}
