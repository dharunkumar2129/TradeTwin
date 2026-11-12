package com.tradetwin.catalog.service;

import com.tradetwin.catalog.dto.CatalogSearchRequest;
import com.tradetwin.catalog.dto.CatalogSearchResponse;
import com.tradetwin.catalog.dto.ProductPublicDto;
import com.tradetwin.catalog.dto.SearchResultItemDto;
import com.tradetwin.catalog.model.Product;
import com.tradetwin.catalog.repository.ProductRepository;
import com.tradetwin.seller.model.SellerProfile;
import com.tradetwin.seller.repository.SellerProfileRepository;
import com.tradetwin.user.model.User;
import com.tradetwin.user.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
public class CatalogSearchService {

    private static final Logger log = LoggerFactory.getLogger(CatalogSearchService.class);

    private final ProductRepository productRepository;
    private final ProductService productService;
    private final SellerProfileRepository sellerProfileRepository;
    private final UserRepository userRepository;

    public CatalogSearchService(ProductRepository productRepository,
                                ProductService productService,
                                SellerProfileRepository sellerProfileRepository,
                                UserRepository userRepository) {
        this.productRepository = productRepository;
        this.productService = productService;
        this.sellerProfileRepository = sellerProfileRepository;
        this.userRepository = userRepository;
    }

    public CatalogSearchResponse search(CatalogSearchRequest request) {
        List<Product> activeProducts = productRepository.findByActiveTrue();
        int quantity = request.getQuantity() > 0 ? request.getQuantity() : 1;

        List<ScoredItem> scoredCandidates = new ArrayList<>();

        for (Product product : activeProducts) {
            // Inventory check: must have enough stock
            if (product.getAvailableInventory() < quantity) {
                continue;
            }

            ScoredItem item = scoreProduct(product, request, quantity);
            if (item.score > 15.0) { // Reasonable baseline relevance
                scoredCandidates.add(item);
            }
        }

        // Sort descending by match score
        scoredCandidates.sort((a, b) -> Double.compare(b.score, a.score));

        // Best 3 to 6 eligible merchant listings
        int limit = Math.min(6, Math.max(3, scoredCandidates.size()));
        List<SearchResultItemDto> results = scoredCandidates.stream()
                .limit(limit)
                .map(sc -> sc.dto)
                .collect(Collectors.toList());

        String summary = buildSummary(request, results.size());

        return CatalogSearchResponse.builder()
                .results(results)
                .totalMatched(scoredCandidates.size())
                .searchSummary(summary)
                .build();
    }

