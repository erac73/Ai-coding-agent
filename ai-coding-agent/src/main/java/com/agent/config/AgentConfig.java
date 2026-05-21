package com.agent.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Optional;

/**
 * Configuración del agente.
 *
 * Prioridad:
 *   1. Variables de entorno (ANTHROPIC_API_KEY, etc.)
 *   2. Archivo ~/.agent/config.json
 *   3. Valores por defecto
 */
public class AgentConfig {

    private static final Logger log = LoggerFactory.getLogger(AgentConfig.class);
    private static final Path CONFIG_PATH = Path.of(
        System.getProperty("user.home"), ".agent", "config.json");

    private final Map<String, Object> config;

    @SuppressWarnings("unchecked")
    public AgentConfig() {
        Map<String, Object> loaded = Map.of();
        if (Files.exists(CONFIG_PATH)) {
            try {
                loaded = new ObjectMapper().readValue(CONFIG_PATH.toFile(), Map.class);
                log.debug("Loaded config from {}", CONFIG_PATH);
            } catch (IOException e) {
                log.warn("Failed to load config file: {}", e.getMessage());
            }
        }
        this.config = loaded;
    }

    public Optional<String> getAnthropicApiKey() {
        return getEnvOrConfig("ANTHROPIC_API_KEY", "anthropicApiKey");
    }

    public Optional<String> getOpenAiApiKey() {
        return getEnvOrConfig("OPENAI_API_KEY", "openAiApiKey");
    }

    public String getAnthropicModel() {
        return getEnvOrConfig("ANTHROPIC_MODEL", "anthropicModel")
            .orElse("claude-sonnet-4-20250514");
    }

    public String getOllamaBaseUrl() {
        return getEnvOrConfig("OLLAMA_BASE_URL", "ollamaBaseUrl")
            .orElse("http://localhost:11434");
    }

    public boolean isStrictSecurity() {
        String env = System.getenv("AGENT_STRICT_SECURITY");
        if (env != null) return Boolean.parseBoolean(env);
        return Boolean.parseBoolean(config.getOrDefault("strictSecurity", "false").toString());
    }

    /** Crea el archivo de configuración de ejemplo. */
    public static void createDefaultConfig() throws IOException {
        Files.createDirectories(CONFIG_PATH.getParent());
        var example = """
            {
              "anthropicApiKey": "your-key-here",
              "openAiApiKey": "your-key-here",
              "anthropicModel": "claude-sonnet-4-20250514",
              "ollamaBaseUrl": "http://localhost:11434",
              "strictSecurity": false
            }
            """;
        Files.writeString(CONFIG_PATH, example);
        System.out.println("Config created at: " + CONFIG_PATH);
    }

    private Optional<String> getEnvOrConfig(String envVar, String configKey) {
        String env = System.getenv(envVar);
        if (env != null && !env.isBlank()) return Optional.of(env);
        Object val = config.get(configKey);
        if (val != null && !val.toString().isBlank()) return Optional.of(val.toString());
        return Optional.empty();
    }
}
