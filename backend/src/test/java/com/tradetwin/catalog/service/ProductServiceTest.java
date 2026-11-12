package com.tradetwin.catalog.service;

import com.tradetwin.catalog.dto.CreateProductRequest;
import com.tradetwin.catalog.dto.ProductPublicDto;
import com.tradetwin.catalog.dto.UpdateProductRequest;
import com.tradetwin.catalog.model.NegotiationPolicy;
import com.tradetwin.catalog.model.Product;
import com.tradetwin.catalog.repository.ProductRepository;
import com.tradetwin.common.exception.BusinessRuleException;
import com.tradetwin.common.exception.UnauthorizedException;
import com.tradetwin.seller.model.NegotiationStrategy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock
    private ProductRepository productRepository;

    @InjectMocks
    private ProductService productService;

    private NegotiationPolicy validPolicy;

    @BeforeEach
    void setUp() {
        validPolicy = NegotiationPolicy.builder()
                .autoNegotiate(true)
                .floorPrice(new BigDecimal("1000.00"))
                .targetPrice(new BigDecimal("1200.00"))
                .strategy(NegotiationStrategy.BALANCED)
                .maxRounds(5)
                .build();
    }

    @Test
    @DisplayName("Should reject product creation when floor price exceeds list price")
    void testFloorExceedsListPrice() {
        CreateProductRequest request = CreateProductRequest.builder()
                .title("Test Product")
                .description("Description")
                .category("Electronics")
                .listPrice(new BigDecimal("900.00")) // Lower than floor (1000.00)
                .negotiationPolicy(validPolicy)
                .build();

        assertThrows(BusinessRuleException.class, () ->
                productService.createProduct("seller123", request)
        );
    }

    @Test
    @DisplayName("Should conceal private floorPrice and targetPrice in public DTO")
    void testConcealPrivateLimits() {
        Product product = Product.builder()
                .id("prod-1")
                .sellerId("seller-1")
                .title("Super Laptop")
                .brand("QuantumBlade")
                .condition("Brand New")
                .listPrice(new BigDecimal("1500.00"))
                .inventoryCount(10)
                .negotiationPolicy(validPolicy)
                .build();

        ProductPublicDto publicDto = productService.toPublicDto(product);

        assertNotNull(publicDto);
        assertEquals("Super Laptop", publicDto.getTitle());
        assertEquals("QuantumBlade", publicDto.getBrand());
        assertEquals("Brand New", publicDto.getCondition());
        assertEquals(new BigDecimal("1500.00"), publicDto.getListPrice());
        assertEquals(10, publicDto.getAvailableInventory());
        assertFalse(publicDto.toString().contains("1000.00"), "Floor price must not be leaked into public DTO");
    }

    @Test
    @DisplayName("Should allow owner to update product and reject unauthorized seller")
    void testUpdateProductOwnershipEnforcement() {
        Product product = Product.builder()
                .id("prod-100")
                .sellerId("legit-seller")
                .title("Old Title")
                .listPrice(new BigDecimal("100.00"))
                .negotiationPolicy(validPolicy)
                .build();

        when(productRepository.findById("prod-100")).thenReturn(Optional.of(product));
        when(productRepository.save(any(Product.class))).thenAnswer(i -> i.getArgument(0));

        // Unauthorized attempt throws UnauthorizedException
        UpdateProductRequest unauthorizedReq = UpdateProductRequest.builder().title("Hacked Title").build();
        assertThrows(UnauthorizedException.class, () ->
                productService.updateProduct("rogue-seller", "prod-100", unauthorizedReq)
        );

        // Legitimate owner update succeeds
        UpdateProductRequest legitReq = UpdateProductRequest.builder()
                .title("New Legit Title")
                .brand("New Brand")
                .inventoryCount(20)
                .build();
        Product updated = productService.updateProduct("legit-seller", "prod-100", legitReq);
        assertEquals("New Legit Title", updated.getTitle());
        assertEquals("New Brand", updated.getBrand());
        assertEquals(20, updated.getInventoryCount());
    }

    @Test
    @DisplayName("Should enforce ownership on product delete")
    void testDeleteProductOwnership() {
        Product product = Product.builder().id("p1").sellerId("seller-1").build();
        when(productRepository.findById("p1")).thenReturn(Optional.of(product));

        assertThrows(UnauthorizedException.class, () -> productService.deleteProduct("wrong-seller", "p1"));

        productService.deleteProduct("seller-1", "p1");
        verify(productRepository, times(1)).delete(product);
    }

    @Test
    @DisplayName("Should toggle status and update stock for product owner")
    void testToggleStatusAndStock() {
        Product product = Product.builder().id("p1").sellerId("seller-1").active(true).inventoryCount(5).build();
        when(productRepository.findById("p1")).thenReturn(Optional.of(product));
        when(productRepository.save(any(Product.class))).thenAnswer(i -> i.getArgument(0));

        Product paused = productService.toggleProductStatus("seller-1", "p1", false);
        assertFalse(paused.isActive());

        Product restocked = productService.updateProductStock("seller-1", "p1", 50);
        assertEquals(50, restocked.getInventoryCount());
    }
}