    private ScoredItem scoreProduct(Product product, CatalogSearchRequest request, int quantity) {
        double score = 40.0; // Base presence score
        List<String> highlights = new ArrayList<>();
        List<String> tradeOffs = new ArrayList<>();

        String title = (product.getTitle() != null ? product.getTitle() : "").toLowerCase();
        String desc = (product.getDescription() != null ? product.getDescription() : "").toLowerCase();
        String brand = (product.getBrand() != null ? product.getBrand() : "").toLowerCase();
        String category = (product.getCategory() != null ? product.getCategory() : "").toLowerCase();
        String tags = (product.getTags() != null ? String.join(" ", product.getTags()) : "").toLowerCase();
        String fullCatalogText = title + " " + desc + " " + brand + " " + category + " " + tags;

        // 1. Product Type & Keyword Match
        String productType = request.getProductType() != null ? request.getProductType().toLowerCase() : "";
        if (StringUtils.hasText(productType)) {
            String[] tokens = productType.split("\\s+");
            int matches = 0;
            for (String token : tokens) {
                if (token.length() > 2 && fullCatalogText.contains(token)) {
                    matches++;
                }
            }
            if (matches > 0) {
                double typeBonus = Math.min(25.0, matches * 8.0);
                score += typeBonus;
                highlights.add("Matched requested product category (" + product.getCategory() + ")");
            }
        }

        // 2. Exact & Preferred Brand Match
        boolean brandSpecified = request.getBrandPreferences() != null && !request.getBrandPreferences().isEmpty();
        if (brandSpecified) {
            boolean exactBrandFound = false;
            for (String prefBrand : request.getBrandPreferences()) {
                if (StringUtils.hasText(prefBrand) && brand.equalsIgnoreCase(prefBrand.trim())) {
                    score += 25.0;
                    exactBrandFound = true;
                    highlights.add("Exact brand match: " + product.getBrand());
                    break;
                }
            }
            if (!exactBrandFound) {
                // If user insisted on a brand and this product is a different brand
                score -= 15.0;
                tradeOffs.add("Alternative brand (" + product.getBrand() + ") to preferred " + String.join(", ", request.getBrandPreferences()));
            }
        }

        // 3. Price & Shipping Evaluation
        BigDecimal unitPrice = product.getListPrice();
        BigDecimal shippingCost = product.getShippingCost() != null ? product.getShippingCost() : BigDecimal.ZERO;
        BigDecimal estimatedTotal = unitPrice.multiply(BigDecimal.valueOf(quantity)).add(shippingCost);

        if (request.getMaxBudget() != null && request.getMaxBudget().compareTo(BigDecimal.ZERO) > 0) {
            if (estimatedTotal.compareTo(request.getMaxBudget()) <= 0) {
                score += 18.0;
                BigDecimal savings = request.getMaxBudget().subtract(estimatedTotal);
                if (savings.compareTo(BigDecimal.ZERO) > 0) {
                    highlights.add(String.format("Under budget by $%s (Total: $%s with shipping)", savings.setScale(2, RoundingMode.HALF_UP), estimatedTotal));
                } else {
                    highlights.add(String.format("Fits within $%s max budget exactly", request.getMaxBudget()));
                }
            } else {
                BigDecimal over = estimatedTotal.subtract(request.getMaxBudget());
                if (product.getNegotiationPolicy() != null && product.getNegotiationPolicy().isAutoNegotiate()) {
                    score += 4.0;
                    highlights.add("Asking price exceeds budget by $" + over.setScale(2, RoundingMode.HALF_UP) + ", but listing accepts autonomous negotiation");
                } else {
                    score -= 20.0;
                    tradeOffs.add("Total price ($" + estimatedTotal + ") exceeds budget limit");
                }
            }
        }

        // 4. Delivery Days Check
        int deliveryDays = product.getEstimatedDeliveryDays() > 0 ? product.getEstimatedDeliveryDays() : 3;
        if (request.getMaxDeliveryDays() != null && request.getMaxDeliveryDays() > 0) {
            if (deliveryDays <= request.getMaxDeliveryDays()) {
                score += 10.0;
                highlights.add(String.format("Fast delivery: %d days (within %d-day window)", deliveryDays, request.getMaxDeliveryDays()));
            } else {
                score -= 8.0;
                tradeOffs.add(String.format("Estimated delivery (%d days) slightly exceeds %d-day preference", deliveryDays, request.getMaxDeliveryDays()));
            }
        }

        // 5. Warranty Check
        int warrantyMonths = parseWarrantyMonths(product.getWarrantyInfo());
        if (request.getMinWarrantyMonths() != null && request.getMinWarrantyMonths() > 0) {
            if (warrantyMonths >= request.getMinWarrantyMonths()) {
                score += 10.0;
                highlights.add(String.format("Includes %d-month warranty coverage", warrantyMonths));
            } else {
                score -= 6.0;
                tradeOffs.add(String.format("Warranty (%d months) is below requested %d months", warrantyMonths, request.getMinWarrantyMonths()));
            }
        }

        // 6. Condition Check
        if (request.getPreferredConditions() != null && !request.getPreferredConditions().isEmpty()) {
            boolean condMatch = request.getPreferredConditions().stream()
                    .anyMatch(c -> c.equalsIgnoreCase(product.getCondition()));
            if (condMatch) {
                score += 6.0;
                highlights.add("Condition: " + product.getCondition());
            }
        }

        // 7. Specifications Match (e.g. ANC, Wireless, OLED)
        if (request.getRequiredSpecifications() != null && !request.getRequiredSpecifications().isEmpty()) {
            for (Map.Entry<String, String> specReq : request.getRequiredSpecifications().entrySet()) {
                String val = specReq.getValue().toLowerCase();
                if (fullCatalogText.contains(val)) {
                    score += 5.0;
                    highlights.add("Verified spec: " + specReq.getValue());
                }
            }
        }

        // Normalize score between 10.0 and 99.0
        double finalScore = Math.round(Math.max(10.0, Math.min(99.0, score)) * 10.0) / 10.0;

        // Seller details
        String sellerName = "Verified Merchant";
        double sellerRep = 98.0;
        Optional<SellerProfile> profileOpt = sellerProfileRepository.findByUserId(product.getSellerId());
        if (profileOpt.isPresent() && StringUtils.hasText(profileOpt.get().getStoreName())) {
            sellerName = profileOpt.get().getStoreName();
        } else {
            Optional<User> userOpt = userRepository.findById(product.getSellerId());
            if (userOpt.isPresent()) {
                sellerName = userOpt.get().getFullName();
                sellerRep = userOpt.get().getReputationScore();
            }
        }

        ProductPublicDto publicDto = productService.toPublicDto(product);

        SearchResultItemDto dto = SearchResultItemDto.builder()
                .product(publicDto)
                .sellerStoreName(sellerName)
                .sellerReputation(sellerRep)
                .unitPrice(unitPrice)
                .shippingCost(shippingCost)
                .estimatedTotalPrice(estimatedTotal)
                .estimatedDeliveryDays(deliveryDays)
                .warrantyInfo(product.getWarrantyInfo() != null ? product.getWarrantyInfo() : warrantyMonths + " Months Warranty")
                .warrantyMonths(warrantyMonths)
                .condition(product.getCondition())
                .availableInventory(product.getAvailableInventory())
                .matchScore(finalScore)
                .matchHighlights(highlights)
                .tradeOffs(tradeOffs)
                .eligibleForNegotiation(product.getNegotiationPolicy() != null && product.getNegotiationPolicy().isAutoNegotiate())
                .build();

        return new ScoredItem(finalScore, dto);
    }

