package com.agent.tools;

import com.agent.model.AgentContext;
import com.agent.model.ToolResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Registro central de todas las tools disponibles.
 *
 * El OrchestratOR consulta este registro para:
 * 1. Generar el prompt del sistema (lista de tools disponibles)
 * 2. Ejecutar una tool por nombre cuando el LLM la invoca
 */
public class ToolRegistry {

    private static final Logger log = LoggerFactory.getLogger(ToolRegistry.class);
    private final Map<String, AgentTool> tools = new LinkedHashMap<>();

    public ToolRegistry() {}

    /** Registra una tool. Falla si ya existe con ese nombre. */
    public ToolRegistry register(AgentTool tool) {
        if (tools.containsKey(tool.getName())) {
            throw new IllegalArgumentException("Tool already registered: " + tool.getName());
        }
        tools.put(tool.getName(), tool);
        log.debug("Registered tool: {}", tool.getName());
        return this;
    }

    /** Ejecuta una tool por nombre con los parámetros dados. */
    public ToolResult execute(String toolName, Map<String, String> params, AgentContext context) {
        AgentTool tool = tools.get(toolName);
        if (tool == null) {
            return ToolResult.fail(toolName, "Unknown tool: '%s'. Available: %s"
                .formatted(toolName, String.join(", ", tools.keySet())));
        }
        log.info("Executing tool: {} | params: {}", toolName, params);
        try {
            return tool.execute(params, context);
        } catch (Exception e) {
            log.error("Tool '{}' threw exception: {}", toolName, e.getMessage(), e);
            return ToolResult.fail(toolName, "Unexpected error: " + e.getMessage());
        }
    }

    /** Genera el bloque de tools para el system prompt. */
    public String toSystemPromptBlock() {
        return "## Available Tools\n\n" +
            tools.values().stream()
                .map(AgentTool::toPromptDescription)
                .collect(Collectors.joining("\n"));
    }

    public Set<String> getToolNames()          { return Collections.unmodifiableSet(tools.keySet()); }
    public Collection<AgentTool> getAllTools()  { return Collections.unmodifiableCollection(tools.values()); }
    public boolean hasTool(String name)        { return tools.containsKey(name); }
    public int size()                          { return tools.size(); }
}
