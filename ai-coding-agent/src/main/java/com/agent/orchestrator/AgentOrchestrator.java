package com.agent.orchestrator;

import com.agent.ai.ModelRouter;
import com.agent.ai.PromptManager;
import com.agent.memory.ShortTermMemory;
import com.agent.model.AgentContext;
import com.agent.model.ToolResult;
import com.agent.tools.ToolRegistry;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Cerebro del agente. Implementa el ciclo ReAct:
 *
 *   1. REASON  — el LLM decide qué hacer
 *   2. ACT     — ejecuta una tool
 *   3. OBSERVE — añade el resultado al contexto
 *   4. REPEAT  — hasta que el LLM diga TASK_COMPLETE o se agoten los pasos
 */
public class AgentOrchestrator {

    private static final Logger log = LoggerFactory.getLogger(AgentOrchestrator.class);

    // Máximo de pasos por tarea para evitar loops infinitos
    private static final int MAX_STEPS = 20;

    private static final Pattern TOOL_BLOCK = Pattern.compile(
        "```tool\\s*\\n(.*?)\\n```", Pattern.DOTALL);

    private final ModelRouter modelRouter;
    private final ToolRegistry toolRegistry;
    private final PromptManager promptManager;
    private final ObjectMapper mapper;

    // Callback para mostrar output al usuario en tiempo real
    private Consumer<String> outputHandler = System.out::println;

    public AgentOrchestrator(
            ModelRouter modelRouter,
            ToolRegistry toolRegistry,
            PromptManager promptManager) {
        this.modelRouter = modelRouter;
        this.toolRegistry = toolRegistry;
        this.promptManager = promptManager;
        this.mapper = new ObjectMapper();
    }

    public void setOutputHandler(Consumer<String> handler) {
        this.outputHandler = handler;
    }

    /**
     * Ejecuta una tarea completa. Bloquea hasta completarla o alcanzar MAX_STEPS.
     *
     * @param task    descripción de la tarea en lenguaje natural
     * @param context contexto de sesión
     * @return resumen final de la ejecución
     */
    public String run(String task, AgentContext context) {
        log.info("Starting task: {}", task);

        ShortTermMemory memory = new ShortTermMemory();
        String systemPrompt = promptManager.buildSystemPrompt();

        // Mensaje inicial del usuario
        memory.addUserMessage(task);
        output("\n Agente iniciado. Tarea: " + task);
        output("─".repeat(60));

        for (int step = 1; step <= MAX_STEPS; step++) {
            if (context.isAborted()) {
                output("\n  Tarea abortada por el usuario.");
                return "Task aborted.";
            }

            context.incrementStep();
            log.debug("Step {}/{}", step, MAX_STEPS);

            // ── REASON: llamar al LLM ──────────────────────────────
            String prompt = buildPrompt(memory);
            String llmResponse;

            try {
                output("\n Pensando...");
                llmResponse = modelRouter.complete(systemPrompt, prompt);
            } catch (Exception e) {
                output(" Error llamando al modelo: " + e.getMessage());
                return "Error: " + e.getMessage();
            }

            memory.addAssistantMessage(llmResponse);

            // ── Verificar si la tarea está completa ────────────────
            if (llmResponse.contains("TASK_COMPLETE")) {
                String finalResponse = llmResponse.replace("TASK_COMPLETE", "").trim();
                output("\n Tarea completada en " + step + " paso(s).");
                output("\n" + finalResponse);
                return finalResponse;
            }

            // ── ACT: buscar y ejecutar tool call ───────────────────
            Matcher toolMatcher = TOOL_BLOCK.matcher(llmResponse);

            if (toolMatcher.find()) {
                String jsonBlock = toolMatcher.group(1).trim();

                try {
                    ToolCall toolCall = parseToolCall(jsonBlock);
                    output("\n Tool: " + toolCall.tool());
                    if (toolCall.reasoning() != null) {
                        output("   Razón: " + toolCall.reasoning());
                    }

                    // ── OBSERVE ──────────────────────────────────
                    ToolResult result = toolRegistry.execute(
                        toolCall.tool(), toolCall.params(), context);

                    if (result.success()) {
                        output("   ✓ " + summarizeOutput(result.output()));
                    } else {
                        output("   ✗ Error: " + result.error());
                    }

                    memory.addToolResult(toolCall.tool(), result.toPromptString());

                } catch (Exception e) {
                    log.error("Failed to parse or execute tool call: {}", e.getMessage());
                    String errorMsg = "Failed to parse tool call: " + e.getMessage();
                    memory.addToolResult("parse_error", errorMsg);
                    output("   ✗ Error parseando tool: " + e.getMessage());
                }

            } else {
                // El LLM respondió texto sin tool call — mostrar y continuar
                output("\n" + llmResponse);

                // Si parece una respuesta final (sin más tools), terminar
                if (looksLikeFinalResponse(llmResponse)) {
                    output("\n Tarea completada en " + step + " paso(s).");
                    return llmResponse;
                }
            }
        }

        output("\n  Se alcanzó el límite de " + MAX_STEPS + " pasos.");
        return "Task reached maximum steps limit. Last state: " +
            memory.getLastAssistantMessage();
    }

    // ── Helpers ────────────────────────────────────────────────────────────

    private String buildPrompt(ShortTermMemory memory) {
        return memory.toPromptString();
    }

    private ToolCall parseToolCall(String json) throws Exception {
        var node = mapper.readTree(json);
        String tool = node.get("tool").asText();
        String reasoning = node.has("reasoning") ? node.get("reasoning").asText() : null;

        Map<String, String> params = new java.util.HashMap<>();
        if (node.has("params")) {
            node.get("params").fields().forEachRemaining(entry ->
                params.put(entry.getKey(), entry.getValue().asText()));
        }
        return new ToolCall(tool, params, reasoning);
    }

    private String summarizeOutput(String output) {
        if (output == null) return "(no output)";
        String firstLine = output.lines().findFirst().orElse("").trim();
        return firstLine.length() > 100 ? firstLine.substring(0, 97) + "..." : firstLine;
    }

    private boolean looksLikeFinalResponse(String text) {
        // Heurística: si no menciona tools ni pasos siguientes, es respuesta final
        return !text.contains("I will") && !text.contains("Next,") &&
               !text.contains("Step ") && text.length() > 50;
    }

    private void output(String message) {
        outputHandler.accept(message);
    }

    // Record interno para representar una llamada a tool
    private record ToolCall(String tool, Map<String, String> params, String reasoning) {}
}
