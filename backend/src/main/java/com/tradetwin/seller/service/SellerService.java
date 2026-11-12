package com.tradetwin.seller.service;

import com.tradetwin.catalog.model.Product;
import com.tradetwin.catalog.repository.ProductRepository;
import com.tradetwin.negotiation.model.Negotiation;
import com.tradetwin.negotiation.model.NegotiationStatus;
import com.tradetwin.negotiation.repository.NegotiationRepository;
import com.tradetwin.order.model.Order;
import com.tradetwin.order.model.OrderStatus;
import com.tradetwin.order.repository.OrderRepository;
import com.tradetwin.seller.dto.SellerDashboardSummaryDto;
import com.tradetwin.seller.dto.SellerDashboardSummaryDto.SellerActivityItem;
import com.tradetwin.seller.dto.SellerOnboardingRequest;
import com.tradetwin.seller.model.SellerProfile;
import com.tradetwin.seller.repository.SellerProfileRepository;
import com.tradetwin.user.model.UserRole;
import com.tradetwin.user.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
public class SellerService {

    private final SellerProfileRepository sellerProfileRepository;
    private final UserRepository userRepository;
    private final ProductRepository productRepository;
    private final NegotiationRepository negotiationRepository;
    private final OrderRepository orderRepository;

    public SellerService(SellerProfileRepository sellerProfileRepository,
                         UserRepository userRepository,
                         ProductRepository productRepository,
                         NegotiationRepository negotiationRepository,
                         OrderRepository orderRepository) {
        this.sellerProfileRepository = sellerProfileRepository;
        this.userRepository = userRepository;
        this.productRepository = productRepository;
        this.negotiationRepository = negotiationRepository;
        this.orderRepository = orderRepository;
    }

    public SellerProfile getProfileByUserId(String userId) {
        return sellerProfileRepository.findByUserId(userId)
                .orElseGet(() -> createDefaultProfile(userId));
    }

    public SellerProfile onboardSeller(String userId, SellerOnboardingRequest request) {
        SellerProfile profile = sellerProfileRepository.findByUserId(userId)
                .orElseGet(() -> SellerProfile.builder().userId(userId).build());

        if (request.getStoreName() != null && !request.getStoreName().trim().isEmpty()) {
            profile.setStoreName(request.getStoreName().trim());
        }
        if (request.getDescription() != null && !request.getDescription().trim().isEmpty()) {
            profile.setDescription(request.getDescription().trim());
        }
        if (request.getLogoUrl() != null) {
            profile.setLogoUrl(request.getLogoUrl().trim());
        }
        if (request.getBusinessContact() != null) {
            profile.setBusinessContact(request.getBusinessContact());
        }
        if (request.getShippingRegions() != null && !request.getShippingRegions().isEmpty()) {
            profile.setShippingRegions(request.getShippingRegions());
        }
        if (request.getDefaultShippingTerms() != null) {
            profile.setDefaultShippingTerms(request.getDefaultShippingTerms());
        }
        if (request.getDefaultStrategy() != null) {
            profile.setDefaultStrategy(request.getDefaultStrategy());
        }
        if (request.getAutoNegotiateEnabled() != null) {
            profile.setAutoNegotiateEnabled(request.getAutoNegotiateEnabled());
        }
        if (request.getMinimumReputationRequired() != null) {
            profile.setMinimumReputationRequired(request.getMinimumReputationRequired());
        }
        profile.setOnboardingCompleted(request.getOnboardingCompleted() != null ? request.getOnboardingCompleted() : true);
        profile.setOnboardingStep(request.getOnboardingStep() != null ? request.getOnboardingStep() : 4);
        profile.setUpdatedAt(Instant.now());

        // Ensure user has ROLE_SELLER if not already ADMIN
        userRepository.findById(userId).ifPresent(user -> {
            if (user.getRole() == UserRole.ROLE_BUYER) {
                user.setRole(UserRole.ROLE_SELLER);
                user.setUpdatedAt(Instant.now());
                userRepository.save(user);
            }
        });

        return sellerProfileRepository.save(profile);
    }

    public SellerProfile updateProfile(String userId, SellerProfile updated) {
        SellerProfile profile = getProfileByUserId(userId);
        if (updated.getStoreName() != null) {
            profile.setStoreName(updated.getStoreName());
        }
        if (updated.getDescription() != null) {
            profile.setDescription(updated.getDescription());
        }
        if (updated.getLogoUrl() != null) {
            profile.setLogoUrl(updated.getLogoUrl());
        }
        if (updated.getBusinessContact() != null) {
            profile.setBusinessContact(updated.getBusinessContact());
        }
        if (updated.getShippingRegions() != null) {
            profile.setShippingRegions(updated.getShippingRegions());
        }
        if (updated.getDefaultShippingTerms() != null) {
            profile.setDefaultShippingTerms(updated.getDefaultShippingTerms());
        }
        if (updated.getDefaultStrategy() != null) {
            profile.setDefaultStrategy(updated.getDefaultStrategy());
        }
        profile.setAutoNegotiateEnabled(updated.isAutoNegotiateEnabled());
        profile.setMinimumReputationRequired(updated.getMinimumReputationRequired());
        profile.setOnboardingCompleted(updated.isOnboardingCompleted());
        profile.setOnboardingStep(updated.getOnboardingStep());
        profile.setUpdatedAt(Instant.now());

        return sellerProfileRepository.save(profile);
    }

