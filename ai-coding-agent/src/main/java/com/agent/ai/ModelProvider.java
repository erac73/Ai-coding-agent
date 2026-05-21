package com.agent.ai;

/**
 * Contrato para todos los proveedores de LLM.
 * El ModelRouter usa esta interfaz para abstraer OpenAI, Anthropic, Ollama, etc.
 */
public interface ModelProvider {

    String getName();

    /**
     * Envía un prompt al modelo y devuelve la respuesta completa como String.
     *
     * @param systemPrompt instrucciones del sistema
     * @param userPrompt   mensaje del usuario / historial actual
     * @return texto de la respuesta del modelo
     */
    String complete(String systemPrompt, String userPrompt);

    /** Indica si este proveedor está disponible (API key configurada, servicio up, etc.) */
    boolean isAvailable();
}
