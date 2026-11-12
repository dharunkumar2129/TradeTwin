package com.tradetwin.catalog.service;

import com.tradetwin.catalog.dto.CreateProductRequest;
import com.tradetwin.catalog.dto.ProductPublicDto;
import com.tradetwin.catalog.dto.UpdateProductRequest;
import com.tradetwin.catalog.model.NegotiationPolicy;
import com.tradetwin.catalog.model.Product;
import com.tradetwin.catalog.repository.ProductRepository;
import com.tradetwin.common.exception.BusinessRuleException;
import com.tradetwin.common.exception.ResourceNotFoundException;
import com.tradetwin.common.exception.UnauthorizedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@Service
public class ProductService {

    private static final Logger log = LoggerFactory.getLogger(ProductService.class);

    private final ProductRepository productRepository;

    public ProductService(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    public Product createProduct(String sellerId, CreateProductRequest request) {
        validatePricingPolicy(request.getListPrice(), request.getNegotiationPolicy());

        Product product = Product.builder()
                .sellerId(sellerId)
                .title(request.getTitle().trim())
                .brand(request.getBrand() != null ? request.getBrand().trim() : "")
                .sku(request.getSku() != null ? request.getSku().trim() : null)
                .description(request.getDescription().trim())
                .category(request.getCategory().trim())
                .condition(request.getCondition() != null ? request.getCondition() : "Brand New")
                .listPrice(request.getListPrice())
                .currency(request.getCurrency() != null ? request.getCurrency() : "USD")
                .inventoryCount(request.getInventoryCount())
                .reservedCount(0)
                .minOrderQuantity(request.getMinOrderQuantity() > 0 ? request.getMinOrderQuantity() : 1)
                .maxOrderQuantity(request.getMaxOrderQuantity() > 0 ? request.getMaxOrderQuantity() : 10)
                .shippingCost(request.getShippingCost() != null ? request.getShippingCost() : BigDecimal.ZERO)
                .estimatedDeliveryDays(request.getEstimatedDeliveryDays() > 0 ? request.getEstimatedDeliveryDays() : 3)
                .warrantyInfo(request.getWarrantyInfo() != null ? request.getWarrantyInfo() : "12 Months Limited Warranty")
                .returnPolicy(request.getReturnPolicy() != null ? request.getReturnPolicy() : "30 Days Return Policy")
                .specifications(request.getSpecifications())
                .negotiationPolicy(request.getNegotiationPolicy())
                .images(request.getImages() != null ? request.getImages() : List.of())
                .tags(request.getTags() != null ? request.getTags() : List.of())
                .active(true)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        return productRepository.save(product);
    }

    public Product updateProduct(String sellerId, String productId, UpdateProductRequest request) {
        Product product = getProductById(productId);
        verifySellerOwnership(sellerId, product);

        if (request.getTitle() != null && !request.getTitle().trim().isEmpty()) {
            product.setTitle(request.getTitle().trim());
        }
        if (request.getBrand() != null) {
            product.setBrand(request.getBrand().trim());
        }
        if (request.getSku() != null) {
            product.setSku(request.getSku().trim());
        }
        if (request.getDescription() != null && !request.getDescription().trim().isEmpty()) {
            product.setDescription(request.getDescription().trim());
        }
        if (request.getCategory() != null && !request.getCategory().trim().isEmpty()) {
            product.setCategory(request.getCategory().trim());
        }
        if (request.getCondition() != null) {
            product.setCondition(request.getCondition());
        }
        if (request.getListPrice() != null) {
            product.setListPrice(request.getListPrice());
        }
        if (request.getCurrency() != null) {
            product.setCurrency(request.getCurrency());
        }
        if (request.getInventoryCount() != null) {
            product.setInventoryCount(Math.max(0, request.getInventoryCount()));
        }
        if (request.getMinOrderQuantity() != null && request.getMinOrderQuantity() > 0) {
            product.setMinOrderQuantity(request.getMinOrderQuantity());
        }
        if (request.getMaxOrderQuantity() != null && request.getMaxOrderQuantity() > 0) {
            product.setMaxOrderQuantity(request.getMaxOrderQuantity());
        }
        if (request.getShippingCost() != null) {
            product.setShippingCost(request.getShippingCost());
        }
        if (request.getEstimatedDeliveryDays() != null && request.getEstimatedDeliveryDays() > 0) {
            product.setEstimatedDeliveryDays(request.getEstimatedDeliveryDays());
        }
        if (request.getWarrantyInfo() != null) {
            product.setWarrantyInfo(request.getWarrantyInfo());
        }
        if (request.getReturnPolicy() != null) {
            product.setReturnPolicy(request.getReturnPolicy());
        }
        if (request.getSpecifications() != null) {
            product.setSpecifications(request.getSpecifications());
        }
        if (request.getImages() != null) {
            product.setImages(request.getImages());
        }
        if (request.getTags() != null) {
            product.setTags(request.getTags());
        }
        if (request.getActive() != null) {
            product.setActive(request.getActive());
        }
        if (request.getNegotiationPolicy() != null) {
            validatePricingPolicy(product.getListPrice(), request.getNegotiationPolicy());
            product.setNegotiationPolicy(request.getNegotiationPolicy());
        }

        product.setUpdatedAt(Instant.now());
        return productRepository.save(product);
    }

    public void deleteProduct(String sellerId, String productId) {
        Product product = getProductById(productId);
        verifySellerOwnership(sellerId, product);
        productRepository.delete(product);
    }

    public Product toggleProductStatus(String sellerId, String productId, boolean active) {
        Product product = getProductById(productId);
        verifySellerOwnership(sellerId, product);
        product.setActive(active);
        product.setUpdatedAt(Instant.now());
        return productRepository.save(product);
    }

    public Product updateProductStock(String sellerId, String productId, int newInventory) {
        Product product = getProductById(productId);
        verifySellerOwnership(sellerId, product);
        product.setInventoryCount(Math.max(0, newInventory));
        product.setUpdatedAt(Instant.now());
        return productRepository.save(product);
    }

    public Product updateNegotiationPolicy(String sellerId, String productId, NegotiationPolicy policy) {
        Product product = getProductById(productId);
        verifySellerOwnership(sellerId, product);
        validatePricingPolicy(product.getListPrice(), policy);
        product.setNegotiationPolicy(policy);
        product.setUpdatedAt(Instant.now());
        return productRepository.save(product);
    }

    public Product getProductForSeller(String sellerId, String productId) {
        Product product = getProductById(productId);
        verifySellerOwnership(sellerId, product);
        return product;
    }

    public Product getProductById(String id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product", "id", id));
    }

    public ProductPublicDto getPublicProductById(String id) {
        Product product = getProductById(id);
        return toPublicDto(product);
    }

    public List<ProductPublicDto> getAllPublicProducts() {
        return productRepository.findByActiveTrue().stream()
                .map(this::toPublicDto)
                .toList();
    }

    public List<ProductPublicDto> searchProducts(String query) {
        if (query == null || query.isBlank()) {
            return getAllPublicProducts();
        }
        return productRepository.searchProducts(query.trim()).stream()
                .map(this::toPublicDto)
                .toList();
    }

    public List<Product> getProductsBySellerId(String sellerId) {
        return productRepository.findBySellerId(sellerId);
    }

    public synchronized void reserveStock(String productId, int quantity) {
        Product product = getProductById(productId);
        if (product.getAvailableInventory() < quantity) {
            throw new BusinessRuleException("Insufficient inventory to reserve: available=" + product.getAvailableInventory());
        }
        product.setReservedCount(product.getReservedCount() + quantity);
        product.setUpdatedAt(Instant.now());
        productRepository.save(product);
    }

    public synchronized void releaseStock(String productId, int quantity) {
        Product product = getProductById(productId);
        int newReserved = Math.max(0, product.getReservedCount() - quantity);
        product.setReservedCount(newReserved);
        product.setUpdatedAt(Instant.now());
        productRepository.save(product);
    }

    public synchronized void deductStock(String productId, int quantity) {
        Product product = getProductById(productId);
        int newReserved = Math.max(0, product.getReservedCount() - quantity);
        int newInventory = Math.max(0, product.getInventoryCount() - quantity);
        product.setReservedCount(newReserved);
        product.setInventoryCount(newInventory);
        product.setUpdatedAt(Instant.now());
        productRepository.save(product);
    }

    public synchronized void restockInventory(String productId, int quantity) {
        Product product = getProductById(productId);
        int newInventory = product.getInventoryCount() + Math.max(0, quantity);
        product.setInventoryCount(newInventory);
        product.setUpdatedAt(Instant.now());
        productRepository.save(product);
    }

    public ProductPublicDto toPublicDto(Product product) {
        NegotiationPolicy policy = product.getNegotiationPolicy();
        return ProductPublicDto.builder()
                .id(product.getId())
                .sellerId(product.getSellerId())
                .title(product.getTitle())
                .brand(product.getBrand())
                .sku(product.getSku())
                .description(product.getDescription())
                .category(product.getCategory())
                .condition(product.getCondition())
                .listPrice(product.getListPrice())
                .currency(product.getCurrency())
                .availableInventory(product.getAvailableInventory())
                .minOrderQuantity(product.getMinOrderQuantity())
                .maxOrderQuantity(product.getMaxOrderQuantity())
                .shippingCost(product.getShippingCost())
                .estimatedDeliveryDays(product.getEstimatedDeliveryDays())
                .warrantyInfo(product.getWarrantyInfo())
                .returnPolicy(product.getReturnPolicy())
                .specifications(product.getSpecifications())
                .autoNegotiate(policy != null && policy.isAutoNegotiate())
                .maxNegotiationRounds(policy != null ? policy.getMaxRounds() : 5)
                .allowWarrantySweeteners(policy != null && policy.isAllowWarrantySweeteners())
                .allowFreeShipping(policy != null && policy.isAllowFreeShipping())
                .images(product.getImages())
                .tags(product.getTags())
                .createdAt(product.getCreatedAt())
                .build();
    }

    private void verifySellerOwnership(String sellerId, Product product) {
        if (sellerId == null || !sellerId.equals(product.getSellerId())) {
            throw new UnauthorizedException("You do not have permission to manage this listing.");
        }
    }

    private void validatePricingPolicy(BigDecimal listPrice, NegotiationPolicy policy) {
        if (policy == null) {
            throw new BusinessRuleException("Negotiation policy must be provided");
        }
        if (policy.getFloorPrice() == null || policy.getFloorPrice().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessRuleException("Floor price must be greater than zero");
        }
        if (policy.getFloorPrice().compareTo(listPrice) > 0) {
            throw new BusinessRuleException("Floor price (" + policy.getFloorPrice() + ") cannot exceed list price (" + listPrice + ")");
        }
        if (policy.getTargetPrice() != null && policy.getTargetPrice().compareTo(policy.getFloorPrice()) < 0) {
            throw new BusinessRuleException("Target price (" + policy.getTargetPrice() + ") cannot be below floor price (" + policy.getFloorPrice() + ")");
        }
        if (policy.getAutoAcceptPriceThreshold() != null && policy.getAutoAcceptPriceThreshold().compareTo(policy.getFloorPrice()) < 0) {
            throw new BusinessRuleException("Auto-accept price threshold cannot be below floor price");
        }
    }
}