    private int parseWarrantyMonths(String warrantyText) {
        if (!StringUtils.hasText(warrantyText)) {
            return 12; // default standard
        }
        Pattern pattern = Pattern.compile("([0-9]+)\\s*(?:month|mo|year|yr)", Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(warrantyText);
        if (matcher.find()) {
            int num = Integer.parseInt(matcher.group(1));
            if (warrantyText.toLowerCase().contains("year") || warrantyText.toLowerCase().contains("yr")) {
                return num * 12;
            }
            return num;
        }
        return 12;
    }

    private String buildSummary(CatalogSearchRequest req, int count) {
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("Found %d verified merchant listing%s", count, count == 1 ? "" : "s"));
        if (StringUtils.hasText(req.getProductType())) {
            sb.append(" matching \"").append(req.getProductType()).append("\"");
        }
        if (req.getBrandPreferences() != null && !req.getBrandPreferences().isEmpty()) {
            sb.append(" for brand ").append(String.join("/", req.getBrandPreferences()));
        }
        if (req.getMaxBudget() != null && req.getMaxBudget().compareTo(BigDecimal.ZERO) > 0) {
            sb.append(" under $").append(req.getMaxBudget());
        }
        sb.append(" available for immediate autonomous negotiation.");
        return sb.toString();
    }

    private static class ScoredItem {
        final double score;
        final SearchResultItemDto dto;

        ScoredItem(double score, SearchResultItemDto dto) {
            this.score = score;
            this.dto = dto;
        }
    }
}
