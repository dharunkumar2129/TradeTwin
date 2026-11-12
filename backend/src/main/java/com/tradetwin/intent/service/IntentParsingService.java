package com.tradetwin.intent.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tradetwin.catalog.dto.CatalogSearchRequest;
import com.tradetwin.catalog.dto.CatalogSearchResponse;
import com.tradetwin.catalog.dto.SearchResultItemDto;
import com.tradetwin.catalog.service.CatalogSearchService;
import com.tradetwin.common.ai.GeminiClientHelper;
import com.tradetwin.common.exception.ResourceNotFoundException;
import com.tradetwin.intent.model.BuyerIntent;
import com.tradetwin.intent.repository.BuyerIntentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class IntentParsingService {

    private static final Logger log = LoggerFactory.getLogger(IntentParsingService.class);

    private final CatalogSearchService catalogSearchService;
    private final BuyerIntentRepository buyerIntentRepository;
    private final ObjectMapper objectMapper;
    private final RestTemplate restTemplate;

    @Value("${tradetwin.ai.gemini.api-key:}")
    private String geminiApiKey;

    @Value("${tradetwin.ai.gemini.model:gemini-3.5-flash-lite}")
    private String geminiModel;

    public IntentParsingService(CatalogSearchService catalogSearchService,
                                BuyerIntentRepository buyerIntentRepository,
                                ObjectMapper objectMapper) {
        this.catalogSearchService = catalogSearchService;
        this.buyerIntentRepository = buyerIntentRepository;
        this.objectMapper = objectMapper;
        this.restTemplate = GeminiClientHelper.createTimeoutRestTemplate();
    }

    public Map<String, Object> parseAndMatch(String rawQuery, String buyerId) {
        BuyerIntent intent;
        if (StringUtils.hasText(geminiApiKey) && !geminiApiKey.startsWith("your_")) {
            try {
                intent = parseWithGemini(rawQuery);
            } catch (Exception ex) {
                log.warn("Gemini intent extraction failed, falling back to intelligent heuristic parser: {}", ex.getMessage());
                intent = parseHeuristic(rawQuery);
            }
        } else {
            intent = parseHeuristic(rawQuery);
        }

        if (StringUtils.hasText(buyerId)) {
            intent.setBuyerId(buyerId);
        }

        // Persist draft intent in MongoDB
        BuyerIntent savedDraft = buyerIntentRepository.save(intent);

        // Perform deterministic catalogue retrieval using MongoDB
        CatalogSearchRequest searchReq = buildSearchRequest(savedDraft);
        CatalogSearchResponse searchResponse = catalogSearchService.search(searchReq);

        Map<String, Object> result = new HashMap<>();
        result.put("intent", savedDraft);
        result.put("candidates", searchResponse.getResults());
        result.put("totalMatched", searchResponse.getTotalMatched());
        result.put("searchSummary", searchResponse.getSearchSummary());
        result.put("followUpQuestion", savedDraft.getFollowUpQuestion());
        return result;
    }

    public BuyerIntent confirmIntent(BuyerIntent intent, String buyerId) {
        if (StringUtils.hasText(buyerId)) {
            intent.setBuyerId(buyerId);
        }
        intent.setStatus("CONFIRMED");
        intent.setUpdatedAt(Instant.now());
        if (intent.getCreatedAt() == null) {
            intent.setCreatedAt(Instant.now());
        }
        return buyerIntentRepository.save(intent);
    }

    public List<BuyerIntent> getIntentsForBuyer(String buyerId) {
        return buyerIntentRepository.findByBuyerIdOrderByCreatedAtDesc(buyerId);
    }

    public BuyerIntent getIntentById(String id) {
        return buyerIntentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("BuyerIntent", "id", id));
    }

    private CatalogSearchRequest buildSearchRequest(BuyerIntent intent) {
        return CatalogSearchRequest.builder()
                .query(intent.getRawQuery())
                .productType(intent.getProductType())
                .brandPreferences(intent.getBrandPreferences())
                .requiredSpecifications(intent.getRequiredSpecifications())
                .optionalPreferences(intent.getOptionalPreferences())
                .maxBudget(intent.getMaxBudget())
                .quantity(intent.getQuantity())
                .minWarrantyMonths(intent.getMinWarrantyMonths())
                .maxDeliveryDays(intent.getMaxDeliveryDays())
                .preferredConditions(intent.getPreferredConditions())
                .currency(intent.getCurrency())
                .build();
    }

    private BuyerIntent parseWithGemini(String rawQuery) throws Exception {
        String systemPrompt = """
                You are TradeTwin's e-commerce intent parser. Analyze the buyer's natural language request and extract structured output.
                Return ONLY valid JSON matching this schema:
                {
                  "productType": "string (e.g. wireless noise-cancelling headphones, developer laptop)",
                  "brandPreferences": ["string (e.g. Sony, Apple)"],
                  "requiredSpecifications": {"key": "value (e.g. anc: Active Noise Cancelling, ram: 32GB)"},
                  "optionalPreferences": ["string (e.g. foldable, black color)"],
                  "maxBudget": number or null (the private ceiling budget in dollars),
                  "targetBudget": number or null (estimated target spending, e.g. 85-90% of max budget),
                  "quantity": number (default 1),
                  "minWarrantyMonths": number or null (e.g. 12 for 1 year),
                  "maxDeliveryDays": number or null (e.g. 4 for delivery within four days),
                  "preferredConditions": ["string (e.g. Brand New, Open Box)"],
                  "shippingLocation": "string or null",
                  "currency": "USD",
                  "isAmbiguous": boolean (true if key details like product type or budget are missing or contradictory),
                  "followUpQuestion": "string or null (a friendly clarification question if isAmbiguous is true)"
                }
                """;

        JsonNode json = GeminiClientHelper.generateJson(
                restTemplate, objectMapper, geminiApiKey, geminiModel, systemPrompt + "\nBuyer Query: " + rawQuery
        );

        String productType = json.path("productType").asText("");
        List<String> brands = new ArrayList<>();
        if (json.has("brandPreferences") && json.get("brandPreferences").isArray()) {
            json.get("brandPreferences").forEach(b -> brands.add(b.asText()));
        }

        Map<String, String> requiredSpecs = new HashMap<>();
        if (json.has("requiredSpecifications") && json.get("requiredSpecifications").isObject()) {
            json.get("requiredSpecifications").fields().forEachRemaining(entry ->
                    requiredSpecs.put(entry.getKey(), entry.getValue().asText()));
        }

        List<String> optionalPrefs = new ArrayList<>();
        if (json.has("optionalPreferences") && json.get("optionalPreferences").isArray()) {
            json.get("optionalPreferences").forEach(p -> optionalPrefs.add(p.asText()));
        }

        BigDecimal maxBudget = json.hasNonNull("maxBudget") ? new BigDecimal(json.get("maxBudget").asText()) : null;
        BigDecimal targetBudget = json.hasNonNull("targetBudget") ? new BigDecimal(json.get("targetBudget").asText()) : null;
        if (targetBudget == null && maxBudget != null) {
            targetBudget = maxBudget.multiply(new BigDecimal("0.88")).setScale(2, RoundingMode.HALF_UP);
        }

        int quantity = json.hasNonNull("quantity") ? json.get("quantity").asInt(1) : 1;
        Integer minWarranty = json.hasNonNull("minWarrantyMonths") ? json.get("minWarrantyMonths").asInt() : null;
        Integer maxDelivery = json.hasNonNull("maxDeliveryDays") ? json.get("maxDeliveryDays").asInt() : null;

        List<String> conditions = new ArrayList<>();
        if (json.has("preferredConditions") && json.get("preferredConditions").isArray()) {
            json.get("preferredConditions").forEach(c -> conditions.add(c.asText()));
        }
        if (conditions.isEmpty()) {
            conditions.add("Brand New");
        }

        String shippingLocation = json.path("shippingLocation").asText(null);
        String currency = json.path("currency").asText("USD");
        boolean isAmbiguous = json.path("isAmbiguous").asBoolean(false);
        String followUp = json.path("followUpQuestion").asText(null);

        List<String> keywords = new ArrayList<>(brands);
        if (StringUtils.hasText(productType)) {
            for (String token : productType.toLowerCase().split("\\s+")) {
                if (token.length() >= 4 && !keywords.contains(token)) {
                    keywords.add(token);
                }
            }
        }

        return BuyerIntent.builder()
                .rawQuery(rawQuery)
                .productType(productType)
                .brandPreferences(brands)
                .requiredSpecifications(requiredSpecs)
                .optionalPreferences(optionalPrefs)
                .maxBudget(maxBudget)
                .targetBudget(targetBudget)
                .quantity(quantity)
                .minWarrantyMonths(minWarranty)
                .maxDeliveryDays(maxDelivery)
                .preferredConditions(conditions)
                .shippingLocation(shippingLocation)
                .currency(currency)
                .ambiguous(isAmbiguous)
                .followUpQuestion(followUp)
                .status("DRAFT")
                .category("Electronics")
                .keywords(keywords)
                .build();
    }

    public BuyerIntent parseHeuristic(String rawQuery) {
        String lower = rawQuery.toLowerCase();

        // 1. Extract Budget
        BigDecimal maxBudget = extractBudget(lower);
        BigDecimal targetBudget = maxBudget != null ? maxBudget.multiply(new BigDecimal("0.88")).setScale(2, RoundingMode.HALF_UP) : null;

        // 2. Extract Brand Preferences
        List<String> brands = new ArrayList<>();
        if (lower.contains("sony")) brands.add("Sony");
        if (lower.contains("bose")) brands.add("Bose");
        if (lower.contains("apple")) brands.add("Apple");
        if (lower.contains("quantumblade")) brands.add("QuantumBlade");
        if (lower.contains("sonicpulse")) brands.add("SonicPulse");
        if (lower.contains("apexfit")) brands.add("ApexFit");
        if (lower.contains("keyzen")) brands.add("KeyZen");
        if (lower.contains("ultraview")) brands.add("UltraView");

        // 3. Extract Product Type & Keywords
        String productType = "Consumer Electronics";
        List<String> keywords = new ArrayList<>();
        Map<String, String> requiredSpecs = new HashMap<>();

        if (lower.contains("headphone") || lower.contains("earbuds") || lower.contains("audio") || lower.contains("earphones")) {
            productType = "Wireless Noise-Cancelling Headphones";
            keywords.add("headphones");
            keywords.add("audio");
        } else if (lower.contains("laptop") || lower.contains("workstation") || lower.contains("computer") || lower.contains("macbook")) {
            productType = "Developer Workstation Laptop";
            keywords.add("laptop");
            keywords.add("workstation");
        } else if (lower.contains("watch") || lower.contains("smartwatch") || lower.contains("tracker")) {
            productType = "Titanium GPS Smartwatch";
            keywords.add("smartwatch");
            keywords.add("gps");
        } else if (lower.contains("keyboard") || lower.contains("mechanical")) {
            productType = "Ergonomic Mechanical Keyboard";
            keywords.add("keyboard");
        } else if (lower.contains("monitor") || lower.contains("oled") || lower.contains("display") || lower.contains("screen")) {
            productType = "Curved Ultrawide OLED Monitor";
            keywords.add("monitor");
        }

        // 4. Specifications Extraction
        if (lower.contains("noise-cancelling") || lower.contains("noise cancelling") || lower.contains("anc")) {
            requiredSpecs.put("anc", "Active Noise Cancellation");
        }
        if (lower.contains("wireless") || lower.contains("bluetooth")) {
            requiredSpecs.put("connectivity", "Wireless Bluetooth");
        }
        if (lower.contains("32gb") || lower.contains("ram")) {
            requiredSpecs.put("ram", "32GB RAM");
        }
        if (lower.contains("oled") || lower.contains("4k")) {
            requiredSpecs.put("display", "High Resolution OLED");
        }

        // 5. Warranty extraction
        Integer minWarranty = extractWarrantyMonths(lower);

        // 6. Delivery time extraction
        Integer maxDelivery = extractDeliveryDays(lower);

        // 7. Preferred Conditions
        List<String> conditions = new ArrayList<>();
        if (lower.contains("refurbished")) {
            conditions.add("Refurbished");
        } else if (lower.contains("open box")) {
            conditions.add("Open Box");
        } else {
            conditions.add("Brand New");
        }

        // 8. Quantity
        int quantity = extractQuantity(lower);

        // 9. Ambiguity detection
        boolean isAmbiguous = false;
        String followUp = null;

        if (lower.trim().length() < 8 || (!lower.contains("headphone") && !lower.contains("laptop") && !lower.contains("watch") && !lower.contains("keyboard") && !lower.contains("monitor") && maxBudget == null)) {
            isAmbiguous = true;
            followUp = "Could you clarify what kind of product you are looking for (e.g. headphones, laptop) and your approximate budget?";
        } else if (maxBudget == null) {
            followUp = "What is your preferred maximum total budget for this request?";
        }

        return BuyerIntent.builder()
                .rawQuery(rawQuery)
                .productType(productType)
                .brandPreferences(brands)
                .requiredSpecifications(requiredSpecs)
                .optionalPreferences(new ArrayList<>())
                .maxBudget(maxBudget)
                .targetBudget(targetBudget)
                .quantity(quantity)
                .minWarrantyMonths(minWarranty != null ? minWarranty : 12)
                .maxDeliveryDays(maxDelivery != null ? maxDelivery : 4)
                .preferredConditions(conditions)
                .shippingLocation("United States")
                .currency("USD")
                .ambiguous(isAmbiguous)
                .followUpQuestion(followUp)
                .status("DRAFT")
                .category("Electronics")
                .keywords(keywords)
                .build();
    }

    private BigDecimal extractBudget(String text) {
        Pattern pattern = Pattern.compile("(?:under|below|budget|max|up to|less than|\\$)\\s*\\$?([0-9]+(?:\\.[0-9]{2})?)");
        Matcher matcher = pattern.matcher(text);
        if (matcher.find()) {
            try {
                return new BigDecimal(matcher.group(1));
            } catch (Exception ignored) {
            }
        }
        return null;
    }

    private Integer extractWarrantyMonths(String text) {
        if (text.contains("one year") || text.contains("1 year") || text.contains("12 month")) {
            return 12;
        }
        if (text.contains("two year") || text.contains("2 year") || text.contains("24 month")) {
            return 24;
        }
        Pattern pattern = Pattern.compile("([0-9]+)\\s*(?:month|year|yr)", Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(text);
        if (matcher.find()) {
            int num = Integer.parseInt(matcher.group(1));
            return text.contains("year") || text.contains("yr") ? num * 12 : num;
        }
        return 12; // default
    }

    private Integer extractDeliveryDays(String text) {
        if (text.contains("four days") || text.contains("4 days")) {
            return 4;
        }
        if (text.contains("two days") || text.contains("2 days")) {
            return 2;
        }
        if (text.contains("three days") || text.contains("3 days")) {
            return 3;
        }
        if (text.contains("one day") || text.contains("1 day") || text.contains("next day")) {
            return 1;
        }
        Pattern pattern = Pattern.compile("within\\s*([0-9]+)\\s*day", Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(text);
        if (matcher.find()) {
            return Integer.parseInt(matcher.group(1));
        }
        return 4; // default
    }

    private int extractQuantity(String text) {
        Pattern pattern = Pattern.compile("(?:qty|quantity|count|amount|buy)\\s*([0-9]+)", Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(text);
        if (matcher.find()) {
            try {
                return Integer.parseInt(matcher.group(1));
            } catch (Exception ignored) {}
        }
        return 1;
    }
}
