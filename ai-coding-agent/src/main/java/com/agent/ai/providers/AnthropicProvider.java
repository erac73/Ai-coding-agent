package com.agent.ai.providers;

import com.agent.ai.ModelProvider;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.Map;

/**
 * Proveedor Anthropic (Claude).
 *
 * Usa java.net.http nativo — sin dependencias adicionales más allá del SDK.
 */
public class AnthropicProvider implements ModelProvider {

    private static final Logger log = LoggerFactory.getLogger(AnthropicProvider.class);
    private static final String API_URL = "https://api.anthropic.com/v1/messages";
    private static final String DEFAULT_MODEL = "claude-sonnet-4-20250514";

    private final String apiKey;
    private final String model;
    private final HttpClient http;
    private final ObjectMapper mapper;

    public AnthropicProvider(String apiKey) {
        this(apiKey, DEFAULT_MODEL);
    }

    public AnthropicProvider(String apiKey, String model) {
        this.apiKey = apiKey;
        this.model = model;
        this.http = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();
        this.mapper = new ObjectMapper();
    }

    @Override
    public String getName() { return "anthropic/" + model; }

    @Override
    public boolean isAvailable() {
        return apiKey != null && !apiKey.isBlank();
    }

    @Override
    public String complete(String systemPrompt, String userPrompt) {
        try {
            var body = Map.of(
                "model", model,
                "max_tokens", 8096,
                "system", systemPrompt,
                "messages", List.of(
                    Map.of("role", "user", "content", userPrompt)
                )
            );

            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(API_URL))
                .header("Content-Type", "application/json")
                .header("x-api-key", apiKey)
                .header("anthropic-version", "2023-06-01")
                .timeout(Duration.ofSeconds(120))
                .POST(HttpRequest.BodyPublishers.ofString(mapper.writeValueAsString(body)))
                .build();

            HttpResponse<String> response = http.send(request,
                HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                log.error("Anthropic API error {}: {}", response.statusCode(), response.body());
                throw new RuntimeException("Anthropic API returned " + response.statusCode());
            }

            var responseMap = mapper.readValue(response.body(), Map.class);
            var content = (List<?>) responseMap.get("content");
            var firstBlock = (Map<?, ?>) content.get(0);
            return (String) firstBlock.get("text");

        } catch (Exception e) {
            log.error("Error calling Anthropic API: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to call Anthropic API: " + e.getMessage(), e);
        }
    }
}
