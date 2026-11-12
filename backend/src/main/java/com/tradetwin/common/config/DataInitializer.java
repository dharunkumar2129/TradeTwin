package com.tradetwin.common.config;

import com.tradetwin.catalog.model.NegotiationPolicy;
import com.tradetwin.catalog.model.Product;
import com.tradetwin.catalog.model.QuantityDiscountRule;
import com.tradetwin.catalog.repository.ProductRepository;
import com.tradetwin.deal.model.AgreementSeal;
import com.tradetwin.negotiation.model.BuyerPreferences;
import com.tradetwin.negotiation.model.Negotiation;
import com.tradetwin.negotiation.model.NegotiationStatus;
import com.tradetwin.negotiation.model.OfferProposal;
import com.tradetwin.negotiation.model.Turn;
import com.tradetwin.negotiation.repository.NegotiationRepository;
import com.tradetwin.order.model.Order;
import com.tradetwin.order.model.OrderStatus;
import com.tradetwin.order.model.OrderStatusChange;
import com.tradetwin.order.repository.OrderRepository;
import com.tradetwin.payment.model.PaymentRecord;
import com.tradetwin.payment.model.PaymentStatus;
import com.tradetwin.payment.repository.PaymentRepository;
import com.tradetwin.seller.model.BusinessContact;
import com.tradetwin.seller.model.NegotiationStrategy;
import com.tradetwin.seller.model.SellerProfile;
import com.tradetwin.seller.model.ShippingTerms;
import com.tradetwin.seller.repository.SellerProfileRepository;
import com.tradetwin.user.model.ShippingAddress;
import com.tradetwin.user.model.ShoppingPreferences;
import com.tradetwin.user.model.User;
import com.tradetwin.user.model.UserRole;
import com.tradetwin.user.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;

