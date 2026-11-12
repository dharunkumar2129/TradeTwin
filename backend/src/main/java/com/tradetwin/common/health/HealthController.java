package com.tradetwin.common.health;

import com.tradetwin.common.api.ApiResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/health")
public class HealthController {

    @Value("${spring.application.name:tradetwin-backend}")
    private String applicationName;

    @Value("${tradetwin.ai.gemini.model:gemini-3.8-flash}")
    private String geminiModel;

    @GetMapping
    public ResponseEntity<ApiResponse<Map<String, Object>>> getHealth() {
        Map<String, Object> healthInfo = Map.of(
                "status", "UP",
                "application", applicationName,
                "timestamp", Instant.now().toString(),
                "geminiModel", geminiModel,
                "version", "1.0.0-SNAPSHOT",
                "system", "TradeTwin Agentic Commerce Engine"
        );
        return ResponseEntity.ok(ApiResponse.ok("System healthy", healthInfo));
    }
}
