package com.tradetwin.catalog.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tradetwin.catalog.dto.CatalogSearchRequest;
import com.tradetwin.catalog.dto.CatalogSearchResponse;
import com.tradetwin.catalog.dto.CreateProductRequest;
import com.tradetwin.catalog.dto.ProductPublicDto;
import com.tradetwin.catalog.dto.SearchResultItemDto;
import com.tradetwin.catalog.dto.UpdateProductRequest;
import com.tradetwin.catalog.model.NegotiationPolicy;
import com.tradetwin.catalog.model.Product;
import com.tradetwin.catalog.service.CatalogSearchService;
import com.tradetwin.catalog.service.ProductService;
import com.tradetwin.seller.model.NegotiationStrategy;
import com.tradetwin.user.model.User;
import com.tradetwin.user.model.UserRole;
import com.tradetwin.user.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ProductControllerHttpTest {

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;
    private UsernamePasswordAuthenticationToken authPrincipal;

    static class StubUserService extends UserService {
        public StubUserService() { super(null); }
        @Override
        public User getUserByEmail(String email) {
            return User.builder().id("seller-u1").email(email).role(UserRole.ROLE_SELLER).build();
        }
    }

    static class StubProductService extends ProductService {
        public StubProductService() { super(null); }

        @Override
        public Product createProduct(String sellerId, CreateProductRequest request) {
            return Product.builder()
                    .id("new-p1")
                    .sellerId(sellerId)
                    .title(request.getTitle())
                    .brand(request.getBrand())
                    .listPrice(request.getListPrice())
                    .inventoryCount(request.getInventoryCount())
                    .negotiationPolicy(request.getNegotiationPolicy())
                    .build();
        }

        @Override
        public Product updateProduct(String sellerId, String productId, UpdateProductRequest request) {
            return Product.builder()
                    .id(productId)
                    .sellerId(sellerId)
                    .title(request.getTitle())
                    .build();
        }

        @Override
        public void deleteProduct(String sellerId, String productId) {}

        @Override
        public Product toggleProductStatus(String sellerId, String productId, boolean active) {
            return Product.builder().id(productId).sellerId(sellerId).active(active).build();
        }

        @Override
        public Product updateProductStock(String sellerId, String productId, int newInventory) {
            return Product.builder().id(productId).sellerId(sellerId).inventoryCount(newInventory).build();
        }

        @Override
        public List<Product> getProductsBySellerId(String sellerId) {
            return List.of(Product.builder().id("p1").sellerId(sellerId).title("Product 1").build());
        }

        @Override
        public ProductPublicDto getPublicProductById(String id) {
            return ProductPublicDto.builder()
                    .id(id)
                    .title("Sony WH-CH720N")
                    .brand("Sony")
                    .listPrice(new BigDecimal("98.00"))
                    .availableInventory(20)
                    .build();
        }

        @Override
        public Product updateNegotiationPolicy(String sellerId, String productId, NegotiationPolicy policy) {
            return Product.builder().id(productId).sellerId(sellerId).negotiationPolicy(policy).build();
        }
    }

    static class StubCatalogSearchService extends CatalogSearchService {
        public StubCatalogSearchService() { super(null, null, null, null); }

        @Override
        public CatalogSearchResponse search(CatalogSearchRequest request) {
            return CatalogSearchResponse.builder()
                    .results(List.of(
                            SearchResultItemDto.builder()
                                    .unitPrice(new BigDecimal("98.00"))
                                    .estimatedTotalPrice(new BigDecimal("98.00"))
                                    .matchScore(98.5)
                                    .sellerStoreName("QuantumTech Direct")
                                    .matchHighlights(List.of("Exact brand match: Sony"))
                                    .build()
                    ))
                    .totalMatched(1)
                    .searchSummary("Found 1 verified merchant listing matching Sony headphones")
                    .build();
        }
    }

    @BeforeEach
    void setUp() {
        ProductController productController = new ProductController(
                new StubProductService(),
                new StubCatalogSearchService(),
                new StubUserService()
        );
        mockMvc = MockMvcBuilders.standaloneSetup(productController)
                .setCustomArgumentResolvers(new AuthenticationPrincipalArgumentResolver())
                .build();
        objectMapper = new ObjectMapper();

        UserDetails mockUserDetails = org.springframework.security.core.userdetails.User
                .withUsername("seller@tradetwin.com")
                .password("password123")
                .roles("SELLER")
                .build();
        authPrincipal = new UsernamePasswordAuthenticationToken(mockUserDetails, null, mockUserDetails.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(authPrincipal);
    }

    @Test
    @DisplayName("POST /api/catalog creates a new product listing with negotiation policy")
    void testCreateProduct() throws Exception {
        NegotiationPolicy policy = NegotiationPolicy.builder()
                .autoNegotiate(true)
                .floorPrice(new BigDecimal("80.00"))
                .targetPrice(new BigDecimal("95.00"))
                .strategy(NegotiationStrategy.BALANCED)
                .maxRounds(5)
                .build();

        CreateProductRequest req = CreateProductRequest.builder()
                .title("Developer Headphones")
                .brand("SoundPro")
                .description("Studio quality headphones")
                .category("Electronics")
                .listPrice(new BigDecimal("100.00"))
                .inventoryCount(20)
                .negotiationPolicy(policy)
                .build();

        mockMvc.perform(post("/api/catalog")
                        .principal(authPrincipal)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.title").value("Developer Headphones"));
    }

    @Test
    @DisplayName("PUT /api/catalog/{id} updates existing listing")
    void testUpdateProduct() throws Exception {
        UpdateProductRequest req = UpdateProductRequest.builder()
                .title("Updated Title")
                .build();

        mockMvc.perform(put("/api/catalog/prod-123")
                        .principal(authPrincipal)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.title").value("Updated Title"));
    }

    @Test
    @DisplayName("DELETE /api/catalog/{id} deletes listing")
    void testDeleteProduct() throws Exception {
        mockMvc.perform(delete("/api/catalog/prod-123")
                        .principal(authPrincipal))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @DisplayName("PATCH /api/catalog/{id}/status toggles listing active state")
    void testToggleStatus() throws Exception {
        mockMvc.perform(patch("/api/catalog/prod-123/status")
                        .principal(authPrincipal)
                        .param("active", "false"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.active").value(false));
    }

    @Test
    @DisplayName("PATCH /api/catalog/{id}/stock updates inventory count")
    void testUpdateStock() throws Exception {
        mockMvc.perform(patch("/api/catalog/prod-123/stock")
                        .principal(authPrincipal)
                        .param("count", "45"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.inventoryCount").value(45));
    }

    @Test
    @DisplayName("GET /api/catalog/seller/my-products returns seller listings")
    void testGetMyProducts() throws Exception {
        mockMvc.perform(get("/api/catalog/seller/my-products")
                        .principal(authPrincipal))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].title").value("Product 1"));
    }

    @Test
    @DisplayName("POST /api/catalog/search returns structured merchant match rankings")
    void testSearchCatalog() throws Exception {
        CatalogSearchRequest request = CatalogSearchRequest.builder()
                .productType("headphones")
                .brandPreferences(List.of("Sony"))
                .maxBudget(new BigDecimal("100.00"))
                .build();

        mockMvc.perform(post("/api/catalog/search")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.totalMatched").value(1))
                .andExpect(jsonPath("$.data.results[0].sellerStoreName").value("QuantumTech Direct"))
                .andExpect(jsonPath("$.data.results[0].unitPrice").value(98.00));
    }

    @Test
    @DisplayName("GET /api/catalog/products/{id} returns product by ID")
    void testGetProductByProductsIdRoute() throws Exception {
        mockMvc.perform(get("/api/catalog/products/prod-sony-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.title").value("Sony WH-CH720N"));
    }
}
