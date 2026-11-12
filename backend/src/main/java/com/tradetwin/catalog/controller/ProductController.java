package com.tradetwin.catalog.controller;

import com.tradetwin.catalog.dto.CatalogSearchRequest;
import com.tradetwin.catalog.dto.CatalogSearchResponse;
import com.tradetwin.catalog.dto.CreateProductRequest;
import com.tradetwin.catalog.dto.ProductPublicDto;
import com.tradetwin.catalog.dto.UpdateProductRequest;
import com.tradetwin.catalog.model.NegotiationPolicy;
import com.tradetwin.catalog.model.Product;
import com.tradetwin.catalog.service.CatalogSearchService;
import com.tradetwin.catalog.service.ProductService;
import com.tradetwin.common.api.ApiResponse;
import com.tradetwin.user.model.User;
import com.tradetwin.user.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping({"/api/catalog", "/api/v1/catalog"})
public class ProductController {

    private final ProductService productService;
    private final CatalogSearchService catalogSearchService;
    private final UserService userService;

    public ProductController(ProductService productService,
                             CatalogSearchService catalogSearchService,
                             UserService userService) {
        this.productService = productService;
        this.catalogSearchService = catalogSearchService;
        this.userService = userService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<ProductPublicDto>>> getAllProducts(
            @RequestParam(required = false) String search) {
        List<ProductPublicDto> products = productService.searchProducts(search);
        return ResponseEntity.ok(ApiResponse.ok(products));
    }

    @GetMapping({"/products/{id}", "/{id}"})
    public ResponseEntity<ApiResponse<ProductPublicDto>> getProductById(@PathVariable String id) {
        ProductPublicDto product = productService.getPublicProductById(id);
        return ResponseEntity.ok(ApiResponse.ok(product));
    }

    @PostMapping("/search")
    public ResponseEntity<ApiResponse<CatalogSearchResponse>> searchCatalog(
            @RequestBody CatalogSearchRequest request) {
        CatalogSearchResponse response = catalogSearchService.search(request);
        return ResponseEntity.ok(ApiResponse.ok("Catalog search completed", response));
    }

    @PostMapping
    @PreAuthorize("hasRole('SELLER') or hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Product>> createProduct(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody CreateProductRequest request) {
        User seller = userService.getUserByEmail(userDetails.getUsername());
        Product product = productService.createProduct(seller.getId(), request);
        return new ResponseEntity<>(ApiResponse.ok("Product created successfully", product), HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('SELLER') or hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Product>> updateProduct(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable String id,
            @Valid @RequestBody UpdateProductRequest request) {
        User seller = userService.getUserByEmail(userDetails.getUsername());
        Product updated = productService.updateProduct(seller.getId(), id, request);
        return ResponseEntity.ok(ApiResponse.ok("Product updated successfully", updated));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('SELLER') or hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteProduct(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable String id) {
        User seller = userService.getUserByEmail(userDetails.getUsername());
        productService.deleteProduct(seller.getId(), id);
        return ResponseEntity.ok(ApiResponse.ok("Product deleted successfully", null));
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasRole('SELLER') or hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Product>> toggleStatus(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable String id,
            @RequestParam boolean active) {
        User seller = userService.getUserByEmail(userDetails.getUsername());
        Product product = productService.toggleProductStatus(seller.getId(), id, active);
        return ResponseEntity.ok(ApiResponse.ok("Product status updated successfully", product));
    }

    @PatchMapping("/{id}/stock")
    @PreAuthorize("hasRole('SELLER') or hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Product>> updateStock(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable String id,
            @RequestParam int count) {
        User seller = userService.getUserByEmail(userDetails.getUsername());
        Product product = productService.updateProductStock(seller.getId(), id, count);
        return ResponseEntity.ok(ApiResponse.ok("Product stock updated successfully", product));
    }

    @GetMapping("/seller/my-products")
    @PreAuthorize("hasRole('SELLER') or hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<List<Product>>> getMyProducts(@AuthenticationPrincipal UserDetails userDetails) {
        User seller = userService.getUserByEmail(userDetails.getUsername());
        List<Product> products = productService.getProductsBySellerId(seller.getId());
        return ResponseEntity.ok(ApiResponse.ok(products));
    }

    @GetMapping("/seller/products/{id}")
    @PreAuthorize("hasRole('SELLER') or hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Product>> getMyProductById(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable String id) {
        User seller = userService.getUserByEmail(userDetails.getUsername());
        Product product = productService.getProductForSeller(seller.getId(), id);
        return ResponseEntity.ok(ApiResponse.ok(product));
    }

    @PutMapping("/seller/products/{id}/negotiation-policy")
    @PreAuthorize("hasRole('SELLER') or hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Product>> updateNegotiationPolicy(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable String id,
            @Valid @RequestBody NegotiationPolicy policy) {
        User seller = userService.getUserByEmail(userDetails.getUsername());
        Product product = productService.updateNegotiationPolicy(seller.getId(), id, policy);
        return ResponseEntity.ok(ApiResponse.ok("Negotiation policy updated successfully", product));
    }
}
