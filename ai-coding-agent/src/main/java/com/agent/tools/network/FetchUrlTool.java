package com.agent.tools.network;

import com.agent.model.AgentContext;
import com.agent.model.ToolResult;
import com.agent.tools.AgentTool;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;

public class FetchUrlTool implements AgentTool {

    private static final int MAX_RESPONSE_CHARS = 20_000;
    private static final Duration TIMEOUT = Duration.ofSeconds(30);

    private final HttpClient http;

    public FetchUrlTool() {
        this.http = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();
    }

    @Override
    public String getName() { return "fetch_url"; }

    @Override
    public String getDescription() {
        return "Fetches content from a URL. Returns the response body as text. " +
               "Use to read documentation, API responses, or web pages. " +
               "Truncates responses larger than 20,000 characters. " +
               "Timeouts after 30 seconds.";
    }

    @Override
    public Map<String, String> getParameterDescriptions() {
        return Map.of(
            "url", "The URL to fetch (must include http:// or https://)",
            "format", "(Optional) 'text' (default) or 'markdown'"
        );
    }

    @Override
    public ToolResult execute(Map<String, String> params, AgentContext context) {
        String url = params.get("url");
        if (url == null || url.isBlank()) {
            return ToolResult.fail(getName(), "Parameter 'url' is required");
        }

        if (!url.startsWith("http://") && !url.startsWith("https://")) {
            return ToolResult.fail(getName(), "URL must start with http:// or https://");
        }

        try {
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("User-Agent", "AICodingAgent/1.0")
                .timeout(TIMEOUT)
                .GET()
                .build();

            HttpResponse<String> response = http.send(request,
                HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                return ToolResult.fail(getName(),
                    "HTTP %d: %s".formatted(response.statusCode(), response.body()));
            }

            String body = response.body();
            boolean truncated = body.length() > MAX_RESPONSE_CHARS;
            if (truncated) {
                body = body.substring(0, MAX_RESPONSE_CHARS) + "\n[...TRUNCATED]";
            }

            return ToolResult.ok(getName(), body,
                Map.of("url", url, "status", response.statusCode(), "truncated", truncated));

        } catch (Exception e) {
            return ToolResult.fail(getName(), "Failed to fetch URL: " + e.getMessage());
        }
    }
}
