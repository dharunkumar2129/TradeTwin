package com.tradetwin.common.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.util.StringUtils;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Shared Gemini REST client helper with strict connect/read timeouts,
 * safe model fallback (e.g. when a preview/quota-limited model stalls or returns 429),
 * and temporary model cooldown caching so requests never block for >15 seconds.
 */
public final class GeminiClientHelper {

    private static final Logger log = LoggerFactory.getLogger(GeminiClientHelper.class);

    public static final String DEFAULT_MODEL = "gemini-3.5-flash-lite";
    private static final List<String> FALLBACK_MODELS = List.of("gemini-3.5-flash-lite", "gemini-3.5-flash");
    private static final Duration CONNECT_TIMEOUT = Duration.ofMillis(2500);
    private static final Duration READ_TIMEOUT = Duration.ofMillis(5000);
    private static final Duration MODEL_COOLDOWN = Duration.ofMinutes(10);

    private static final ConcurrentHashMap<String, Instant> MODEL_COOLDOWN_UNTIL = new ConcurrentHashMap<>();

    private GeminiClientHelper() {
    }

    public static RestTemplate createTimeoutRestTemplate() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(CONNECT_TIMEOUT);
        factory.setReadTimeout(READ_TIMEOUT);
        return new RestTemplate(factory);
    }

    public static JsonNode generateJson(RestTemplate restTemplate,
                                        ObjectMapper objectMapper,
                                        String apiKey,
                                        String configuredModel,
                                        String prompt) throws Exception {
        if (!StringUtils.hasText(apiKey) || apiKey.startsWith("your_")) {
            throw new IllegalStateException("Gemini API key is not configured");
        }

        List<String> candidates = resolveHealthyCandidates(configuredModel);
        if (candidates.isEmpty()) {
            throw new IllegalStateException("All configured Gemini models are in cooldown; using deterministic fallback");
        }

        Map<String, Object> body = Map.of(
                "contents", List.of(
                        Map.of("role", "user", "parts", List.of(Map.of("text", prompt)))
                ),
                "generationConfig", Map.of(
                        "response_mime_type", "application/json",
                        "temperature", 0.35
                )
        );

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);

        Exception lastError = null;
        for (String model : candidates) {
            String url = String.format(
                    "https://generativelanguage.googleapis.com/v1beta/models/%s:generateContent?key=%s",
                    model, apiKey.trim()
            );
            try {
                ResponseEntity<String> response = restTemplate.postForEntity(url, request, String.class);
                JsonNode root = objectMapper.readTree(response.getBody());
                JsonNode parts = root.path("candidates").path(0).path("content").path("parts");
                if (!parts.isArray() || parts.isEmpty()) {
                    throw new IllegalStateException("Gemini returned empty candidates payload for model " + model);
                }
                String rawText = parts.get(0).path("text").asText("");
                String cleanedJson = stripMarkdownJsonFence(rawText);
                return objectMapper.readTree(cleanedJson);
            } catch (HttpStatusCodeException httpEx) {
                int code = httpEx.getStatusCode().value();
                if (code == 429 || code == 404 || code == 403 || code == 400) {
                    markCooldown(model, "HTTP " + code);
                }
                lastError = httpEx;
                log.warn("Gemini call failed on model [{}] with HTTP {}", model, code);
            } catch (ResourceAccessException timeoutEx) {
                markCooldown(model, "timeout/connectivity error");
                lastError = timeoutEx;
                log.warn("Gemini call timed out on model [{}]: {}", model, timeoutEx.getMessage());
            } catch (Exception ex) {
                lastError = ex;
                log.warn("Gemini parse/call error on model [{}]: {}", model, ex.getMessage());
            }
        }

        throw lastError != null ? lastError : new IllegalStateException("Gemini request failed across all models");
    }

    static List<String> resolveHealthyCandidates(String configuredModel) {
        Set<String> ordered = new LinkedHashSet<>();
        if (StringUtils.hasText(configuredModel)) {
            ordered.add(configuredModel.trim());
        } else {
            ordered.add(DEFAULT_MODEL);
        }
        ordered.addAll(FALLBACK_MODELS);

        Instant now = Instant.now();
        List<String> healthy = new ArrayList<>();
        for (String model : ordered) {
            Instant until = MODEL_COOLDOWN_UNTIL.get(model);
            if (until == null || now.isAfter(until)) {
                if (until != null) {
                    MODEL_COOLDOWN_UNTIL.remove(model, until);
                }
                healthy.add(model);
            }
        }
        return healthy;
    }

    static void clearCooldowns() {
        MODEL_COOLDOWN_UNTIL.clear();
    }

    private static void markCooldown(String model, String reason) {
        MODEL_COOLDOWN_UNTIL.put(model, Instant.now().plus(MODEL_COOLDOWN));
        log.warn("Placing Gemini model [{}] in {}-minute cooldown ({})", model, MODEL_COOLDOWN.toMinutes(), reason);
    }

    static String stripMarkdownJsonFence(String text) {
        if (!StringUtils.hasText(text)) {
            return "{}";
        }
        String trimmed = text.trim();
        if (trimmed.startsWith("```")) {
            int firstNewline = trimmed.indexOf('\n');
            if (firstNewline > 0) {
                trimmed = trimmed.substring(firstNewline + 1);
            }
            if (trimmed.endsWith("```")) {
                trimmed = trimmed.substring(0, trimmed.length() - 3);
            }
        }
        return trimmed.trim();
    }
}
