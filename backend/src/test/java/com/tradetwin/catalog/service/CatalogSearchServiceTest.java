package com.tradetwin.catalog.service;

import com.tradetwin.catalog.dto.CatalogSearchRequest;
import com.tradetwin.catalog.dto.CatalogSearchResponse;
import com.tradetwin.catalog.dto.ProductPublicDto;
import com.tradetwin.catalog.model.NegotiationPolicy;
import com.tradetwin.catalog.model.Product;
import com.tradetwin.catalog.repository.ProductRepository;
import com.tradetwin.seller.model.SellerProfile;
import com.tradetwin.seller.repository.SellerProfileRepository;
import com.tradetwin.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CatalogSearchServiceTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private SellerProfileRepository sellerProfileRepository;

    @Mock
    private UserRepository userRepository;

    private CatalogSearchService catalogSearchService;

    private Product sonyHeadphones;
    private Product boseHeadphones;
    private Product outOfStockProduct;

    static class StubProductService extends ProductService {
        public StubProductService() { super(null); }

        @Override
        public ProductPublicDto toPublicDto(Product p) {
            return ProductPublicDto.builder()
                    .id(p.getId())
                    .title(p.getTitle())
                    .brand(p.getBrand())
                    .listPrice(p.getListPrice())
                    .availableInventory(p.getAvailableInventory())
                    .build();
        }
    }

    @BeforeEach
    void setUp() {
        catalogSearchService = new CatalogSearchService(
                productRepository,
                new StubProductService(),
                sellerProfileRepository,
                userRepository
        );

        sonyHeadphones = Product.builder()
                .id("p-sony-1")
                .sellerId("seller-1")
                .title("Sony WH-CH720N Wireless Noise Canceling Headphones")
                .brand("Sony")
                .category("Audio & Acoustics")
                .condition("Brand New")
                .listPrice(new BigDecimal("98.00"))
                .shippingCost(BigDecimal.ZERO)
                .estimatedDeliveryDays(2)
                .warrantyInfo("12 Months Sony Warranty")
                .inventoryCount(15)
                .active(true)
                .negotiationPolicy(NegotiationPolicy.builder().autoNegotiate(true).floorPrice(new BigDecimal("80.00")).build())
                .build();

        boseHeadphones = Product.builder()
                .id("p-bose-1")
                .sellerId("seller-1")
                .title("Bose QuietComfort 45")
                .brand("Bose")
                .category("Audio & Acoustics")
                .condition("Brand New")
                .listPrice(new BigDecimal("220.00"))
                .shippingCost(new BigDecimal("10.00"))
                .estimatedDeliveryDays(3)
                .warrantyInfo("24 Months Warranty")
                .inventoryCount(8)
                .active(true)
                .negotiationPolicy(NegotiationPolicy.builder().autoNegotiate(true).floorPrice(new BigDecimal("180.00")).build())
                .build();

        outOfStockProduct = Product.builder()
                .id("p-sold-out")
                .sellerId("seller-1")
                .title("Sony Vintage Headphones")
                .brand("Sony")
                .inventoryCount(0)
                .active(true)
                .build();
    }

    @Test
    @DisplayName("Deterministic search matches brand, price, shipping and excludes out-of-stock")
    void testSearchMatchingAndRanking() {
        when(productRepository.findByActiveTrue()).thenReturn(List.of(sonyHeadphones, boseHeadphones, outOfStockProduct));
        when(sellerProfileRepository.findByUserId("seller-1")).thenReturn(Optional.of(
                SellerProfile.builder().userId("seller-1").storeName("AudioSphere Official").build()
        ));

        CatalogSearchRequest request = CatalogSearchRequest.builder()
                .productType("wireless noise-cancelling headphones")
                .brandPreferences(List.of("Sony"))
                .maxBudget(new BigDecimal("100.00"))
                .minWarrantyMonths(12)
                .maxDeliveryDays(4)
                .quantity(1)
                .build();

        CatalogSearchResponse response = catalogSearchService.search(request);

        assertNotNull(response);
        assertFalse(response.getResults().isEmpty());
        // Verify Sony is ranked #1 with higher match score
        assertEquals("Sony", response.getResults().get(0).getProduct().getBrand());
        assertEquals(new BigDecimal("98.00"), response.getResults().get(0).getEstimatedTotalPrice());
        assertEquals("AudioSphere Official", response.getResults().get(0).getSellerStoreName());
        assertTrue(response.getResults().get(0).getMatchScore() > 70.0);
        // Verify out-of-stock product was omitted
        assertTrue(response.getResults().stream().noneMatch(r -> "p-sold-out".equals(r.getProduct().getId())));
    }
}