@Configuration
public class DataInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    private final UserRepository userRepository;
    private final SellerProfileRepository sellerProfileRepository;
    private final ProductRepository productRepository;
    private final NegotiationRepository negotiationRepository;
    private final OrderRepository orderRepository;
    private final PaymentRepository paymentRepository;
    private final PasswordEncoder passwordEncoder;

    public DataInitializer(UserRepository userRepository,
                           SellerProfileRepository sellerProfileRepository,
                           ProductRepository productRepository,
                           NegotiationRepository negotiationRepository,
                           OrderRepository orderRepository,
                           PaymentRepository paymentRepository,
                           PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.sellerProfileRepository = sellerProfileRepository;
        this.productRepository = productRepository;
        this.negotiationRepository = negotiationRepository;
        this.orderRepository = orderRepository;
        this.paymentRepository = paymentRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        try {
            initUsersAndProducts();
        } catch (Exception ex) {
            log.warn("Database initialization deferred: {}", ex.getMessage());
        }
    }

    private void initUsersAndProducts() {
        if (userRepository.count() > 0) {
            log.info("Database users already seeded. Upserting multi-seller marketplace demo catalog...");
            userRepository.findByEmail("seller@tradetwin.com").ifPresent(seller -> seedSonyHeadphonesIfMissing(seller.getId()));
            seedSecondSellerAndCompetingSonyIfMissing();
            seedMultiMerchantDemoMarketplace();
            return;
        }

        log.info("Seeding initial TradeTwin demo users, rich catalog, and negotiation data...");

        // 1. Demo Buyer
        ShippingAddress buyerAddress = ShippingAddress.builder()
                .fullName("Alice Walker")
                .addressLine1("742 Evergreen Terrace")
                .addressLine2("Suite 4B")
                .city("Springfield")
                .state("OR")
                .postalCode("97477")
                .country("United States")
                .build();

        ShoppingPreferences buyerPreferences = ShoppingPreferences.builder()
                .defaultCurrency("USD")
                .categoriesOfInterest(List.of("Electronics", "Audio & Acoustics", "Developer Tools", "Wearables"))
                .priceSensitivity("BALANCED")
                .preferredWarrantyMonths(24)
                .autoNegotiateDefault(true)
                .build();

        User buyer = User.builder()
                .email("buyer@tradetwin.com")
                .passwordHash(passwordEncoder.encode("password123"))
                .fullName("Alice Walker")
                .role(UserRole.ROLE_BUYER)
                .reputationScore(98.5)
                .defaultCurrency("USD")
                .shippingAddress(buyerAddress)
                .shoppingPreferences(buyerPreferences)
                .createdAt(Instant.now().minus(30, ChronoUnit.DAYS))
                .updatedAt(Instant.now().minus(2, ChronoUnit.DAYS))
                .build();
        User savedBuyer = userRepository.save(buyer);

        // 2. Demo Seller
        User seller = User.builder()
                .email("seller@tradetwin.com")
                .passwordHash(passwordEncoder.encode("password123"))
                .fullName("QuantumTech Direct")
                .role(UserRole.ROLE_SELLER)
                .reputationScore(99.0)
                .defaultCurrency("USD")
                .createdAt(Instant.now().minus(60, ChronoUnit.DAYS))
                .updatedAt(Instant.now().minus(1, ChronoUnit.DAYS))
                .build();
        User savedSeller = userRepository.save(seller);

        // Seller Profile with comprehensive onboarding details
        BusinessContact businessContact = BusinessContact.builder()
                .phone("+1 (555) 234-5678")
                .supportEmail("support@quantumtech-direct.com")
                .supportHours("Mon-Fri 09:00 - 18:00 EST")
                .build();

        ShippingTerms shippingTerms = ShippingTerms.builder()
                .handlingTimeDays(1)
                .freeShippingThreshold(new BigDecimal("99.00"))
                .allowExpressShipping(true)
                .returnPolicyDays(30)
                .build();

        SellerProfile sellerProfile = SellerProfile.builder()
                .userId(savedSeller.getId())
                .storeName("QuantumTech Direct Official")
                .description("Premier supplier of high performance developer workstations, audio engineering hardware, and certified optics.")
                .logoUrl("https://images.unsplash.com/photo-1618005182384-a83a8bd57fbe?auto=format&fit=crop&w=400&q=80")
                .businessContact(businessContact)
                .shippingRegions(List.of("United States", "Canada", "United Kingdom", "European Union"))
                .defaultShippingTerms(shippingTerms)
                .defaultStrategy(NegotiationStrategy.BALANCED)
                .autoNegotiateEnabled(true)
                .minimumReputationRequired(70.0)
                .onboardingCompleted(true)
                .onboardingStep(4)
                .createdAt(Instant.now().minus(60, ChronoUnit.DAYS))
                .updatedAt(Instant.now().minus(1, ChronoUnit.DAYS))
                .build();
        sellerProfileRepository.save(sellerProfile);

        // 3. Demo Admin
        User admin = User.builder()
                .email("admin@tradetwin.com")
                .passwordHash(passwordEncoder.encode("password123"))
                .fullName("System Administrator")
                .role(UserRole.ROLE_ADMIN)
                .reputationScore(100.0)
                .defaultCurrency("USD")
                .createdAt(Instant.now().minus(90, ChronoUnit.DAYS))
                .updatedAt(Instant.now())
                .build();
        userRepository.save(admin);

        // 4. Seed 5 Realistic Catalog Products with Private Policies
        Product laptop = Product.builder()
                .sellerId(savedSeller.getId())
                .title("QuantumBlade Pro 14 Laptop (32GB RAM, 1TB NVMe)")
                .brand("QuantumBlade")
                .sku("QB-14-32-1TB")
                .description("Ultra-thin 14-inch developer workstation featuring 120Hz 2.8K OLED display, 14-core Intel Core Ultra 9 processor, 32GB LPDDR5X RAM, and 1TB PCIe 4.0 SSD.")
                .category("Electronics")
                .condition("Brand New")
                .listPrice(new BigDecimal("1299.99"))
                .currency("USD")
                .inventoryCount(15)
                .reservedCount(1)
                .minOrderQuantity(1)
                .maxOrderQuantity(3)
                .shippingCost(BigDecimal.ZERO)
                .estimatedDeliveryDays(2)
                .warrantyInfo("24 Months Manufacturer On-Site Warranty")
                .returnPolicy("30 Days Risk-Free Return Window")
                .specifications(Map.of(
                        "Display", "14-inch 2.8K (2880x1800) 120Hz OLED, 500 nits HDR",
                        "Processor", "Intel Core Ultra 9 185H (16 Cores, 22 Threads)",
                        "Memory", "32GB LPDDR5X-7467MHz Dual-Channel",
                        "Storage", "1TB M.2 PCIe 4.0 NVMe SSD",
                        "Battery", "75Wh Lithium-Ion (Up to 14 hours battery life)",
                        "Weight", "1.39 kg (3.06 lbs)"
                ))
                .negotiationPolicy(NegotiationPolicy.builder()
                        .autoNegotiate(true)
                        .floorPrice(new BigDecimal("1099.00"))
                        .targetPrice(new BigDecimal("1249.00"))
                        .autoAcceptPriceThreshold(new BigDecimal("1220.00"))
                        .maxDiscountPct(new BigDecimal("15.4"))
                        .strategy(NegotiationStrategy.BALANCED)
                        .maxRounds(5)
                        .quantityDiscounts(List.of(new QuantityDiscountRule(2, new BigDecimal("5.00"))))
                        .allowWarrantySweeteners(true)
                        .maxWarrantyMonthsSweetener(24)
                        .allowFreeShipping(true)
                        .maxDeliveryDaysCompromise(2)
                        .minimumBuyerReputation(80.0)
                        .build())
                .images(List.of(
                        "https://images.unsplash.com/photo-1517336714731-489689fd1ca8?auto=format&fit=crop&w=1000&q=80",
                        "https://images.unsplash.com/photo-1541807084-5c52b6b3adef?auto=format&fit=crop&w=1000&q=80"
                ))
                .tags(List.of("laptop", "developer", "32gb", "oled", "workstation", "portable"))
                .active(true)
                .createdAt(Instant.now().minus(20, ChronoUnit.DAYS))
                .updatedAt(Instant.now().minus(2, ChronoUnit.DAYS))
                .build();

        Product headphones = Product.builder()
                .sellerId(savedSeller.getId())
                .title("SonicPulse Studio ANC Wireless Headphones")
                .brand("SonicPulse")
                .sku("SP-ANC-STU")
                .description("Audiophile grade over-ear wireless headphones with adaptive hybrid noise cancellation, 40mm beryllium drivers, and 45-hour battery life.")
                .category("Audio & Acoustics")
                .condition("Brand New")
                .listPrice(new BigDecimal("349.99"))
                .currency("USD")
                .inventoryCount(25)
                .reservedCount(0)
                .minOrderQuantity(1)
                .maxOrderQuantity(5)
                .shippingCost(new BigDecimal("5.00"))
                .estimatedDeliveryDays(3)
                .warrantyInfo("12 Months Standard Coverage")
                .returnPolicy("30 Days Money-Back Guarantee")
                .specifications(Map.of(
                        "Driver Size", "40mm Custom Electro-Acoustic Beryllium Drivers",
                        "Active Noise Cancellation", "Hybrid Quad-Mic Adaptive Active Noise Cancelling",
                        "Battery Life", "45 Hours (ANC On), 65 Hours (ANC Off)",
                        "Codecs", "LDAC, aptX Adaptive, AAC, SBC",
                        "Connectivity", "Bluetooth 5.3 & 3.5mm Hi-Res Audio Jack"
                ))
                .negotiationPolicy(NegotiationPolicy.builder()
                        .autoNegotiate(true)
                        .floorPrice(new BigDecimal("279.00"))
                        .targetPrice(new BigDecimal("319.00"))
                        .autoAcceptPriceThreshold(new BigDecimal("310.00"))
                        .maxDiscountPct(new BigDecimal("20.0"))
                        .strategy(NegotiationStrategy.COOPERATIVE)
                        .maxRounds(4)
                        .quantityDiscounts(List.of(new QuantityDiscountRule(3, new BigDecimal("8.00"))))
                        .allowWarrantySweeteners(true)
                        .maxWarrantyMonthsSweetener(12)
                        .allowFreeShipping(true)
                        .maxDeliveryDaysCompromise(2)
                        .minimumBuyerReputation(75.0)
                        .build())
                .images(List.of(
                        "https://images.unsplash.com/photo-1505740420928-5e560c06d30e?auto=format&fit=crop&w=1000&q=80",
                        "https://images.unsplash.com/photo-1484704849700-f032a568e944?auto=format&fit=crop&w=1000&q=80"
                ))
                .tags(List.of("headphones", "audio", "anc", "wireless", "studio", "hi-res"))
                .active(true)
                .createdAt(Instant.now().minus(15, ChronoUnit.DAYS))
                .updatedAt(Instant.now().minus(1, ChronoUnit.DAYS))
                .build();

        Product watch = Product.builder()
                .sellerId(savedSeller.getId())
                .title("ApexFit Titanium GPS Multisport Smartwatch")
                .brand("ApexFit")
                .sku("AF-TITAN-GPS")
                .description("Grade 5 titanium sapphire multisport smartwatch with offline topographic mapping, ECG sensor, and 14-day battery life.")
                .category("Wearables")
                .condition("Brand New")
                .listPrice(new BigDecimal("449.99"))
                .currency("USD")
                .inventoryCount(18)
                .reservedCount(0)
                .minOrderQuantity(1)
                .maxOrderQuantity(4)
                .shippingCost(BigDecimal.ZERO)
                .estimatedDeliveryDays(2)
                .warrantyInfo("24 Months Hardware Guarantee")
                .returnPolicy("45 Days Athlete Satisfaction Guarantee")
                .specifications(Map.of(
                        "Bezel & Case", "Grade 5 Aerospace Titanium with Sapphire Lens",
                        "Water Rating", "10 ATM (100 Meters Submersible)",
                        "Battery", "14 Days Smartwatch mode / 42 Hours Continuous Dual-GPS",
                        "Sensors", "Multi-band GNSS, Optical ECG, Pulse Ox, Altimeter, Barometer"
                ))
                .negotiationPolicy(NegotiationPolicy.builder()
                        .autoNegotiate(true)
                        .floorPrice(new BigDecimal("369.00"))
                        .targetPrice(new BigDecimal("419.00"))
                        .autoAcceptPriceThreshold(new BigDecimal("400.00"))
                        .maxDiscountPct(new BigDecimal("18.0"))
                        .strategy(NegotiationStrategy.BALANCED)
                        .maxRounds(5)
                        .allowWarrantySweeteners(true)
                        .maxWarrantyMonthsSweetener(24)
                        .allowFreeShipping(true)
                        .maxDeliveryDaysCompromise(2)
                        .minimumBuyerReputation(70.0)
                        .build())
                .images(List.of(
                        "https://images.unsplash.com/photo-1523275335684-37898b6baf30?auto=format&fit=crop&w=1000&q=80"
                ))
                .tags(List.of("smartwatch", "gps", "titanium", "fitness", "watch", "outdoor"))
                .active(true)
                .createdAt(Instant.now().minus(10, ChronoUnit.DAYS))
                .updatedAt(Instant.now().minus(1, ChronoUnit.DAYS))
                .build();

        Product keyboard = Product.builder()
                .sellerId(savedSeller.getId())
                .title("KeyZen Pro Split Ergonomic Mechanical Keyboard")
                .brand("KeyZen")
                .sku("KZ-SPLIT-PRO")
                .description("Ergonomic split mechanical keyboard featuring factory-lubed linear switches, OLED status cluster, hot-swappable PCB, and wireless tri-mode connectivity.")
                .category("Electronics")
                .condition("Brand New")
                .listPrice(new BigDecimal("219.99"))
                .currency("USD")
                .inventoryCount(12)
                .reservedCount(0)
                .minOrderQuantity(1)
                .maxOrderQuantity(2)
                .shippingCost(new BigDecimal("8.00"))
                .estimatedDeliveryDays(3)
                .warrantyInfo("12 Months Replacement Warranty")
                .returnPolicy("30 Days Ergonomic Trial")
                .specifications(Map.of(
                        "Layout", "68-Key Split Alice Ergonomic with OLED Displays",
                        "Switches", "Factory-Lubed Gateron Oil King Linear (Hot-Swappable 5-Pin)",
                        "Keycaps", "Thick Double-Shot PBT Cherry Profile",
                        "Connectivity", "2.4GHz Wireless, Bluetooth 5.1 & USB-C Wired"
                ))
                .negotiationPolicy(NegotiationPolicy.builder()
                        .autoNegotiate(true)
                        .floorPrice(new BigDecimal("179.00"))
                        .targetPrice(new BigDecimal("199.00"))
                        .autoAcceptPriceThreshold(new BigDecimal("195.00"))
                        .maxDiscountPct(new BigDecimal("18.6"))
                        .strategy(NegotiationStrategy.BALANCED)
                        .maxRounds(4)
                        .allowWarrantySweeteners(true)
                        .maxWarrantyMonthsSweetener(12)
                        .allowFreeShipping(true)
                        .maxDeliveryDaysCompromise(2)
                        .minimumBuyerReputation(70.0)
                        .build())
                .images(List.of(
                        "https://images.unsplash.com/photo-1587829741301-dc798b83add3?auto=format&fit=crop&w=1000&q=80"
                ))
                .tags(List.of("keyboard", "mechanical", "split", "ergonomic", "developer", "wireless"))
                .active(true)
                .createdAt(Instant.now().minus(5, ChronoUnit.DAYS))
                .updatedAt(Instant.now().minus(1, ChronoUnit.DAYS))
                .build();

        Product monitor = Product.builder()
                .sellerId(savedSeller.getId())
                .title("UltraView 34-inch QD-OLED Curved Ultrawide Monitor")
                .brand("UltraView")
                .sku("UV-34-OLED-UW")
                .description("Quantum Dot OLED 3440x1440p 21:9 curved ultrawide creator monitor with 175Hz refresh rate, 0.03ms response time, and factory color calibration.")
                .category("Electronics")
                .condition("Open Box - Like New")
                .listPrice(new BigDecimal("899.99"))
                .currency("USD")
                .inventoryCount(4)
                .reservedCount(0)
                .minOrderQuantity(1)
                .maxOrderQuantity(2)
                .shippingCost(new BigDecimal("15.00"))
                .estimatedDeliveryDays(4)
                .warrantyInfo("36 Months Zero-Burn-In Protection Warranty")
                .returnPolicy("30 Days Zero-Dead-Pixel Guarantee")
                .specifications(Map.of(
                        "Panel Type", "Samsung Quantum Dot OLED (QD-OLED) 1800R Curve",
                        "Resolution & Ratio", "3440 x 1440 WQHD, 21:9 Ultrawide",
                        "Refresh & Response", "175Hz Native, 0.03ms GtG Response Time",
                        "Color Accuracy", "99.3% DCI-P3, Delta E < 2 Factory Calibrated",
                        "I/O Ports", "1x USB-C (90W PD), 2x HDMI 2.1, 1x DP 1.4"
                ))
                .negotiationPolicy(NegotiationPolicy.builder()
                        .autoNegotiate(true)
                        .floorPrice(new BigDecimal("749.00"))
                        .targetPrice(new BigDecimal("829.00"))
                        .autoAcceptPriceThreshold(new BigDecimal("810.00"))
                        .maxDiscountPct(new BigDecimal("16.7"))
                        .strategy(NegotiationStrategy.BALANCED)
                        .maxRounds(5)
                        .allowWarrantySweeteners(true)
                        .maxWarrantyMonthsSweetener(36)
                        .allowFreeShipping(true)
                        .maxDeliveryDaysCompromise(3)
                        .minimumBuyerReputation(75.0)
                        .build())
                .images(List.of(
                        "https://images.unsplash.com/photo-1527443224154-c4a3942d3acf?auto=format&fit=crop&w=1000&q=80"
                ))
                .tags(List.of("monitor", "oled", "ultrawide", "34inch", "175hz", "creator"))
                .active(true)
                .createdAt(Instant.now().minus(3, ChronoUnit.DAYS))
                .updatedAt(Instant.now())
                .build();

        productRepository.saveAll(List.of(laptop, headphones, watch, keyboard, monitor));
        seedSonyHeadphonesIfMissing(savedSeller.getId());
        User savedSeller2 = seedSecondSellerAndCompetingSonyIfMissing();

        // 5. Seed Complete PaymentRecord, Order, Completed Negotiation & Checkout-Ready Negotiation
        Instant oneDayAgo = Instant.now().minus(1, ChronoUnit.DAYS);
        PaymentRecord samplePayment = PaymentRecord.builder()
                .negotiationId("neg-seed-01")
                .orderId("ORD-2026-9812")
                .buyerId(savedBuyer.getId())
                .sellerId(savedSeller.getId())
                .productId(laptop.getId())
                .provider("PAYPAL")
                .paypalOrderId("SANDBOX-SEED-9812")
                .captureId("CAP-SEED-9812")
                .amount(new BigDecimal("1219.00"))
                .currency("USD")
                .status(PaymentStatus.COMPLETED)
                .payerId("PAYER-ALICE-01")
                .payerEmail("sb-buyer@personal.example.com")
                .paidAt(oneDayAgo)
                .createdAt(oneDayAgo)
                .updatedAt(oneDayAgo)
                .build();
        PaymentRecord savedSamplePayment = paymentRepository.save(samplePayment);

        Order sampleOrder = Order.builder()
                .orderNumber("ORD-2026-9812")
                .buyerId(savedBuyer.getId())
                .sellerId(savedSeller.getId())
                .productId(laptop.getId())
                .productTitle(laptop.getTitle())
                .productImage(laptop.getImages() != null && !laptop.getImages().isEmpty() ? laptop.getImages().get(0) : null)
                .sellerStoreName("QuantumTech Direct Official")
                .quantity(1)
                .unitPrice(new BigDecimal("1219.00"))
                .negotiationId("neg-seed-01")
                .dealToken("deal_token_laptop_001")
                .finalAmount(new BigDecimal("1219.00"))
                .currency("USD")
                .payment(savedSamplePayment)
                .shippingDetails(Map.of(
                        "fullName", "Alice Walker",
                        "addressLine1", "742 Evergreen Terrace, Suite 4B",
                        "city", "Springfield",
                        "state", "OR",
                        "postalCode", "97477",
                        "country", "United States"
                ))
                .orderStatus(OrderStatus.CONFIRMED)
                .statusHistory(List.of(
                        new OrderStatusChange(null, OrderStatus.PAYMENT_PENDING, "BUYER", "PayPal checkout initiated", oneDayAgo.minus(5, ChronoUnit.MINUTES)),
                        new OrderStatusChange(OrderStatus.PAYMENT_PENDING, OrderStatus.PAID, "PAYPAL_CAPTURE", "PayPal transaction SANDBOX-SEED-9812 captured", oneDayAgo.minus(2, ChronoUnit.MINUTES)),
                        new OrderStatusChange(OrderStatus.PAID, OrderStatus.CONFIRMED, "SELLER", "Order verified against FairDeal agreement seal", oneDayAgo)
                ))
                .createdAt(oneDayAgo)
                .updatedAt(oneDayAgo)
                .build();
        Order savedSampleOrder = orderRepository.save(sampleOrder);
        savedSamplePayment.setOrderId(savedSampleOrder.getId());
        paymentRepository.save(savedSamplePayment);

        Negotiation sampleNegotiation = Negotiation.builder()
                .id("neg-seed-01")
                .productId(laptop.getId())
                .buyerId(savedBuyer.getId())
                .sellerId(savedSeller.getId())
                .status(NegotiationStatus.COMPLETED)
                .roundCount(3)
                .maxRounds(5)
                .buyerPreferences(BuyerPreferences.builder()
                        .targetPrice(new BigDecimal("1150.00"))
                        .maxCeilingPrice(new BigDecimal("1250.00"))
                        .urgency("MEDIUM")
                        .desiredDeliveryDays(2)
                        .requireExtendedWarranty(true)
                        .build())
                .currentOffer(OfferProposal.builder()
                        .price(new BigDecimal("1219.00"))
                        .currency("USD")
                        .deliveryDays(2)
                        .warrantyMonths(24)
                        .bonusPerks(List.of("Free Express Delivery", "24-Mo Extended Warranty"))
                        .build())
                .agreementSeal(AgreementSeal.builder()
                        .dealToken("deal_token_laptop_001")
                        .sealedAt(oneDayAgo)
                        .expiresAt(oneDayAgo.plus(15, ChronoUnit.MINUTES))
                        .finalPrice(new BigDecimal("1219.00"))
                        .currency("USD")
                        .status("PAID")
                        .build())
                .turns(List.of(
                        new Turn(1, "BUYER_AGENT", "PROPOSE",
                                OfferProposal.builder().price(new BigDecimal("1150.00")).unitPrice(new BigDecimal("1150.00")).shippingCost(BigDecimal.ZERO).totalPayable(new BigDecimal("1150.00")).quantity(1).currency("USD").deliveryDays(2).warrantyMonths(24).offerStatus("OFFERED").roundNumber(1).build(),
                                "Hi! I'm interested in the Developer Laptop 16\" Pro — could we start around $1,150 with 2-day shipping and a 24-month warranty?", Instant.now().minus(26, ChronoUnit.HOURS)),
                        new Turn(2, "SELLER_AGENT", "COUNTER",
                                OfferProposal.builder().price(new BigDecimal("1249.00")).unitPrice(new BigDecimal("1249.00")).shippingCost(BigDecimal.ZERO).totalPayable(new BigDecimal("1249.00")).quantity(1).currency("USD").deliveryDays(2).warrantyMonths(24).offerStatus("COUNTERED").roundNumber(2).bonusPerks(List.of("Free Express Delivery", "24-Mo Extended Warranty")).build(),
                                "Thanks for reaching out! We can step down from $1,399 to $1,249 and include complimentary 2-day express shipping plus 24-month warranty protection.", Instant.now().minus(25, ChronoUnit.HOURS)),
                        new Turn(3, "BUYER_AGENT", "ACCEPT",
                                OfferProposal.builder().price(new BigDecimal("1219.00")).unitPrice(new BigDecimal("1219.00")).shippingCost(BigDecimal.ZERO).totalPayable(new BigDecimal("1219.00")).quantity(1).currency("USD").deliveryDays(2).warrantyMonths(24).offerStatus("ACCEPTED").roundNumber(3).bonusPerks(List.of("Free Express Delivery", "24-Mo Extended Warranty")).build(),
                                "Meeting at $1,219 with complimentary 2-day shipping and 24-month warranty works great for me. Deal accepted!", Instant.now().minus(24, ChronoUnit.HOURS))
                ))
                .createdAt(Instant.now().minus(26, ChronoUnit.HOURS))
                .updatedAt(oneDayAgo)
                .build();
        negotiationRepository.save(sampleNegotiation);

        seedMultiMerchantDemoMarketplace();

        log.info("Demo catalog, 6+ multi-merchant stores, sample negotiation, and confirmed order seeded successfully.");
    }

    private void seedSonyHeadphonesIfMissing(String sellerId) {
        boolean exists = productRepository.findAll().stream()
                .anyMatch(p -> "Sony".equalsIgnoreCase(p.getBrand()));
        if (exists) {
            return;
        }

        log.info("Seeding authentic Sony wireless noise-cancelling headphones and accessories...");

        Product sonyCH720N = Product.builder()
                .sellerId(sellerId)
                .title("Sony WH-CH720N Wireless Noise Canceling Headphones")
                .brand("Sony")
                .sku("SNY-WH-CH720N-BLK")
                .description("Lightweight wireless noise-canceling headphones powered by Sony Integrated Processor V1, 35-hour battery life with quick charge, multipoint Bluetooth connection, and crystal-clear hands-free calls.")
                .category("Audio & Acoustics")
                .condition("Brand New")
                .listPrice(new BigDecimal("98.00"))
                .currency("USD")
                .inventoryCount(20)
                .reservedCount(0)
                .minOrderQuantity(1)
                .maxOrderQuantity(4)
                .shippingCost(BigDecimal.ZERO)
                .estimatedDeliveryDays(2)
                .warrantyInfo("12 Months Sony Manufacturer Warranty")
                .returnPolicy("30 Days Free Return Window")
                .specifications(Map.of(
                        "Processor", "Sony Integrated Processor V1 for Adaptive Noise Cancelling",
                        "Driver Unit", "30mm Dynamic Drivers with DSEE Sound Enhancement",
                        "Battery Life", "35 Hours (ANC On) / 50 Hours (ANC Off)",
                        "Quick Charge", "3-minute charge gives 60 minutes playback",
                        "Weight", "192g Ultra-Lightweight Ergonomic Design",
                        "Connectivity", "Multipoint Bluetooth 5.2 & 3.5mm Aux Audio Cable"
                ))
                .negotiationPolicy(NegotiationPolicy.builder()
                        .autoNegotiate(true)
                        .floorPrice(new BigDecimal("79.00"))
                        .targetPrice(new BigDecimal("92.00"))
                        .autoAcceptPriceThreshold(new BigDecimal("89.00"))
                        .maxDiscountPct(new BigDecimal("19.4"))
                        .strategy(NegotiationStrategy.BALANCED)
                        .maxRounds(4)
                        .allowWarrantySweeteners(true)
                        .maxWarrantyMonthsSweetener(24)
                        .allowFreeShipping(true)
                        .maxDeliveryDaysCompromise(2)
                        .minimumBuyerReputation(70.0)
                        .build())
                .images(List.of(
                        "https://images.unsplash.com/photo-1546435770-a3e426bf472b?auto=format&fit=crop&w=1000&q=80",
                        "https://images.unsplash.com/photo-1505740420928-5e560c06d30e?auto=format&fit=crop&w=1000&q=80"
                ))
                .tags(List.of("sony", "headphones", "wireless", "anc", "noise-cancelling", "audio", "bluetooth"))
                .active(true)
                .createdAt(Instant.now().minus(7, ChronoUnit.DAYS))
                .updatedAt(Instant.now())
                .build();

        Product sonyC700N = Product.builder()
                .sellerId(sellerId)
                .title("Sony WF-C700N Truly Wireless Noise Canceling Earbuds")
                .brand("Sony")
                .sku("SNY-WF-C700N-WHT")
                .description("Ergonomic compact true wireless earbuds with noise sensor technology, IPX4 splash resistance, 15-hour battery with pocket charging case, and ambient sound mode.")
                .category("Audio & Acoustics")
                .condition("Brand New")
                .listPrice(new BigDecimal("89.99"))
                .currency("USD")
                .inventoryCount(16)
                .reservedCount(0)
                .minOrderQuantity(1)
                .maxOrderQuantity(3)
                .shippingCost(new BigDecimal("4.99"))
                .estimatedDeliveryDays(3)
                .warrantyInfo("12 Months Sony Warranty")
                .returnPolicy("30 Days Return Policy")
                .specifications(Map.of(
                        "Design", "Ergonomic Surface Truly Wireless in-ear",
                        "Water Resistance", "IPX4 Splash and Sweat Resistant",
                        "Battery", "7.5 Hours (Earbuds) + 7.5 Hours (Case)",
                        "ANC", "Noise Sensor Ambient Sound Pass-Through"
                ))
                .negotiationPolicy(NegotiationPolicy.builder()
                        .autoNegotiate(true)
                        .floorPrice(new BigDecimal("72.00"))
                        .targetPrice(new BigDecimal("84.00"))
                        .autoAcceptPriceThreshold(new BigDecimal("82.00"))
                        .maxDiscountPct(new BigDecimal("20.0"))
                        .strategy(NegotiationStrategy.BALANCED)
                        .maxRounds(4)
                        .allowFreeShipping(true)
                        .build())
                .images(List.of(
                        "https://images.unsplash.com/photo-1590658268037-6bf12165a8df?auto=format&fit=crop&w=1000&q=80"
                ))
                .tags(List.of("sony", "earbuds", "wireless", "anc", "compact", "audio"))
                .active(true)
                .createdAt(Instant.now().minus(6, ChronoUnit.DAYS))
                .updatedAt(Instant.now())
                .build();

        Product sonyXM4 = Product.builder()
                .sellerId(sellerId)
                .title("Sony WH-1000XM4 Wireless Premium Noise Canceling Headphones")
                .brand("Sony")
                .sku("SNY-WH-1000XM4-SIL")
                .description("Industry-leading noise canceling over-ear headphones with Dual Noise Sensor technology, LDAC high-res audio codec, touch sensor controls, and 30-hour battery life.")
                .category("Audio & Acoustics")
                .condition("Refurbished - Like New")
                .listPrice(new BigDecimal("189.99"))
                .currency("USD")
                .inventoryCount(10)
                .reservedCount(0)
                .minOrderQuantity(1)
                .maxOrderQuantity(2)
                .shippingCost(BigDecimal.ZERO)
                .estimatedDeliveryDays(2)
                .warrantyInfo("12 Months Certified Refurbished Warranty")
                .returnPolicy("30 Days Satisfaction Guarantee")
                .specifications(Map.of(
                        "Noise Canceling", "HD Noise Canceling Processor QN1 & Dual Sensor",
                        "Audio Codec", "LDAC, DSEE Extreme, AAC, SBC Hi-Res Wireless",
                        "Battery", "30 Hours Playback with USB-C Quick Charge",
                        "Sensors", "Wear detection sensor & Speak-to-Chat automation"
                ))
                .negotiationPolicy(NegotiationPolicy.builder()
                        .autoNegotiate(true)
                        .floorPrice(new BigDecimal("159.00"))
                        .targetPrice(new BigDecimal("179.00"))
                        .autoAcceptPriceThreshold(new BigDecimal("172.00"))
                        .maxDiscountPct(new BigDecimal("16.3"))
                        .strategy(NegotiationStrategy.BALANCED)
                        .maxRounds(5)
                        .allowFreeShipping(true)
                        .build())
                .images(List.of(
                        "https://images.unsplash.com/photo-1583394838336-acd977736f90?auto=format&fit=crop&w=1000&q=80"
                ))
                .tags(List.of("sony", "xm4", "headphones", "anc", "hi-res", "premium"))
                .active(true)
                .createdAt(Instant.now().minus(4, ChronoUnit.DAYS))
                .updatedAt(Instant.now())
                .build();

        productRepository.saveAll(List.of(sonyCH720N, sonyC700N, sonyXM4));
        log.info("Successfully seeded 3 authentic Sony listings into MongoDB catalog.");
    }

    private User seedSecondSellerAndCompetingSonyIfMissing() {
        User seller2 = userRepository.findByEmail("seller2@tradetwin.com").orElseGet(() -> {
            User created = userRepository.save(User.builder()
                    .email("seller2@tradetwin.com")
                    .passwordHash(passwordEncoder.encode("password123"))
                    .fullName("Apex Audio Collective")
                    .role(UserRole.ROLE_SELLER)
                    .reputationScore(97.5)
                    .defaultCurrency("USD")
                    .createdAt(Instant.now().minus(45, ChronoUnit.DAYS))
                    .updatedAt(Instant.now().minus(1, ChronoUnit.DAYS))
                    .build());

            SellerProfile profile2 = SellerProfile.builder()
                    .userId(created.getId())
                    .storeName("Apex Audio Collective")
                    .description("Specialized audio boutique offering competitive Sony noise-cancelling bundles with extended warranty protection.")
                    .logoUrl("https://images.unsplash.com/photo-1511671782779-c97d3d27a1d4?auto=format&fit=crop&w=400&q=80")
                    .businessContact(BusinessContact.builder()
                            .phone("+1 (555) 890-4421")
                            .supportEmail("concierge@apexaudio-collective.com")
                            .supportHours("Mon-Sat 08:00 - 20:00 EST")
                            .build())
                    .shippingRegions(List.of("United States", "Canada", "European Union"))
                    .defaultShippingTerms(ShippingTerms.builder()
                            .handlingTimeDays(1)
                            .freeShippingThreshold(new BigDecimal("50.00"))
                            .allowExpressShipping(true)
                            .returnPolicyDays(45)
                            .build())
                    .defaultStrategy(NegotiationStrategy.COOPERATIVE)
                    .autoNegotiateEnabled(true)
                    .minimumReputationRequired(65.0)
                    .onboardingCompleted(true)
                    .onboardingStep(4)
                    .createdAt(Instant.now().minus(45, ChronoUnit.DAYS))
                    .updatedAt(Instant.now().minus(1, ChronoUnit.DAYS))
                    .build();
            sellerProfileRepository.save(profile2);
            log.info("Seeded second merchant store: seller2@tradetwin.com (Apex Audio Collective)");
            return created;
        });

        boolean hasSeller2Products = productRepository.findBySellerId(seller2.getId()).stream()
                .anyMatch(p -> "Sony".equalsIgnoreCase(p.getBrand()));
        if (!hasSeller2Products) {
            Product apexSony720 = Product.builder()
                    .sellerId(seller2.getId())
                    .title("Sony WH-CH720N Wireless Noise Canceling Headphones (24-Mo Bundle)")
                    .brand("Sony")
                    .sku("APX-WH-CH720N-BLU")
                    .description("Brand new Sony WH-CH720N Wireless ANC headphones bundled with Apex 24-month rapid replacement warranty and free priority express delivery.")
                    .category("Audio & Acoustics")
                    .condition("Brand New")
                    .listPrice(new BigDecimal("95.00"))
                    .currency("USD")
                    .inventoryCount(14)
                    .reservedCount(0)
                    .minOrderQuantity(1)
                    .maxOrderQuantity(4)
                    .shippingCost(BigDecimal.ZERO)
                    .estimatedDeliveryDays(3)
                    .warrantyInfo("24 Months Apex Extended Sony Warranty")
                    .returnPolicy("45 Days Free Satisfaction Return")
                    .specifications(Map.of(
                            "Processor", "Sony Integrated Processor V1 ANC",
                            "Driver Unit", "30mm Dynamic Drivers with DSEE",
                            "Battery Life", "35 Hours (ANC On) / 50 Hours (ANC Off)",
                            "Connectivity", "Multipoint Bluetooth 5.2 & USB-C Fast Charge"
                    ))
                    .negotiationPolicy(NegotiationPolicy.builder()
                            .autoNegotiate(true)
                            .floorPrice(new BigDecimal("77.50"))
                            .targetPrice(new BigDecimal("88.00"))
                            .autoAcceptPriceThreshold(new BigDecimal("85.00"))
                            .maxDiscountPct(new BigDecimal("18.4"))
                            .strategy(NegotiationStrategy.COOPERATIVE)
                            .maxRounds(5)
                            .allowWarrantySweeteners(true)
                            .maxWarrantyMonthsSweetener(24)
                            .allowFreeShipping(true)
                            .maxDeliveryDaysCompromise(2)
                            .minimumBuyerReputation(65.0)
                            .build())
                    .images(List.of(
                            "https://images.unsplash.com/photo-1505740420928-5e560c06d30e?auto=format&fit=crop&w=1000&q=80"
                    ))
                    .tags(List.of("sony", "headphones", "wireless", "anc", "noise-cancelling", "bluetooth"))
                    .active(true)
                    .createdAt(Instant.now().minus(5, ChronoUnit.DAYS))
                    .updatedAt(Instant.now())
                    .build();

            Product apexSonyEarbuds = Product.builder()
                    .sellerId(seller2.getId())
                    .title("Sony WF-C700N Wireless Noise Canceling Earbuds (Express Pack)")
                    .brand("Sony")
                    .sku("APX-WF-C700N-NVY")
                    .description("Compact Sony WF-C700N noise canceling wireless earbuds with free express 2-day courier delivery and 18-month warranty.")
                    .category("Audio & Acoustics")
                    .condition("Brand New")
                    .listPrice(new BigDecimal("86.00"))
                    .currency("USD")
                    .inventoryCount(12)
                    .reservedCount(0)
                    .minOrderQuantity(1)
                    .maxOrderQuantity(3)
                    .shippingCost(BigDecimal.ZERO)
                    .estimatedDeliveryDays(2)
                    .warrantyInfo("18 Months Sony & Apex Joint Warranty")
                    .returnPolicy("45 Days Free Return")
                    .specifications(Map.of(
                            "Design", "Pocket-Size Compact Ergonomic Earbuds",
                            "Water Resistance", "IPX4 Splash Resistant",
                            "Battery", "15 Hours Combined Charging Case"
                    ))
                    .negotiationPolicy(NegotiationPolicy.builder()
                            .autoNegotiate(true)
                            .floorPrice(new BigDecimal("69.00"))
                            .targetPrice(new BigDecimal("79.00"))
                            .autoAcceptPriceThreshold(new BigDecimal("76.00"))
                            .maxDiscountPct(new BigDecimal("19.7"))
                            .strategy(NegotiationStrategy.COOPERATIVE)
                            .maxRounds(4)
                            .allowFreeShipping(true)
                            .maxWarrantyMonthsSweetener(24)
                            .build())
                    .images(List.of(
                            "https://images.unsplash.com/photo-1590658268037-6bf12165a8df?auto=format&fit=crop&w=1000&q=80"
                    ))
                    .tags(List.of("sony", "earbuds", "headphones", "wireless", "anc", "noise-cancelling"))
                    .active(true)
                    .createdAt(Instant.now().minus(4, ChronoUnit.DAYS))
                    .updatedAt(Instant.now())
                    .build();

            productRepository.saveAll(List.of(apexSony720, apexSonyEarbuds));
            log.info("Seeded 2 competing Sony listings under seller2@tradetwin.com for multi-seller discovery.");
        }

        return seller2;
    }

    private void seedMultiMerchantDemoMarketplace() {
        User novatech = upsertSellerStore(
                "novatech@tradetwin.com", "NovaTech Electronics", 97.4, NegotiationStrategy.BALANCED,
                "Authorized consumer audio & workstation dealer focused on rapid US fulfillment.",
                1, new BigDecimal("50.00"), 30);
        User apex = upsertSellerStore(
                "apexdigital@tradetwin.com", "Apex Digital Store", 98.1, NegotiationStrategy.COOPERATIVE,
                "Express direct-to-consumer electronics store with complimentary extended warranty tiers.",
                1, BigDecimal.ZERO, 45);
        User quantum = upsertSellerStore(
                "quantum@tradetwin.com", "Quantum Gadgets", 94.8, NegotiationStrategy.AGGRESSIVE,
                "High-volume gadget merchant with selective bundle flexibility and fast shipping.",
                2, new BigDecimal("75.00"), 30);
        User orbit = upsertSellerStore(
                "orbit@tradetwin.com", "Orbit Electronics", 92.5, NegotiationStrategy.AGGRESSIVE,
                "Boutique electronics reseller specializing in factory-sealed audio and monitors.",
                2, new BigDecimal("99.00"), 30);
        User primeAudio = upsertSellerStore(
                "primeaudio@tradetwin.com", "Prime Audio Market", 98.6, NegotiationStrategy.COOPERATIVE,
                "Dedicated hi-fi and ANC headphones specialist with multi-year protection plans.",
                1, BigDecimal.ZERO, 45);
        User vertex = upsertSellerStore(
                "vertex@tradetwin.com", "Vertex Technology", 96.2, NegotiationStrategy.BALANCED,
                "Workspace gear, smartwatches, mechanical keyboards, and mobile hardware partner.",
                2, new BigDecimal("60.00"), 30);

        String imgSony720 = "https://images.unsplash.com/photo-1505740420928-5e560c06d30e?auto=format&fit=crop&w=1000&q=80";

        // Overlapping Sony WH-CH720N Wireless Headphones across all 6 merchants:
        // Orbit intentionally carries an uncompromisable high floor ($106 ask + $7 shipping, floor $103) to demonstrate automatic next-seller fallback!
        upsertProductBySku(Product.builder()
                .sellerId(orbit.getId())
                .title("Sony WH-CH720N Wireless Noise Canceling Headphones")
                .brand("Sony").sku("ORB-SONY-WHCH720N-BLK").category("Audio & Acoustics").condition("Brand New")
                .description("Factory-sealed Sony WH-CH720N wireless noise-canceling headphones with Dual Noise Sensor V1 chip.")
                .listPrice(new BigDecimal("112.00")).shippingCost(new BigDecimal("7.00")).estimatedDeliveryDays(4)
                .inventoryCount(6).reservedCount(0).minOrderQuantity(1).maxOrderQuantity(3)
                .warrantyInfo("12 Months Sony Standard Warranty").returnPolicy("30 Days Unopened Return")
                .specifications(Map.of("Driver Unit", "30mm Dynamic", "ANC", "Integrated Processor V1", "Battery", "35 Hours ANC On"))
                .negotiationPolicy(NegotiationPolicy.builder()
                        .autoNegotiate(true).floorPrice(new BigDecimal("104.00")).targetPrice(new BigDecimal("109.00"))
                        .autoAcceptPriceThreshold(new BigDecimal("108.00")).maxDiscountPct(new BigDecimal("7.1"))
                        .strategy(NegotiationStrategy.AGGRESSIVE).maxRounds(4).allowFreeShipping(false).allowWarrantySweeteners(false).build())
                .images(List.of(imgSony720))
                .tags(List.of("sony", "headphones", "wireless", "anc", "noise-cancelling"))
                .active(true).build());

        upsertProductBySku(Product.builder()
                .sellerId(quantum.getId())
                .title("Sony WH-CH720N Wireless Noise Canceling Headphones")
                .brand("Sony").sku("QTM-SONY-WHCH720N-BLK").category("Audio & Acoustics").condition("Brand New")
                .description("Lightweight Sony WH-CH720N wireless headphones with multipoint Bluetooth 5.2 and quick USB-C charge.")
                .listPrice(new BigDecimal("106.00")).shippingCost(new BigDecimal("5.00")).estimatedDeliveryDays(3)
                .inventoryCount(11).reservedCount(0).minOrderQuantity(1).maxOrderQuantity(4)
                .warrantyInfo("12 Months Official Warranty").returnPolicy("30 Days Hassle-Free Return")
                .specifications(Map.of("Driver Unit", "30mm Dynamic", "ANC", "Integrated Processor V1", "Battery", "35 Hours ANC On"))
                .negotiationPolicy(NegotiationPolicy.builder()
                        .autoNegotiate(true).floorPrice(new BigDecimal("89.00")).targetPrice(new BigDecimal("98.00"))
                        .autoAcceptPriceThreshold(new BigDecimal("96.00")).maxDiscountPct(new BigDecimal("16.0"))
                        .strategy(NegotiationStrategy.BALANCED).maxRounds(4).allowFreeShipping(true).allowWarrantySweeteners(true).maxWarrantyMonthsSweetener(18).maxDeliveryDaysCompromise(1).build())
                .images(List.of(imgSony720))
                .tags(List.of("sony", "headphones", "wireless", "anc", "noise-cancelling"))
                .active(true).build());

        upsertProductBySku(Product.builder()
                .sellerId(novatech.getId())
                .title("Sony WH-CH720N Wireless Noise Canceling Headphones")
                .brand("Sony").sku("Nvt-SONY-WHCH720N-BLK").category("Audio & Acoustics").condition("Brand New")
                .description("Sony WH-CH720N Wireless Noise Canceling Headphones backed by NovaTech rapid 3-day shipping and free return protection.")
                .listPrice(new BigDecimal("99.00")).shippingCost(BigDecimal.ZERO).estimatedDeliveryDays(3)
                .inventoryCount(22).reservedCount(0).minOrderQuantity(1).maxOrderQuantity(5)
                .warrantyInfo("24 Months NovaTech Extended Sony Coverage").returnPolicy("30 Days Free Satisfaction Return")
                .specifications(Map.of("Driver Unit", "30mm Dynamic", "ANC", "Integrated Processor V1", "Battery", "35 Hours ANC On", "Weight", "192g"))
                .negotiationPolicy(NegotiationPolicy.builder()
                        .autoNegotiate(true).floorPrice(new BigDecimal("82.00")).targetPrice(new BigDecimal("92.00"))
                        .autoAcceptPriceThreshold(new BigDecimal("90.00")).maxDiscountPct(new BigDecimal("17.1"))
                        .strategy(NegotiationStrategy.BALANCED).maxRounds(4).allowFreeShipping(true).allowWarrantySweeteners(true).maxWarrantyMonthsSweetener(24).maxDeliveryDaysCompromise(1).build())
                .images(List.of(imgSony720))
                .tags(List.of("sony", "headphones", "wireless", "anc", "noise-cancelling"))
                .active(true).build());

        upsertProductBySku(Product.builder()
                .sellerId(primeAudio.getId())
                .title("Sony WH-CH720N Wireless Noise Canceling Headphones")
                .brand("Sony").sku("PAM-SONY-WHCH720N-SLV").category("Audio & Acoustics").condition("Brand New")
                .description("Prime Audio certified Sony WH-CH720N with tuned acoustic EQ support, free 2-day express courier, and 24-month protection.")
                .listPrice(new BigDecimal("97.00")).shippingCost(BigDecimal.ZERO).estimatedDeliveryDays(2)
                .inventoryCount(16).reservedCount(0).minOrderQuantity(1).maxOrderQuantity(4)
                .warrantyInfo("24 Months Prime Audio Platinum Coverage").returnPolicy("45 Days Free Satisfaction Return")
                .specifications(Map.of("Driver Unit", "30mm Dynamic", "ANC", "Dual Noise Sensor V1", "Battery", "35 Hours ANC On"))
                .negotiationPolicy(NegotiationPolicy.builder()
                        .autoNegotiate(true).floorPrice(new BigDecimal("79.50")).targetPrice(new BigDecimal("89.00"))
                        .autoAcceptPriceThreshold(new BigDecimal("87.00")).maxDiscountPct(new BigDecimal("18.0"))
                        .strategy(NegotiationStrategy.COOPERATIVE).maxRounds(5).allowFreeShipping(true).allowWarrantySweeteners(true).maxWarrantyMonthsSweetener(24).maxDeliveryDaysCompromise(1).build())
                .images(List.of(imgSony720))
                .tags(List.of("sony", "headphones", "wireless", "anc", "noise-cancelling"))
                .active(true).build());

        upsertProductBySku(Product.builder()
                .sellerId(apex.getId())
                .title("Sony WH-CH720N Wireless Noise Canceling Headphones")
                .brand("Sony").sku("APD-SONY-WHCH720N-BLU").category("Audio & Acoustics").condition("Brand New")
                .description("Apex Digital Edition Sony WH-CH720N ANC Wireless Headphones with 2-day courier shipping.")
                .listPrice(new BigDecimal("104.00")).shippingCost(new BigDecimal("4.00")).estimatedDeliveryDays(2)
                .inventoryCount(9).reservedCount(0).minOrderQuantity(1).maxOrderQuantity(3)
                .warrantyInfo("18 Months Apex Extended Warranty").returnPolicy("45 Days Free Return")
                .specifications(Map.of("Driver Unit", "30mm Dynamic", "ANC", "Integrated Processor V1", "Battery", "35 Hours ANC On"))
                .negotiationPolicy(NegotiationPolicy.builder()
                        .autoNegotiate(true).floorPrice(new BigDecimal("85.00")).targetPrice(new BigDecimal("94.00"))
                        .autoAcceptPriceThreshold(new BigDecimal("92.00")).maxDiscountPct(new BigDecimal("18.2"))
                        .strategy(NegotiationStrategy.COOPERATIVE).maxRounds(4).allowFreeShipping(true).allowWarrantySweeteners(true).maxWarrantyMonthsSweetener(24).maxDeliveryDaysCompromise(1).build())
                .images(List.of(imgSony720))
                .tags(List.of("sony", "headphones", "wireless", "anc", "noise-cancelling"))
                .active(true).build());

        upsertProductBySku(Product.builder()
                .sellerId(vertex.getId())
                .title("Sony WH-CH720N Wireless Noise Canceling Headphones")
                .brand("Sony").sku("VTX-SONY-WHCH720N-GRY").category("Audio & Acoustics").condition("Brand New")
                .description("Vertex Technology genuine Sony WH-CH720N Wireless ANC Headphones with protective travel pouch.")
                .listPrice(new BigDecimal("101.00")).shippingCost(BigDecimal.ZERO).estimatedDeliveryDays(4)
                .inventoryCount(14).reservedCount(0).minOrderQuantity(1).maxOrderQuantity(5)
                .warrantyInfo("12 Months Sony Official Warranty").returnPolicy("30 Days Easy Return")
                .specifications(Map.of("Driver Unit", "30mm Dynamic", "ANC", "Dual Noise Sensor V1", "Battery", "35 Hours ANC On"))
                .negotiationPolicy(NegotiationPolicy.builder()
                        .autoNegotiate(true).floorPrice(new BigDecimal("84.00")).targetPrice(new BigDecimal("93.00"))
                        .autoAcceptPriceThreshold(new BigDecimal("91.00")).maxDiscountPct(new BigDecimal("16.8"))
                        .strategy(NegotiationStrategy.BALANCED).maxRounds(4).allowFreeShipping(true).allowWarrantySweeteners(true).maxWarrantyMonthsSweetener(24).maxDeliveryDaysCompromise(1).build())
                .images(List.of(imgSony720))
                .tags(List.of("sony", "headphones", "wireless", "anc", "noise-cancelling"))
                .active(true).build());

        // Additional multi-category catalog listings across Smartwatches, Monitors, Keyboards, Smartphones, Laptop Accessories
        upsertProductBySku(Product.builder()
                .sellerId(vertex.getId())
                .title("Garmin Venu 3 AMOLED GPS Smartwatch")
                .brand("Garmin").sku("VTX-GARMIN-VENU3").category("Smartwatches").condition("Brand New")
                .description("45mm AMOLED fitness & wellness smartwatch with 14-day battery, sleep coach, and built-in speaker/mic.")
                .listPrice(new BigDecimal("399.00")).shippingCost(BigDecimal.ZERO).estimatedDeliveryDays(2)
                .inventoryCount(10).reservedCount(0).minOrderQuantity(1).maxOrderQuantity(2)
                .warrantyInfo("24 Months Garmin + Vertex Coverage").returnPolicy("30 Days Unopened Return")
                .specifications(Map.of("Display", "1.4\" AMOLED", "Battery", "14 Days", "Water Rating", "5 ATM"))
                .negotiationPolicy(NegotiationPolicy.builder()
                        .autoNegotiate(true).floorPrice(new BigDecimal("349.00")).targetPrice(new BigDecimal("379.00"))
                        .autoAcceptPriceThreshold(new BigDecimal("372.00")).maxDiscountPct(new BigDecimal("12.5"))
                        .strategy(NegotiationStrategy.BALANCED).maxRounds(4).allowFreeShipping(true).allowWarrantySweeteners(true).maxWarrantyMonthsSweetener(24).build())
                .images(List.of("https://images.unsplash.com/photo-1523275335684-37898b6baf30?auto=format&fit=crop&w=1000&q=80"))
                .tags(List.of("smartwatch", "garmin", "wearable", "fitness", "amoled")).active(true).build());

        upsertProductBySku(Product.builder()
                .sellerId(apex.getId())
                .title("LG UltraGear 27\" QHD 180Hz Nano IPS Monitor")
                .brand("LG").sku("APD-LG-27GS85Q").category("Monitors").condition("Brand New")
                .description("27-inch 2560x1440 Nano IPS 1ms GtG gaming and creative monitor with HDR400 and height-adjustable stand.")
                .listPrice(new BigDecimal("329.00")).shippingCost(BigDecimal.ZERO).estimatedDeliveryDays(3)
                .inventoryCount(8).reservedCount(0).minOrderQuantity(1).maxOrderQuantity(2)
                .warrantyInfo("24 Months LG On-Site Panel Protection").returnPolicy("30 Days Zero-Dead-Pixel Return")
                .specifications(Map.of("Resolution", "2560 x 1440 QHD", "Refresh Rate", "180Hz", "Panel", "Nano IPS 1ms"))
                .negotiationPolicy(NegotiationPolicy.builder()
                        .autoNegotiate(true).floorPrice(new BigDecimal("279.00")).targetPrice(new BigDecimal("305.00"))
                        .autoAcceptPriceThreshold(new BigDecimal("299.00")).maxDiscountPct(new BigDecimal("15.2"))
                        .strategy(NegotiationStrategy.COOPERATIVE).maxRounds(4).allowFreeShipping(true).allowWarrantySweeteners(true).maxWarrantyMonthsSweetener(24).build())
                .images(List.of("https://images.unsplash.com/photo-1527443224154-c4a3942d3acf?auto=format&fit=crop&w=1000&q=80"))
                .tags(List.of("monitor", "lg", "display", "qhd", "ips", "180hz")).active(true).build());

        upsertProductBySku(Product.builder()
                .sellerId(quantum.getId())
                .title("Keychron Q1 Pro Wireless QMK Mechanical Keyboard")
                .brand("Keychron").sku("QTM-KEYCHRON-Q1P").category("Keyboards").condition("Brand New")
                .description("Full aluminum 75% custom wireless mechanical keyboard with hot-swappable Banana tactile switches and QMK/VIA support.")
                .listPrice(new BigDecimal("189.00")).shippingCost(BigDecimal.ZERO).estimatedDeliveryDays(3)
                .inventoryCount(15).reservedCount(0).minOrderQuantity(1).maxOrderQuantity(4)
                .warrantyInfo("12 Months Keychron Hardware Warranty").returnPolicy("30 Days Free Return")
                .specifications(Map.of("Layout", "75% CNC Aluminum", "Connectivity", "Bluetooth 5.1 & USB-C", "Switches", "Hot-Swappable Tactile"))
                .negotiationPolicy(NegotiationPolicy.builder()
                        .autoNegotiate(true).floorPrice(new BigDecimal("158.00")).targetPrice(new BigDecimal("175.00"))
                        .autoAcceptPriceThreshold(new BigDecimal("171.00")).maxDiscountPct(new BigDecimal("16.4"))
                        .strategy(NegotiationStrategy.BALANCED).maxRounds(4).allowFreeShipping(true).allowWarrantySweeteners(true).maxWarrantyMonthsSweetener(24).build())
                .images(List.of("https://images.unsplash.com/photo-1587829741301-dc798b83add3?auto=format&fit=crop&w=1000&q=80"))
                .tags(List.of("keyboard", "mechanical", "keychron", "wireless", "custom")).active(true).build());

        upsertProductBySku(Product.builder()
                .sellerId(novatech.getId())
                .title("Google Pixel 9 Unlocked 5G Smartphone (128GB)")
                .brand("Google").sku("NVT-PIXEL9-128").category("Smartphones").condition("Brand New")
                .description("Unlocked Google Pixel 9 with Tensor G4 processor, Actua OLED display, and dual flagship cameras.")
                .listPrice(new BigDecimal("699.00")).shippingCost(BigDecimal.ZERO).estimatedDeliveryDays(2)
                .inventoryCount(7).reservedCount(0).minOrderQuantity(1).maxOrderQuantity(2)
                .warrantyInfo("24 Months NovaTech Device Care").returnPolicy("30 Days Sealed/Opened Guarantee")
                .specifications(Map.of("Chipset", "Google Tensor G4", "Storage", "128GB", "Display", "6.3\" 120Hz OLED"))
                .negotiationPolicy(NegotiationPolicy.builder()
                        .autoNegotiate(true).floorPrice(new BigDecimal("629.00")).targetPrice(new BigDecimal("669.00"))
                        .autoAcceptPriceThreshold(new BigDecimal("660.00")).maxDiscountPct(new BigDecimal("10.0"))
                        .strategy(NegotiationStrategy.BALANCED).maxRounds(4).allowFreeShipping(true).allowWarrantySweeteners(true).maxWarrantyMonthsSweetener(24).build())
                .images(List.of("https://images.unsplash.com/photo-1511707171634-5f897ff02aa9?auto=format&fit=crop&w=1000&q=80"))
                .tags(List.of("smartphone", "google", "pixel", "5g", "unlocked")).active(true).build());

        upsertProductBySku(Product.builder()
                .sellerId(primeAudio.getId())
                .title("Anker Prime 100W 3-Port GaN Laptop Charger & USB-C Hub")
                .brand("Anker").sku("PAM-ANKER-100W-GAN").category("Laptop Accessories").condition("Brand New")
                .description("Compact GaN II 100W USB-C power adapter with braided 240W cable for MacBook, Windows laptops, and handhelds.")
                .listPrice(new BigDecimal("69.00")).shippingCost(BigDecimal.ZERO).estimatedDeliveryDays(2)
                .inventoryCount(30).reservedCount(0).minOrderQuantity(1).maxOrderQuantity(5)
                .warrantyInfo("24 Months Anker Worry-Free Warranty").returnPolicy("45 Days Easy Return")
                .specifications(Map.of("Output", "100W Max GaN II", "Ports", "2x USB-C + 1x USB-A", "Compatibility", "USB-C PD 3.0 Laptops"))
                .negotiationPolicy(NegotiationPolicy.builder()
                        .autoNegotiate(true).floorPrice(new BigDecimal("52.00")).targetPrice(new BigDecimal("60.00"))
                        .autoAcceptPriceThreshold(new BigDecimal("58.00")).maxDiscountPct(new BigDecimal("24.6"))
                        .strategy(NegotiationStrategy.COOPERATIVE).maxRounds(4).allowFreeShipping(true).allowWarrantySweeteners(true).maxWarrantyMonthsSweetener(24).build())
                .images(List.of("https://images.unsplash.com/photo-1618384887929-16ec33fab9ef?auto=format&fit=crop&w=1000&q=80"))
                .tags(List.of("laptop accessories", "charger", "anker", "usbc", "gan")).active(true).build());

        log.info("Upserted 6 realistic demo merchant stores and multi-category catalog successfully.");
    }

    private User upsertSellerStore(String email, String storeName, double reputation, NegotiationStrategy strategy,
                                   String description, int handlingDays, BigDecimal freeShippingThreshold, int returnDays) {
        User user = userRepository.findByEmail(email).orElseGet(() -> userRepository.save(User.builder()
                .email(email)
                .passwordHash(passwordEncoder.encode("password123"))
                .fullName(storeName)
                .role(UserRole.ROLE_SELLER)
                .reputationScore(reputation)
                .defaultCurrency("USD")
                .createdAt(Instant.now().minus(30, ChronoUnit.DAYS))
                .updatedAt(Instant.now())
                .build()));

        if (!sellerProfileRepository.existsByUserId(user.getId())) {
            SellerProfile profile = SellerProfile.builder()
                    .userId(user.getId())
                    .storeName(storeName)
                    .description(description)
                    .logoUrl("https://images.unsplash.com/photo-1511671782779-c97d3d27a1d4?auto=format&fit=crop&w=400&q=80")
                    .businessContact(BusinessContact.builder()
                            .phone("+1 (555) 400-2200")
                            .supportEmail("support@" + storeName.toLowerCase().replaceAll("[^a-z0-9]", "") + ".example")
                            .supportHours("Mon-Sat 09:00 - 18:00 EST")
                            .build())
                    .shippingRegions(List.of("United States", "Canada"))
                    .defaultShippingTerms(ShippingTerms.builder()
                            .handlingTimeDays(handlingDays)
                            .freeShippingThreshold(freeShippingThreshold)
                            .allowExpressShipping(true)
                            .returnPolicyDays(returnDays)
                            .build())
                    .defaultStrategy(strategy)
                    .autoNegotiateEnabled(true)
                    .minimumReputationRequired(65.0)
                    .onboardingCompleted(true)
                    .onboardingStep(4)
                    .createdAt(Instant.now().minus(30, ChronoUnit.DAYS))
                    .updatedAt(Instant.now())
                    .build();
            sellerProfileRepository.save(profile);
        }
        return user;
    }

    private void upsertProductBySku(Product template) {
        List<Product> existing = productRepository.findBySellerId(template.getSellerId());
        boolean exists = existing.stream().anyMatch(p -> template.getSku() != null && template.getSku().equalsIgnoreCase(p.getSku()));
        if (!exists) {
            template.setCreatedAt(Instant.now().minus(3, ChronoUnit.DAYS));
            template.setUpdatedAt(Instant.now());
            productRepository.save(template);
        }
    }
}