    public SellerDashboardSummaryDto getDashboardSummary(String sellerId) {
        List<Product> products = productRepository.findBySellerId(sellerId);
        List<Negotiation> negotiations = negotiationRepository.findBySellerIdOrderByCreatedAtDesc(sellerId);
        List<Order> orders = orderRepository.findBySellerIdOrderByCreatedAtDesc(sellerId);

        long totalProducts = products.size();
        long activeListings = products.stream().filter(Product::isActive).count();
        int totalInventory = products.stream().mapToInt(Product::getAvailableInventory).sum();
        int reservedInventory = products.stream().mapToInt(Product::getReservedCount).sum();
        int lowStockCount = (int) products.stream().filter(p -> p.getAvailableInventory() <= 5).count();

        long negotiationsReceived = negotiations.size();
        long acceptedDeals = negotiations.stream()
                .filter(n -> n.getStatus() == NegotiationStatus.ACCEPTED || n.getStatus() == NegotiationStatus.AGREED || n.getStatus() == NegotiationStatus.COMPLETED)
                .count();

        long totalOrders = orders.size();
        BigDecimal totalSalesVolume = orders.stream()
                .filter(o -> o.getOrderStatus() != OrderStatus.CANCELLED && o.getOrderStatus() != OrderStatus.REFUNDED)
                .map(Order::getFinalAmount)
                .filter(amt -> amt != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Aggregate recent activity
        List<SellerActivityItem> activities = new ArrayList<>();

        for (Order o : orders.stream().limit(5).toList()) {
            activities.add(new SellerActivityItem(
                    o.getId(),
                    "ORDER_PLACED",
                    "Order #" + o.getOrderNumber(),
                    "Confirmed checkout captured at $" + o.getFinalAmount(),
                    o.getFinalAmount(),
                    o.getCurrency(),
                    o.getCreatedAt()
            ));
        }

        for (Negotiation n : negotiations.stream().limit(5).toList()) {
            if ((n.getStatus() == NegotiationStatus.ACCEPTED || n.getStatus() == NegotiationStatus.AGREED || n.getStatus() == NegotiationStatus.COMPLETED) && n.getAgreementSeal() != null) {
                activities.add(new SellerActivityItem(
                        n.getId(),
                        "DEAL_ACCEPTED",
                        "FairDeal Agreed",
                        "Autonomous consensus reached at $" + n.getAgreementSeal().getFinalPrice(),
                        n.getAgreementSeal().getFinalPrice(),
                        n.getAgreementSeal().getCurrency(),
                        n.getUpdatedAt()
                ));
            } else {
                activities.add(new SellerActivityItem(
                        n.getId(),
                        "NEGOTIATION_RECEIVED",
                        "Negotiation Session",
                        "Buyer Twin negotiation round " + n.getRoundCount() + " (" + n.getStatus() + ")",
                        n.getCurrentOffer() != null ? n.getCurrentOffer().getPrice() : BigDecimal.ZERO,
                        n.getCurrentOffer() != null ? n.getCurrentOffer().getCurrency() : "USD",
                        n.getUpdatedAt()
                ));
            }
        }

        for (Product p : products.stream().limit(3).toList()) {
            activities.add(new SellerActivityItem(
                    p.getId(),
                    "PRODUCT_LISTED",
                    p.getTitle(),
                    "Listed at $" + p.getListPrice() + " (" + (p.isActive() ? "Active" : "Paused") + ")",
                    p.getListPrice(),
                    p.getCurrency(),
                    p.getCreatedAt()
            ));
        }

        activities.sort(Comparator.comparing(SellerActivityItem::getTimestamp, Comparator.nullsLast(Comparator.reverseOrder())));
        List<SellerActivityItem> recentActivity = activities.stream().limit(8).toList();

        long pendingFulfilment = orders.stream()
                .filter(o -> o.getOrderStatus() == OrderStatus.CONFIRMED || o.getOrderStatus() == OrderStatus.PROCESSING)
                .count();
        long shippedOrders = orders.stream()
                .filter(o -> o.getOrderStatus() == OrderStatus.SHIPPED)
                .count();
        long deliveredOrders = orders.stream()
                .filter(o -> o.getOrderStatus() == OrderStatus.DELIVERED)
                .count();

        return SellerDashboardSummaryDto.builder()
                .totalProducts(totalProducts)
                .activeListings(activeListings)
                .totalInventory(totalInventory)
                .reservedInventory(reservedInventory)
                .lowStockCount(lowStockCount)
                .negotiationsReceived(negotiationsReceived)
                .acceptedDeals(acceptedDeals)
                .totalOrders(totalOrders)
                .pendingFulfilmentOrders(pendingFulfilment)
                .shippedOrders(shippedOrders)
                .deliveredOrders(deliveredOrders)
                .totalSalesVolume(totalSalesVolume)
                .currency("USD")
                .recentActivity(recentActivity)
                .build();
    }

    private SellerProfile createDefaultProfile(String userId) {
        SellerProfile defaultProfile = SellerProfile.builder()
                .userId(userId)
                .storeName("Merchant Store")
                .description("Verified TradeTwin Merchant")
                .onboardingCompleted(false)
                .onboardingStep(1)
                .build();
        return sellerProfileRepository.save(defaultProfile);
    }
}
