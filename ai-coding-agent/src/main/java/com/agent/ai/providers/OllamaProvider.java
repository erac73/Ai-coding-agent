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

public class OllamaProvider implements ModelProvider {

    private static final Logger log = LoggerFactory.getLogger(OllamaProvider.class);
    private static final String DEFAULT_MODEL = "codellama";

    private final String baseUrl;
    private final String model;
    private final HttpClient http;
    private final ObjectMapper mapper;

    public OllamaProvider(String baseUrl) {
        this(baseUrl, DEFAULT_MODEL);
    }

    public OllamaProvider(String baseUrl, String model) {
        this.baseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        this.model = model;
        this.http = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();
        this.mapper = new ObjectMapper();
    }

    @Override
    public String getName() { return "ollama/" + model; }

    @Override
    public boolean isAvailable() {
        return baseUrl != null && !baseUrl.isBlank();
    }

    @Override
    public String complete(String systemPrompt, String userPrompt) {
        try {
            var body = Map.of(
                "model", model,
                "stream", false,
                "messages", List.of(
                    Map.of("role", "system", "content", systemPrompt),
                    Map.of("role", "user", "content", userPrompt)
                )
            );

            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/api/chat"))
                .header("Content-Type", "application/json")
                .timeout(Duration.ofSeconds(120))
                .POST(HttpRequest.BodyPublishers.ofString(mapper.writeValueAsString(body)))
                .build();

            HttpResponse<String> response = http.send(request,
                HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                log.error("Ollama API error {}: {}", response.statusCode(), response.body());
                throw new RuntimeException("Ollama API returned %d: %s"
                    .formatted(response.statusCode(), response.body()));
            }

            var responseMap = mapper.readValue(response.body(), Map.class);
            var message = (Map<?, ?>) responseMap.get("message");
            return (String) message.get("content");

        } catch (Exception e) {
            log.error("Error calling Ollama API: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to call Ollama: " + e.getMessage(), e);
        }
    }
}
