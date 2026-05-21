package com.agent.ai;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * Enruta las peticiones al proveedor más adecuado.
 *
 * Estrategia actual: intenta el proveedor primario,
 * si falla hace fallback a los secundarios en orden.
 *
 * (coding → Claude, reasoning → o3, local → Ollama).
 */
public class ModelRouter {

    private static final Logger log = LoggerFactory.getLogger(ModelRouter.class);

    private final List<ModelProvider> providers = new ArrayList<>();

    public ModelRouter addProvider(ModelProvider provider) {
        providers.add(provider);
        return this;
    }

    /**
     * Llama al modelo. Intenta los proveedores en orden hasta que uno responda.
     */
    public String complete(String systemPrompt, String userPrompt) {
        if (providers.isEmpty()) {
            throw new IllegalStateException("No model providers configured. " +
                "Set ANTHROPIC_API_KEY or OPENAI_API_KEY environment variable.");
        }

        List<Exception> errors = new ArrayList<>();

        for (ModelProvider provider : providers) {
            if (!provider.isAvailable()) {
                log.debug("Provider {} not available, skipping", provider.getName());
                continue;
            }
            try {
                log.debug("Using provider: {}", provider.getName());
                return provider.complete(systemPrompt, userPrompt);
            } catch (Exception e) {
                log.warn("Provider {} failed: {}. Trying next...", provider.getName(), e.getMessage());
                errors.add(e);
            }
        }

        throw new RuntimeException("All providers failed:\n" +
            errors.stream()
                .map(e -> "  - " + e.getMessage())
                .reduce("", (a, b) -> a + "\n" + b));
    }

    public boolean hasAvailableProvider() {
        return providers.stream().anyMatch(ModelProvider::isAvailable);
    }

    public List<ModelProvider> getProviders() {
        return List.copyOf(providers);
    }
}
