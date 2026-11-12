package com.tradetwin.common.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class GeminiClientHelperTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @AfterEach
    void resetCooldowns() {
        GeminiClientHelper.clearCooldowns();
    }

    @Test
    @DisplayName("Falls back from quota-exhausted primary model to healthy flash-lite model and cools down failed model")
    void generateJson_fallsBackOn429AndCachesCooldown() throws Exception {
        RestTemplate restTemplate = GeminiClientHelper.createTimeoutRestTemplate();
        MockRestServiceServer server = MockRestServiceServer.createServer(restTemplate);

        server.expect(requestTo("https://generativelanguage.googleapis.com/v1beta/models/gemini-3.8-flash:generateContent?key=test-key"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withStatus(HttpStatus.TOO_MANY_REQUESTS).body("{\"error\":{\"code\":429}}"));

        server.expect(requestTo("https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash-lite:generateContent?key=test-key"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess("""
                        {
                          "candidates": [
                            {
                              "content": {
                                "parts": [
                                  { "text": "```json\\n{\\"productType\\":\\"Wireless Headphones\\"}\\n```" }
                                ]
                              }
                            }
                          ]
                        }
                        """, MediaType.APPLICATION_JSON));

        JsonNode result = GeminiClientHelper.generateJson(
                restTemplate, objectMapper, "test-key", "gemini-3.8-flash", "Parse headphones"
        );
        assertEquals("Wireless Headphones", result.path("productType").asText());

        // Second call must skip cooled-down gemini-3.8-flash and hit gemini-3.5-flash-lite directly
        assertFalse(GeminiClientHelper.resolveHealthyCandidates("gemini-3.8-flash").contains("gemini-3.8-flash"));

        server.verify();
        server.reset();

        server.expect(requestTo("https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash-lite:generateContent?key=test-key"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess("""
                        {"candidates":[{"content":{"parts":[{"text":"{\\"ok\\":true}"}]}}]}
                        """, MediaType.APPLICATION_JSON));

        JsonNode second = GeminiClientHelper.generateJson(
                restTemplate, objectMapper, "test-key", "gemini-3.8-flash", "Second call"
        );
        assertTrue(second.path("ok").asBoolean());
        server.verify();
    }
}
