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

public class OpenAiProvider implements ModelProvider {

    private static final Logger log = LoggerFactory.getLogger(OpenAiProvider.class);
    private static final String API_URL = "https://api.openai.com/v1/chat/completions";
    private static final String DEFAULT_MODEL = "gpt-4o";

    private final String apiKey;
    private final String model;
    private final HttpClient http;
    private final ObjectMapper mapper;

    public OpenAiProvider(String apiKey) {
        this(apiKey, DEFAULT_MODEL);
    }

    public OpenAiProvider(String apiKey, String model) {
        this.apiKey = apiKey;
        this.model = model;
        this.http = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();
        this.mapper = new ObjectMapper();
    }

    @Override
    public String getName() { return "openai/" + model; }

    @Override
    public boolean isAvailable() {
        return apiKey != null && !apiKey.isBlank();
    }

    @Override
    public String complete(String systemPrompt, String userPrompt) {
        try {
            var body = Map.of(
                "model", model,
                "max_tokens", 8192,
                "messages", List.of(
                    Map.of("role", "system", "content", systemPrompt),
                    Map.of("role", "user", "content", userPrompt)
                )
            );

            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(API_URL))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + apiKey)
                .timeout(Duration.ofSeconds(120))
                .POST(HttpRequest.BodyPublishers.ofString(mapper.writeValueAsString(body)))
                .build();

            HttpResponse<String> response = http.send(request,
                HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                log.error("OpenAI API error {}: {}", response.statusCode(), response.body());
                throw new RuntimeException("OpenAI API returned %d: %s"
                    .formatted(response.statusCode(), response.body()));
            }

            var responseMap = mapper.readValue(response.body(), Map.class);
            var choices = (List<?>) responseMap.get("choices");
            var firstChoice = (Map<?, ?>) choices.get(0);
            var message = (Map<?, ?>) firstChoice.get("message");
            return (String) message.get("content");

        } catch (Exception e) {
            log.error("Error calling OpenAI API: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to call OpenAI API: " + e.getMessage(), e);
        }
    }
}
