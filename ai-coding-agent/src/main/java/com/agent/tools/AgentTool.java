package com.agent.tools;

import com.agent.model.AgentContext;
import com.agent.model.ToolResult;

import java.util.Map;

/**
 * Contrato base para todas las tools del agente.
 *
 * Implementar esta interfaz es suficiente para registrar
 * una nueva capacidad. El ToolRegistry la detecta automáticamente.
 */
public interface AgentTool {

    /** Nombre único de la tool. El LLM lo usa para invocarla. */
    String getName();

    /** Descripción clara de qué hace. Aparece en el prompt del sistema. */
    String getDescription();

    /**
     * Parámetros que acepta la tool.
     * Key: nombre del parámetro
     * Value: descripción del parámetro
     */
    Map<String, String> getParameterDescriptions();

    /**
     * Ejecuta la tool con los parámetros dados.
     *
     * @param params  mapa de parámetros parseados del LLM
     * @param context contexto de la sesión actual
     * @return resultado de la ejecución
     */
    ToolResult execute(Map<String, String> params, AgentContext context);

    /**
     * Genera la descripción de la tool en formato prompt para el LLM.
     */
    default String toPromptDescription() {
        StringBuilder sb = new StringBuilder();
        sb.append("- **").append(getName()).append("**: ").append(getDescription()).append("\n");
        sb.append("  Parameters:\n");
        getParameterDescriptions().forEach((k, v) ->
            sb.append("    - `").append(k).append("`: ").append(v).append("\n")
        );
        return sb.toString();
    }
}
